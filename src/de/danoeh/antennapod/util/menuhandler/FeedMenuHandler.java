// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod.util.menuhandler;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.ShareUtils;

/** Handles interactions with the FeedItemMenu. */
public class FeedMenuHandler {
	private static final String TAG = "FeedMenuHandler";

	public static boolean onCreateOptionsMenu(MenuInflater inflater, Menu menu) {
		inflater.inflate(R.menu.feedlist, menu);
		return true;
	}

	public static boolean onPrepareOptionsMenu(Menu menu, Feed selectedFeed) {
        if (selectedFeed == null) {
            return true;
        }

		if (BuildConfig.DEBUG)
			Log.d(TAG, "Preparing options menu");
		menu.findItem(R.id.mark_all_read_item).setVisible(
				selectedFeed.hasNewItems(true));
		if (selectedFeed.getPaymentLink() != null && selectedFeed.getFlattrStatus().flattrable()) 
			menu.findItem(R.id.support_item).setVisible(true);
		else
			menu.findItem(R.id.support_item).setVisible(false);	
		MenuItem refresh = menu.findItem(R.id.refresh_item);
		if (DownloadService.isRunning
				&& DownloadRequester.getInstance().isDownloadingFile(
						selectedFeed)) {
			refresh.setVisible(false);
		} else {
			refresh.setVisible(true);
		}

		return true;
	}

	/**
	 * NOTE: This method does not handle clicks on the 'remove feed' - item.
	 * 
	 * @throws DownloadRequestException
	 */
	public static boolean onOptionsItemClicked(Context context, MenuItem item,
			Feed selectedFeed) throws DownloadRequestException {
        int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            DBTasks.refreshFeed(context, selectedFeed);

        } else if (itemId == R.id.mark_all_read_item) {
            DBWriter.markFeedRead(context, selectedFeed.getId());

        } else if (itemId == R.id.visit_website_item) {
            Uri uri = Uri.parse(selectedFeed.getLink());
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));

        } else if (itemId == R.id.support_item) {
            DBTasks.flattrFeedIfLoggedIn(context, selectedFeed);

        } else if (itemId == R.id.share_link_item) {
            ShareUtils.shareFeedlink(context, selectedFeed);

        } else if (itemId == R.id.share_source_item) {
            ShareUtils.shareFeedDownloadLink(context, selectedFeed);

        } else {
            return false;
        }
		return true;
	}
}
