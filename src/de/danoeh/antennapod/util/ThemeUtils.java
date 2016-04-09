// The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.
package de.danoeh.antennapod.util;

import android.util.Log;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.preferences.UserPreferences;

public class ThemeUtils {
	private static final String TAG = "ThemeUtils";

	public static int getSelectionBackgroundColor() {
        int itemId = UserPreferences.getTheme();
        if (itemId == R.style.Theme_AntennaPod_Dark) {
            return R.color.selection_background_color_dark;
        } else if (itemId == R.style.Theme_AntennaPod_Light) {
            return R.color.selection_background_color_light;
        } else {
            Log.e(
                    TAG,
                    "getSelectionBackgroundColor could not match the current theme to any color!");
            return R.color.selection_background_color_light;
        }
	}
}
