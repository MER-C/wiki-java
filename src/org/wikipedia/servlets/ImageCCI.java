/**
 *  @(#)ImageCCI.java 0.02 23/11/2011
 *  Copyright (C) 2011 - 2013 MER-C
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

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.servlet.*;
import javax.servlet.http.*;
import org.wikipedia.Wiki;
import org.wikipedia.tools.ContributionSurveyor;

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
        enWiki.setMaxLag(0);
        commons.setMaxLag(0);
    }

    /**
     *  Main for testing/offline stuff. The results are found in results.html,
     *  which is in either the current or home directory.
     *  @param args command line arguments (ignored)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        String user = JOptionPane.showInputDialog(null, "Enter user to survey");
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(user + ".txt"), "UTF-8");
        StringBuilder buffer = new StringBuilder(10000);
        fetchCCI(user, buffer);
        out.write(buffer.toString());
        out.close();
        System.exit(0);
    }

    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  https://cloud.google.com/appengine/docs/quotas here } and { @link
     *  https://cloud.google.com/appengine/docs/java#Java_The_sandbox here } 
     *  for what you can and cannot do in this environment.
     *  <p>
     *  This servlet runs at { @link https://wikipediatools.appspot.com/imagecci.jsp }.
     * 
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (ServletUtils.checkBlacklist(request, response))
            return;
        ServletUtils.addSecurityHeaders(response);

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
                fetchCCI(user, buffer);
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
    public static void fetchCCI(String user, StringBuilder buffer) throws IOException
    {
        Wiki.User wpuser = enWiki.getUser(user);
        String[][] survey = ContributionSurveyor.imageContributionSurvey(enWiki, wpuser);

        // output results
        buffer.append("=== Uploads to en.wikipedia.org ===\n");
        int i = 0;
        for (String entry : survey[0])
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
        for (String entry : survey[1])
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
        for (String entry : survey[2])
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
