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
        
        Wiki wiki = Wiki.createInstance("en.wikipedia.org");
        Map<String, Object>[] results = wiki.search(args[0], Wiki.USER_NAMESPACE);
        HashSet<String> users = new HashSet<>(500);
        for (Map<String, Object> result : results)
        {
            String username = (String)result.get("title");
            if (username.contains("/Books/"))
                continue;
            username = wiki.getRootPage(username);
            users.add(username.substring(5)); // remove User: prefix
        }

        String[] usernames = users.toArray(new String[0]);
        Wiki.User[] userinfo = wiki.getUsers(usernames);
        
        System.out.println("==Results for " + args[0] + "==");
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("|-");
        System.out.println("! Username !! Last edit !! Editcount !! Mainspace edits");
        
        for (int i = 0; i < usernames.length; i++)
        {
            if (userinfo[i] == null)
            {
                System.out.println("|-");
                System.out.printf("| [[User:%s]] ([[Special:Contributions/%s|contribs]]) || NA || NA || NA \n",
                    usernames[i], usernames[i]);
                continue;
            }
            if (userinfo[i].countEdits() > 50)
                continue;
            Wiki.Revision[] contribs = wiki.contribs(usernames[i]);
            if (contribs.length == 0)
                continue; // all deleted
            int mainspace = 0, userspace = 0;
            for (Wiki.Revision edit : contribs)
            {
                int namespace = wiki.namespace(edit.getPage());
                if (namespace == Wiki.MAIN_NAMESPACE)
                    mainspace++;
                if (namespace == Wiki.USER_NAMESPACE || namespace == Wiki.USER_TALK_NAMESPACE)
                    userspace++;
            }
            // skip users whose userspace edits have been deleted
            if (userspace == 0)
                continue;
            
            String lastedit = contribs[0].getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
            System.out.println("|-");
            System.out.printf("| [[User:%s]] ([[Special:Contributions/%s|contribs]]) || %s || %d || %d \n", 
                usernames[i], usernames[i], lastedit, contribs.length, mainspace);
        }
        System.out.println("|}");
    }
}
