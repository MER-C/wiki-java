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
import javax.servlet.http.*;

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
    
    // LEGACY STUFF (remove when servlets are converted to JSP)
    
    public static void addSecurityHeaders(HttpServletResponse response)
    {
        response.setHeader("Strict-Transport-Security", "max-age=31536000");
        response.setHeader("Content-Security-Policy", 
            "frame-ancestors 'none'; " + 
            "default-src 'none'; " +     
            "script-src 'self'; " +      
            "style-src 'self'");         
        response.setHeader("Referrer-Policy", "no-referrer");
    }
    
    public static boolean checkBlacklist(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException
    {
        String useragent = request.getHeader("User-Agent");
        boolean bad = useragent.contains("Tweetmeme") || useragent.contains("bing.com");
        if (bad)
            response.sendError(403, "robots.txt exists for a reason. Follow it!");
        return bad;
    }
    
    public static String generateHead(String title, String optional)
    {
        // don't forget to update timeout.html and index.html
        StringBuilder sb = new StringBuilder(500);
        sb.append("<!doctype html>\n<html>\n<head>\n<title>");
        sb.append(title);
        sb.append("</title>\n<link rel=stylesheet href=\"styles.css\">\n");
        if (optional != null)
            sb.append(optional);
        sb.append("</head>\n\n<body>\n");
        return sb.toString();
    }
    
    public static String generateFooter(String toolname)
    {
        // don't forget to update timeout.html and index.html
        StringBuilder sb = new StringBuilder(500);
        sb.append("<hr>\n<p>");
        sb.append(toolname);
        sb.append(": Copyright (C) MER-C 2007-2017. This tool is free software: ");
        sb.append("you can redistribute it and/or modify it\nunder the terms of ");
        sb.append("the <a href=\"//gnu.org/licenses/agpl.html\">Affero GNU General ");
        sb.append("Public License</a> as published by the Free Software Foundation, ");
        sb.append("either version 3 of the License, or (at your\noption) any later ");
        sb.append("version.\n<p>Source code is available ");
        sb.append("<a href=\"//github.com/MER-C/wiki-java\">here</a>. Report ");
        sb.append("bugs at <a href=\"//en.wikipedia.org/wiki/User_talk:MER-C\">my ");
        sb.append("talk page</a>\nor the <a href=\"//github.com/MER-C/wiki-java/issues\">Github issue tracker</a>.");
        
        sb.append("\n\n<p><b>Tools:</b>\n");
        sb.append("  <a href=\"./linksearch.jsp\">Cross-wiki linksearch</a> | \n");
        sb.append("  <a href=\"./masslinksearch.jsp\">Mass linksearch</a> | \n");
        sb.append("  <a href=\"./imagecci.jsp\">Image contribution surveyor</a> |\n");
        sb.append("  <a href=\"./spamarchivesearch.jsp\">Spam blacklist archive search</a> | \n");
        sb.append("  <a href=\"./prefixcontribs.jsp\">Prefix contributions</a> \n");
        sb.append("</body>\n</html>");
        return sb.toString();
    }

}
