/**
 *  @(#)XWikiLinksearch.java 0.02 01/10/2012
 *  Copyright (C) 2011 - 2012 MER-C
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
 *  @version 0.02
 */
public class XWikiLinksearch extends HttpServlet
{
    // wiki groups
    public static final Wiki[] top20wikis, top40wikis, importantwikis;
    
    /**
     *  Initializes wiki groups.
     */
    static
    {
        // top 20 Wikipedias
        String[] temp = { "en", "de", "fr", "nl", "it", "pl", "es", "ru", "ja", "pt",
            "zh", "sv", "vi", "uk", "ca", "no", "fi", "cs", "hu", "fa" };
        top20wikis = new Wiki[20];
        for (int i = 0; i < temp.length; i++)
        {
            top20wikis[i] = new Wiki(temp[i] + ".wikipedia.org");
            top20wikis[i].setUsingCompressedRequests(false); // This is Google's fault.
            top20wikis[i].setMaxLag(-1);
        }
        
        // top 40 Wikipedias
        top40wikis = new Wiki[40];
        System.arraycopy(top20wikis, 0, top40wikis, 0, 20);
        temp = new String[] { "ro", "ko", "ar", "tr", "id", "sk", "eo", "da", "sr", "kk",
            "lt", "ms", "he", "bg", "eu", "sl", "vo", "hr", "war", "hi" };
        for (int i = 20; i < temp.length; i++)
        {
            top40wikis[i] = new Wiki(temp[i - 20] + ".wikipedia.org");
            top40wikis[i].setUsingCompressedRequests(false); // This is Google's fault.
            top40wikis[i].setMaxLag(-1);
        }
        
        // a collection of important wikis
        temp = new String[] { "en", "de", "fr" };
        importantwikis = new Wiki[19];
        for (int i = 0; i < temp.length; i++)
        {
            importantwikis[5 * i    ] = new Wiki(temp[i] + ".wikipedia.org");
            importantwikis[5 * i + 1] = new Wiki(temp[i] + ".wiktionary.org");
            importantwikis[5 * i + 2] = new Wiki(temp[i] + ".wikibooks.org");
            importantwikis[5 * i + 3] = new Wiki(temp[i] + ".wikiquote.org");
            importantwikis[5 * i + 4] = new Wiki(temp[i] + ".wikivoyage.org");
        }
        importantwikis[15] = new Wiki("meta.wikimedia.org");
        importantwikis[16] = new Wiki("commons.wikimedia.org");
        importantwikis[17] = new Wiki("mediawiki.org");
        importantwikis[18] = new Wiki("wikidata.org");
        for (int i = 0; i < importantwikis.length; i++)
        {
            importantwikis[i].setUsingCompressedRequests(false);
            importantwikis[i].setMaxLag(-1);
        }
    }
    /**
     *  Main for testing/offline stuff. The results are found in results.html,
     *  which is in either the current or home directory.
     */
    public static void main(String[] args) throws IOException
    {
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("results.html"), "UTF-8");
        String domain = JOptionPane.showInputDialog(null, "Enter domain to search");
        if (domain == null)
            System.exit(0);
        StringBuilder builder = new StringBuilder(10000);
        linksearch(domain, builder, top40wikis, true);
        linksearch(domain, builder, importantwikis, true);
        out.write(builder.toString());
        out.close();
    }

    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  http://code.google.com/appengine/docs/quotas.html here } and { @link
     *  http://code.google.com/appengine/docs/java/runtime.html#The_Sandbox
     *  here } for what you can and cannot do in this environment. More
     *  precisely, at ~1s / wiki, we cannot search more than 40 wikis.
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
        buffer.append(ServletUtils.generateHead("Cross-wiki linksearch"));
        buffer.append("<p>This tool searches various Wikimedia projects for a ");
        buffer.append("specific link. Enter a domain name (example.com, not *.example.com or ");
        buffer.append("http://example.com) below. This process takes up to 20 seconds.\n");

        String domain = request.getParameter("link");
        String set = request.getParameter("set");
        buffer.append("<form action=\"./linksearch.jsp\" method=GET>\n");
        // wiki set combo box
        buffer.append("<table>");
        buffer.append("<tr><td>Wikis to search:\n<td>");
        LinkedHashMap<String, String> options = new LinkedHashMap<String, String>(10);
        options.put("top20", "Top 20 Wikipedias");
        options.put("top40", "Top 40 Wikipedias");
        options.put("major", "Major Wikimedia projects");
        buffer.append(ServletUtils.generateComboBox("set", options, set));
        // domain name text box
        buffer.append("<tr><td>Domain to search: <td><input type=text name=link");
        if (domain != null)
        {
            buffer.append(" value=\"");
            buffer.append(ServletUtils.sanitize(domain));
            buffer.append("\"");
        }
        // https checkbox
        boolean https = (request.getParameter("https") != null);
        buffer.append(">\n</table>\n<input type=checkbox name=\"https\" value=\"1\"");
        if (https)
            buffer.append(" checked");
        buffer.append(">Include HTTPS (timeout more likely)<br>\n");
        buffer.append("\n<input type=submit value=\"Search\">\n</form>\n");
        if (domain != null)
        {
            try
            {
                if (set == null || set.equals("top20"))
                    linksearch(domain, buffer, top20wikis, https);
                else if (set.equals("top40"))
                    linksearch(domain, buffer, top40wikis, https);
                else if (set.equals("major"))
                    linksearch(domain, buffer, importantwikis, https);
                else
                    buffer.append("ERROR: Invalid wiki set.");
            }
            catch (IOException ex)
            {
                buffer.append(ex.toString());
            }
        }

        // put a footer
        buffer.append("<br><br>");
        buffer.append(ServletUtils.generateFooter("Cross-wiki linksearch tool"));
        out.write(buffer.toString());
        out.close();
    }

    public static void linksearch(String domain, StringBuilder buffer, Wiki[] wikis, boolean https) throws IOException
    {
        buffer.append("<hr>\n<h2>Searching for links to ");
        buffer.append(ServletUtils.sanitize(domain));
        buffer.append(".\n");
        for (Wiki wiki : wikis)
        {
            ArrayList[] temp = wiki.linksearch("*." + domain, "http");
            // silly api designs aplenty here!
            if (https)
            {
                ArrayList[] temp2 = wiki.linksearch("*." + domain, "https");
                temp[0].addAll(temp2[0]);
                temp[1].addAll(temp2[1]);
            }
            buffer.append("<h3>Results for ");
            buffer.append(wiki.getDomain());
            buffer.append(":</h3>\n<p><ol>\n");
            for (int j = 0; j < temp[0].size(); j++)
            {
                buffer.append("<li><a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/");
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
            buffer.append(wiki.getDomain());
            buffer.append("/wiki/Special:Linksearch/*.");
            buffer.append(ServletUtils.sanitize(domain));
            buffer.append("\">Linksearch</a>)\n");
        }
    }
}