/**
 *  @(#)Users.java 0.01 23/06/2018
 *  Copyright (C) 2018-20XX MER-C and contributors
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

package org.wikipedia;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.security.auth.login.FailedLoginException;

/**
 *  Utility methods for wiki users.
 *  @author MER-C
 *  @version 0.01
 */
public class Users
{
    private final Wiki wiki;
    private final Pages pageutils;
    
    private Users(Wiki wiki)
    {
        this.wiki = wiki;
        pageutils = Pages.of(wiki);
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Users of(Wiki wiki)
    {
        return new Users(wiki);
    }
    
    /**
     *  Creates user links in HTML of the form <samp>User (talk &middot;
     *  contribs)</samp>
     *  @param username the username
     *  @return the generated HTML
     *  @see #generateHTMLSummaryLinks(String)
     */
    public String generateHTMLSummaryLinksShort(String username)
    {
        return pageutils.generatePageLink("User:" + username, username) + " ("
            +  pageutils.generatePageLink("User talk:" + username, "talk") + " &middot; "
            +  pageutils.generatePageLink("Special:Contributions/" + username, "contribs") + ")";
    }
    
    /**
     *  Creates user links in HTML of the form <samp>User (talk | contribs | 
     *  deletedcontribs | block | block log)</samp>
     *  @param username the username
     *  @return the generated HTML
     *  @see #generateWikitextSummaryLinks(String)
     *  @see #generateHTMLSummaryLinksShort(String)
     */
    public String generateHTMLSummaryLinks(String username)
    {
        String indexPHPURL = wiki.getIndexPhpUrl();
        username = WikitextUtils.recode(username);
        String userenc = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return pageutils.generatePageLink("User:" + username, username) + " ("
            +  pageutils.generatePageLink("User talk:" + username, "talk") + " | "
            +  pageutils.generatePageLink("Special:Contributions/" + username, "contribs") + " | "
            +  pageutils.generatePageLink("Special:DeletedContributions/" + username, "deleted contribs") + " | "
            +  "<a href=\"" + indexPHPURL + "?title=Special:Log&user=" + userenc + "\">logs</a> | "
            +  pageutils.generatePageLink("Special:Block/" + username, "block") + " | "
            +  "<a href=\"" + indexPHPURL + "?title=Special:Log&type=block&page=User:" + userenc + "\">block log</a>)";
    }
    
    /**
     *  Creates user links in wikitext of the form <samp>User (talk | contribs | 
     *  deletedcontribs | block | block log)</samp>
     *  @param username the username
     *  @return the generated wikitext
     *  @see #generateHTMLSummaryLinks(String) 
     */
    public static String generateWikitextSummaryLinks(String username)
    {
        String userenc = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return "* [[User:" + username + "|" + username + "]] (" 
            +  "[[User talk:" + username + "|talk]] | "
            +  "[[Special:Contributions/" + username + "|contribs]] | "
            +  "[[Special:DeletedContributions/" + username + "|deleted contribs]] | "
            +  "[{{fullurl:Special:Log|user=" + userenc + "}} logs] | "
            +  "[[Special:Block/" + username + "|block]] | "
            +  "[{{fullurl:Special:Log|type=block&page=User:" + userenc + "}} block log])\n";
    }

    /**
     *  Creates user links in wikitext of the form <samp>User (talk &middot; 
     *  contribs)</samp>.
     *  @param username the username
     *  @return the generated wikitext
     *  @see #generateHTMLSummaryLinksShort(String) 
     */
    public static String generateWikitextSummaryLinksShort(String username)
    {
        return "[[User:" + username + "|" + username + "]] (" 
            +  "[[User talk:" + username + "|talk]] &middot; "
            +  "[[Special:Contributions/" + username + "|contribs]])";
    }
    
    /**
     *  Returns a list of pages created by this user in the given namespaces
     *  with full revision metadata.
     *  @param users the users to fetch page creations for
     *  @param rh a {@link Wiki.RequestHelper} object that is passed to {@link
     *  Wiki#contribs(List, String, Wiki.RequestHelper)}
     *  @return the list of pages created by this user with revision metadata
     *  for the corresponding revisions
     *  @throws IOException if a network error occurs
     *  @see #createdPagesWithText(List, Wiki.RequestHelper) 
     */
    public List<Wiki.Revision> createdPages(List<String> users, Wiki.RequestHelper rh) throws IOException
    {
        rh = Objects.requireNonNullElse(rh, wiki.new RequestHelper())
            .filterBy(Map.of("new", Boolean.TRUE));
        List<List<Wiki.Revision>> contribs = wiki.contribs(users, null, rh);
        List<Wiki.Revision> ret = new ArrayList<>();
        for (List<Wiki.Revision> rev : contribs)
            ret.addAll(rev);
        return ret;
    }
    
    /**
     *  Fetches the pages created by the given users and the text of the current
     *  revision of those pages.
     *  @param users the users to fetch page creations for
     *  @param rh a {@link Wiki.RequestHelper} object that is passed to {@link
     *  Wiki#contribs(List, String, Wiki.RequestHelper)}
     *  @return a map containing revision the page was created &#8594; current 
     *  text of that page
     *  @throws IOException if a network error occurs
     *  @see #createdPages(List, Wiki.RequestHelper) 
     */
    public Map<Wiki.Revision, String> createdPagesWithText(List<String> users, Wiki.RequestHelper rh) throws IOException
    {
        rh = Objects.requireNonNullElse(rh, wiki.new RequestHelper())
            .filterBy(Map.of("new", Boolean.TRUE));
        List<List<Wiki.Revision>> contribs = wiki.contribs(users, null, rh);
        
        // get text of all pages
        List<Wiki.Revision> temp = new ArrayList<>();
        for (List<Wiki.Revision> rev : contribs)
            temp.addAll(rev);
        List<String> pages = new ArrayList<>();
        for (Wiki.Revision revision : temp)
            pages.add(revision.getTitle());
        List<String> pagetexts = wiki.getPageText(pages);
        Map<Wiki.Revision, String> ret = new HashMap<>();
        for (int i = 0; i < temp.size(); i++)
        {
            Wiki.Revision revision = temp.get(i);
            ret.putIfAbsent(revision, pagetexts.get(i));
        }
        return ret;
    }
    
    /**
     *  Generates a CLI login prompt and logs in if successful. Exits with exit
     *  code 1 if login is unsuccessful or code 2 if a network error occurs.
     */
    public void cliLogin()
    {
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
        catch (IOException ex)
        {
            System.err.println("A network error occurred.");
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
