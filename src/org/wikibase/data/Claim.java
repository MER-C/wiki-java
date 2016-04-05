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

    private WikibaseDataType value;

    private Map<Property, Set<WikibaseDataType>> qualifiers = new HashMap<Property, Set<WikibaseDataType>>();

    public Map<Property, Set<WikibaseDataType>> getQualifiers() {
        return Collections.unmodifiableMap(qualifiers);
    }

    public WikibaseDataType getValue() {
        return value;
    }

    public void setValue(WikibaseDataType value) {
        this.value = value;
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

    public void addQualifier(Property property, WikibaseDataType data) {
        Set<WikibaseDataType> dataset = qualifiers.get(property);
        if (null == dataset) {
            dataset = new HashSet<WikibaseDataType>();
        }
        dataset.add(data);
        qualifiers.put(property, dataset);
    }

    @Override
    public String toString() {
        return "Claim [id=" + id + ", property=" + property + ", value=" + value + "]";
    }

    public String toJSON() {
        StringBuilder sbuild = new StringBuilder("{");
        sbuild.append("\"mainsnak\":");

        sbuild.append('{');
        sbuild.append("\"snaktype\":\"value\"").append(',').append("\"property\":\"").append("P")
            .append(property.getId().startsWith("P") ? property.getId().substring(1) : property.getId()).append("\"");
        sbuild.append(',');
        sbuild.append("\"datavalue\":").append(value.toJSON());
        if (!qualifiers.isEmpty()) {
            sbuild.append(',');
            sbuild.append("\"qualifiers\": {");
            for (Entry<Property, Set<WikibaseDataType>> qualEntry : qualifiers.entrySet()) {
                String propId = qualEntry.getKey().getId().startsWith("P") ? qualEntry.getKey().getId()
                    : ("P" + qualEntry.getKey().getId());
                sbuild.append('\"').append(propId).append("\":");
                if (!qualEntry.getValue().isEmpty()) {
                    boolean started = false;
                    sbuild.append('[');
                    for (WikibaseDataType eachData : qualEntry.getValue()) {
                        if (started) {
                            sbuild.append(',');
                        }

                        sbuild.append('{');
                        sbuild.append("\"snaktype\":\"value\"");
                        sbuild.append(',');
                        sbuild.append("\"property\":\"").append(propId).append("\"");
                        sbuild.append(',');
                        sbuild.append("\"datavalue\":").append(eachData.toJSON());
                        if (eachData.getDatatype() != null) {
                            sbuild.append(',');
                            sbuild.append("\"datatype\":").append(eachData.getDatatype()).append('\"');
                        }
                        sbuild.append('}');
                        started = true;
                    }
                    sbuild.append(']');
                }
            }
            sbuild.append('}');
        }
        sbuild.append('}');

        sbuild.append(',');
        sbuild.append("\"type\":\"").append(null == type ? "statement" : type).append("\"");
        sbuild.append(',');
        sbuild.append("\"rank\":\"").append((null != rank ? rank : Rank.NORMAL).toString()).append("\"");

        sbuild.append('}');
        return sbuild.toString();
    }
}
