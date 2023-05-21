/**
 *  @(#)XWikiContributionSurveyor.java 0.01 21/08/2021
 *  Copyright (C) 2021-20XX MER-C and contributors
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

package org.wikipedia.tools;

import java.io.BufferedWriter;
import java.nio.file.*;
import java.util.*;
import org.wikipedia.*;

/**
 *  Performs a contribution survey for a user and an optional additional 
 *  category of users.
 * 
 *  @see ContributionSurveyor
 *  @author MER-C
 *  @version 0.01
 */
public class XWikiContributionSurveyor
{
    /**
     *  Runs this program.
     *  @param args the command line arguments, args[0] = individual user,
     *  args[1] = optional additional category
     *  @throws Exception if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        WMFWikiFarm sessions = WMFWikiFarm.instance();
        WMFWiki enWiki = sessions.sharedSession("en.wikipedia.org");
        // Users.of(enWiki).cliLogin();
        // TODO: add locked after command line options
        
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.XWikiContributionSurveyor", "[options]")
            .description("Survey the contributions of a large number of wiki editors across all wikis.")
            .addHelp()
            .addVersion("XWikiContributionSurveyor v0.01\n" + CommandLineParser.GPL_VERSION_STRING)
            .addSingleArgumentFlag("--user", "user", "Survey the given user.")
            .addSingleArgumentFlag("--category", "category", "Fetch a list of users from the given category (recursive).")
            .addBooleanFlag("--newonly", "Survey only page creations.")
            .parse(args);
        List<String> users = CommandLineParser.parseUserOptions(parsedargs, enWiki);
        boolean newonly = parsedargs.containsKey("--newonly");
        
        Set<String> wikis = new HashSet<>();
        wikis.add("en.wikipedia.org");
        for (String user : users)
        {
            Map<String, Object> ginfo = sessions.getGlobalUserInfo(user);
            for (var entry : ginfo.entrySet())
            {
                Object value = entry.getValue();
                if (value instanceof Map m)
                {
                    String url = ((String)m.get("url")).replace("https://", "");
                    int edits = (Integer)m.get("editcount");
                    if (edits > 0)
                        wikis.add(url);
                }
            }
        }
        Path path = Paths.get("spam.txt");
        try (BufferedWriter outwriter = Files.newBufferedWriter(path))
        {
            for (String wiki : wikis)
            {
                WMFWiki wikisession = sessions.sharedSession(wiki);
                outwriter.write("==" + wiki + "==\n\n");
                ContributionSurveyor cs = makeContributionSurveyor(wikisession, newonly);
                
                String prefix = wiki.substring(0, wiki.indexOf("."));
                if (wiki.equals("www.wikidata.org"))
                    prefix = "d";
                List<String> pages;
                if (wiki.equals("commons.wikimedia.org"))
                    pages = cs.outputContributionSurvey(users, true, false, true, Wiki.MAIN_NAMESPACE);
                else
                    pages = cs.outputContributionSurvey(users, true, false, false, Wiki.MAIN_NAMESPACE);
                
                for (String page : pages)
                {    
                    page = page.replace("[[:", "[[:" + prefix + ":");
                    page = page.replace("[[Special", "[[:" + prefix + ":Special");
                    outwriter.write(page);
                    outwriter.write("\n\n");
                }
            }
        }
    }
    
    private static ContributionSurveyor makeContributionSurveyor(Wiki wiki, boolean newonly)
    {
        ContributionSurveyor cs = new ContributionSurveyor(wiki);
        cs.setComingled(true);
        cs.setNewOnly(newonly);
        return cs;
    }
}
