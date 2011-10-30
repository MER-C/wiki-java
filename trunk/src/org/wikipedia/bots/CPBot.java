/**
 *  @(#)CPBot.java 0.01 29/08/2011
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

package org.wikipedia.bots;

import java.io.*;
import java.util.*;
import javax.security.auth.login.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.wikipedia.Wiki;

public class CPBot extends HttpServlet
{
    private static final String CP = "Wikipedia:Copyright_problems";

    /**
     *  The idea here is run this with a cron job on Google App Engine. That
     *  means we can run for no more than 10 minutes. See {@link
     *  http://code.google.com/appengine/docs/java/config/cron.html here} for
     *  more information.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            main(new String[0]);
        }
        catch(Exception ex)
        {
            // something
        }
    }
    
    public static void main(String[] args) throws IOException, LoginException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        enWiki.login("username", "password".toCharArray());

        // create daily CP page
        GregorianCalendar cal = new GregorianCalendar();
        enWiki.edit(createCPPageName(cal), "{{subst:cppage}}", "edit summary", false, false);

        // check for close paraphrase, etc.
        String cpText = enWiki.getRenderedText(CP);
        StringBuilder cpAddition = new StringBuilder(1000);
        for (String article : enWiki.getCategoryMembers("Articles tagged for copyright problems"))
        {
            if (!cpText.contains(article))
            {
                cpAddition.append("*{{subst:article-cv|");
                cpAddition.append(article);
                cpAddition.append("}}: unlisted copyvio. ~~~~\n");
            }
        }
        for (String article : enWiki.getCategoryMembers("All copied and pasted articles and sections"))
        {
            if (!cpText.contains(article))
            {
                cpAddition.append("*{{subst:article-cv|");
                cpAddition.append(article);
                cpAddition.append("}}: copied and pasted. ~~~~\n");
            }
        }
        String cpTextToday = enWiki.getPageText(createCPPageName(cal));
        enWiki.edit(createCPPageName(cal), cpTextToday + cpAddition.toString(), "edit summary", false, false);

        // move expired copyright problems around for closing
        cal.add(Calendar.DAY_OF_MONTH, -7);
        String cpOldSection = enWiki.getSectionText(CP, 2);
        enWiki.edit(CP, cpOldSection + "{{" + createCPPageName(cal) + "}}\n", "edit summary", false, false, 2);
    }

    public static String createCPPageName(GregorianCalendar date)
    {
        return CP + "/" + date.get(Calendar.YEAR) + "_" +
            date.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) + "_" +
            date.get(Calendar.DAY_OF_MONTH);
    }

}
