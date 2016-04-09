package de.danoeh.antennapod.adapter.gpodnet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageDiskCache;
import de.danoeh.antennapod.gpoddernet.model.GpodnetPodcast;

import java.util.List;

/**
 * Adapter for displaying a list of GPodnetPodcast-Objects.
 */
public class PodcastListAdapter extends ArrayAdapter<GpodnetPodcast> {
    private final ImageDiskCache diskCache;
    private final int thumbnailLength;

    public PodcastListAdapter(Context context, int resource, List<GpodnetPodcast> objects) {
        super(context, resource, objects);
        diskCache = ImageDiskCache.getDefaultInstance();
        thumbnailLength = (int) context.getResources().getDimension(R.dimen.thumbnail_length);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        GpodnetPodcast podcast = getItem(position);

        // Inflate Layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.gpodnet_podcast_listitem, null);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.description = (TextView) convertView.findViewById(R.id.txtvDescription);
            holder.image = (ImageView) convertView.findViewById(R.id.imgvCover);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.title.setText(podcast.getTitle());
        holder.description.setText(podcast.getDescription());
        diskCache.loadThumbnailBitmap(podcast.getLogoUrl(), holder.image, thumbnailLength);

        return convertView;
    }

    static class Holder {
        TextView title;
        TextView description;
        ImageView image;
    }
}
