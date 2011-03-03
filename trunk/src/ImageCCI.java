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

import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *  Crude tool for generating image CCIs for users. See [[WP:CCI]].
 */
public class ImageCCI extends HttpServlet
{
    private static HashSet list = new HashSet(1000);

    /**
     *  Main for testing/offline stuff. The results are found in results.html,
     *  which is in either the current or home directory.
     */
    public static void main(String[] args) throws IOException
    {
        String user = JOptionPane.showInputDialog(null, "Enter user to survey");
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(user + ".txt"), "UTF-8");
        conductCCI("en.wikipedia.org", user, out);
        conductCCI("commons.wikimedia.org", user, out);
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
        if (user != null)
        {
            response.setContentType("text/plain; charset=utf-8");
            // create a download prompt
            response.setHeader("Content-Disposition", "attachment; filename=" + user + ".txt");
            PrintWriter out = response.getWriter();
            conductCCI("en.wikipedia.org", user, out);
            conductCCI("commons.wikimedia.org", user, out);
            out.close();
        }
        else
        {
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.write("<!doctype html>\n<html>\n<head>\n<title>Image contribution surveyor"
                + "</title>\n</head>\n\n<body>\n<p>This tool generates a listing of a user's "
                + "image uploads (regardless of whether they are deleted) for use at\n"
                + "<a href=\"http://en.wikipedia.org/wiki/WP:CCI\">Contributor copyright "
                + "investigations.</a>\n");
            // build HTML form
            out.write("<form action=\"./imagecci.jsp\" method=GET>\n<p>User to survey: "
            + "<input type=text name=user>\n<input type=submit value=\"Survey user\">\n</form>\n");
            // footer
            out.write(ServletUtils.generateFooter("Image contribution surveyor"));
            out.close();
        }
    }

    /**
     *  @param w the wiki domain
     *  @param u the user to survey
     *  @param out the output stream to write the results to
     *  @throws IOException
     */
    public static void conductCCI(String w, String u, Writer out) throws IOException
    {
        Wiki wiki = new Wiki(w);
        wiki.setMaxLag(0);
        wiki.setUsingCompressedRequests(false); // this is Google's fault
        Wiki.User user = wiki.getUser(u);

        Wiki.LogEntry[] entries = wiki.getLogEntries(null, null, Integer.MAX_VALUE, Wiki.UPLOAD_LOG, user, "", Wiki.ALL_NAMESPACES);
        for (int i = 0; i < entries.length; i++)
        {
            if (i == 0)
                out.write("===Uploads on " + w + " ===\n");
            int size = list.size();
            String page = entries[i].getTarget();
            if (size % 20 == 0 && !list.contains(page))
                out.write("\n====Files " + (size + 1) + " to " + (size + 21) + " ====\n");
            // remove duplicates
            if (!list.contains(page))
            {
                out.write("*[[:" + page + "]]\n");
                list.add(page);
            }
        }
        list.clear();
        out.write("\n");
    }
}
