package org.wikibase.data;

import java.net.URL;

public class URLData extends WikibaseDataType {
    private URL url;

    public URLData(URL url) {
        super();
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "URLData [url=" + url + "]";
    }

}
