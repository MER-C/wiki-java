/**
 *  @(#)MassLinksearch.java 0.01 29/12/2016
 *  Copyright (C) 2016 - 2017 MER-C
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
import java.net.MalformedURLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.wikipedia.*;

/**
 *  Online interface to [[Special:Linksearch]] that makes dealing with large
 *  number of domains more manageable.
 *  @author MER-C
 *  @version 0.01
 */
public class MassLinksearch extends HttpServlet
{
    /**
     *  Handles the HTTP <code>GET</code> method.
     *  @param request servlet request
     *  @param response servlet response
     *  @throws ServletException if a servlet-specific error occurs
     *  @throws IOException if an I/O error occurs
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        generateResponse(request, response);
    }
    
    /**
     *  Handles the HTTP <code>POST</code> method.
     *  @param request servlet request
     *  @param response servlet response
     *  @throws ServletException if a servlet-specific error occurs
     *  @throws IOException if an I/O error occurs
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        generateResponse(request, response);
    }
    
    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  https://cloud.google.com/appengine/docs/quotas here } and { @link
     *  https://cloud.google.com/appengine/docs/java#Java_The_sandbox here }
     *  for what you can and cannot do in this environment. More precisely, at
     *  ~1s / query, we cannot search more than 40 domains.
     *  <p>
     *  This servlet runs at { @link https://wikipediatools.appspot.com/masslinksearch.jsp }.
     * 
     *  @param request servlet request
     *  @param response servlet response
     *  @throws ServletException if a servlet-specific error occurs
     *  @throws IOException if an I/O error occurs
     */
    protected void generateResponse(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (ServletUtils.checkBlacklist(request, response))
            return;
        ServletUtils.addSecurityHeaders(response);
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder buffer = new StringBuilder(10000);
        
        // header
        buffer.append(ServletUtils.generateHead("Mass linksearch", null));
        buffer.append("<p>This tool searches a single project for a large collection of ");
        buffer.append("links. Enter domain names (example.com, *{{LinkSummary|example.com}} ");
        buffer.append("and \\bexample\\.com\\b are all acceptable) below, one per line. A ");
        buffer.append("timeout is more likely when searching for more domains.\n");
        
        String wiki = request.getParameter("wiki");
        String inputdomains = request.getParameter("domains");
        boolean https = (request.getParameter("https") != null);
        
        // parse inputdomains to pure list of domains
        if (inputdomains != null)
        {
            inputdomains = ServletUtils.sanitizeForHTML(inputdomains).trim().toLowerCase()
            // \\bexample\\.com\\b to example.com
                .replace("\\b", "").replace("\\.", ".")
            // *{{LinkSummary|example.com}} to example.com
                .replaceAll("\\*\\s*?\\{\\{(link\\s?summary(live)?|spamlink)\\|", "")
                .replace("}}", "");
        }
        
        buffer.append("<p><form action=\"./masslinksearch.jsp\" method=POST>\n");
        buffer.append("<table>\n");
        // wiki textfield
        buffer.append("<tr>\n<td>Wiki:");
        buffer.append("<td><input type=text name=wiki required");
        if (wiki != null)
        {
            buffer.append(" value=\"");
            buffer.append(ServletUtils.sanitizeForHTML(wiki));
            buffer.append("\">\n");
        }
        else
            buffer.append(">\n");
        // domains textarea
        buffer.append("<tr>\n");
        buffer.append("<td valign=top>Domains:\n");
        buffer.append("<td><textarea name=domains rows=10 required>\n");
        if (inputdomains != null)
            buffer.append(inputdomains);
        buffer.append("\n</textarea>\n");
        // https checkbox
        buffer.append("<tr>\n");
        buffer.append("<td>Additional protocols:\n");
        buffer.append("<td><input type=checkbox name=https value=1");
        if (https || inputdomains == null)
            buffer.append(" checked");
        buffer.append(">HTTPS\n");
        buffer.append("</table>\n");
        // submit
        buffer.append("<br>\n<input type=submit value=Search>\n</form>\n");
        
        if (inputdomains != null && wiki != null)
        {
            try
            {
                String[] domains = inputdomains.split("\r\n");
                Wiki w = new Wiki(wiki);
                w.setMaxLag(5);
                massLinksearch(w, domains, buffer, true);
                
                // reformat domain list to regex and linksummary
                StringBuilder regex = new StringBuilder();
                StringBuilder linksummary = new StringBuilder();
                for (String domain : domains)
                {
                    regex.append("\\b");
                    regex.append(domain.replace(".", "\\."));
                    regex.append("\\b\n");
                    linksummary.append("*{{LinkSummary|");
                    linksummary.append(domain);
                    linksummary.append("}}\n");
                }
                buffer.append("<hr>\n");
                buffer.append("<h3>Reformatted domain lists</h3>\n");
                buffer.append("<textarea readonly rows=10>\n");
                buffer.append(regex.toString());
                buffer.append("</textarea>\n");
                buffer.append("<textarea readonly rows=10>\n");
                buffer.append(linksummary.toString());
                buffer.append("</textarea>\n");
            }
            catch (MalformedURLException ex)
            {
                buffer.append("<span class=\"error\">ERROR: malformed URL!</span>");
            }
            catch (IOException ex)
            {
                buffer.append(ex.toString());
            }
        }
        
        // footer
        buffer.append("<br><br>");
        buffer.append(ServletUtils.generateFooter("Mass linksearch tool"));
        out.write(buffer.toString());
        out.close();
    }
    
    public static void massLinksearch(Wiki wiki, String[] domains, StringBuilder buffer, 
        boolean https, int... ns) throws IOException
    {
        buffer.append("<hr>");
        for (String domain : domains)
        {
            String sanitizedDomain = ServletUtils.sanitizeForHTML(domain).trim();
            buffer.append("<h3>Results for ");
            buffer.append(sanitizedDomain);
            buffer.append("</h3>\n");
            
            // add results
            List[] temp = wiki.linksearch("*." + domain, "http", ns);
            if (https)
            {
                List[] temp2 = wiki.linksearch("*." + domain, "https", ns);
                temp[0].addAll(temp2[0]);
                temp[1].addAll(temp2[1]);
            }
            buffer.append(ParserUtils.linksearchResultsToHTML(temp, wiki, sanitizedDomain));
        }
    }
}
