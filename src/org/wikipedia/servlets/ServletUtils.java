/**
 *  @(#)ServletUtils.java 0.01 22/02/2011
 *  Copyright (C) 2011 - 2017 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipedia.servlets;

import java.util.*;

/**
 *  Common servlet code so that I can maintain it easier.
 *  @author MER-C
 */
public class ServletUtils
{
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in the HTML
     *  body.
     *  @param input an input string
     *  @see https://www.owasp.org/index.php/XSS_Prevention Rule 1
     *  @return <tt>input</tt>, sanitized
     */
    public static String sanitizeForHTML(String input)
    {
        return input.replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;").replaceAll(">", "&gt;")
            .replaceAll("'", "&#x27;").replaceAll("\"", "&quot;")
            .replaceAll("/", "&#x2F;");
    }
    
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in boring
     *  HTML attributes.
     *  @param input the input to be sanitized
     *  @see https://www.owasp.org/index.php/XSS_Prevention Rule 2
     *  @return the sanitized input
     */
    public static String sanitizeForAttribute(String input)
    {
        return input.replaceAll("\"", "&quot;");
    }

    /**
     *  Generates a HTML combo box.
     *  @param param the form parameter for the combo box
     *  @param options a map, where key = option value, value = what is displayed
     *  to the user in the order that they are specified
     *  @param selected the initially selected option value
     *  @param disabled whether this element is disabled
     *  @return HTML text that displays a combo box
     */
    public static String generateComboBox(String param, LinkedHashMap<String, String> options, String selected, boolean disabled)
    {
        StringBuilder temp = new StringBuilder(500);
        temp.append("<select name=\"");
        temp.append(param);
        temp.append("\" id=\"");
        temp.append(param);
        temp.append("\"");
        if (disabled)
            temp.append(" disabled");
        temp.append(">\n");
        for (Map.Entry<String, String> entry : options.entrySet())
        {
            temp.append("<option value=\"");
            temp.append(entry.getKey());
            if (selected == null || selected.equals(entry.getKey()))
            {
                temp.append("\" selected>");
                selected = entry.getKey();
            }
            else
                temp.append("\">");
            temp.append(entry.getValue());
            temp.append("</option>\n");
        }
        temp.append("</select>\n");
        return temp.toString();
    }
}
