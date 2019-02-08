/**
 *  @(#)ArticleEditorIntersector.java 0.02 28/01/2018
 *  Copyright (C) 2011 - 2018 MER-C
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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.*;
import javax.security.auth.login.*;
import org.wikipedia.*;

/**
 *  This tool finds the common set of editors and corresponding revisions of a 
 *  set of wiki pages. Useful for sockpuppet analysis.
 * 
 *  @version 0.02
 *  @see <a href="https://wikipediatools.appspot.com/editorintersection.jsp">
 *  Article-editor intersection (online version)</a> 
 *  @author MER-C
 */
public class ArticleEditorIntersector
{
    // TODO
    // 1) Make offline mode print out more than revids.
    // 2) Require edits to more than X articles
    // 3) Require more than X edits per article
    
    // Worth thinking about:
    // 1) Start from multiple users see articlesEdited()
    // 2) Count/return lists of new users in article histories in 
    //    articlesEdited()
    
    private final Wiki wiki;
    private boolean adminmode, nominor, noreverts;
    private OffsetDateTime earliestdate, latestdate;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments (see code for documentation)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        // parse command line arguments
        if (args.length == 0)
            args = new String[] { "--help" };
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.ArticleEditorIntersector", "[options] [source] [pages]")
            .description("For a given set of pages, finds the common set of editors and lists the edits made.")
            .addHelp()
            .addVersion("ArticleEditorIntersector v0.02\n" + CommandLineParser.GPL_VERSION_STRING)
            .addSection("Global options:")
            .addSingleArgumentFlag("--wiki", "wiki", "Fetch data from wiki (default: en.wikipedia.org).")
            .addBooleanFlag("--adminmode", "Fetch deleted edits (REQUIRES ADMIN LOGIN).")
            .addBooleanFlag("--nominor", "Ignore minor edits.")
            .addBooleanFlag("--noreverts", "Ignore reverts.")
            .addSingleArgumentFlag("--editsafter", "date", "Include edits made after this date (ISO format).")
            .addSingleArgumentFlag("--editsbefore", "date", "Include edits made before this date (ISO format).")
            .addSection("Options:")
            .addBooleanFlag("--nobot", "Exclude bots from the analysis.")
            .addBooleanFlag("--noadmin", "Exclude admins from the analysis.")
            .addBooleanFlag("--noanon", "Exclude IPs from the analysis.")
            .addSection("Sources of pages:")
            .addSingleArgumentFlag("--category", "category", "Use the members of the specified category as the list of articles.")
            .addSingleArgumentFlag("--contribs", "user", "Use the list of articles edited by the given user.")
            .addSingleArgumentFlag("--file", "file", "Read in the list of articles from the given file.")
            .parse(args);
        
        Wiki wiki = Wiki.createInstance(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        String category = parsedargs.get("--category");
        String user = parsedargs.get("--contribs");
        boolean adminmode = parsedargs.containsKey("--adminmode");
        boolean nominor = parsedargs.containsKey("--nominor");
        boolean noreverts = parsedargs.containsKey("--noreverts");
        boolean nobot = parsedargs.containsKey("--nobot");
        boolean noadmin = parsedargs.containsKey("--noadmin");
        boolean noanon = parsedargs.containsKey("--noanon");
        String defaultstring = parsedargs.get("default");
        String earliestdatestring = parsedargs.get("--editsafter");
        String latestdatestring = parsedargs.get("--editsbefore");
        String filename = parsedargs.get("--file");
        
        OffsetDateTime editsafter = (earliestdatestring == null) ? null : OffsetDateTime.parse(earliestdatestring);
        OffsetDateTime editsbefore = (latestdatestring == null) ? null : OffsetDateTime.parse(latestdatestring);
        List<String> articles = null;
        if (defaultstring != null)
            articles = Arrays.asList(defaultstring.split("\\s"));
        if (filename != null)
            articles = Files.readAllLines(Paths.get(filename));
        
        ArticleEditorIntersector aei = new ArticleEditorIntersector(wiki);
        aei.setIgnoringMinorEdits(nominor);
        aei.setDateRange(editsafter, editsbefore);
        aei.setIgnoringReverts(noreverts);
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
            Wiki.RequestHelper rh = wiki.new RequestHelper()
                .withinDateRange(editsafter, editsbefore);
            Stream<Wiki.Revision> stuff = wiki.contribs(user, rh).stream();
            if (adminmode)
            {
                try
                {
                    stuff = Stream.concat(stuff, wiki.deletedContribs(user, rh).stream());
                }
                catch (SecurityException ex)
                {
                    System.err.println("Permission denied: Cannot retrieve deleted revisions.");
                    System.exit(2);
                }
            }
            if (nominor)
                stuff = stuff.filter(rev -> !rev.isMinor());
            articles = stuff.map(Wiki.Revision::getTitle)
                .distinct()
                .collect(Collectors.toList());
        }
        // grab from category
        if (category != null)
            articles = Arrays.asList(wiki.getCategoryMembers(category));
        
        if (articles.isEmpty())
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
                .collect(Collectors.groupingBy(Wiki.Revision::getTitle));
            grouppage.forEach((article, articleedits) ->
            {
                System.out.print("\t" + article);
                System.out.print(" => ");
                for (Wiki.Revision rev : articleedits)
                    System.out.print(rev.getID() + " ");
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
     *  Checks whether reverts are ignored (a revert is defined as a revision
     *  that has a SHA-1 equal to a previous revision on the same page).
     *  @return whether reverts are ignored
     *  @see #setIgnoringReverts(boolean) 
     *  @see Revisions#removeReverts(List)
     *  @since 0.02
     */
    public boolean isIgnoringReverts()
    {
        return noreverts;
    }
    
    /**
     *  Sets whether reverts will be ignored (a revert is defined as a revision
     *  that has a SHA-1 equal to a previous revision on the same page).
     *  @param ignorereverts whether reverts will be ignored
     *  @see #isIgnoringReverts()
     *  @see Revisions#removeReverts(List)
     *  @since 0.02
     */
    public void setIgnoringReverts(boolean ignorereverts)
    {
        noreverts = ignorereverts;
    }
    
    /**
     *  Sets the dates/times at which surveys start and finish; no edits will be 
     *  returned outside this range. The default, {@code null}, indicates no
     *  bound.
     *  @param earliest the desired start date/time
     *  @param latest the desired end date/time
     *  @throws IllegalArgumentException if <var>earliest</var> is after
     *  <var>latest</var>
     *  @see #getEarliestDateTime() 
     *  @see #getLatestDateTime() 
     *  @since 0.02
     */
    public void setDateRange(OffsetDateTime earliest, OffsetDateTime latest)
    {
        if (earliest != null && latest != null && earliest.isAfter(latest))
            throw new IllegalArgumentException("Date range is reversed.");
        earliestdate = earliest;
        latestdate = latest;
    }
    
    /**
     *  Gets the date/time at which surveys start; no edits will be returned 
     *  before then.
     *  @return (see above)
     *  @see #setDateRange(OffsetDateTime, OffsetDateTime)  
     *  @since 0.02
     */
    public OffsetDateTime getEarliestDateTime()
    {
        return earliestdate;
    }
    
    /**
     *  Gets the date at which surveys finish. 
     *  @return (see above)
     *  @see #setDateRange(OffsetDateTime, OffsetDateTime)  
     *  @since 0.02
     */
    public OffsetDateTime getLatestDateTime()
    {
        return latestdate;
    }
    
    /**
     *  Finds the set of common editors for a given set of <var>articles</var>
     *  between {@link #getEarliestDateTime()} and {@link #getLatestDateTime()}.
     *  Includes deleted edits if {@link #isUsingAdminPrivileges()} is 
     *  {@code true} and ignores minor edits if {@link #isIgnoringMinorEdits()}
     *  is {@code true}.
     * 
     *  @param articles a list of at least two unique pages to analyze for 
     *  common editors
     *  @param noadmin exclude admins from the analysis
     *  @param nobot exclude flagged bots from the analysis
     *  @param noanon exclude IPs from the analysis
     *  @return a map with user &#8594; list of revisions made. If the total
     *  number of pages does not exceed one after applying all exclusions and 
     *  removing revisions with deleted/suppressed usernames and pages with no
     *  (deleted) history or there is no intersection, return an empty map.
     *  @throws IOException if a network error occurs
     *  @throws IllegalArgumentException if {@code articles.length < 2} after
     *  duplicates (as in {@code String.equals}) and Special/Media pages are removed
     */
    public Map<String, List<Wiki.Revision>> intersectArticles(Iterable<String> articles, 
        boolean noadmin, boolean nobot, boolean noanon) throws IOException
    {
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .withinDateRange(earliestdate, latestdate);
                
        // remove duplicates and fail quickly if less than two pages
        Set<String> pageset = new HashSet<>();
        for (String article : articles)
            if (wiki.namespace(article) >= 0) // remove Special: and Media: pages
                pageset.add(article);
        if (pageset.size() < 2)
            throw new IllegalArgumentException("At least two articles are needed to derive a meaningful intersection.");
        
        // fetch histories and group by user
        Stream<Wiki.Revision> revstream = pageset.stream()
            .flatMap(article ->  
            {
                Stream<Wiki.Revision> str = Stream.empty();
                try
                {
                    str = wiki.getPageHistory(article, rh).stream();
                    if (adminmode)
                        str = Stream.concat(str, wiki.getDeletedHistory(article, rh).stream());
                }
                catch (IOException | SecurityException ignored)
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
        if (noreverts)
            revstream = Revisions.removeReverts(revstream.collect(Collectors.toList())).stream();
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
                .map(Wiki.Revision::getTitle)
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
            List<String> usernames = new ArrayList<>(keyset);
            List<Wiki.User> userinfo = wiki.getUsers(usernames);
            for (int i = 0; i < userinfo.size(); i++)
            {
                // skip IPs because getUsers returns null
                Wiki.User user = userinfo.get(i);
                if (user == null)
                {
                    if (noanon)
                        keyset.remove(usernames.get(i));
                    continue;
                }
                
                for (String group : user.getGroups())
                {
                    if (group.equals("sysop") && noadmin)
                    {
                        keyset.remove(usernames.get(i));
                        continue;
                    }
                    if (group.equals("bot") && nobot)
                    {
                        keyset.remove(usernames.get(i));
                        continue;
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     *  Given a set of <var>users</var>, find the list of articles they have 
     *  edited between {@link #getEarliestDateTime()} and {@link #getLatestDateTime()}.
     *  Includes deleted contributions if {@link #isUsingAdminPrivileges()} is 
     *  {@code true} and ignores minor edits if {@link #isIgnoringMinorEdits()}
     *  is {@code true}.
     * 
     *  @param users the list of users to fetch contributions for
     *  @return a map with page &#8594; list of revisions made
     *  @throws IOException if a network error occurs
     */
    public Map<String, List<Wiki.Revision>> intersectEditors(List<String> users) throws IOException
    {
        Map<String, Boolean> options = new HashMap<>();
        if (nominor)
            options.put("minor", Boolean.FALSE);
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .withinDateRange(earliestdate, latestdate)
            .filterBy(options);
                
        // fetch the list of (deleted) edits
        List<List<Wiki.Revision>> revisions = wiki.contribs(users, null, rh);
        Stream<Wiki.Revision> revstream = revisions.stream().flatMap(List::stream);
        
        if (adminmode)
        {
            Stream<Wiki.Revision> revstream2 = users.stream().flatMap(user -> 
            {
                try
                {
                    return wiki.deletedContribs(user, rh).stream();
                }
                catch (IOException | SecurityException ex)
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
        // this information for page histories or deleted revisions
        if (nominor)
            revstream = revstream.filter(rev -> !rev.isMinor());
        return revstream.collect(Collectors.groupingBy(Wiki.Revision::getTitle));
    }
}
