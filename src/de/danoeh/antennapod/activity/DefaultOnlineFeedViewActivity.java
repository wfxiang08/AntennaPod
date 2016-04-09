package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.FeedItemlistDescriptionAdapter;
import de.danoeh.antennapod.asynctask.ImageDiskCache;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by daniel on 24.08.13.
 */
public class DefaultOnlineFeedViewActivity extends OnlineFeedViewActivity {

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED | EventDistributor.DOWNLOAD_QUEUED | EventDistributor.FEED_LIST_UPDATE;
    private volatile List<Feed> feeds;
    private Feed feed;
    private String selectedDownloadUrl;

    private Button subscribeButton;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent destIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, destIntent)) {
                    startActivity(destIntent);
                } else {
                    NavUtils.navigateUpFromSameTask(this);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void loadData() {
        super.loadData();
        feeds = DBReader.getFeedList(this);
    }

    @Override
    protected void showFeedInformation(final Feed feed, final Map<String, String> alternateFeedUrls) {
        super.showFeedInformation(feed, alternateFeedUrls);
        setContentView(R.layout.listview_activity);

        this.feed = feed;
        this.selectedDownloadUrl = feed.getDownload_url();
        EventDistributor.getInstance().register(listener);
        ListView listView = (ListView) findViewById(R.id.listview);
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.onlinefeedview_header, null);
        listView.addHeaderView(header);

        listView.setAdapter(new FeedItemlistDescriptionAdapter(this, 0, feed.getItems()));

        ImageView cover = (ImageView) header.findViewById(R.id.imgvCover);
        TextView title = (TextView) header.findViewById(R.id.txtvTitle);
        TextView author = (TextView) header.findViewById(R.id.txtvAuthor);
        TextView description = (TextView) header.findViewById(R.id.txtvDescription);
        Spinner spAlternateUrls = (Spinner) header.findViewById(R.id.spinnerAlternateUrls);

        subscribeButton = (Button) header.findViewById(R.id.butSubscribe);

        if (feed.getImage() != null) {
            ImageDiskCache.getDefaultInstance().loadThumbnailBitmap(feed.getImage().getDownload_url(), cover, (int) getResources().getDimension(
                    R.dimen.thumbnail_length));
        }
        title.setText(feed.getTitle());
        author.setText(feed.getAuthor());
        description.setText(feed.getDescription());

        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Feed f = new Feed(selectedDownloadUrl, new Date(), feed.getTitle());
                    f.setPreferences(feed.getPreferences());
                    DefaultOnlineFeedViewActivity.this.feed = f;

                    DownloadRequester.getInstance().downloadFeed(
                            DefaultOnlineFeedViewActivity.this,
                            f);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                    DownloadRequestErrorDialogCreator.newRequestErrorDialog(DefaultOnlineFeedViewActivity.this,
                            e.getMessage());
                }
                setSubscribeButtonState(feed);
            }
        });

        if (alternateFeedUrls.isEmpty()) {
            spAlternateUrls.setVisibility(View.GONE);
        } else {
            spAlternateUrls.setVisibility(View.VISIBLE);

            final List<String> alternateUrlsList = new ArrayList<String>();
            final List<String> alternateUrlsTitleList = new ArrayList<String>();

            alternateUrlsList.add(feed.getDownload_url());
            alternateUrlsTitleList.add(feed.getTitle());


            alternateUrlsList.addAll(alternateFeedUrls.keySet());
            for (String url : alternateFeedUrls.keySet()) {
                alternateUrlsTitleList.add(alternateFeedUrls.get(url));
            }
            ArrayAdapter<String> adapter  = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, alternateUrlsTitleList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spAlternateUrls.setAdapter(adapter);
            spAlternateUrls.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedDownloadUrl = alternateUrlsList.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


        }
        setSubscribeButtonState(feed);

    }

    private boolean feedInFeedlist(Feed feed) {
        if (feeds == null || feed == null)
            return false;
        for (Feed f : feeds) {
            if (f.getIdentifyingValue().equals(feed.getIdentifyingValue())) {
                return true;
            }
        }
        return false;
    }

    private void setSubscribeButtonState(Feed feed) {
        if (subscribeButton != null && feed != null) {
            if (DownloadRequester.getInstance().isDownloadingFile(feed.getDownload_url())) {
                subscribeButton.setEnabled(false);
                subscribeButton.setText(R.string.downloading_label);
            } else if (feedInFeedlist(feed)) {
                subscribeButton.setEnabled(false);
                subscribeButton.setText(R.string.subscribed_label);
            } else {
                subscribeButton.setEnabled(true);
                subscribeButton.setText(R.string.subscribe_label);
            }
        }
    }

    EventDistributor.EventListener listener = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EventDistributor.FEED_LIST_UPDATE) != 0) {
                new AsyncTask<Void, Void, List<Feed>>() {
                    @Override
                    protected List<Feed> doInBackground(Void... params) {
                        return DBReader.getFeedList(DefaultOnlineFeedViewActivity.this);
                    }

                    @Override
                    protected void onPostExecute(List<Feed> feeds) {
                        super.onPostExecute(feeds);
                        DefaultOnlineFeedViewActivity.this.feeds = feeds;
                        setSubscribeButtonState(feed);
                    }
                }.execute();
            } else if ((arg & EVENTS) != 0) {
                setSubscribeButtonState(feed);
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(listener);
    }
}

