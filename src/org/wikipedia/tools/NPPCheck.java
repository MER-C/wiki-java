/**
 *  @(#)NPPCheck.java 0.01 11/05/2019
 *  Copyright (C) 2019 - 20xx MER-C
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

import java.io.*;
import java.time.*;
import java.util.*;
import org.wikipedia.Events;
import org.wikipedia.Wiki;

/**
 *  Provides a listing of NPP article patrols and AFC acceptances for a given
 *  user with metadata.
 *  @author MER-C
 *  @version 0.01
 */
public class NPPCheck
{
    private static Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    
    /**
     *  Runs this program.
     *  @param args args[0] is the username
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            System.err.println("No user specified.");
            System.exit(1);
        }
        
        // patrol log
        Wiki.RequestHelper rh = enWiki.new RequestHelper().byUser(args[0]).inNamespaces(Wiki.MAIN_NAMESPACE);
        List<Wiki.LogEntry> le = enWiki.getLogEntries("patrol", "patrol", rh);
        System.out.println("==NPP patrols ==");        
        if (le.isEmpty())
            System.out.println("No new pages patrolled.");
        else
        {
            List<Duration> dt_patrol = Events.timeBetweenEvents(le);
            dt_patrol.add(Duration.ofSeconds(-1));
            Map<String, Object>[] pageinfo = processLogEntries(le, false);

            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! Title !! Create timestamp !! Patrol timestamp !! Article age at patrol (s) !! "
                + "Time between patrols (s) !! Page size !! Author !! Author registration timestamp !! "
                + "Author edit count !! Author age at creation (days) !! Author blocked?");
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                String title = (String)info.get("pagename");
                if (enWiki.namespace(title) != Wiki.MAIN_NAMESPACE)
                    continue;
                System.out.println("|-");
                System.out.print("| [[:" + title + "]] || ");
                outputRow(info, dt_patrol.get(i));
            }
            System.out.println("|}\n");
        }
        
        // AFC acceptances
        rh = enWiki.new RequestHelper().byUser(args[0]).inNamespaces(118);
        le = enWiki.getLogEntries(Wiki.MOVE_LOG, "move", rh);
        System.out.println("==AFC acceptances ==");
        if (le.isEmpty())
            System.out.println("No AFCs accepted.");
        else
        {
            List<Duration> dt_patrol = Events.timeBetweenEvents(le);
            dt_patrol.add(Duration.ofSeconds(-1));
            Map<String, Object>[] pageinfo = processLogEntries(le, true);

            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! Draft !! Title !! Create timestamp !! Accept timestamp !! Draft age at accept (s) !! "
                + "Time between accepts (s) !! Page size !! Author !! Author registration timestamp !! "
                + "Author edit count !! Author age at creation (days) !! Author blocked?");
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
                String title = (String)info.get("pagename");
                if (enWiki.namespace(title) != Wiki.MAIN_NAMESPACE)
                    continue;
                System.out.println("|-");
                System.out.printf("| [[:%s]] || [[:%s]] || ", entry.getTitle(), title);
                outputRow(info, dt_patrol.get(i));            
            }
            System.out.println("|}");
        }
    }
    
    /**
     *  For each log entry, fetches metadata of the accepted/patrolled article, 
     *  the first revision of the article and metadata associated with the author.
     *  @param logs a list of logs to examine
     *  @param move is this Wiki.MOVE_LOG?
     *  @return the page metadata, augmented with keys "firstrevision", 
     *  "logentry" and "creator", in the order corresponding with logs
     *  @throws IOException if a network error occurs
     */
    public static Map<String, Object>[] processLogEntries(List<Wiki.LogEntry> logs, boolean move) throws IOException
    {
        List<String> pages = new ArrayList<>();
        List<String> users = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
        {
            String title = move ? (String)log.getDetails() : log.getTitle();
            pages.add(title);
        }
        
        // account for pages subsequently moved in namespace
        enWiki.setResolveRedirects(true);
        Map<String, Object>[] pageinfo = enWiki.getPageInfo(pages.toArray(new String[0]));
        enWiki.setResolveRedirects(false);
        
        for (int i = 0; i < pageinfo.length; i++)
        {
            Wiki.LogEntry le = logs.get(i);
            String title = (String)pageinfo[i].get("pagename");
            if (enWiki.namespace(title) == Wiki.MAIN_NAMESPACE)
            {
                Wiki.Revision first = enWiki.getFirstRevision(title);
                pageinfo[i].put("firstrevision", first);
                if (first != null && !first.getUser().contains(">")) // ContentTranslation ([[Bucket crusher]])
                    users.add(first.getUser());
                else
                    users.add("Example"); // dummy value
            }
            else
                users.add("Example");
            pageinfo[i].put("logentry", le);
        }
        
        // fetch info of creators
        List<Wiki.User> userinfo = enWiki.getUsers(users);
        for (int i = 0; i < pageinfo.length; i++)
            pageinfo[i].put("creator", userinfo.get(i));
        return pageinfo;
    }
    
    public static void outputRow(Map<String, Object> info, Duration dt_patrol)
    {
        Wiki.Revision first = (Wiki.Revision)info.get("firstrevision");
        Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
        Wiki.User creator = (Wiki.User)info.get("creator");
        OffsetDateTime patroldate = entry.getTimestamp();
        int size = (Integer)info.get("size");

        OffsetDateTime createdate = null;
        OffsetDateTime registrationdate = null;
        int editcount = -1;
        String username = null;
        boolean blocked = false;
        if (first != null)
        {
            username = first.getUser();
            createdate = first.getTimestamp();
            if (creator != null)
            {
                editcount = creator.countEdits();
                registrationdate = creator.getRegistrationDate();
                blocked = creator.isBlocked();
            }
        }
        
        if (createdate == null)
        {
            System.out.printf("null || %s || null || %d || %d || %s || ", 
                patroldate, dt_patrol.getSeconds(), size, "{{user|" + username + "}}");
        }
        else
        {
            Duration dt_article = Duration.between(createdate, patroldate);
            System.out.printf("%s || %s || %d || %d || %d || %s || ", 
                createdate, patroldate, dt_article.getSeconds(), dt_patrol.getSeconds(),
                size, "{{user|" + username + "}}");                    
        }
        if (creator == null)
            System.out.println("null || -1 || -1 || " + blocked);
        else
        {
            Duration dt_account = createdate == null || registrationdate == null
                ? Duration.ofSeconds(-86401) : Duration.between(registrationdate, createdate);
            System.out.printf("%s || %d || %d || %b %n", 
                registrationdate, editcount, dt_account.getSeconds() / 86400, blocked);
        }
    }
}
