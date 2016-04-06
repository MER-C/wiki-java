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

public class Snak {
    private WikibaseData data;
    private Property property;
    private String datatype;

    public Snak(WikibaseData data, Property prop) {
        super();
        this.data = data;
        this.property = prop;
    }

    public Snak() {
        super();
    }

    public WikibaseData getData() {
        return data;
    }

    public void setData(WikibaseData data) {
        this.data = data;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property prop) {
        this.property = prop;
    }

    public String toJSON() {
        StringBuilder sbuild = new StringBuilder("{");
        sbuild.append("\"snaktype\":\"value\"").append(',').append("\"property\":\"").append("P")
            .append(property.getId().startsWith("P") ? property.getId().substring(1) : property.getId()).append("\"");
        sbuild.append(',');
        sbuild.append("\"datavalue\":").append(data.toJSON());
        if (null != datatype) {
            sbuild.append(',');
            sbuild.append("\"datatype\":\"").append(datatype).append('\"');
        }
        sbuild.append('}');
        return sbuild.toString();
    }

    @Override
    public String toString() {
        return "Snak [data=" + data + ", prop=" + property + ", datatype=" + datatype + "]";
    }

}
