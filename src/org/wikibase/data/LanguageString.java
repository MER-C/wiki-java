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

public class LanguageString extends WikibaseData {
    private String language;
    private String text;

    public LanguageString(String language, String text) {
        super();
        this.language = language;
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(text);
        sb.append(" (");
        sb.append(language);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getDatatype() {
        return "monolingualtext";
    }

    public String valueToJSON() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"text\":\"").append(text).append('\"');
        sb.append(',');
        sb.append("\"language\":\"").append(language).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
