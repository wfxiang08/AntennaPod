package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.SearchFragment;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.util.menuhandler.MenuItemUtils;

import java.util.List;

/**
 * Performs a search on the gpodder.net directory and displays the results.
 */
public class SearchListFragment extends PodcastListFragment {
    private static final String ARG_QUERY = "query";

    private String query;

    public static SearchListFragment newInstance(String query) {
        SearchListFragment fragment = new SearchListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_QUERY)) {
            this.query = getArguments().getString(ARG_QUERY);
        } else {
            this.query = "";
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final SearchView sv = new SearchView(getActivity());
        MenuItemUtils.addSearchItem(menu, sv);
        sv.setQueryHint(getString(R.string.gpodnet_search_hint));
        sv.setQuery(query, false);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                changeQuery(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    @Override
    protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
        return service.searchPodcasts(query, 0);
    }

    public void changeQuery(String query) {
        if (query == null) {
            throw new NullPointerException();
        }
        this.query = query;
        loadData();
    }
}
