// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.activity.StorageErrorActivity;
import de.danoeh.antennapod.preferences.UserPreferences;

import java.io.File;

/** Utility functions for handling storage errors */
public class StorageUtils {
	private static final String TAG = "StorageUtils";

	public static boolean storageAvailable(Context context) {
		File dir = UserPreferences.getDataFolder(context, null);
		if (dir != null) {
			return dir.exists() && dir.canRead() && dir.canWrite();
		} else {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Storage not available: data folder is null");
			return false;
		}
	}

	/**
	 * Checks if external storage is available. If external storage isn't
	 * available, the current activity is finsished an an error activity is
	 * launched.
	 * 
	 * @param activity
	 *            the activity which would be finished if no storage is
	 *            available
	 * @return true if external storage is available
	 */
	public static boolean checkStorageAvailability(Activity activity) {
		boolean storageAvailable = storageAvailable(activity);
		if (!storageAvailable) {
			activity.finish();
			activity.startActivity(new Intent(activity,
					StorageErrorActivity.class));
		}
		return storageAvailable;
	}

	/** Get the number of free bytes that are available on the external storage. */
	public static long getFreeSpaceAvailable() {
		StatFs stat = new StatFs(UserPreferences.getDataFolder(
				PodcastApp.getAppContext(), null).getAbsolutePath());
        long availableBlocks;
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
		    availableBlocks = stat.getAvailableBlocksLong();
            blockSize = stat.getBlockSizeLong();
        } else {
            availableBlocks = stat.getAvailableBlocks();
            blockSize = stat.getBlockSize();
        }
		return availableBlocks * blockSize;
	}
}
