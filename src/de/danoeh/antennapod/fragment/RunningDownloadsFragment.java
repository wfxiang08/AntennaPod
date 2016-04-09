package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.View;
import de.danoeh.antennapod.adapter.DownloadlistAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DownloadRequester;

import java.util.List;

/**
 * Displays all running downloads and provides actions to cancel them
 */
public class RunningDownloadsFragment extends ListFragment {
    private static final String TAG = "RunningDownloadsFragment";

    private DownloadObserver downloadObserver;
    private List<Downloader> downloaderList;


    @Override
    public void onDetach() {
        super.onDetach();
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final DownloadlistAdapter downloadlistAdapter = new DownloadlistAdapter(getActivity(), itemAccess);
        setListAdapter(downloadlistAdapter);

        downloadObserver = new DownloadObserver(getActivity(), new Handler(), new DownloadObserver.Callback() {
            @Override
            public void onContentChanged() {
                downloadlistAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDownloadDataAvailable(List<Downloader> downloaderList) {
                RunningDownloadsFragment.this.downloaderList = downloaderList;
                downloadlistAdapter.notifyDataSetChanged();
            }
        });
        downloadObserver.onResume();
    }

    private DownloadlistAdapter.ItemAccess itemAccess = new DownloadlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (downloaderList != null) ? downloaderList.size() : 0;
        }

        @Override
        public Downloader getItem(int position) {
            return (downloaderList != null) ? downloaderList.get(position) : null;
        }

        @Override
        public void onSecondaryActionClick(Downloader downloader) {
            DownloadRequester.getInstance().cancelDownload(getActivity(), downloader.getDownloadRequest().getSource());
        }
    };
}
