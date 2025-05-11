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
import java.time.OffsetDateTime;
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
     *  @param args the command line arguments
     *  @throws Exception if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        WMFWikiFarm sessions = WMFWikiFarm.instance();
        WMFWiki enWiki = sessions.sharedSession("en.wikipedia.org");
        WMFWiki meta = sessions.sharedSession("meta.wikimedia.org");

        CommandLineParser clp = new CommandLineParser("org.wikipedia.tools.XWikiContributionSurveyor")
            .synopsis("[options]")
            .description("Survey the contributions of a large number of wiki editors across all wikis.")
            .addVersion("XWikiContributionSurveyor v0.01\n" + CommandLineParser.GPL_VERSION_STRING)
            .addSingleArgumentFlag("--outfile", "file", "Save results to file(s).")
            .addSingleArgumentFlag("--lockedafter", "date", "Only survey unlocked users or those locked after a certain date.")
            .addSingleArgumentFlag("--wikipage", "'Main Page'", "Fetch a list of users from the en.wp page [[Main Page]].");
        Map<String, String> parsedargs = ContributionSurveyor.addSharedOptions(clp).parse(args);
        List<String> users = CommandLineParser.parseUserOptions(parsedargs, enWiki);
        String lockedafterstring = parsedargs.get("--lockedafter");
        OffsetDateTime lockedafter = (lockedafterstring == null) ? null : OffsetDateTime.parse(lockedafterstring);
        
        Set<String> wikis = new HashSet<>();
        wikis.add("en.wikipedia.org");
        List<String> toremove = new ArrayList<>();
        Wiki.RequestHelper rhlocked = meta.new RequestHelper()
            .limitedTo(1);
        for (String user : users)
        {
            Map<String, Object> ginfo = sessions.getGlobalUserInfo(user);
            if (ginfo == null)
            {
                toremove.add(user);
                continue;
            }
            if (lockedafter != null && (Boolean)ginfo.get("locked"))
            {
                // not guaranteed but should work in nearly all cases
                rhlocked = rhlocked.byTitle(meta.namespaceIdentifier(Wiki.USER_NAMESPACE) + ":" + user + "@global");
                List<Wiki.LogEntry> le = meta.getLogEntries("globalauth", null, rhlocked);
                if (!le.isEmpty() && le.get(0).getTimestamp().isBefore(lockedafter))
                {
                    toremove.add(user);
                    continue;
                }
            }
            Map<?, ?> m = (Map)ginfo.get("wikis");
            for (var entry : m.entrySet())
            {
                Map wikimap = (Map)entry.getValue();
                String url = ((String)wikimap.get("url")).replace("https://", "");
                int edits = (Integer)wikimap.get("editcount");
                if (edits > 0)
                    wikis.add(url);
            }
        }
        users.removeAll(toremove);
        int[] ns;
        if (parsedargs.containsKey("--userspace"))
            ns = new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE };
        else
            ns = new int[] { Wiki.MAIN_NAMESPACE };
        
        Path path = CommandLineParser.parseFileOption(parsedargs, "--outfile", "Select output file", 
            "Error: No output file selected.", true);
        try (BufferedWriter outwriter = Files.newBufferedWriter(path))
        {
            Map<String, String> iwmap = WMFWikiFarm.invertInterWikiMap(meta.interWikiMap());
            String footer = "Command line: <kbd>" + clp.commandString(args) + "</kbd>";
            for (String wiki : wikis)
            {
                WMFWiki wikisession = sessions.sharedSession(wiki);
                ContributionSurveyor cs = ContributionSurveyor.makeContributionSurveyor(wikisession, parsedargs);
                cs.setSurveyingTransferredFiles(false);
                cs.setFooter(footer);
                
                String prefix = iwmap.get(wiki);
                List<String> pages = cs.outputContributionSurvey(users, true, false, 
                    wiki.equals("commons.wikimedia.org"), ns);
                                
                if (!pages.isEmpty())
                {
                    outwriter.write("=" + wiki + "=\n\n");
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
    }
}
