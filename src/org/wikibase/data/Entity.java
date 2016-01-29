/**
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU General Public License
*  as published by the Free Software Foundation; either version 3
*  of the License, or (at your option) any later version. Additionally
*  this file is subject to the "Classpath" exception.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software Foundation,
*  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
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
