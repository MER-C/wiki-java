/**
 *  @(#)XWikiLinksearch.java 0.01 14/02/2011
 *  Copyright (C) 2011 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
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
import org.wikipedia.Wiki;

/**
 *  A crude replacement for Eagle's cross-wiki linksearch tool. 
 *  @author MER-C
 *  @version 0.01
 */
public class XWikiLinksearch extends HttpServlet
{
    /**
     *  Main for testing/offline stuff. The results are found in results.html,
     *  which is in either the current or home directory.
     */
    public static void main(String[] args) throws IOException
    {
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("results.html"), "UTF-8");
        String domain = JOptionPane.showInputDialog(null, "Enter domain to search");
        StringBuilder builder = new StringBuilder(10000);
        linksearch(domain, builder);
        out.write(builder.toString());
        out.close();
    }

    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  http://code.google.com/appengine/docs/quotas.html here } and { @link
     *  http://code.google.com/appengine/docs/java/runtime.html#The_Sandbox
     *  here } for what you can and cannot do in this environment. More
     *  precisely, at ~1s / wiki, we cannot search more than 30 wikis.
     *  <p>
     *  This servlet runs at { @link http://wikipediatools.appspot.com/linksearch.jsp }.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder buffer = new StringBuilder(10000);

        // header
        buffer.append("<!doctype html>\n<html>\n<head>\n<title>Cross-wiki linksearch</title>");
        buffer.append("\n</head>\n\n<body>\n<p>This tool searches the top 20 Wikipedias for a ");
        buffer.append("specific link. Enter a domain name (example.com, not *.example.com or ");
        buffer.append("http://example.com) below. This process takes up to 20 seconds.\n");

        // form for input
        String domain = request.getParameter("link");
        buffer.append("<form action=\"./linksearch.jsp\" method=GET>\n<p>Domain to search: ");
        buffer.append("<input type=text name=link");
        if (domain != null)
        {
            buffer.append(" value=\"");
            buffer.append(domain);
            buffer.append("\"");
        }
        buffer.append(">\n<input type=submit value=\"Search\">\n</form>\n");
        if (domain != null)
            linksearch(domain, buffer);

        // put a footer
        buffer.append(ServletUtils.generateFooter("Cross-wiki linksearch tool"));
        out.write(buffer.toString());
        out.close();
    }

    public static void linksearch(String domain, StringBuilder buffer) throws IOException
    {
        String[] wikis = { "en", "de", "fr", "pl", "it", "ja", "es", "nl", "pt", "ru",
            "sv", "zh", "ca", "no", "fi", "uk", "hu", "cs", "ro" };
        buffer.append("<hr>\n<h2>Searching for links to ");
        buffer.append(domain);
        buffer.append(".\n");
        for (int i = 0; i < wikis.length; i++)
        {
            Wiki wiki = new Wiki(wikis[i] + ".wikipedia.org");
            wiki.setUsingCompressedRequests(false); // This is Google's fault.
            wiki.setMaxLag(0);
            ArrayList[] temp = wiki.linksearch("*." + domain, Wiki.ALL_NAMESPACES, "http");
            // silly api designs aplenty here!
            ArrayList[] temp2 = wiki.linksearch("*." + domain, Wiki.ALL_NAMESPACES, "https");
            temp[0].addAll(temp2[0]);
            temp[1].addAll(temp2[1]);
            buffer.append("<h3>Results for ");
            buffer.append(wikis[i]);
            buffer.append(".wikipedia.org:</h3>\n<p><ol>\n");
            for (int j = 0; j < temp[0].size(); j++)
            {
                buffer.append("<li><a href=\"//");
                buffer.append(wikis[i]);
                buffer.append(".wikipedia.org/wiki/");
                buffer.append((String)temp[0].get(j));
                buffer.append("\">");
                buffer.append((String)temp[0].get(j));
                buffer.append("</a> uses link <a href=\"");
                buffer.append(temp[1].get(j).toString());
                buffer.append("\">");
                buffer.append(temp[1].get(j).toString());
                buffer.append("</a>\n");
            }
            buffer.append("</ol>\n<p>");
            buffer.append(temp[0].size());
            buffer.append(" links found. (<a href=\"//");
            buffer.append(wikis[i]);
            buffer.append(".wikipedia.org/wiki/Special:Linksearch/*.");
            buffer.append(domain);
            buffer.append("\">Linksearch</a>)\n");
        }
    }
}