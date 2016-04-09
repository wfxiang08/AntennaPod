// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;
import com.facebook.buck.android.support.exopackage.DefaultApplicationLike;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.spa.SPAUtil;

/** Main application class. */
public class PodcastApp extends DefaultApplicationLike {

	private static final String TAG = "PodcastApp";
	public static final String EXPORT_DIR = "export/";

	private static float LOGICAL_DENSITY;

	private static PodcastApp singleton;

    public static PodcastApp getInstance() {
		return singleton;
	}

    private final Application appContext;

    public static Application getAppContext() {
        return getInstance().appContext;
    }

    public PodcastApp(Application appContext) {
        this.appContext = appContext;
    }

	@Override
	public void onCreate() {
		singleton = this;
		LOGICAL_DENSITY = appContext.getResources().getDisplayMetrics().density;

		UserPreferences.createInstance(appContext);
		PlaybackPreferences.createInstance(appContext);
		EventDistributor.getInstance();

        SPAUtil.sendSPAppsQueryFeedsIntent(appContext);
	}

	@Override
	public void onLowMemory() {
		Log.w(TAG, "Received onLowOnMemory warning. Cleaning image cache...");
		ImageLoader.getInstance().wipeImageCache();
	}

    public static float getLogicalDensity() {
		return LOGICAL_DENSITY;
	}

	public boolean isLargeScreen() {
		return (appContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| (appContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

	}
}
