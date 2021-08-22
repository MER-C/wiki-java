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
     */
    public static void main(String[] args) throws Exception
    {
        WMFWiki enWiki = WMFWiki.newSession("en.wikipedia.org");
        // Users.of(enWiki).cliLogin();
        List<String> users = new ArrayList<>();
        users.add(args[0]);
        if (args.length > 1)
            users.addAll(enWiki.getCategoryMembers(args[1], true, Wiki.USER_NAMESPACE));
        users.replaceAll(enWiki::removeNamespace);
        
        Set<String> wikis = new HashSet<>();
        wikis.add("en.wikipedia.org");
        for (String luser : users)
        {
            Map<String, Object> ginfo = WMFWiki.getGlobalUserInfo(luser);
            for (var entry : ginfo.entrySet())
            {
                Object value = entry.getValue();
                if (value instanceof Map)
                {
                    Map m = (Map)value;
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
                WMFWiki wikisession = WMFWiki.newSession(wiki);
                outwriter.write("==" + wiki + "==\n\n");
                ContributionSurveyor cs = makeContributionSurveyor(wikisession);
                for (String page : cs.outputContributionSurvey(users, true, false, false, Wiki.MAIN_NAMESPACE))
                {
                    String prefix = wiki.substring(0, wiki.indexOf("."));
                    if (wiki.equals("www.wikidata.org"))
                        prefix = "d";
                    page = page.replace("[[:", "[[:" + prefix + ":");
                    page = page.replace("[[Special", "[[:" + prefix + ":Special");
                    outwriter.write(page);
                    outwriter.write("\n\n");
                }
            }
        }
    }
    
    private static ContributionSurveyor makeContributionSurveyor(Wiki wiki)
    {
        ContributionSurveyor cs = new ContributionSurveyor(wiki);
        cs.setComingled(true);
        cs.setNewOnly(true);
        return cs;
    }
}
