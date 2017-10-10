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
import java.nio.file.*;
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
    // 1) Make offline mode print out more than revids.
    // 2) Article links
    // 3) Date cut off.
    // 4) Admin mode -- deleted contributions and articles
    
    /**
     *  Runs this program.
     *  @param args the command line arguments (see --help below for 
     *  documentation).
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
        String[] articles;
        
        // parse command line arguments
        if (args.length == 0)
            args = new String[] { "--help" };
        switch (args[0])
        {
            case "--help":
                System.out.println("SYNOPSIS:\n\t java org.wikipedia.tools.AllWikiLinksearch [options] [articles]\n\n"
                    + "DESCRIPTION:\n\tSearches Wikimedia projects for links.\n\n"
                    + "\t--help\n\t\tPrints this screen and exits.\n"
                    + "\t--category category\n\t\tUse the members of the specified category as the list of articles.\n"
                    + "\t--contribs user\n\t\tUse the list of articles edited by the given user.\n"
                    + "\t--file file\n\t\tRead in the list of articles from the given file.\n");
                System.exit(0);
            case "--category":
                articles = enWiki.getCategoryMembers(args[1]);
                break;
            case "--file":
                List<String> temp = Files.readAllLines(new File(args[1]).toPath());
                articles = temp.toArray(new String[temp.size()]);
                break;
            case "--contribs":
                articles = Arrays.stream(enWiki.contribs(args[1]))
                    .map(Wiki.Revision::getPage)
                    .distinct()
                    .toArray(String[]::new);
                break;
            default:
                articles = args;
                break;
        }
        
        if (articles.length == 0)
        {
            System.err.println("Input has no articles!");
            System.exit(0);
        }
        
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
