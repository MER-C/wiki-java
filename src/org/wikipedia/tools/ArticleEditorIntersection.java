/**
 *  @(#)AllWikiLinksearch.java 0.03 26/12/2016
 *  Copyright (C) 2011 - 2017 MER-C
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
import java.util.*;
import java.util.stream.*;
import org.wikipedia.Wiki;

/**
 *  This tool finds the common set of editors and corresponding revisions of a 
 *  set of wiki pages. Useful for sockpuppet analysis. Servlet version (with 
 *  slightly limited functionality) is available <a 
 *  href="https://wikipediatools.appspot.com/editorintersection.jsp">here</a>.
 * 
 *  @version 0.01
 *  @author MER-C
 */
public class ArticleEditorIntersection
{
    // TODO
    // 1) Make offline mode fully functional, as opposed to just a test
    // 2) Add category option to servlets
    // 3) Add category option to offline mode
    // 4) Collapsible revision lists
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
        String[] articles = enWiki.getCategoryMembers("Category:Indian general election, 2009", Wiki.MAIN_NAMESPACE);
        Map<String, List<Wiki.Revision>> data = articleEditorIntersection(enWiki, articles, true, true, false);
        for (Map.Entry<String, List<Wiki.Revision>> entry : data.entrySet())
        {
            System.out.print(entry.getKey());
            System.out.println(" => {");
            
            // group by article
            Map<String, List<Wiki.Revision>> grouppage = entry.getValue()
                .stream()
                .collect(Collectors.groupingBy(Wiki.Revision::getPage));
            
            for (Map.Entry<String, List<Wiki.Revision>> entry2 : grouppage.entrySet())
            {
                System.out.print("\t" + entry2.getKey());
                System.out.print(" => ");
                for (Wiki.Revision rev : entry2.getValue())
                    System.out.print(rev.getRevid() + " ");
                System.out.println();
            }
            System.out.println("}");
        }
    }
    
    /**
     *  Finds the set of common editors for a given set of <tt>articles</tt> on 
     *  <tt>wiki</tt>.
     * 
     *  @param wiki the wiki to fetch content from
     *  @param articles a list of pages to analyze for common editors
     *  @param noadmin exclude admins from the analysis
     *  @param nobot exclude flagged bots from the analysis
     *  @param noanon exclude IPs from the analysis
     *  @return a map with user => list of revisions made
     *  @throws IOException if a network error occurs
     */
    public static Map<String, List<Wiki.Revision>> articleEditorIntersection(Wiki wiki, 
        String[] articles, boolean noadmin, boolean nobot, boolean noanon) throws IOException
    {
        // fetch histories and group by user
        Map<String, List<Wiki.Revision>> results = Arrays.stream(articles).flatMap(article -> 
        {
            try
            {
                return Arrays.stream(wiki.getPageHistory(article));
            }
            catch (IOException ex)
            {
                return Arrays.stream(new Wiki.Revision[0]);
            }
        }).collect(Collectors.groupingBy(Wiki.Revision::getUser));
        
        Iterator<Map.Entry<String, List<Wiki.Revision>>> iter = results.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, List<Wiki.Revision>> item = iter.next();
            List<Wiki.Revision> list = item.getValue();
            // throw out any account that appears in only one revision
            if (list.size() < 2)
            {
                iter.remove();
                continue;
            }
            // throw out any account that appears in only one article
            Set<String> allpages = list.stream()
                .map(Wiki.Revision::getPage)
                .collect(Collectors.toCollection(HashSet::new));
            if (allpages.size() < 2)
                iter.remove();
        } 

        // remove admins, bots and anons if necessary
        Set<String> keyset = results.keySet();
        if (noadmin || nobot || noanon)
        {
            String[] users = keyset.toArray(new String[0]);
            Map<String, Object>[] userinfo = wiki.getUserInfo(users);
            for (int i = 0; i < users.length; i++)
            {
                // skip IPs because getUserInfo returns null
                if (userinfo[i] == null)
                {
                    if (noanon)
                        keyset.remove(users[i]);
                    continue;
                }
                
                String[] groups = (String[])userinfo[i].get("groups");
                for (String group : groups)
                {
                    if (group.equals("sysop") && noadmin)
                    {
                        keyset.remove(users[i]);
                        continue;
                    }
                    if (group.equals("bot") && nobot)
                    {
                        keyset.remove(users[i]);
                        continue;
                    }
                }
            }
        }
        
        return results;
    }
}
