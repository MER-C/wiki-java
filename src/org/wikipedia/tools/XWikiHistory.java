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
    private static WMFWiki wikidata = WMFWiki.newSession("www.wikidata.org");
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        // TODO: 
        // (1) excerpts from page history - first 5 and last 5 revisions
        // (2) page metadata and usual page links (talk, history, delete, undelete, etc.)
        // (3) page logs
        // (4) more creator metadata
        
        // expected: args[0] language, args[1] = article
        WMFWiki firstwiki = WMFWiki.newSession(args[0] + ".wikipedia.org");
        List<Map<String, String>> interwikis = firstwiki.getInterWikiLinks(List.of(args[1]));
        Map<WMFWiki, String> wikiarticles = new LinkedHashMap<>();
        wikiarticles.put(firstwiki, args[1]);
        for (var entry : interwikis.get(0).entrySet())
        {
            WMFWiki new_wiki = WMFWiki.newSession(entry.getKey() + ".wikipedia.org");
            wikiarticles.put(new_wiki, entry.getValue());
        }
        
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("! Language !! Page !! Creation date !! Creator !! Creator foreign edit count !! Snippet");
        
        // Wikidata
        String wdtitle = firstwiki.getWikidataItems(List.of(args[1])).get(0);
        Map<Wiki, List<Wiki.LogEntry>> deletions = new HashMap<>();
        if (wdtitle != null)
        {
            deletions = fetchCrossWikiDeletionLogs(wdtitle);
            Wiki.Revision last = wikidata.getFirstRevision(wdtitle);
            Wiki.User creator = wikidata.getUsers(List.of(last.getUser())).get(0);
            List<String> cells = List.of(
                "Wikidata",
                "[[d:" + wdtitle + "|" + wdtitle + "]]",
                last.getTimestamp().toString(),
                Users.generateWikitextSummaryLinksShort(last.getUser()), 
                creator == null ? "null" : String.valueOf(creator.countEdits()), "-");
            System.out.println(WikitextUtils.addTableRow(cells));
        }
        
        for (var entry : wikiarticles.entrySet())
        {
            WMFWiki wiki = entry.getKey();
            String page = entry.getValue();
            
            // Map<String, Object> pageinfo = wiki.getPageInfo(List.of(page)).get(0);
            String snippet = wiki.getLedeAsPlainText(List.of(page)).get(0);
            
            // page history (top, bottom)
            Wiki.RequestHelper rh = wiki.new RequestHelper()
                .limitedTo(10);
            // List<Wiki.Revision> tophistory = wiki.getPageHistory(page, rh);
            rh = rh.reverse(true);
            List<Wiki.Revision> bottomhistory = wiki.getPageHistory(page, rh);
            
            // creator 
            // some wikis still allow article creation by IPs
            String username = bottomhistory.get(0).getUser();
            Wiki.User creator = null;
            if (username != null)
                creator = wiki.getUsers(List.of(username)).get(0);
            Collections.reverse(bottomhistory);
            
            List<String> tablerows = List.of(wiki.getDomain(),
                "[" + wiki.getPageUrl(page) + " " + page + "]",
                bottomhistory.get(0).getTimestamp().toString(),
                Users.generateWikitextSummaryLinksShort(username) 
                    + "<br>([" + wiki.getPageUrl("User:" + username) + " foreign user] &middot; "
                    + "[" + wiki.getPageUrl("User talk:" + username) + " foreign talk] &middot; "
                    + "[" + wiki.getPageUrl("Special:Contributions/" + username) + " foreign contribs] &middot; "
                    + "[[m:Special:CentralAuth/" + username + "|CA]])",
                creator == null ? "0" : String.valueOf(creator.countEdits()),
                snippet == null ? "null" : snippet);
            System.out.println(WikitextUtils.addTableRow(tablerows));
        }
        System.out.println("|}");
        
        // output deletion logs
        System.out.println("==Cross-wiki deletion log==");
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
    
    /**
     *  Deduces whether a Wikidata item has had corresponding pages deleted, 
     *  and if so returns the wikis where a deletion has occurred and the deletion 
     *  logs for that page.
     *  @param wditem the Wikidata item to look up
     *  @return a map: the wiki and the corresponding deletion log entries
     *  @throws Exception if a network error occurs
     */
    public static Map<Wiki, List<Wiki.LogEntry>> fetchCrossWikiDeletionLogs(String wditem) throws Exception
    {
        // Implemention note: deletion removes a page from its corresponding
        // Wikidata item with a specific edit summary. However, edit summaries 
        // on Wikidata are NOT what you see in the GUI. Instead they look like this:
        //
        // /* clientsitelink-remove:1||enwiki */ DeletedPageName
        //
        // WikiBase does some substitution before you see it. The API does not.
        List<Wiki.Revision> wdhistory = wikidata.getPageHistory(wditem, null);
        Map<Wiki, List<Wiki.LogEntry>> ret = new HashMap<>();
        for (Wiki.Revision revision : wdhistory)
        {
            String comment = revision.getComment();
            if (comment.startsWith("/* clientsitelink-remove"))
            {
                int end = comment.indexOf("*/") - 1;
                String dbname = comment.substring(comment.indexOf("||") + 2, end);
                String pagename = comment.substring(end + 3);
                WMFWiki local = WMFWiki.newSessionFromDBName(dbname);
                Wiki.RequestHelper rh = local.new RequestHelper().byTitle(pagename);
                ret.put(local, local.getLogEntries(Wiki.DELETION_LOG, null, rh));
            }
        }
        return ret;
    }
}
