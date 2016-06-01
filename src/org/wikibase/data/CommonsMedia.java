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

public class CommonsMedia extends WikibaseData {
    private String fileName;

    public CommonsMedia(String fileName) {
        super();
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "CommonsMedia [fileName=" + fileName + "]";
    }

    @Override
    public String getDatatype() {
        return "commonsMedia";
    }

    @Override
    public String valueToJSON() {
        StringBuilder sbuild = new StringBuilder("{");
        sbuild.append("\"value\":\"").append(fileName).append("\"");
        sbuild.append(',');
        sbuild.append("\"type\":\"string\"");
        sbuild.append('}');
        return sbuild.toString();
    }
}
