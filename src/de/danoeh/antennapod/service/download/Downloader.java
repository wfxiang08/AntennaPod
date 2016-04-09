// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod.service.download;

import android.content.Context;
import android.net.wifi.WifiManager;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;

import java.util.concurrent.Callable;

/** Downloads files */
public abstract class Downloader implements Callable<Downloader> {
	private static final String TAG = "Downloader";

	protected volatile boolean finished;

	protected volatile boolean cancelled;

	protected DownloadRequest request;
	protected DownloadStatus result;

	public Downloader(DownloadRequest request) {
		super();
		this.request = request;
		this.request.setStatusMsg(R.string.download_pending);
		this.cancelled = false;
        this.result = new DownloadStatus(request, null, false, false, null);
	}

	protected abstract void download();

	public final Downloader call() {
        WifiManager wifiManager = (WifiManager) PodcastApp.getAppContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock = null;
        if (wifiManager != null) {
            wifiLock = wifiManager.createWifiLock(TAG);
            wifiLock.acquire();
        }

		download();

        if (wifiLock != null) {
            wifiLock.release();
        }

		if (result == null) {
			throw new IllegalStateException(
					"Downloader hasn't created DownloadStatus object");
		}
        finished = true;
		return this;
	}

	public DownloadRequest getDownloadRequest() {
		return request;
	}

	public DownloadStatus getResult() {
		return result;
	}

	public boolean isFinished() {
		return finished;
	}

	public void cancel() {
		cancelled = true;
	}

}