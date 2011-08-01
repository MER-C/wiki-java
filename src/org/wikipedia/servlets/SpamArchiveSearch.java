/**
 *  @(#)SpamArchiveSearch.java 0.01 06/07/2011
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
 *  A crude replacement for Eagle's spam archive search tool.
 *  @author MER-C
 *  @version 0.01
 */
public class SpamArchiveSearch extends HttpServlet
{
    /**
     *  Main for testing/offline stuff. The results are found in results.html,
     *  which is in either the current or home directory.
     */
    public static void main(String[] args) throws IOException
    {
        String query = JOptionPane.showInputDialog(null, "Enter query string");
        StringBuilder builder = new StringBuilder(10000);
        archivesearch(query, builder);
        System.out.println(builder.toString());
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
        buffer.append("<!doctype html>\n<html>\n<head>\n<title>Spam archive search</title>");
        buffer.append("\n</head>\n\n<body>\n<p>This tool searches various spam related noticeboards ");
        buffer.append("for a given query string. If you want to search a domain name, please enclose\n");
        buffer.append("it in quotation marks.");

        // form for input
        String query = request.getParameter("query");
        buffer.append("<form action=\"./spamarchivesearch.jsp\" method=GET>\n<p>Search string: ");
        buffer.append("<input type=text name=query");
        if (query != null)
        {
            buffer.append(" value=\"");
            buffer.append(query);
            buffer.append("\"");
        }
        buffer.append(">\n<input type=submit value=\"Search\">\n</form>\n");
        if (query != null)
            archivesearch(query, buffer);

        // put a footer
        buffer.append(ServletUtils.generateFooter("Spam archive search tool"));
        out.write(buffer.toString());
        out.close();
    }

    public static void archivesearch(String query, StringBuilder buffer) throws IOException
    {
        buffer.append("<hr>\n<h2>Searching for \"");
        buffer.append(query);
        buffer.append("\".</h2>\n");

        Wiki enwiki = new Wiki("en.wikipedia.org");
        Wiki meta = new Wiki("meta.wikimedia.org");
        enwiki.setUsingCompressedRequests(false); // This is Google's fault.
        meta.setUsingCompressedRequests(false);
        enwiki.setMaxLag(0);
        meta.setMaxLag(0);

        // search
        // there's some silly api bugs
        ArrayList<String[]> results = new ArrayList<String[]>(20);
        results.addAll(Arrays.asList(meta.search(query + " prefix:Talk:Spam_blacklist")));
        results.addAll(Arrays.asList(enwiki.search(query + " prefix:MediaWiki_talk:Spam-blacklist")));
        results.addAll(Arrays.asList(enwiki.search(query + " prefix:MediaWiki_talk:Spam-whitelist")));
        results.addAll(Arrays.asList(enwiki.search(query + " prefix:Wikipedia_talk:WikiProject_Spam")));
        results.addAll(Arrays.asList(enwiki.search(query + " prefix:Wikipedia:Reliable_sources/Noticeboard")));
        results.addAll(Arrays.asList(enwiki.search(query + " prefix:Wikipedia:External_links/Noticeboard")));

        // write to output
        buffer.append("<ul>\n");
        for (int i = 0; i < results.size(); i++)
        {
            String[] result = results.get(i);
            buffer.append("<li><a href=\"http://");
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