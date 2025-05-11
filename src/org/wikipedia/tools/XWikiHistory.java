/**
 *  @(#)XWikiHistory.java 0.01 10/07/2019
 *  Copyright (C) 2019-20XX MER-C and contributors
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

import java.time.format.DateTimeFormatter;
import java.util.*;
import org.wikipedia.*;

/**
 *  Fetches page history and metadata for all language versions of Wikipedia that
 *  correspond to a given page on a given Wikipedia. Useful for cross-wiki 
 *  paid-for spamming.
 *  @author MER-C
 *  @version 0.01
 */
public class XWikiHistory
{
    private final static WMFWikiFarm sessions = WMFWikiFarm.instance();
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        // TODO: 
        // (1) multi-article
        // (3) page logs
        // (4) Wikidata item input - this is harder than it seems, it is not obvious what to do with the item number
        
        Map<String, String> parsedargs = new CommandLineParser("org.wikipedia.tools.XWikiHistory")
            .synopsis("[options]")
            .description("Fetches information about other language versions of an article")
            .addVersion("0.01")
            .addHelp()
            .addSingleArgumentFlag("--wiki", "en.wikipedia.org", "The wiki that hosts the article to get history for")
            .addSingleArgumentFlag("--article", "Main Page", "The article on the wiki to get history for.")
            //.addSingleArgumentFlag("--item", "Q123456", "Fetch cross-wiki history for this Wikidata item.")
            .addSingleArgumentFlag("--user", "Example", "Fetch cross-wiki history for all articles created by this user")
            .parse(args);
        String article = parsedargs.get("--article");
        
        WMFWiki wikidata = sessions.sharedSession("www.wikidata.org");
        Map<WMFWiki, String> wikiarticles = new LinkedHashMap<>();
        //if (item != null)
        //{
            WMFWiki home = sessions.sharedSession(parsedargs.get("--wiki"));
            List<Map<String, String>> interwikis = home.getInterWikiLinks(List.of(article));
            wikiarticles.put(wikidata, sessions.getWikidataItems(home, List.of(article)).get(0));
            wikiarticles.put(home, article);
            for (var entry : interwikis.get(0).entrySet())
            {
                WMFWiki new_wiki = sessions.sharedSession(entry.getKey() + ".wikipedia.org");
                wikiarticles.put(new_wiki, entry.getValue());
            }
        //}
        Map<WMFWiki, List<Wiki.Revision>> histories = getHistories(wikiarticles);
        Map<WMFWiki, Wiki.User> creators = getCreators(histories);
        Map<WMFWiki, String> snippets = getSnippets(wikiarticles);
        
        System.out.println("==" + article + "==");
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("! Wiki !! Page !! Creation date !! Creator !! Creator foreign edit count !! Snippet");
        
        for (var entry : wikiarticles.entrySet())
        {
            WMFWiki wiki = entry.getKey();
            String page = entry.getValue();
            List<Wiki.Revision> bottomhistory = histories.get(wiki);
            String username = bottomhistory.get(0).getUser();
            Wiki.User creator = creators.get(wiki);
            // Map<String, Object> pageinfo = wiki.getPageInfo(List.of(page)).get(0);
            String snippet = snippets.get(wiki);
            
            List<String> tablerows = List.of(wiki.getDomain(),
                "[" + wiki.getPageUrl(page) + " " + page + "] ("
                    + "[" + wiki.getPageUrl(wiki.getTalkPage(page)) + " talk] &middot; "
                    + "[" + wiki.getPageUrl("Special:PageHistory/" + page) + " history])",
                bottomhistory.get(0).getTimestamp().toString(),
                "[" + wiki.getPageUrl("User:" + username) + " " + username + "] ("
                    + "[" + wiki.getPageUrl("User talk:" + username) + " talk] &middot; "
                    + "[" + wiki.getPageUrl("Special:Contributions/" + username) + " contribs])",
                creator == null ? "0" : String.valueOf(creator.countEdits()),
                snippet == null ? "null" : snippet);
            System.out.println(WikitextUtils.addTableRow(tablerows));
        }
        System.out.println("|}");
        
        // output deletion logs
        String wdtitle = wikiarticles.get(wikidata);
        if (wdtitle != null)
        {
            Map<WMFWiki, List<Wiki.LogEntry>> deletions = getCrossWikiDeletionLogs(wdtitle);
            System.out.println("===Cross-wiki deletion log===");
            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! Project !! Date !! Admin !! Action !! Title !! Reason");
            deletions.forEach((wiki, entries) ->
            {
                String domain = wiki.getDomain();
                for (Wiki.LogEntry le : entries)
                {
                    System.out.println(WikitextUtils.addTableRow(List.of(
                        domain,
                        le.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        le.getUser(),
                        le.getAction(),
                        le.getTitle(),
                        "<nowiki>" + le.getComment() + "</nowiki>")
                    ));
                }
            });
            System.out.println("|}");
        }
        
        // global user information
        System.out.println("===Creator global user info===");
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("! Username !! Global edit count !! Home !! Wikis edited !! Locked?");
        Set<String> users = new TreeSet<>();
        for (WMFWiki wiki : wikiarticles.keySet())
        {
            Wiki.User user = creators.get(wiki);
            if (user != null)
                users.add(user.getUsername());
        }
        for (String username : users)
        {
            Map<String, Object> globaluserinfo = sessions.getGlobalUserInfo(username);
            System.out.println(WikitextUtils.addTableRow(List.of(
                Users.generateWikitextSummaryLinksShort(username),
                "" + globaluserinfo.get("editcount"),
                (String)globaluserinfo.get("home"),
                "[[m:Special:CentralAuth/" + username + "|" + globaluserinfo.get("wikicount") + "]]",
                globaluserinfo.get("locked").toString()
                )));
        }
        System.out.println("|}");
    }
    
    public static Map<WMFWiki, List<Wiki.Revision>> getHistories(Map<WMFWiki, String> articles)
    {
        ThrowingFunction<WMFWiki, List<Wiki.Revision>> tf = wiki ->
        {
            Wiki.RequestHelper rh = wiki.new RequestHelper()
                .limitedTo(10)
                .reverse(true);
            return wiki.getPageHistory(articles.get(wiki), rh);
        };
        return sessions.forAllWikis(articles.keySet(), tf, 1);
    }
    
    public static Map<WMFWiki, Wiki.User> getCreators(Map<WMFWiki, List<Wiki.Revision>> histories)
    {
        ThrowingFunction<WMFWiki, Wiki.User> tf = wiki ->
        {
            String creator = histories.get(wiki).get(0).getUser();
            return wiki.getUsers(List.of(creator)).get(0);
        };
        return sessions.forAllWikis(histories.keySet(), tf, 1);
    }
    
    public static Map<WMFWiki, String> getSnippets(Map<WMFWiki, String> articles)
    {
        ThrowingFunction<WMFWiki, String> tf = wiki -> 
        {
            String article = articles.get(wiki);
            return wiki.getLedeAsPlainText(List.of(article)).get(0);
        };
        return sessions.forAllWikis(articles.keySet(), tf, 1);
    }
    
    /**
     *  Deduces whether a Wikidata item has had corresponding pages deleted, 
     *  and if so returns the wikis where a deletion has occurred and the deletion 
     *  logs for that page.
     *  @param wditem the Wikidata item to look up
     *  @return a map: the wiki and the corresponding deletion log entries
     *  @throws Exception if a network error occurs
     */
    public static Map<WMFWiki, List<Wiki.LogEntry>> getCrossWikiDeletionLogs(String wditem) throws Exception
    {
        // Implemention note: deletion removes a page from its corresponding
        // Wikidata item with a specific edit summary. However, edit summaries 
        // on Wikidata are NOT what you see in the GUI. Instead they look like this:
        //
        // /* clientsitelink-remove:1||enwiki */ DeletedPageName
        //
        // WikiBase does some substitution before you see it. The API does not.
        WMFWiki wikidata = sessions.sharedSession("www.wikidata.org");
        List<Wiki.Revision> wdhistory = wikidata.getPageHistory(wditem, null);
        Map<WMFWiki, List<Wiki.LogEntry>> ret = new HashMap<>();
        for (Wiki.Revision revision : wdhistory)
        {
            String comment = revision.getComment();
            if (comment.startsWith("/* clientsitelink-remove"))
            {
                int end = comment.indexOf("*/") - 1;
                String dbname = comment.substring(comment.indexOf("||") + 2, end);
                String pagename = comment.substring(end + 3);
                WMFWiki local = sessions.sharedSession(WMFWikiFarm.dbNameToDomainName(dbname));
                Wiki.RequestHelper rh = local.new RequestHelper().byTitle(pagename);
                ret.put(local, local.getLogEntries(Wiki.DELETION_LOG, null, rh));
            }
        }
        return ret;
    }
}
