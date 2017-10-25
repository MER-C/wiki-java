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
import javax.security.auth.login.*;
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
    // 2) Date cut off.  (backend)
    // 3) Deleted articles in articleEditorIntersection()
    // 4) Remove minor edits (backend)
    // 5) Require edits to more than X articles
    // 6) Require more than X edits per article
    
    // Worth thinking about:
    // 1) Start from multiple users see articlesEdited()
    // 2) Count/return lists of new users in article histories in 
    //    articlesEdited()
    
    /**
     *  Runs this program.
     *  @param args the command line arguments (see --help below for 
     *  documentation).
     */
    public static void main(String[] args) throws IOException
    {
        String wikidomain = "en.wikipedia.org";
        String user = null;
        String category = null;
        boolean nobot = false, noadmin = false, noanon = false;
        boolean deletedcontribs = false;
        List<String> articlelist = new ArrayList<>();
        
        // parse command line arguments
        if (args.length == 0)
            args = new String[] { "--help" };
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--help":
                    System.out.println("SYNOPSIS:\n\t java org.wikipedia.tools.ArticleEditorIntersection [options] [source] [pages]\n\n"
                        + "DESCRIPTION:\n\tFor a given set of pages, finds the common set of editors and lists the edits made.\n\n"
                        + "\t--help\n\t\tPrints this screen and exits.\n\n"
                        + "Options:\n"
                        + "\t--wiki wiki\n\t\tFetch data from wiki (default: en.wikipedia.org).\n"
                        + "\t--nobot\n\t\tExclude bots from the analysis.\n"
                        + "\t--noadmin\n\t\tExclude admins from the analysis.\n"
                        + "\t--noanon\n\t\tExclude IPs from the analysis.\n"
                        + "Sources of pages:\n"
                        + "\t--category category\n\t\tUse the members of the specified category as the list of articles.\n"
                        + "\t--contribs user\n\t\tUse the list of articles edited by the given user.\n"
                        + "\t--deleted\n\t\tAdds deleted contributions to the analysis (REQUIRES ADMIN LOGIN)\n\n"
                        + "\t--file file\n\t\tRead in the list of articles from the given file.\n");
                    System.exit(0);
                case "--wiki":
                    wikidomain = args[++i];
                    break;
                case "--category":
                    category = args[++i];
                    break;
                case "--file":
                    articlelist = Files.readAllLines(new File(args[++i]).toPath());
                    break;
                case "--contribs":
                    user = args[++i];
                    break;
                case "--nobot":
                    nobot = true;
                    break;
                case "--noadmin":
                    noadmin = true;
                    break;
                case "--noanon":
                    noanon = true;
                    break;
                case "--deleted":
                    deletedcontribs = true;
                    break;
                default:
                    articlelist.add(args[i]);
                    break;
            }
        }
        
        Wiki wiki = Wiki.createInstance(wikidomain);
        String[] articles = null;
        if (articlelist.size() > 0)
            articles = articlelist.toArray(new String[articlelist.size()]);
        
        // grab user contributions
        if (user != null)
        {
            Stream<Wiki.Revision> stuff = Arrays.stream(wiki.contribs(user));
            if (deletedcontribs)
            {
                try
                {
                    // CLI login
                    Console console = System.console();
                    wiki.login(console.readLine("Username: "), console.readPassword("Password: "));
                    stuff = Stream.concat(stuff, Arrays.stream(wiki.deletedContribs(user)));
                }
                catch (FailedLoginException ex)
                {
                    System.err.println("Invalid username or password.");
                    System.exit(1);
                }
                catch (CredentialNotFoundException ex)
                {
                    System.err.println("Permission denied: Cannot retrieve deleted revisions.");
                    System.exit(1);
                }
            }
            articles = stuff.map(Wiki.Revision::getPage)
                .distinct()
                .toArray(String[]::new);
        }
        // grab from category
        if (category != null)
            articles = wiki.getCategoryMembers(category);
        
        if (articles.length == 0)
        {
            System.err.println("Input has no articles!");
            System.exit(0);
        }
        
        Map<String, List<Wiki.Revision>> data = articleEditorIntersection(wiki, articles, noadmin, nobot, noanon);
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
    
    /**
     *  Given a set of users of <tt>wiki</tt>, find the list of articles they 
     *  have edited.
     * 
     *  @param wiki the wiki to fetch content from
     *  @param users the list of users on <tt>wiki</tt>to fetch contributions for
     *  @param deletedcontribs include deleted contributions (REQUIRES ADMIN
     *  PRIVILEGES)
     *  @param nominor exclude minor edits
     *  @throws SecurityException if permission is denied when getting deleted 
     *  contribs
     *  @return a map with page => list of revisions made
     */
    public static Map<String, List<Wiki.Revision>> articlesEdited(Wiki wiki, 
        String[] users, boolean deletedcontribs, boolean nominor)
    {
        // fetch the list of (deleted) edits
        Stream<Wiki.Revision> revstream = Arrays.stream(users)
            .flatMap(user -> 
            {
                try
                {
                    Stream<Wiki.Revision> str = Arrays.stream(wiki.contribs(user));
                    if (deletedcontribs)
                        str = Stream.concat(str, Arrays.stream(wiki.deletedContribs(user)));
                    return str;
                }
                catch (IOException ex)
                {
                    return Arrays.stream(new Wiki.Revision[0]);
                }
                catch (CredentialNotFoundException ex)
                {
                    throw new SecurityException(ex);
                }
            });
        // we cannot filter by sizediff here, the MediaWiki API does not return
        // this information for deleted revisions
        if (nominor)
            revstream = revstream.filter(rev -> !rev.isMinor());
        return revstream.collect(Collectors.groupingBy(Wiki.Revision::getPage));
    }
}
