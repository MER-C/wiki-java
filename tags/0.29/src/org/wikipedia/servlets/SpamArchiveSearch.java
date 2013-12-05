/**
 *  @(#)SpamArchiveSearch.java 0.01 06/07/2011
 *  Copyright (C) 2011 - 2013 MER-C
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
 *  A crude replacement for Eagle's spam archive search tool.
 *  @author MER-C
 *  @version 0.01
 */
public class SpamArchiveSearch extends HttpServlet
{
    private static final Wiki enWiki, meta;
    
    /**
     *  Initialize all Wiki objects.
     */
    static
    {
        enWiki = new Wiki("en.wikipedia.org");
        meta = new Wiki("meta.wikimedia.org");
        enWiki.setUsingCompressedRequests(false); // This is Google's fault.
        meta.setUsingCompressedRequests(false);
        enWiki.setMaxLag(0);
        meta.setMaxLag(0);
    }
    
    /**
     *  Main for testing/offline stuff. 
     */
    public static void main(String[] args) throws IOException
    {
        String query = JOptionPane.showInputDialog(null, "Enter query string");
        if (query == null)
            System.exit(0);
        StringBuilder builder = new StringBuilder(10000);
        archivesearch(query, builder);
        System.out.println(builder.toString());
    }

    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  https://code.google.com/appengine/docs/quotas.html here } and { @link
     *  https://code.google.com/appengine/docs/java/runtime.html#The_Sandbox
     *  here } for what you can and cannot do in this environment. More
     *  precisely, at ~1s / wiki, we cannot search more than 30 wikis.
     *  <p>
     *  This servlet runs at { @link https://wikipediatools.appspot.com/linksearch.jsp }.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (ServletUtils.checkBlacklist(request, response))
            return;
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder buffer = new StringBuilder(10000);

        // header
        buffer.append(ServletUtils.generateHead("Spam archive search", null));
        buffer.append("<p>This tool searches various spam related noticeboards ");
        buffer.append("for a given query string. If you want to search a domain name, please enclose\n");
        buffer.append("it in quotation marks.");

        // form for input
        String query = request.getParameter("query");
        buffer.append("<form action=\"./spamarchivesearch.jsp\" method=GET>\n<p>Search string: ");
        buffer.append("<input type=text name=query");
        if (query != null)
        {
            buffer.append(" value=\"");
            buffer.append(ServletUtils.sanitize(query));
            buffer.append("\"");
        }
        buffer.append(">\n<input type=submit value=\"Search\">\n</form>\n");
        if (query != null)
        {
            try
            {
                archivesearch(query, buffer);
            }
            catch (IOException ex)
            {
                buffer.append(ex.toString());
            }
        }

        // put a footer
        buffer.append(ServletUtils.generateFooter("Spam archive search tool"));
        out.write(buffer.toString());
        out.close();
    }

    public static void archivesearch(String query, StringBuilder buffer) throws IOException
    {
        buffer.append("<hr>\n<h2>Searching for \"");
        buffer.append(ServletUtils.sanitize(query));
        buffer.append("\".</h2>\n");

        // search
        // there's some silly api bugs
        ArrayList<String[]> results = new ArrayList<String[]>(20);
        results.addAll(Arrays.asList(meta.search(query + " \"spam blacklist\"", Wiki.TALK_NAMESPACE)));
        results.addAll(Arrays.asList(enWiki.search(query + " \"spam blacklist\"", Wiki.MEDIAWIKI_TALK_NAMESPACE)));
        results.addAll(Arrays.asList(enWiki.search(query + " \"spam whitelist\"", Wiki.MEDIAWIKI_TALK_NAMESPACE)));
        results.addAll(Arrays.asList(enWiki.search(query + " \"wikiproject spam\"", Wiki.PROJECT_TALK_NAMESPACE)));
        results.addAll(Arrays.asList(enWiki.search(query + " \"reliable sources noticeboard\"", Wiki.PROJECT_NAMESPACE)));
        results.addAll(Arrays.asList(enWiki.search(query + " \"external links noticeboard\"", Wiki.PROJECT_NAMESPACE)));

        // write to output
        buffer.append("<ul>\n");
        for (int i = 0; i < results.size(); i++)
        {
            String[] result = results.get(i);
            buffer.append("<li><a href=\"//");
            buffer.append(result[0].contains("Talk:Spam blacklist") ? "meta.wikimedia" : "en.wikipedia");
            buffer.append(".org/wiki/");
            buffer.append(result[0]);
            buffer.append("\">");
            buffer.append(result[0]);
            buffer.append("</a>\n");
        }
        buffer.append("</ul>\n<p>");
        buffer.append(results.size());
        buffer.append(" results.\n");
    }
}