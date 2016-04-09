package de.danoeh.antennapod.util.menuhandler;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import de.danoeh.antennapod.R;

/**
 * Utilities for menu items
 */
public class MenuItemUtils {

    public static MenuItem addSearchItem(Menu menu, SearchView searchView) {
        MenuItem item = menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, searchView);
        return item;
    }
}
