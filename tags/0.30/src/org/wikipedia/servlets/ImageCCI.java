/**
 *  @(#)ImageCCI.java 0.02 23/11/2011
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
    // wiki variables
    private static final Wiki enWiki = new Wiki("en.wikipedia.org");
    private static final Wiki commons = new Wiki("commons.wikimedia.org");

    static
    {
         // non-compression is Google's fault
        enWiki.setMaxLag(0);
        enWiki.setUsingCompressedRequests(false);
        commons.setMaxLag(0);
        commons.setUsingCompressedRequests(false);
    }

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
     *  https://code.google.com/appengine/docs/quotas.html here } and { @link
     *  https://code.google.com/appengine/docs/java/runtime.html#The_Sandbox
     *  here } for what you can and cannot do in this environment. More
     *  precisely, at ~1s / wiki, we cannot search more than 30 wikis.
     *  <p>
     *  This servlet runs at { @link https://wikipediatools.appspot.com/imagecci.jsp }.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (ServletUtils.checkBlacklist(request, response))
            return;
        String user = request.getParameter("user");
        StringBuilder buffer = new StringBuilder(10000);
        PrintWriter out;
        if (user != null)
        {
            response.setContentType("text/plain; charset=utf-8");
            // create a download prompt
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(user, "UTF-8") + ".txt");
            out = response.getWriter();
            try
            {
                churn(user, buffer);
            }
            catch (IOException ex)
            {
                buffer.append(ex.toString());
            }
        }
        else
        {
            response.setContentType("text/html; charset=utf-8");
            out = response.getWriter();
            buffer.append(ServletUtils.generateHead("Image contribution surveyor", null));
            buffer.append("<p>This tool generates a listing of a user's ");
            buffer.append("image uploads (regardless of whether they are deleted) for use at\n");
            buffer.append("<a href=\"//en.wikipedia.org/wiki/WP:CCI\">Contributor copyright ");
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
        // search enwiki upload log
        Wiki.User wpuser = enWiki.getUser(user);
        HashSet<String> wpUploads = new HashSet<String>(10000);
        if (wpuser == null)
        {
            buffer.append("Error: user does not exist!");
            return;
        }
        Wiki.LogEntry[] entries = enWiki.getUploads(wpuser);
        for (int i = 0; i < entries.length; i++)
            wpUploads.add((String)entries[i].getTarget());

        // search commons upload log
        Wiki.User comuser = commons.getUser(user);
        HashSet<String> commonsUploads = new HashSet<String>(10000);
        if (comuser != null)
        {
            entries = commons.getUploads(comuser);
            for (int i = 0; i < entries.length; i++)
                commonsUploads.add((String)entries[i].getTarget());
        }

        // search for transferred images
        HashSet<String> commonsTransfer = new HashSet<String>(10000);
        String[][] temp = commons.search("\"" + user + "\"", Wiki.FILE_NAMESPACE);
        for (int i = 0; i < temp.length; i++)
            commonsTransfer.add(temp[i][0]);

        // remove all files that have been reuploaded to Commons
        wpUploads.removeAll(commonsUploads);
        wpUploads.removeAll(commonsTransfer);
        commonsTransfer.removeAll(commonsUploads);

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
        buffer.append("\n=== Transferred files on commons.wikimedia.org ===\n");
        buffer.append("WARNING: may be inaccurate, depending on username.\n");
        i = 0;
        for (String entry : commonsTransfer)
        {
            i++;
            if (i % 20 == 1)
            {
                buffer.append("\n==== Transferred files ");
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
}
