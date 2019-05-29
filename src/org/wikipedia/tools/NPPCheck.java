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
    private Wiki wiki;

    /**
     *  An enum denoting what type of review to fetch.
     */
    public static enum Mode
    {
        /**
         *  Lists pages patrolled by a particular user.
         */
        PATROLS,

        /**
         *  Lists pages moved from draft space to mainspace by a particular 
         *  user. This includes the majority of [[WP:AFC]] acceptances.
         */
        DRAFTS,

        /**
         *  Lists pages moved from user space to mainspace by a particular 
         *  user. This may include a small number of [[WP:AFC]] acceptances.
         */
        USERSPACE;

        /**
         *  Parses a string into an instance of this enum.
         *  @param s "patrols", "drafts" or "userspace"
         *  @return the obvious value, or null if unrecognized
         */
        public static Mode fromString(String s)
        {
            if (s == null)
                return null;
            switch (s)
            {
                case "patrols": return PATROLS;
                case "drafts": return DRAFTS;
                case "userspace": return USERSPACE;
                default: return null;
            }
        }
    }
    
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
        String user = args[0];
        Wiki enWiki = Wiki.newSession("en.wikipedia.org");
        if (user.equals("--all"))
        {
            user = null;
            enWiki.setQueryLimit(500);
        }
        NPPCheck check = new NPPCheck(enWiki);

        // patrol log
        List<Wiki.LogEntry> le = check.fetchLogs(user, null, null, Mode.PATROLS);
        System.out.println("==NPP patrols ==");        
        if (le.isEmpty())
            System.out.println("No new pages patrolled.");
        else
        {
            List<Duration> dt_patrol = Events.timeBetweenEvents(le);
            dt_patrol.add(Duration.ofSeconds(-1));
            Map<String, Object>[] pageinfo = check.fetchMetadata(le, Mode.PATROLS);

            System.out.println("{| class=\"wikitable sortable\"");
            String header = "! Title !! Create timestamp !! Patrol timestamp !! Article age at patrol (s) !! "
                + "Time between patrols (s) !! Page size !! Author !! Author registration timestamp !! "
                + "Author edit count !! Author age at creation (days) !! Author blocked?";
            if (user == null)
                header += " !! Reviewer";
            System.out.println(header);
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                String title = (String)info.get("pagename");
                System.out.println("|-");
                System.out.print("| [[:" + title + "]] || ");
                check.outputRow(info, dt_patrol.get(i), user == null);
            }
            System.out.println("|}\n");
        }
        
        // AFC acceptances
        le = check.fetchLogs(user, null, null, Mode.DRAFTS);
        System.out.println("==AFC acceptances ==");
        if (le.isEmpty())
            System.out.println("No AFCs accepted.");
        else
        {
            List<Duration> dt_patrol = Events.timeBetweenEvents(le);
            dt_patrol.add(Duration.ofSeconds(-1));
            Map<String, Object>[] pageinfo = check.fetchMetadata(le, Mode.DRAFTS);

            System.out.println("{| class=\"wikitable sortable\"");
            String header = "! Draft !! Title !! Create timestamp !! Accept timestamp !! Draft age at accept (s) !! "
                + "Time between accepts (s) !! Page size !! Author !! Author registration timestamp !! "
                + "Author edit count !! Author age at creation (days) !! Author blocked?";
            if (user == null)
                header += " !! Reviewer";
            System.out.println(header);
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
                String title = (String)info.get("pagename");
                System.out.println("|-");
                System.out.printf("| [[:%s]] || [[:%s]] || ", entry.getTitle(), title);
                check.outputRow(info, dt_patrol.get(i), user == null);
            }
            System.out.println("|}");
        }
        
        // Pages moved from user to main
        le = check.fetchLogs(user, null, null, Mode.USERSPACE);
        System.out.println("==Pages moved from user to main ==");
        if (le.isEmpty())
            System.out.println("No pages moved from user to main.");
        else
        {
            List<Duration> dt_patrol = Events.timeBetweenEvents(le);
            dt_patrol.add(Duration.ofSeconds(-1));
            Map<String, Object>[] pageinfo = check.fetchMetadata(le, Mode.USERSPACE);

            System.out.println("{| class=\"wikitable sortable\"");
            String header = "! Draft !! Title !! Create timestamp !! Accept timestamp !! Draft age at accept (s) !! "
                + "Time between accepts (s) !! Page size !! Author !! Author registration timestamp !! "
                + "Author edit count !! Author age at creation (days) !! Author blocked?";
            if (user == null)
                header += " !! Reviewer";
            System.out.println(header);
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
                String title = (String)info.get("pagename");
                System.out.println("|-");
                System.out.printf("| [[:%s]] || [[:%s]] || ", entry.getTitle(), title);
                check.outputRow(info, dt_patrol.get(i), user == null);            
            }
            System.out.println("|}");
        }
    }

    /**
     *  Binds an instance of this tool to a given wiki.
     *  @param wiki the wiki to bind to
     */
    public NPPCheck(Wiki wiki)
    {
        this.wiki = wiki;
    }

    /**
     *  Fetches a subset of new articles reviewed by a given user depending on mode.
     *  @param user the user to fetch logs for, null or empty for all users
     *  @param earliest fetch logs no earlier than this date
     *  @param latest fetch logs no later than this date
     *  @param mode which logs to fetch
     *  @return log entries representing new articles reviewed by the user
     *  @throws IOException if a network error occurs
     */
    public List<Wiki.LogEntry> fetchLogs(String user, OffsetDateTime earliest, OffsetDateTime latest, Mode mode) throws IOException
    {
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .withinDateRange(earliest, latest);
        if (user != null && !user.isEmpty())
            rh = rh.byUser(user);
        List<Wiki.LogEntry> le = Collections.emptyList();
        switch (mode)
        {
            case PATROLS:
                rh = rh.inNamespaces(Wiki.MAIN_NAMESPACE);
                return wiki.getLogEntries("patrol", "patrol", rh);
            case DRAFTS:
                rh = rh.inNamespaces(118);
                le = wiki.getLogEntries(Wiki.MOVE_LOG, "move", rh);
                break;
            case USERSPACE:
                rh = rh.inNamespaces(Wiki.USER_NAMESPACE);
                le = wiki.getLogEntries(Wiki.MOVE_LOG, "move", rh);
                break;
        }
        List<Wiki.LogEntry> ret = new ArrayList<>();
        for (Wiki.LogEntry log : le)
        {
            String newtitle = (String)log.getDetails();
            if (wiki.namespace(newtitle) == Wiki.MAIN_NAMESPACE)
                ret.add(log);
        }
        return ret;
    }
    
    /**
     *  For each log entry, fetches metadata of the accepted/patrolled article, 
     *  the first revision of the article and metadata associated with the author.
     *  @param logs a list of logs to examine
     *  @param mode which mode this is?
     *  @return the page metadata, augmented with keys "firstrevision", 
     *  "logentry" and "creator", in the order corresponding with logs
     *  @throws IOException if a network error occurs
     */
    public Map<String, Object>[] fetchMetadata(List<Wiki.LogEntry> logs, Mode mode) throws IOException
    {
        // TODO: filter out pages that were redirects when patrolled
        
        List<String> pages = new ArrayList<>();
        List<String> users = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
        {
            String title = mode == Mode.PATROLS ? log.getTitle() : (String)log.getDetails();
            pages.add(title);
        }
        
        // account for pages subsequently moved in namespace
        wiki.setResolveRedirects(true);
        Map<String, Object>[] pageinfo = wiki.getPageInfo(pages.toArray(new String[0]));
        wiki.setResolveRedirects(false);
        
        for (int i = 0; i < pageinfo.length; i++)
        {
            Wiki.LogEntry le = logs.get(i);
            String title = (String)pageinfo[i].get("pagename");
            if (wiki.namespace(title) == Wiki.MAIN_NAMESPACE && (Boolean)pageinfo[i].get("exists"))
            {
                Wiki.Revision first = wiki.getFirstRevision(title);
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
        List<Wiki.User> userinfo = wiki.getUsers(users);
        for (int i = 0; i < pageinfo.length; i++)
            pageinfo[i].put("creator", userinfo.get(i));
        return pageinfo;
    }
    
    public void outputRow(Map<String, Object> info, Duration dt_patrol, boolean all)
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
            System.out.printf("%s || %d || %d || %b", 
                registrationdate, editcount, dt_account.getSeconds() / 86400, blocked);
        }
        if (all)
            System.out.println(" || {{user|" + entry.getUser() + "}}");
        else
            System.out.println();
    }
}
