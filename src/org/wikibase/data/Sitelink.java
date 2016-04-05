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

    public String toJSON() {
        StringBuilder sbuild = new StringBuilder('{');
        sbuild.append("\"site\":").append('\"').append(site).append('\"');
        sbuild.append(',');
        sbuild.append("\"value\":").append('\"').append(pageName).append('\"');
        sbuild.append('}');
        return sbuild.toString();
    }
}

