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

/**
 * A data type for a wikibase property representing another entity.
 * 
 * @author acstroe
 *
 */
public class Item extends WikibaseData {
    private Entity ent = null;

    public Entity getEnt() {
        return ent;
    }

    public Item(Entity ent) {
        super();
        this.ent = ent;
    }

    @Override
    public String toJSON() {
        StringBuilder sbuild = new StringBuilder("{");
        
        sbuild.append("\"type\":\"wikibase-entityid\"");
        sbuild.append(',');
        sbuild.append("\"value\":");
        sbuild.append('{');
        sbuild.append("\"entity-type\":\"item\"");
        sbuild.append(',');
        sbuild.append("\"numeric-id\":\"").append(ent.getId().startsWith("Q") ? ent.getId().substring(1) : ent.getId()).append("\"");
        sbuild.append('}');
        
        sbuild.append('}');
        return sbuild.toString();
    }

    @Override
    public Object getDatatype() {
        return "wikibase-item";
    }
}
