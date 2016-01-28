package org.wikibase.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the Wikibase entity (item) with the list of labels, descriptions, claims, sitelinks.
 * 
 * @author acstroe
 *
 */
public class Entity {

    public Entity(String id) {
        super();
        this.id = id;
    }

    private Map<String, String> labels = new HashMap<String, String>();
    private Map<String, String> descriptions = new HashMap<String, String>();
    private String id;
    private Map<Property, Set<Claim>> claims = new HashMap<Property, Set<Claim>>();
    private Map<String, Sitelink> sitelinks = new HashMap<String, Sitelink>();
    private boolean loaded;

    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    public Map<String, String> getDescriptions() {
        return Collections.unmodifiableMap(descriptions);
    }

    public void addLabel(String key, String value) {
        labels.put(key, value);
    }

    public void addDescription(String key, String value) {
        descriptions.put(key, value);
    }

    public String getId() {
        return id;
    }

    public Map<Property, Set<Claim>> getClaims() {
        return Collections.unmodifiableMap(claims);
    }

    public void addClaim(Property prop, Claim claim) {
        if (null == claim) {
            return;
        }
        Set<Claim> propClaims = claims.get(prop);
        if (null == propClaims) {
            propClaims = new HashSet<Claim>();
            claims.put(prop, propClaims);
        }
        propClaims.add(claim);
    }

    public Map<String, Sitelink> getSitelinks() {
        return Collections.unmodifiableMap(sitelinks);
    }

    public void addSitelink(Sitelink link) {
        sitelinks.put(link.getSite(), link);
    }

    public void setLoaded(boolean b) {
        loaded = b;
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String toString() {
        return id + ": " + (0 == labels.size() ? "not loaded" : (labels.get("en") + " (" + descriptions.get("en") + ")"));
    }
}
