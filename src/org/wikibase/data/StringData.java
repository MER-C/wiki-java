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

public class StringData extends WikibaseDataType {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public StringData(String value) {
        super();
        this.value = value;
    }

    @Override
    public String toString() {
        return "StringData [value=" + value + "]";
    }

    @Override
    public String toJSON() {
        StringBuilder sbuild = new StringBuilder("{");
        sbuild.append("\"value\":\"").append(value).append("\"");
        sbuild.append(',');
        sbuild.append("\"type\":\"string\"");
        sbuild.append('}');
        return sbuild.toString();
    }
}
