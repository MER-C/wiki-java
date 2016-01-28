package org.wikibase.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Sitelink {
    private String site;
    private String pageName;
    private Set<Entity> badges = new HashSet<Entity>();

    public Sitelink(String site, String pageName) {
        super();
        this.site = site;
        this.pageName = pageName;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public Set<Entity> getBadges() {
        return Collections.unmodifiableSet(badges);
    }

    public void addBadge(Entity entity) {
        badges.add(entity);
    }

    @Override
    public String toString() {
        return "Sitelink [site=" + site + ", pageName=" + pageName + "]";
    }
}

