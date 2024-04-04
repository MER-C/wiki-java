/**
 *  @(#)XWikiUserLinkAdditionFinder.java 0.01 06/07/2023
 *  Copyright (C) 2023-20XX MER-C and contributors
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

import java.util.*;
import org.wikipedia.*;

/**
 *  Rudimentary cross-wiki tool that finds links added by a list of users in
 *  the main namespace.
 *  @author MER-C
 *  @version 0.01
 */
public class XWikiUserLinkAdditionFinder
{
    /**
     *  Runs this program.
     *  @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        CommandLineParser clp = new CommandLineParser()
            .synopsis("org.wikipedia.tools.XWikiUserLinkAdditionFinder", "[options]")
            .description("Searches for all links added by users across all wikis.")
            .addVersion("XWikiUserLinkAdditionFinder v0.01\n" + CommandLineParser.GPL_VERSION_STRING)
            .addHelp()
            .addSingleArgumentFlag("--wiki", "example.org", "The wiki to fetch data from (default: en.wikipedia.org)")
            .addUserInputOptions("Get links");
        Map<String, String> parsedargs = clp.parse(args);
        
        WMFWikiFarm wmf = WMFWikiFarm.instance();
        WMFWiki thiswiki = wmf.sharedSession(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        List<String> users = CommandLineParser.parseUserOptions(parsedargs, thiswiki);
        TreeSet<String> domains = new TreeSet<>();
        
        for (String user : users)
        {
            Map<String, Object> userinfo = wmf.getGlobalUserInfo(user);
            Map<?, ?> m = (Map)userinfo.get("wikis");
            System.out.println("==" + user + "==");
            List<WMFWiki> wikisedited = new ArrayList<>();
            for (var entry : m.entrySet())
            {
                Map<?, ?> wikimap = (Map)entry.getValue();
                System.out.println(wikimap);
                String url = ((String)wikimap.get("url")).replace("https://", "");
                int edits = (Integer)wikimap.get("editcount");
                if (edits > 0)
                    wikisedited.add(wmf.sharedSession(url));
            }
            for (WMFWiki wiki : wikisedited)
            {
                UserLinkAdditionFinder finder = new UserLinkAdditionFinder(wiki);
                Map<Wiki.Revision, List<String>> results = finder.getLinksAdded(users, null, null);
                if (results.isEmpty())
                    continue;

                Map<String, String> linkdomains = new HashMap<>();
                for (Map.Entry<Wiki.Revision, List<String>> entry : results.entrySet())
                {
                    for (String link : entry.getValue())
                    {
                        String domain = ExternalLinks.extractDomain(link);
                        if (domain != null) // must be parseable
                            linkdomains.put(link, domain);
                    }
                }
                domains.addAll(linkdomains.values());
            }
        }
        System.out.println("==All domains==");
        for (String domain : domains)
            System.out.println("*{{spamlink|" + domain + "}}");
    }
    
}
