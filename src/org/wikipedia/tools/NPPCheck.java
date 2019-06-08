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
import java.util.regex.*;
import org.wikipedia.*;

/**
 *  Provides a listing of NPP article patrols and AFC acceptances for a given
 *  user with metadata.
 *  @author MER-C
 *  @version 0.01
 */
public class NPPCheck
{
    private WMFWiki wiki;

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
        WMFWiki enWiki = WMFWiki.newSession("en.wikipedia.org");
        if (user.equals("--all"))
        {
            user = null;
            enWiki.setQueryLimit(50);
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
            Map<String, Object>[] pageinfo = check.fetchMetadata(le);
            pageinfo = check.fetchCreatorMetadata(pageinfo);
            List<Wiki.User> reviewerinfo = check.fetchReviewerMetadata(le, user == null);
            List<String> snippets = check.fetchSnippets(le);

            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println(check.outputTableHeader(Mode.PATROLS, user == null));
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                System.out.println("|-");
                System.out.print("| ");
                check.outputRow(info, dt_patrol.get(i), user == null);
                if (user == null)
                    System.out.print(reviewerinfo.get(i).countEdits() + " || ");
                System.out.println(snippets.get(i));
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
            Map<String, Object>[] pageinfo = check.fetchMetadata(le);
            List<Wiki.User> reviewerinfo = check.fetchReviewerMetadata(le, user == null);
            pageinfo = check.fetchCreatorMetadata(pageinfo);
            List<String> snippets = check.fetchSnippets(le);
            
            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println(check.outputTableHeader(Mode.DRAFTS, user == null));
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
                System.out.println("|-");
                System.out.printf("| [[:%s]] || ", entry.getTitle());
                check.outputRow(info, dt_patrol.get(i), user == null);
                if (user == null)
                    System.out.print(reviewerinfo.get(i).countEdits() + " || ");
                System.out.println(snippets.get(i));
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
            Map<String, Object>[] pageinfo = check.fetchMetadata(le);
            pageinfo = check.fetchCreatorMetadata(pageinfo);
            List<Wiki.User> reviewerinfo = check.fetchReviewerMetadata(le, user == null);
            List<String> snippets = check.fetchSnippets(le);

            System.out.println("{| class=\"wikitable sortable\"");            
            System.out.println(check.outputTableHeader(Mode.USERSPACE, user == null));
            for (int i = 0; i < pageinfo.length; i++)
            {
                Map<String, Object> info = pageinfo[i];
                Wiki.LogEntry entry = (Wiki.LogEntry)info.get("logentry");
                System.out.println("|-");
                System.out.printf("| [[:%s]] || ", entry.getTitle());
                check.outputRow(info, dt_patrol.get(i), user == null);
                if (user == null)
                    System.out.print(reviewerinfo.get(i).countEdits() + " || ");
                System.out.println(snippets.get(i));
            }
            System.out.println("|}");
        }

/* 
 *  EXPERIMENTAL - insert to get summary statistics for a given mode

        Pattern pattern = Pattern.compile("REDACTED", Pattern.CASE_INSENSITIVE);


            System.out.println("{| class=\"wikitable sortable\"");
            String header = "! Draft !! Title !! Accept timestamp !! Snippet";
            List<String> snippets = check.fetchSnippets(le);

            // stats
            Map<String, Integer> reviewhist = new HashMap<>();
            Map<String, Integer> delhist = new HashMap<>();
            Map<String, Integer> regexhist = new HashMap<>();
            int totaldeleted = 0, totalregex = 0;
            for (int i = 0; i < le.size(); i++)
            {
                String patroller = le.get(i).getUser();
                String snippet = snippets.get(i);

                int count = reviewhist.getOrDefault(patroller, Integer.valueOf(0));
                reviewhist.put(patroller, count + 1);
                if (snippet == null)
                {
                    count = delhist.getOrDefault(patroller, Integer.valueOf(0));
                    delhist.put(patroller, count + 1);
                    totaldeleted++;
                }
                else
                {
                    Matcher matcher = pattern.matcher(snippet);
                    if (matcher.find())
                    {
                        count = regexhist.getOrDefault(patroller, Integer.valueOf(0));
                        regexhist.put(patroller, count + 1);
                        totalregex++;
                    }
                }
            }
            
            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! User !! Draft !! Article !! Review timestamp !! Snippet");
            for (int i = 0; i < le.size(); i++)
            {
                System.out.println("|-");
                Wiki.LogEntry log = le.get(i);
                String patroller = log.getUser();
                String snippet = snippets.get(i);
                String article = (String)log.getDetails();
                String draft = log.getTitle();
                OffsetDateTime timestamp = log.getTimestamp();
                System.out.printf("| {{user|%s}} || [[%s]] || [[%s]] || %s || %s %n", 
                    patroller, draft, article, timestamp.toString(), snippet);
            }
            System.out.println("|}");

            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! User !! Reviews !! Fraction deleted !! Fraction regex match");
            for (Map.Entry<String, Integer> entry : reviewhist.entrySet())
            {
                String loguser = entry.getKey();
                int reviews = entry.getValue();
                int deleted = delhist.getOrDefault(loguser, Integer.valueOf(0));
                int regex = regexhist.getOrDefault(loguser, Integer.valueOf(0));
                System.out.println("|-");
                System.out.printf("| [[User:%s|%s]] || %d || %2.1f || %2.1f %n", loguser, 
                    loguser, reviews, 100. * deleted/reviews, 100. * regex/reviews);
            }
            System.out.println("|-");
            System.out.printf("| TOTAL || %d || %2.1f || %2.1f %n", le.size(), 100. * totaldeleted/le.size(), 100.*totalregex/le.size());
            System.out.println("|}");
*/
    }

    /**
     *  Binds an instance of this tool to a given wiki.
     *  @param wiki the wiki to bind to
     */
    public NPPCheck(WMFWiki wiki)
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
     *  For each log entry, fetch a snippet of text from the article in its
     *  current state.
     *  @param logs a bunch of log entries
     *  @return a list of snippets for those logs, with null indicating a deleted
     *  page
     *  @throws IOException if a network error occurs
     */
    public List<String> fetchSnippets(List<Wiki.LogEntry> logs) throws IOException
    {
        List<String> pages = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
        {
            String title = log.getType().equals(Wiki.MOVE_LOG) ? (String)log.getDetails() : log.getTitle();
            pages.add(title);
        }
        
        // account for pages subsequently moved in namespace
        wiki.setResolveRedirects(true);
        List<String> pagetext = wiki.getLedeAsPlainText(pages);
        wiki.setResolveRedirects(false);
        return pagetext;
    }

    /**
     *  For each log entry, fetches metadata of the accepted/patrolled article.
     *  @param logs a list of logs to examine
     *  @return the page metadata, augmented with key "logentry" representing 
     *  the input log entry.
     *  @throws IOException if a network error occurs
     */
    public Map<String, Object>[] fetchMetadata(List<Wiki.LogEntry> logs) throws IOException
    {
        // TODO: filter out pages that were redirects when patrolled
        
        List<String> pages = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
        {
            String title = log.getType().equals(Wiki.MOVE_LOG) ? (String)log.getDetails() : log.getTitle();
            pages.add(title);
        }
        
        // account for pages subsequently moved in namespace
        wiki.setResolveRedirects(true);
        Map<String, Object>[] pageinfo = wiki.getPageInfo(pages.toArray(new String[0]));
        wiki.setResolveRedirects(false);
        
        for (int i = 0; i < pageinfo.length; i++)
            pageinfo[i].put("logentry", logs.get(i));
        return pageinfo;
    }

    /**
     *  Augments page metadata with information about the creator and first 
     *  revision. This is a slow query, so don't push too many items through it 
     *  in online mode.
     *  @param metadata the output from Wiki.getPageInfo
     *  @return the metadata, augmented with keys "creator" (Wiki.User) and 
     *  "firstrevision" (Wiki.Revision)
     *  @throws IOException if a network error occurs
     */
    public Map<String, Object>[] fetchCreatorMetadata(Map<String, Object>[] metadata) throws IOException
    {
        List<String> users = new ArrayList<>();
        for (int i = 0; i < metadata.length; i++)
        {
            String title = (String)metadata[i].get("pagename");
            if (wiki.namespace(title) == Wiki.MAIN_NAMESPACE && (Boolean)metadata[i].get("exists"))
            {
                Wiki.Revision first = wiki.getFirstRevision(title);
                metadata[i].put("firstrevision", first);
                if (first != null && !first.getUser().contains(">")) // ContentTranslation ([[Bucket crusher]])
                    users.add(first.getUser());
                else
                    users.add("Example"); // dummy value
            }
            else
                users.add("Example");
        }

        // fetch info of creators
        List<Wiki.User> userinfo = wiki.getUsers(users);
        for (int i = 0; i < metadata.length; i++)
            metadata[i].put("creator", userinfo.get(i));
        return metadata;        
    }
    
    /**
     *  Fetches metadata of reviewers if this is a run that isn't for a single
     *  user. Otherwise returns the empty list.
     *  @param logs the logs to fetch metadata for
     *  @param allusers (see above)
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public List<Wiki.User> fetchReviewerMetadata(List<Wiki.LogEntry> logs, boolean allusers) throws IOException
    {
        if (!allusers)
            return Collections.emptyList();
        List<String> usernames = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
            usernames.add(log.getUser());
        return wiki.getUsers(usernames);
    }
    
    /**
     *  Outputs a table header in wikitext.
     *  @param mode the current mode
     *  @param allusers whether the table contains data for all reviewers - adds 
     *  reviewer metadata columns, removes time between reviews
     *  @return a wikitext table header
     */
    public String outputTableHeader(Mode mode, boolean allusers)
    {
        StringBuilder header = new StringBuilder("! ");
        if (mode != Mode.PATROLS)
            header.append("Draft !! ");
        header.append("Title !! Create timestamp !! Review timestamp !! Age at review !! ");
        if (!allusers)
            header.append("Time between patrols !! ");
        header.append("Size !! Author !! Author registration timestamp !! ");
        header.append("Author edit count !! Author age at creation !! Author blocked !! ");
        if (allusers)
            header.append("Reviewer !! Reviewer edit count !! ");
        header.append("Snippet");
        return header.toString();
    }
    
    public void outputRow(Map<String, Object> pageinfo, Duration dt_patrol, boolean all)
    {
        String title = (String)pageinfo.get("pagename");
        Wiki.Revision first = (Wiki.Revision)pageinfo.get("firstrevision");
        Wiki.LogEntry entry = (Wiki.LogEntry)pageinfo.get("logentry");
        Wiki.User creator = (Wiki.User)pageinfo.get("creator");
        OffsetDateTime patroldate = entry.getTimestamp();
        int size = (Integer)pageinfo.get("size");

        OffsetDateTime createdate = null;
        OffsetDateTime registrationdate = null;
        Duration dt_article = Duration.ofDays(-999999);
        Duration dt_account = Duration.ofDays(-999999);
        int editcount = -1;
        String username = null;
        boolean blocked = false;
        if (first != null)
        {
            username = first.getUser();
            createdate = first.getTimestamp();
            dt_article = Duration.between(createdate, patroldate);
            if (creator != null)
            {
                editcount = creator.countEdits();
                registrationdate = creator.getRegistrationDate();
                if (registrationdate != null)
                    dt_account = Duration.between(registrationdate, createdate);
                blocked = creator.isBlocked();
            }
        }
        
        // Table structure:
        // Article | Create timestamp | Review timestamp | Article age at patrol |
        // Time between reviews (if not all users) | Size | Author | 
        // Author registration timestamp | Author edit count | Author age at creation |
        // Author blocked | Reviewer (if allusers)
        System.out.printf("[[:%s]] || %s || %s || data-sort-value=%d | %s || ", 
            title, createdate, patroldate, dt_article.getSeconds(), 
            MathsAndStats.formatDuration(dt_article));
        if (!all)
            System.out.print("data-sort-value=" + dt_patrol.getSeconds() + " | " 
                + MathsAndStats.formatDuration(dt_patrol) + " || ");
        System.out.printf("%d || {{noping2|%s}} || %s || %d || data-sort-value=%d | %s || %b || ", 
            size, username, registrationdate, editcount, dt_account.getSeconds(), 
            MathsAndStats.formatDuration(dt_account), blocked);
        if (all)
            System.out.print("{{noping2|" + entry.getUser() + "}} || ");        
    }
}
