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
import java.util.Map.Entry;
import java.util.Set;

/**
 * A Wikidata claim.
 * 
 * @author acstroe
 *
 */
public class Claim {

    private String id;
    private String type;
    private Rank rank;
    private Property property;
    private Snak mainsnak;
    private Map<Property, Set<Snak>> qualifiers = new HashMap<Property, Set<Snak>>();

    public Claim() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Claim(Property property, WikibaseData value) {
        super();
        this.property = property;
        this.mainsnak = new Snak(value, property);
    }

    public WikibaseData getValue() {
        return null != mainsnak? mainsnak.getData() : null;
    }

    public void setValue(WikibaseData value) {
        if (mainsnak != null) {
            mainsnak.setData(value);
        } else {
            mainsnak = new Snak(value, this.property);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Claim other = (Claim) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Snak getMainsnak() {
        return mainsnak;
    }

    public void setMainsnak(Snak mainsnak) {
        this.mainsnak = mainsnak;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
    public Map<Property, Set<Snak>> getQualifiers() {
        return Collections.unmodifiableMap(qualifiers);
    }

    public void addQualifier(Property property, WikibaseData data) {
        Set<Snak> dataset = qualifiers.get(property);
        if (null == dataset) {
            dataset = new HashSet<Snak>();
        }
        dataset.add(new Snak(data, property));
        qualifiers.put(property, dataset);
    }

    @Override
    public String toString() {
        return "Claim [id=" + id + ", property=" + property + ", snak=" + mainsnak + "]";
    }

    public String toJSON() {
        StringBuilder sbuild = new StringBuilder("{");
        sbuild.append("\"mainsnak\":");


        sbuild.append(mainsnak.toJSON());
        if (!qualifiers.isEmpty()) {
            sbuild.append(',');
            sbuild.append("\"qualifiers\": {");
            for (Entry<Property, Set<Snak>> qualEntry : qualifiers.entrySet()) {
                String propId = qualEntry.getKey().getId().startsWith("P") ? qualEntry.getKey().getId()
                    : ("P" + qualEntry.getKey().getId());
                sbuild.append('\"').append(propId).append("\":");
                if (!qualEntry.getValue().isEmpty()) {
                    boolean started = false;
                    sbuild.append('[');
                    for (Snak eachSnak : qualEntry.getValue()) {
                        if (started) {
                            sbuild.append(',');
                        }
                        sbuild.append(eachSnak.toJSON());
                        started = true;
                    }
                    sbuild.append(']');
                }
            }
            sbuild.append('}');
        }

        sbuild.append(',');
        sbuild.append("\"type\":\"").append(null == type ? "statement" : type).append("\"");
        sbuild.append(',');
        sbuild.append("\"rank\":\"").append((null != rank ? rank : Rank.NORMAL).toString()).append("\"");

        sbuild.append('}');
        return sbuild.toString();
    }
}
