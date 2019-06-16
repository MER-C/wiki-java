/**
 *  @(#)UserspaceAnalyzer.java 0.01 01/08/2017
 *  Copyright (C) 2017 MER-C
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
import java.time.format.DateTimeFormatter;
import org.wikipedia.Wiki;

/**
 *  Quick userspace search/analysis tool for finding misuse of Wikipedia as a
 *  social networking site. Skips [[Wikipedia:Books]].
 *  @author MER-C
 *  @version 0.01
 */
public class UserspaceAnalyzer
{
    /**
     *  Runs this tool.
     *  @param args command line arguments (args[0] = the search term)
     *  @throws Exception if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        // parse command line arguments
        if (args.length == 0)
            args = new String[] { "--help" };
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.UserspaceAnalyzer", "[search term]")
            .description("Searches userspace for pages eligible for deletion per [[WP:CSD#U5]]")
            .addHelp()
            .addVersion("UserspaceAnalyzer v0.01\n" + CommandLineParser.GPL_VERSION_STRING)
            .parse(args);
        
        Wiki wiki = Wiki.newSession("en.wikipedia.org");
        List<Map<String, Object>> results = wiki.search(args[0], Wiki.USER_NAMESPACE);
        HashSet<String> users = new HashSet<>(500);
        for (Map<String, Object> result : results)
        {
            String username = (String)result.get("title");
            if (username.contains("/Books/"))
                continue;
            username = wiki.getRootPage(username);
            users.add(username.substring(5)); // remove User: prefix
        }

        List<String> usernames = new ArrayList<>(users);
        List<Wiki.User> userinfo = wiki.getUsers(usernames);
        
        System.out.println("==Results for " + args[0] + "==");
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("|-");
        System.out.println("! Username !! Last edit !! Editcount !! Mainspace edits");
        
        for (Wiki.User user : userinfo)
        {
            if (user == null)
            {
                System.out.println("|-");
                System.out.printf("| [[User:%s]] ([[Special:Contributions/%s|contribs]]) || NA || NA || NA \n",
                    user, user);
                continue;
            }
            if (user.countEdits() > 50)
                continue;
            
            String username = user.getUsername();
            List<Wiki.Revision> contribs = wiki.contribs(username, null);
            if (contribs.isEmpty())
                continue; // all deleted
            int mainspace = 0, userspace = 0;
            for (Wiki.Revision edit : contribs)
            {
                int namespace = wiki.namespace(edit.getTitle());
                if (namespace == Wiki.MAIN_NAMESPACE)
                    mainspace++;
                if (namespace == Wiki.USER_NAMESPACE || namespace == Wiki.USER_TALK_NAMESPACE)
                    userspace++;
            }
            // skip users whose userspace edits have been deleted
            if (userspace == 0)
                continue;
            
            String lastedit = contribs.get(0).getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
            System.out.println("|-");
            System.out.printf("| [[User:%s]] ([[Special:Contributions/%s|contribs]]) || %s || %d || %d \n", 
                username, username, lastedit, contribs.size(), mainspace);
        }
        System.out.println("|}");
    }
}
