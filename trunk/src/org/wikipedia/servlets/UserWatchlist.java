/**
 *  @(#)UserWatchlist.java 0.01 x/x/2015
 *  Copyright (C) 2015 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version. Additionally
 *  this file is subject to the "Classpath" exception.
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
        // enWiki.setUsingCompressedRequests(false);
        enWiki.setMaxLag(-1);
    }
    
    /**
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
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder buffer = new StringBuilder(50000);
        
        // header
        buffer.append(ServletUtils.generateHead("User watchlist", null));
        buffer.append("<p>This tool retrieves recent (<5 days) contributions of a list of users.");
        buffer.append("There is a limit of 30 users per request, though the list may be ");
        buffer.append("of indefinite length.<p>");
        buffer.append("Syntax: one user per line, reason after # . Example:");
        buffer.append("<pre>Example user # Copyright violations\nSomeone # Spam</pre>");
        
        // page input
        buffer.append("<form action=\"./userwatchlist.jsp\" method=GET>\n");
        buffer.append("<table>\n");
        buffer.append("<tr><td>Input page:<td><input type=text name=page");
        String page = request.getParameter("page");
        if (page == null)
            buffer.append(">\n");
        else
        {
            buffer.append(" value=\"");
            buffer.append(page);
            buffer.append("\">\n");
        }
        
        // skip input
        String temp = request.getParameter("skip");
        int skip = 0;
        buffer.append("<tr><td>\nSkip:<td><input type=text name=skip");
        if (temp == null || temp.isEmpty())
            buffer.append(">\n");
        else
        {
            skip = Integer.parseInt(temp);
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
        // fetch/count users in list
        String text = "";
        try
        {
            text = enWiki.getPageText(page);
        }
        catch (FileNotFoundException ex) 
        {
            // Page does not exist => DONE
            buffer.append("<span style=\"color: red, size=24pt\">ERROR: page does not exist!</span>");
            buffer.append(ServletUtils.generateFooter("User watchlist"));
            out.write(buffer.toString());
            out.close();
            return;
        }
        StringTokenizer tk = new StringTokenizer(text, "\n");
        int numtokens = tk.countTokens();
        
        // previous/next page
        buffer.append("<hr>");
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
        
        for (int i = skip; i < numtokens && i < (skip + 30); i++)
        {
            String token = tk.nextToken();
            int split = token.indexOf("%23");
            String user = token.substring(0, split - 1).trim();
            String reason = token.substring(split + 3).trim();

            // user summary links and reason
            StringBuilder tempbuffer = new StringBuilder(500);
            tempbuffer.append("<h3>||</h3>\n<p>\n<ul>\n");
            tempbuffer.append("<li><a href=\"//en.wikipedia.org/wiki/User:||\">||</a> ");
            tempbuffer.append("(<a href=\"//en.wikipedia.org/wiki/User_talk:||\">talk</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/wiki/Special:Contributions/||\">contribs</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/wiki/Special:Block/||\">block</a> ");
            tempbuffer.append("| <a href=\"//en.wikipedia.org/wiki/Special:DeletedContributions/||\">deleted contribs</a>)\n");
            tempbuffer.append("<li><i>");
            tempbuffer.append(reason);
            tempbuffer.append("</i>\n</ul>");
            buffer.append(tempbuffer.toString().replace("||", user));
            
            // contribs
            Calendar cutoff = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cutoff.add(Calendar.DAY_OF_MONTH, -3);
            Wiki.Revision[] contribs = enWiki.contribs(user, "", null, cutoff);
            if (contribs.length == 0)
                buffer.append("<p>No recent contributions.");
            else
                buffer.append(ParserUtils.revisionsToHTML(enWiki, contribs));
        }
        
        buffer.append(ServletUtils.generateFooter("User watchlist"));
        out.write(buffer.toString());
        out.close();
    }
}
