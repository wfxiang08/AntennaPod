// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod;

import com.facebook.buck.android.support.exopackage.ExopackageApplication;

public class AppShell extends ExopackageApplication {
    public AppShell() {
        super("de.danoeh.antennapod.PodcastApp", BuildConfig.IS_EXOPACKAGE);
    }
}
