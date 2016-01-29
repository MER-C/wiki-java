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
package org.wikibase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikibase.data.Property;

public class WikibasePropertyFactory {
    private static Map<String, Property> props = new HashMap<String, Property>();
    private static Pattern PROP_ID_PATTERN = Pattern.compile("\\s*(p|P)?(\\d+)\\s*");
    public static Property getWikibaseProperty(String id) {
        Matcher idMatcher = PROP_ID_PATTERN.matcher(id);
        if (!idMatcher.matches()) {
            return null;
        }
        String actualId = "P" + idMatcher.group(2);
        
        Property prop = props.get(actualId);
        if (null == prop) {
            prop = new Property(actualId);
            props.put(actualId, prop);
        }
        return prop;
    }
}