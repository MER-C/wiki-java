/**
 *  @(#)RangeContribs.java 0.01 15/10/2012
 *  Copyright (C) 2012 - 2013 MER-C
 *
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
package org.wikipedia.servlets;

import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import javax.servlet.*;
import javax.servlet.http.*;
import org.wikipedia.*;

/**
 *  Fetches contributions from an IP address range or a group of users that
 *  have a common prefix.
 *  @author MER-C
 *  @version 0.01
 */
public class PrefixContribs extends HttpServlet
{
    private static final Wiki enWiki = new Wiki("en.wikipedia.org");
    
    /**
     *  Initialize all Wiki objects.
     */
    static
    {
        enWiki.setUsingCompressedRequests(false);
        enWiki.setMaxLag(-1);
    }
    
    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  https://code.google.com/appengine/docs/quotas.html here } and { @link
     *  https://code.google.com/appengine/docs/java/runtime.html#The_Sandbox
     *  here } for what you can and cannot do in this environment. The 
     *  <p>
     *  This servlet runs at https://wikipediatools.appspot.com/prefixcontribs.jsp .
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (ServletUtils.checkBlacklist(request, response))
            return;
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder buffer = new StringBuilder(50000);
        
        // header
        buffer.append(ServletUtils.generateHead("Prefix contributions", null));
        buffer.append("<p>This tool retrieves contributions of an IP ");
        buffer.append("range or username prefix for the last 7 days. To search for an IP\n");
        buffer.append("range, use a search key of (say) 111.222. for 111.222.0.0/16. /24s ");
        buffer.append("and IPv6 addresses work similarly.\nNo sanitization is performed on ");
        buffer.append("IP addresses.\n\n");
        
        // form
        buffer.append("<form action=\"./prefixcontribs.jsp\" method=GET>\n<p>Search string: ");
        buffer.append("<input type=text name=prefix");
        String prefix = request.getParameter("prefix");
        if (prefix != null)
        {
            buffer.append(" value=\"");
            buffer.append(ServletUtils.sanitize(prefix));
            buffer.append("\">\n");
        }
        else
            buffer.append(">\n");
        buffer.append("<input type=submit value=\"Search\"></form>\n\n");
        
        if (prefix != null)
        {
            if (prefix.length() < 4)
                buffer.append("Error: search key of insufficient length.\n");
            else
            {
                Calendar cutoff = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                cutoff.add(Calendar.DAY_OF_MONTH, -7);
                Wiki.Revision[] revisions = enWiki.contribs("", prefix, cutoff, null);
                if (revisions.length == 0)
                    buffer.append("No contributions found.");
                else
                    buffer.append(ParserUtils.revisionsToHTML(enWiki, revisions));
            }
        }
        
        // footer
        buffer.append("<br><br>");
        buffer.append(ServletUtils.generateFooter("Prefix contributions"));
        out.write(buffer.toString());
        out.close();
    }
    
    /**
     *  Main for testing/offline stuff. Pipe the output to a HTML file.
     */
    public static void main(String[] args) throws IOException
    {
        String prefix = JOptionPane.showInputDialog(null, "Enter query string");
        if (prefix == null)
            System.exit(0);
        Calendar cutoff = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cutoff.add(Calendar.DAY_OF_MONTH, -7);
        Wiki.Revision[] revisions = enWiki.contribs("", prefix, cutoff, null);
        if (revisions.length == 0)
            System.out.println("No contributions found.");
        else
            System.out.println(ParserUtils.revisionsToHTML(enWiki, revisions));
    }
}
