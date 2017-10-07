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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention">OWASP XSS 
     *  Prevention Cheat Sheet Rule 1</a>
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
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention"> OWASP XSS
     *  Prevention Cheat Sheet Rule 2</a>
     *  @return the sanitized input
     */
    public static String sanitizeForAttribute(String input)
    {
        return input.replaceAll("\"", "&quot;");
    }
    
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in URLs. (Note
     *  that most Wiki.java methods should handle this.)
     *  @param input the input to be sanitized
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention"> OWASP XSS
     *  Prevention Cheat Sheet Rule 5</a>
     *  @return the sanitized input
     */
    public static String sanitizeForURL(String input)
    {
        try
        {
            return URLEncoder.encode(input, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            // should never happen
            return "";
        }
    }
    
    /**
     *  Denotes the start of a collapsible section. Requires the separate 
     *  JavaScript file collapsible.js to function.
     * 
     *  @param title the title of the collapsed section sanitized for XSS
     *  @param collapsed whether to start off in the collapsed state
     *  @return HTML for a collapsible section
     *  @see #endCollapsibleSection
     */
    public static String beginCollapsibleSection(String title, boolean collapsed)
    {
        StringBuilder sb = new StringBuilder();
        // create a container to house a border for the collapsed element and a
        // title for the collapsed section
        sb.append("<div class=\"collapsecontainer\">\n");
        sb.append("<span class=\"collapseboxtop\">\n");
        sb.append("<span class=\"collapseheader\">");
        sb.append(title);
        sb.append("</span>\n");
        // show/hide link
        sb.append("<span class=\"showhidespan\">[<a href=\"#fasfd\" class=\"showhidelink\">");
        sb.append(collapsed ? "show" : "hide");
        sb.append("</a>]</span>\n");
        sb.append("</span>\n");
        // the actual collapsible 
        sb.append("<div class=\"");
        sb.append(collapsed ? "collapsed" : "notcollapsed");
        sb.append("\">\n");
        return sb.toString();
    }
    
    /**
     *  Denotes the end of a collapsible section.
     *  @return HTML denoting the end of a collapsible section
     *  @see #beginCollapsibleSection
     */
    public static String endCollapsibleSection()
    {
        // the only reason this exists is readibility 
        return "</div>\n</div>\n";
    }
}
