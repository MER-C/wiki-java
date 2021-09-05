/**
 *  @(#)Unpatroller.java 0.01 09/08/2020
 *  Copyright (C) 2020 - 20xx MER-C and contributors
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
import java.util.function.Predicate;
import org.wikipedia.*;

/**
 *  Mass unpatrols pages patrolled by a given user.
 *  @author MER-C
 *  @version 0.01
 */
public class Unpatroller
{
    static WMFWiki enWiki = WMFWiki.newSession("en.wikipedia.org");
    
    /**
     *  Runs this program.
     *  @param args the command line arguments: [0] user to unpatrol [1] reason
     *  for unpatrolling
     */
    public static void main(String[] args) throws Exception
    {
        
        org.wikiutils.LoginUtils.guiLogin(enWiki);
        String username = args[0];
        
        // normalise/validate username
        Wiki.User user = enWiki.getUsers(List.of(username)).get(0);
        username = user.getUsername();
        
        // fetch patrolled pages
        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .byUser(username);
        List<Wiki.LogEntry> logs = enWiki.getLogEntries("pagetriage-curation", "reviewed", rh);
        Set<String> titles = new LinkedHashSet<>();
        for (Wiki.LogEntry log : logs)
            titles.add(log.getTitle());
                
        // add all articles they created or moved into mainspace
        // (check for autopatrolled occurs later, cannot rely on user permissions)
        rh = rh.inNamespaces(Wiki.MAIN_NAMESPACE)
               .filterBy(Map.of("patrolled", Boolean.TRUE, "new", Boolean.TRUE));
        List<Wiki.Revision> creations = enWiki.contribs(username, rh);
        for (Wiki.Revision revision : creations)
            titles.add(revision.getTitle());

        rh = rh.filterBy(null);
        List<Wiki.LogEntry> movelogs = getMovesIntoMainspace(rh);
        for (Wiki.LogEntry log : movelogs)
            titles.add((String)log.getDetails().get("target_title"));
        
        // unpatrol
        rh = enWiki.new RequestHelper();
        List<String> titles2 = new ArrayList<>(titles);
        var pageinfo = enWiki.getPageInfo(titles2);
        System.out.println("Unpatrolling " + titles2.size() + " pages.");
        for (int i = 0; i < titles2.size(); i++)
        {
            String title = titles2.get(i);
            if (!(Boolean)pageinfo.get(i).get("exists"))
            {
                System.out.println("Skipping " + title + " - article may have been deleted.");
                continue;
            }
            List<Wiki.LogEntry> pagelogs = enWiki.getLogEntries("pagetriage-curation", null, rh.byTitle(title));
            if (pagelogs.isEmpty() || pagelogs.get(0).getUser().equals(user.getUsername())) // empty = autopatrolled
            {
                System.out.println("Unpatrolling " + title);
                enWiki.triageNewPage((Long)pageinfo.get(i).get("pageid"), args[1], false, true);
            }
            else
                // page has been subsequently un/repatrolled by someone
                System.out.println("Skipping " + title + " - article has been unpatrolled already.");
        }
    }
    
    public static List<Wiki.LogEntry> getMovesIntoMainspace(Wiki.RequestHelper rh) throws Exception
    {
        // The newpages API query does not list pages that were moved into the
        // main namespace.
        Wiki.RequestHelper rh2 = rh.inNamespaces(118); // 118 = Draft namespace
        List<Wiki.LogEntry> logs = enWiki.getLogEntries(Wiki.MOVE_LOG, "move", rh2);
        rh2 = rh.inNamespaces(Wiki.USER_NAMESPACE);
        logs.addAll(enWiki.getLogEntries(Wiki.MOVE_LOG, "move", rh2));
        rh2 = rh.inNamespaces(Wiki.PROJECT_NAMESPACE); // Infrequent
        logs.addAll(enWiki.getLogEntries(Wiki.MOVE_LOG, "move", rh2));
        logs.removeIf(logentry -> 
        {
            // new title must be in the main namespace
            var details = logentry.getDetails();
            if (details == null) 
                return true;
            String movedto = details.get("target_title");
            return (movedto == null || enWiki.namespace(movedto) != Wiki.MAIN_NAMESPACE);
        });
        return logs;
    }
}
