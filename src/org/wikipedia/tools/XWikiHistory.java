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
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        // expected: args[0] language, args[1] = article
        WMFWiki firstwiki = WMFWiki.newSession(args[0] + ".wikipedia.org");
        Map<String, String> interwikis = firstwiki.getInterWikiLinks(args[1]);
        Map<WMFWiki, String> wikiarticles = new LinkedHashMap<>();
        wikiarticles.put(firstwiki, args[1]);
        for (var entry : interwikis.entrySet())
        {
            WMFWiki new_wiki = WMFWiki.newSession(entry.getKey() + ".wikipedia.org");
            wikiarticles.put(new_wiki, entry.getValue());
        }
        
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("! Language !! Page !! Creation date !! Creator !! Creator foreign edit count !! Snippet");        
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
            String username = bottomhistory.get(0).getUser();
            Wiki.User creator = wiki.getUsers(List.of(username)).get(0);
            Collections.reverse(bottomhistory);
            
            List<String> tablerows = List.of(wiki.getDomain(),
                "[" + wiki.getPageUrl(page) + " " + page + "]",
                bottomhistory.get(0).getTimestamp().toString(),
                Users.generateWikitextSummaryLinksShort(username) 
                    + "<br>([" + wiki.getPageUrl("User:" + username) + " foreign user] &middot; "
                    + "[" + wiki.getPageUrl("User talk:" + username) + " foreign talk] &middot; "
                    + "[" + wiki.getPageUrl("Special:Contributions/" + username) + " foreign contribs] &middot; "
                    + "[[m:Special:CentralAuth/" + username + "|CA]])",
                String.valueOf(creator.countEdits()),
                snippet);
            System.out.println(WikitextUtils.addTableRow(tablerows));
        }
        System.out.println("|}");
    }
}
