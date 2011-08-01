/**
 *  @(#)ImageCCI.java 0.01 22/01/2011
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
import java.net.*;
import java.util.*;
import javax.swing.JOptionPane;
import javax.servlet.*;
import javax.servlet.http.*;
import org.wikipedia.Wiki;

/**
 *  Crude tool for generating image CCIs for users. See [[WP:CCI]].
 */
public class ImageCCI extends HttpServlet
{
    /**
     *  Main for testing/offline stuff. The results are found in results.html,
     *  which is in either the current or home directory.
     */
    public static void main(String[] args) throws IOException
    {
        String user = JOptionPane.showInputDialog(null, "Enter user to survey");
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(user + ".txt"), "UTF-8");
        StringBuilder buffer = new StringBuilder(10000);
        churn(user, buffer);
        out.write(buffer.toString());
        out.close();
        System.exit(0);
    }

    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  http://code.google.com/appengine/docs/quotas.html here } and { @link
     *  http://code.google.com/appengine/docs/java/runtime.html#The_Sandbox
     *  here } for what you can and cannot do in this environment. More
     *  precisely, at ~1s / wiki, we cannot search more than 30 wikis.
     *  <p>
     *  This servlet runs at { @link http://wikipediatools.appspot.com/imagecci.jsp }.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String user = request.getParameter("user");
        StringBuilder buffer = new StringBuilder(10000);
        PrintWriter out;
        if (user != null)
        {
            response.setContentType("text/plain; charset=utf-8");
            // create a download prompt
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(user, "UTF-8") + ".txt");
            out = response.getWriter();
            churn(user, buffer);
        }
        else
        {
            response.setContentType("text/html; charset=utf-8");
            out = response.getWriter();
            buffer.append("<!doctype html>\n<html>\n<head>\n<title>Image contribution surveyor");
            buffer.append( "</title>\n</head>\n\n<body>\n<p>This tool generates a listing of a user's ");
            buffer.append("image uploads (regardless of whether they are deleted) for use at\n");
            buffer.append("<a href=\"http://en.wikipedia.org/wiki/WP:CCI\">Contributor copyright ");
            buffer.append("investigations.</a>\n");
            // build HTML form
            buffer.append("<form action=\"./imagecci.jsp\" method=GET>\n<p>User to survey: ");
            buffer.append("<input type=text name=user>\n<input type=submit value=\"Survey user\">\n</form>\n");
            // footer
            buffer.append(ServletUtils.generateFooter("Image contribution surveyor"));
        }
        out.write(buffer.toString());
        out.close();
    }

    /**
     *  Contains the common CCI code for both online and offline modes.
     *  @param user the user to survey
     *  @param buffer the StringBuilder to write to
     *  @throws IOException if a network error occurs
     */
    public static void churn(String user, StringBuilder buffer) throws IOException
    {
        HashSet<String> wpUploads = conductCCI("en.wikipedia.org", user);
        HashSet<String> commonsUploads = conductCCI("commons.wikimedia.org", user);
        // remove all files that have been reuploaded to Commons by this user with the same name
        wpUploads.removeAll(commonsUploads);

        // output results
        buffer.append("=== Uploads to en.wikipedia.org ===\n");
        int i = 0;
        for (String entry : wpUploads)
        {
            i++;
            if (i % 20 == 1)
            {
                buffer.append("\n==== Local files ");
                buffer.append(i);
                buffer.append(" to ");
                buffer.append(i + 19);
                buffer.append(" ====\n");
            }
            buffer.append("*[[:");
            buffer.append(entry);
            buffer.append("]]\n");
        }
        buffer.append("\n=== Uploads to commons.wikimedia.org ===\n");
        i = 0;
        for (String entry : commonsUploads)
        {
            i++;
            if (i % 20 == 1)
            {
                buffer.append("\n==== Commons files ");
                buffer.append(i);
                buffer.append(" to ");
                buffer.append(i + 19);
                buffer.append(" ====\n");
            }
            buffer.append("*[[:");
            buffer.append(entry);
            buffer.append("]]\n");
        }
    }

    /**
     *  Obtains a list of images the user <tt>u</tt> has uploaded.
     *  @param w the wiki domain
     *  @param u the user to survey
     *  @return the list of images the user has uploaded
     *  @throws IOException
     */
    public static HashSet conductCCI(String w, String u) throws IOException
    {
        Wiki wiki = new Wiki(w);
        wiki.setMaxLag(0);
        wiki.setUsingCompressedRequests(false); // this is Google's fault
        Wiki.User user = wiki.getUser(u);
        HashSet<String> list = new HashSet<String>(10000);

        Wiki.LogEntry[] entries = wiki.getLogEntries(null, null, Integer.MAX_VALUE, Wiki.UPLOAD_LOG, user, "", Wiki.ALL_NAMESPACES);
        for (int i = 0; i < entries.length; i++)
            list.add((String)entries[i].getTarget());
        return list;
    }
}
