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
public class ArticleEditorIntersector
{
    // TODO
    // 1) Make offline mode print out more than revids.
    // 2) Date cut off.  (backend)
    // 3) Require edits to more than X articles
    // 4) Require more than X edits per article
    
    // Worth thinking about:
    // 1) Start from multiple users see articlesEdited()
    // 2) Count/return lists of new users in article histories in 
    //    articlesEdited()
    
    private final Wiki wiki;
    private boolean adminmode, nominor;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments (see --help below for 
     *  documentation).
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        String wikidomain = "en.wikipedia.org";
        String user = null;
        String category = null;
        boolean nobot = false, noadmin = false, noanon = false;
        boolean adminmode = false, nominor = false;
        List<String> articlelist = new ArrayList<>();
        
        // parse command line arguments
        if (args.length == 0)
            args = new String[] { "--help" };
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--help":
                    System.out.println("SYNOPSIS:\n\t java org.wikipedia.tools.ArticleEditorIntersector [options] [source] [pages]\n\n"
                        + "DESCRIPTION:\n\tFor a given set of pages, finds the common set of editors and lists the edits made.\n\n"
                        + "\t--help\n\t\tPrints this screen and exits.\n\n"
                        + "Global options:\n"
                        + "\t--wiki wiki\n\t\tFetch data from wiki (default: en.wikipedia.org).\n"
                        + "\t--adminmode\n\t\tFetch deleted edits (REQUIRES ADMIN LOGIN)\n"
                        + "\t--nominor\n\t\tIgnore minor edits.\n\n"
                        + "Options:\n"
                        + "\t--nobot\n\t\tExclude bots from the analysis.\n"
                        + "\t--noadmin\n\t\tExclude admins from the analysis.\n"
                        + "\t--noanon\n\t\tExclude IPs from the analysis.\n"
                        + "Sources of pages:\n"
                        + "\t--category category\n\t\tUse the members of the specified category as the list of articles.\n"
                        + "\t--contribs user\n\t\tUse the list of articles edited by the given user.\n"
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
                case "--adminmode":
                    adminmode = true;
                    break;
                case "--nominor":
                    nominor = true;
                    break;
                default:
                    articlelist.add(args[i]);
                    break;
            }
        }
        
        String[] articles = null;
        if (articlelist.size() > 0)
            articles = articlelist.toArray(new String[articlelist.size()]);
        
        Wiki wiki = Wiki.createInstance(wikidomain);
        ArticleEditorIntersector aei = new ArticleEditorIntersector(wiki);
        aei.setIgnoringMinorEdits(nominor);
        if (adminmode)
        {
            // CLI login
            try
            {
                Console console = System.console();
                wiki.login(console.readLine("Username: "), console.readPassword("Password: "));
            }
            catch (FailedLoginException ex)
            {
                System.err.println("Invalid username or password.");
                System.exit(1);
            }
            aei.setUsingAdminPrivileges(true);
        }
        
        // grab user contributions
        if (user != null)
        {
            Stream<Wiki.Revision> stuff = Arrays.stream(wiki.contribs(user));
            if (adminmode)
            {
                try
                {
                    stuff = Stream.concat(stuff, Arrays.stream(wiki.deletedContribs(user)));
                }
                catch (CredentialNotFoundException ex)
                {
                    System.err.println("Permission denied: Cannot retrieve deleted revisions.");
                    System.exit(2);
                }
            }
            if (nominor)
                stuff = stuff.filter(rev -> !rev.isMinor());
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
            System.exit(3);
        }
        
        Map<String, List<Wiki.Revision>> data = aei.intersectArticles(articles, noadmin, nobot, noanon);
        data.forEach((username, edits) ->
        {
            System.out.print(username);
            System.out.println(" => {");
            
            // group by article
            Map<String, List<Wiki.Revision>> grouppage = edits.stream()
                .collect(Collectors.groupingBy(Wiki.Revision::getPage));
            grouppage.forEach((article, articleedits) ->
            {
                System.out.print("\t" + article);
                System.out.print(" => ");
                for (Wiki.Revision rev : articleedits)
                    System.out.print(rev.getRevid() + " ");
                System.out.println();
            });
            System.out.println("}");
        });
    }
    
    /**
     *  Creates a new intersector instance.
     *  @param wiki the wiki to fetch data from
     */
    public ArticleEditorIntersector(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Returns the wiki that this intersector fetches data from.
     *  @return (see above)
     */
    public Wiki getWiki()
    {
        return wiki;
    }
    
    /**
     *  Sets whether fetching of deleted material will be attempted. You will 
     *  need to login to the wiki with an admin account separately.
     *  @param mode whether to fetch deleted material
     *  @see #isUsingAdminPrivileges()
     */
    public void setUsingAdminPrivileges(boolean mode)
    {
        this.adminmode = mode;
    }
    
    /**
     *  Checks whether fetching of deleted material is attempted.
     *  @return whether fetching of deleted material is attempted
     *  @see #setUsingAdminPrivileges(boolean) 
     */
    public boolean isUsingAdminPrivileges()
    {
        return adminmode;
    }
    
    /**
     *  Sets whether minor edits will be ignored.
     *  @param ignoreminor whether minor edits will be ignored
     *  @see #isIgnoringMinorEdits() 
     */
    public void setIgnoringMinorEdits(boolean ignoreminor)
    {
        nominor = ignoreminor;
    }
    
    /**
     *  Checks whether minor edits are ignored.
     *  @return whether minor edits are ignored
     *  @see #setIgnoringMinorEdits(boolean) 
     */
    public boolean isIgnoringMinorEdits()
    {
        return nominor;
    }
    
    /**
     *  Finds the set of common editors for a given set of <var>articles</var>.
     *  Includes deleted edits if {@link #isUsingAdminPrivileges()} is 
     *  <code>true</code> and ignores minor edits if {@link #isIgnoringMinorEdits()}
     *  is <code>true</code>.
     * 
     *  @param articles a list of at least two unique pages to analyze for 
     *  common editors
     *  @param noadmin exclude admins from the analysis
     *  @param nobot exclude flagged bots from the analysis
     *  @param noanon exclude IPs from the analysis
     *  @return a map with user &#8594; list of revisions made. If the total
     *  number of pages does not exceed one after applying all exclusions and 
     *  removing revisions with deleted/suppressed usernames and pages with no
     *  (deleted) history or are invalid (Special/Media namespaces) or there is
     *  no intersection, return an empty map.
     *  @throws IOException if a network error occurs
     *  @throws IllegalArgumentException if <code><var>articles.length</var> &lt; 2</code>
     *  after duplicates (as in <code>String.equals</code>) are removed
     */
    public Map<String, List<Wiki.Revision>> intersectArticles(String[] articles, 
        boolean noadmin, boolean nobot, boolean noanon) throws IOException
    {
        // remove duplicates and fail quickly if less than two pages
        Set<String> pageset = new HashSet<>(2 * articles.length);
        pageset.addAll(Arrays.asList(articles));
        if (pageset.size() < 2)
            throw new IllegalArgumentException("At least two articles are needed to derive a meaningful intersection.");
        
        // fetch histories and group by user
        Stream<Wiki.Revision> revstream = pageset.stream()
            // remove Special: and Media: pages
            .filter(article -> wiki.namespace(article) >= 0) 
            .flatMap(article ->  
            {
                Stream<Wiki.Revision> str = Stream.empty();
                try
                {
                    str = Arrays.stream(wiki.getPageHistory(article));
                    if (adminmode)
                        str = Stream.concat(str, Arrays.stream(wiki.getDeletedHistory(article)));
                }
                catch (IOException | CredentialNotFoundException ignored)
                {
                    // If a network error occurs when fetching the live history,
                    // that page will be skipped. If a network or privilege error 
                    // occurs when fetching deleted history, that will be skipped
                    // with the live history returned.
                }
                return str;
            }).filter(rev -> rev.getUser() != null); // remove deleted/suppressed usernames
        if (nominor)
            revstream = revstream.filter(rev -> !rev.isMinor());
        Map<String, List<Wiki.Revision>> results = revstream.collect(Collectors.groupingBy(Wiki.Revision::getUser));
        
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
        if (results.isEmpty())
            return results;
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
     *  Given a set of <var>users</var>, find the list of articles they have 
     *  edited. Includes deleted contributions if {@link #isUsingAdminPrivileges()}
     *  is <code>true</code> and ignores minor edits if {@link #isIgnoringMinorEdits()}
     *  is <code>true</code>.
     * 
     *  @param users the list of users to fetch contributions for
     *  @return a map with page &#8594; list of revisions made
     *  @throws IOException if a network error occurs
     */
    public Map<String, List<Wiki.Revision>> intersectEditors(String[] users) throws IOException
    {
        // fetch the list of (deleted) edits
        List<Wiki.Revision>[] revisions = wiki.contribs(users, "", null, null, null);
        Stream<Wiki.Revision> revstream = Arrays.stream(revisions).flatMap(List::stream);
        
        if (adminmode)
        {
            Stream<Wiki.Revision> revstream2 = Arrays.stream(users).flatMap(user -> 
            {
                try
                {
                    return Arrays.stream(wiki.deletedContribs(user));
                }
                catch (IOException | CredentialNotFoundException ex)
                {
                    // If a network or privilege error occurs when fetching 
                    // deleted contributions, that will be skipped with live 
                    // edits returned.
                    return Stream.empty();
                }
            });
            revstream = Stream.concat(revstream, revstream2);
        }
        // we cannot filter by sizediff here, the MediaWiki API does not return
        // this information for deleted revisions
        if (nominor)
            revstream = revstream.filter(rev -> !rev.isMinor());
        return revstream.collect(Collectors.groupingBy(Wiki.Revision::getPage));
    }
}
