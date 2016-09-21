/**
 *  @(#)UserWatchlist.java 0.01 x/x/2015
 *  Copyright (C) 2015 MER-C
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
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.wikipedia.*;

/**
 *  
 *  @version 0.01
 *  @author MER-C
 */
public class UserWatchlist extends HttpServlet
{
    private static final Wiki enWiki = new Wiki("en.wikipedia.org");
    
    /**
     *  Initialize all Wiki objects.
     */
    static
    {
        enWiki.setMaxLag(-1);
    }
    
    /**
     *  This servlet is intended to run on Google App Engine, see { @link
     *  https://cloud.google.com/appengine/docs/quotas here } and { @link
     *  https://cloud.google.com/appengine/docs/java#Java_The_sandbox here }
     *  for what you can and cannot do in this environment.
     *  <p>
     *  This servlet runs at { @link https://wikipediatools.appspot.com/userwatchlist.jsp }.
     * 
     *  @param request servlet request
     *  @param response servlet response
     *  @throws ServletException if a servlet-specific error occurs
     *  @throws IOException if an I/O error occurs
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (ServletUtils.checkBlacklist(request, response))
            return;
        ServletUtils.addSecurityHeaders(response);
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder buffer = new StringBuilder(50000);
        
        // header
        buffer.append(ServletUtils.generateHead("User watchlist", null));
        buffer.append("<p>This tool retrieves recent (<5 days) contributions of a list of users. ");
        buffer.append("There is a limit of 30 users per request, though the list may be ");
        buffer.append("of indefinite length.<p>");
        buffer.append("Syntax: one user per line, reason after # . Example:");
        buffer.append("<pre>Example user # Copyright violations\n// This is a comment\nSomeone # Spam</pre>");
        
        // page input
        buffer.append("<form action=\"./userwatchlist.jsp\" method=GET>\n");
        buffer.append("<table>\n");
        buffer.append("<tr><td>Input page:<td><input type=text size=30 name=page");
        String page = request.getParameter("page");
        if (page == null)
            buffer.append(">\n");
        else
        {
            buffer.append(" value=\"");
            buffer.append(page);
            buffer.append("\">\n");
        }
        // links to input page
        if (page != null && !page.isEmpty())
        {
            buffer.append("<a href=\"//en.wikipedia.org/wiki/");
            buffer.append(page);
            buffer.append("\">visit</a> | <a href=\"//en.wikipedia.org/w/index.php?action=edit&title=");
            buffer.append(page);
            buffer.append("\">edit</a>");
        }
        
        // skip input
        String temp = request.getParameter("skip");
        int skip = 0;
        buffer.append("<tr><td>\nSkip:<td><input type=text size=30 name=skip");
        if (temp == null || temp.isEmpty())
            buffer.append(">\n");
        else
        {
            skip = Integer.parseInt(temp);
            if (skip < 0)
                skip = 0;
            buffer.append(" value=\"");
            buffer.append(skip);
            buffer.append("\">\n");
        }
        
        // submit button
        buffer.append("</table>\n<input type=submit value=\"Submit\">\n</form>\n");
        
        // No page? DONE
        if (page == null || page.isEmpty())
        {
            buffer.append(ServletUtils.generateFooter("User watchlist"));
            out.write(buffer.toString());
            out.close();
            return;
        }
        if (page.matches("^User:.+/.+\\.(cs|j)s$"))
        {
            String us = page.substring(5, page.indexOf('/'));
            Wiki.User us2 = enWiki.getUser(us);
            if (us2 == null || !us2.isA("sysop")) // if (!page.equals("User:MER-C/UserWatchlist.js"))
            {
                buffer.append("TESTING WOOP WOOP WOOP!");
                out.write(buffer.toString());
                out.close();
                return;
            }
        }
        else
        {
            buffer.append("TESTING WOOP WOOP WOOP!");
            out.write(buffer.toString());
            out.close();
            return;
        }
        // fetch/count users in list
        String text = "";
        try
        {
            text = enWiki.getPageText(page);
        }
        catch (FileNotFoundException ex) 
        {
            // Page does not exist => DONE
            buffer.append("<span class=\"error\">ERROR: page does not exist!</span>");
            buffer.append(ServletUtils.generateFooter("User watchlist"));
            out.write(buffer.toString());
            out.close();
            return;
        }
        
        // preliminary token parsing
        StringTokenizer tk = new StringTokenizer(text, "\n");
        ArrayList<String> tokens = new ArrayList<>();
        while (tk.hasMoreTokens())
        {
            String token = tk.nextToken().trim();
            // line starts with "//" == comment
            if (!token.isEmpty() && !token.startsWith("//"))
                tokens.add(token);
        }
        
        // previous/next page
        buffer.append("<hr>");
        makePagination(buffer, page, tokens.size(), skip);
        
        for (int i = skip; i < tokens.size() && i < (skip + 30); i++)
        {
            String token = tokens.get(i);
            int split = token.indexOf("#");
            String user;
            String reason = "";
            if (split < 0)
                user = ServletUtils.sanitize(token);
            else
            {
                user = ServletUtils.sanitize(token.substring(0, split - 1)).trim();
                reason = ServletUtils.sanitize(token.substring(split + 1)).trim();
            }

            // user summary links and reason
            StringBuilder tempbuffer = new StringBuilder(500);
            tempbuffer.append("<h3>||</h3>\n<p>\n<ul>\n");
            tempbuffer.append("<li><a href=\"//en.wikipedia.org/wiki/User:||\">||</a> ");
            tempbuffer.append("(<a href=\"//en.wikipedia.org/wiki/User_talk:||\">talk</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/wiki/Special:Contributions/||\">contribs</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/wiki/Special:DeletedContributions/||\">deleted contribs</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/wiki/Special:Block/||\">block</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/w/index.php?title=Special:Log&type=block&page=User:||\">block log</a>)\n");
            if (!reason.isEmpty())
            {
                tempbuffer.append("<li><i>");
                tempbuffer.append(reason);
                tempbuffer.append("</i>");
            }
            tempbuffer.append("\n</ul>");
            buffer.append(tempbuffer.toString().replace("||", user));
            
            // contribs
            Calendar cutoff = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cutoff.add(Calendar.DAY_OF_MONTH, -5);
            Wiki.Revision[] contribs = enWiki.contribs(user, "", cutoff, null);
            if (contribs.length == 0)
                buffer.append("<p>No recent contributions or user does not exist.");
            else
                buffer.append(ParserUtils.revisionsToHTML(enWiki, contribs));
        }
        makePagination(buffer, page, tokens.size(), skip);
        
        buffer.append(ServletUtils.generateFooter("User watchlist"));
        out.write(buffer.toString());
        out.close();
    }
    
    private void makePagination(StringBuilder buffer, String page, int numtokens, int skip)
    {
        buffer.append("<p>");
        if (skip > 0)
        {
            buffer.append("<a href=\"./userwatchlist.jsp?page=");
            buffer.append(page);
            buffer.append("&skip=");
            buffer.append(skip < 30 ? 0 : skip - 30);
            buffer.append("\">Previous 30</a> | ");
        }
        else
            buffer.append("Previous 30 | ");
        if (numtokens - skip > 30)
        {
            buffer.append("<a href=\"./userwatchlist.jsp?page=");
            buffer.append(page);
            buffer.append("&skip=");
            buffer.append(skip + 30);
            buffer.append("\">Next 30</a>");
        }
        else
            buffer.append("Next 30");
    }
}
