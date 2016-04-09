// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.mobeta.android.dslv.DragSortListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.NewEpisodesListAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.dialog.FeedItemDialog;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.menuhandler.MenuItemUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shows unread or recently published episodes
 */
public class NewEpisodesFragment extends Fragment {
    private static final String TAG = "NewEpisodesFragment";
    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOAD_QUEUED |
            EventDistributor.QUEUE_UPDATE |
            EventDistributor.UNREAD_ITEMS_UPDATE;

    private static final int RECENT_EPISODES_LIMIT = 150;
    private static final String PREF_NAME = "PrefNewEpisodesFragment";
    private static final String PREF_EPISODE_FILTER_BOOL = "newEpisodeFilterEnabled";


    private DragSortListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NewEpisodesListAdapter listAdapter;
    private TextView txtvEmpty;
    private ProgressBar progLoading;

    private List<FeedItem> unreadItems;
    private List<FeedItem> recentItems;
    private QueueAccess queueAccess;
    private List<Downloader> downloaderList;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;
    private boolean showOnlyNewEpisodes = false;

    private AtomicReference<MainActivity> activity = new AtomicReference<MainActivity>();

    private DownloadObserver downloadObserver = null;

    private FeedItemDialog feedItemDialog;
    private FeedItemDialog.FeedItemDialogSavedInstance feedItemDialogSavedInstance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateShowOnlyEpisodes();
    }

    @Override
    public void onResume() {
        super.onResume();
        startItemLoader();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
        this.activity.set((MainActivity) getActivity());
        if (downloadObserver != null) {
            downloadObserver.setActivity(getActivity());
            downloadObserver.onResume();
        }
        if (viewsCreated && itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        stopItemLoader();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity.set((MainActivity) getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetViewState();
    }

    private void resetViewState() {
        listAdapter = null;
        activity.set(null);
        viewsCreated = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
        if (feedItemDialog != null) {
            feedItemDialogSavedInstance = feedItemDialog.save();
        }
        feedItemDialog = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.new_episodes, menu);

        final SearchView sv = new SearchView(getActivity());
        MenuItemUtils.addSearchItem(menu, sv);
        sv.setQueryHint(getString(R.string.search_hint));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance(s));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.mark_all_read_item).setVisible(unreadItems != null && !unreadItems.isEmpty());
        menu.findItem(R.id.episode_filter_item).setChecked(showOnlyNewEpisodes);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            int itemId = item.getItemId();
            if (itemId == R.id.refresh_item) {
                List<Feed> feeds = ((MainActivity) getActivity()).getFeeds();
                if (feeds != null) {
                    DBTasks.refreshAllFeeds(getActivity(), feeds);
                }
                return true;
            } else if (itemId == R.id.mark_all_read_item) {
                DBWriter.markAllItemsRead(getActivity());
                Toast.makeText(
                        getActivity(),
                        R.string.mark_all_read_msg,
                        Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.episode_filter_item) {
                boolean newVal = !item.isChecked();
                setShowOnlyNewEpisodes(newVal);
                item.setChecked(newVal);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.all_episodes_label);

        View root = inflater.inflate(R.layout.new_episodes_fragment, container, false);

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        listView = (DragSortListView) root.findViewById(android.R.id.list);
        txtvEmpty = (TextView) root.findViewById(android.R.id.empty);
        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FeedItem item = (FeedItem) listAdapter.getItem(position - listView.getHeaderViewsCount());
                if (item != null) {
                    feedItemDialog = FeedItemDialog.newInstance(activity.get(), item, queueAccess);
                    feedItemDialog.show();
                }

            }
        });

        final int secondColor = (UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) ? R.color.swipe_refresh_secondary_color_dark : R.color.swipe_refresh_secondary_color_light;
        swipeRefreshLayout.setColorScheme(R.color.bright_blue, secondColor, R.color.bright_blue, secondColor);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                List<Feed> feeds = ((MainActivity) getActivity()).getFeeds();
                if (feeds != null) {
                    DBTasks.refreshAllFeeds(getActivity(), feeds);
                }
            }
        });

        if (!itemsLoaded) {
            progLoading.setVisibility(View.VISIBLE);
            txtvEmpty.setVisibility(View.GONE);
        }

        viewsCreated = true;

        if (itemsLoaded && activity.get() != null) {
            onFragmentLoaded();
        }

        return root;
    }

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            listAdapter = new NewEpisodesListAdapter(activity.get(), itemAccess, new DefaultActionButtonCallback(activity.get()));
            listView.setAdapter(listAdapter);
            listView.setEmptyView(txtvEmpty);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        if (feedItemDialog != null) {
            feedItemDialog.updateContent(queueAccess, unreadItems, recentItems);
        } else if (feedItemDialogSavedInstance != null) {
            feedItemDialog = FeedItemDialog.newInstance(activity.get(), feedItemDialogSavedInstance);
        }
        listAdapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
        updateProgressBarVisibility();
        updateShowOnlyEpisodesListViewState();
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged() {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (feedItemDialog != null && feedItemDialog.isShowing()) {
                feedItemDialog.updateMenuAppearance();
            }
        }

        @Override
        public void onDownloadDataAvailable(List<Downloader> downloaderList) {
            NewEpisodesFragment.this.downloaderList = downloaderList;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    };

    private NewEpisodesListAdapter.ItemAccess itemAccess = new NewEpisodesListAdapter.ItemAccess() {

        @Override
        public int getCount() {
            if (itemsLoaded) {
                return (showOnlyNewEpisodes) ? unreadItems.size() : recentItems.size();
            }
            return 0;
        }

        @Override
        public FeedItem getItem(int position) {
            if (itemsLoaded) {
                return (showOnlyNewEpisodes) ? unreadItems.get(position) : recentItems.get(position);
            }
            return null;
        }

        @Override
        public int getItemDownloadProgressPercent(FeedItem item) {
            if (downloaderList != null) {
                for (Downloader downloader : downloaderList) {
                    if (downloader.getDownloadRequest().getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                            && downloader.getDownloadRequest().getFeedfileId() == item.getMedia().getId()) {
                        return downloader.getDownloadRequest().getProgressPercent();
                    }
                }
            }
            return 0;
        }

        @Override
        public boolean isInQueue(FeedItem item) {
            if (itemsLoaded) {
                return queueAccess.contains(item.getId());
            } else {
                return false;
            }
        }


    };

    private void updateProgressBarVisibility() {
        if (!viewsCreated) {
            return;
        }

        if (DownloadService.isRunning
                && DownloadRequester.getInstance().isDownloadingFeeds()) {
            swipeRefreshLayout.setRefreshing(true);

        } else {
            swipeRefreshLayout.setRefreshing(false);

            // if case other fragments have set this to true, this fragment should remove the progress indicator
            ((ActionBarActivity) getActivity())
                    .setSupportProgressBarIndeterminateVisibility(false);
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                startItemLoader();
                updateProgressBarVisibility();
            }
        }
    };

    private void updateShowOnlyEpisodes() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        showOnlyNewEpisodes = prefs.getBoolean(PREF_EPISODE_FILTER_BOOL, false);
    }

    private void setShowOnlyNewEpisodes(boolean newVal) {
        showOnlyNewEpisodes = newVal;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_EPISODE_FILTER_BOOL, showOnlyNewEpisodes);
        editor.commit();
        if (itemsLoaded && viewsCreated) {
            listAdapter.notifyDataSetChanged();
            activity.get().supportInvalidateOptionsMenu();
            updateShowOnlyEpisodesListViewState();
        }
    }

    private void updateShowOnlyEpisodesListViewState() {
        if (showOnlyNewEpisodes) {
            listView.setEmptyView(null);
            txtvEmpty.setVisibility(View.GONE);
        } else {
            listView.setEmptyView(txtvEmpty);
        }
    }

    private ItemLoader itemLoader;

    private void startItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
        itemLoader = new ItemLoader();
        itemLoader.execute();
    }

    private void stopItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
    }

    private class ItemLoader extends AsyncTask<Void, Void, Object[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (viewsCreated && !itemsLoaded) {
                listView.setVisibility(View.GONE);
                txtvEmpty.setVisibility(View.GONE);
                progLoading.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Object[] doInBackground(Void... params) {
            Context context = activity.get();
            if (context != null) {
                return new Object[]{DBReader.getUnreadItemsList(context),
                        DBReader.getRecentlyPublishedEpisodes(context, RECENT_EPISODES_LIMIT),
                        QueueAccess.IDListAccess(DBReader.getQueueIDList(context))};
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object[] lists) {
            super.onPostExecute(lists);
            listView.setVisibility(View.VISIBLE);
            progLoading.setVisibility(View.GONE);

            if (lists != null) {
                unreadItems = (List<FeedItem>) lists[0];
                recentItems = (List<FeedItem>) lists[1];
                queueAccess = (QueueAccess) lists[2];
                itemsLoaded = true;
                if (viewsCreated && activity.get() != null) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
