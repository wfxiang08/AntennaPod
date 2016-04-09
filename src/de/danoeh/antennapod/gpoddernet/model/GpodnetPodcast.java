package de.danoeh.antennapod.gpoddernet.model;

public class GpodnetPodcast {
    private String url;
    private String title;
    private String description;
    private int subscribers;
    private String logoUrl;
    private String website;
    private String mygpoLink;

    public GpodnetPodcast(String url, String title, String description,
                          int subscribers, String logoUrl, String website, String mygpoLink) {
        if (url == null || title == null || description == null) {
            throw new IllegalArgumentException(
                    "URL, title and description must not be null");
        }

        this.url = url;
        this.title = title;
        this.description = description;
        this.subscribers = subscribers;
        this.logoUrl = logoUrl;
        this.website = website;
        this.mygpoLink = mygpoLink;
    }

    @Override
    public String toString() {
        return "GpodnetPodcast [url=" + url + ", title=" + title
                + ", description=" + description + ", subscribers="
                + subscribers + ", logoUrl=" + logoUrl + ", website=" + website
                + ", mygpoLink=" + mygpoLink + "]";
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getSubscribers() {
        return subscribers;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getWebsite() {
        return website;
    }

    public String getMygpoLink() {
        return mygpoLink;
    }

}
