/**
 *  @(#)Pages.java 0.01 31/03/2018
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
import java.net.URLEncoder;
import java.util.*;
import javax.security.auth.login.*;

/**
 *  Utility methods for lists of wiki pages.
 *  @author MER-C
 *  @version 0.01
 */
public class Pages 
{
    private final Wiki wiki;
    
    private Pages(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Pages of(Wiki wiki)
    {
        return new Pages(wiki);
    }
    
    /**
     *  Parses a wikitext list of links into its individual elements. Such a 
     *  list should be in the form:
     *
     *  <pre>
     *  * [[Main Page]]
     *  * [[Wikipedia:Featured picture candidates]]
     *  * [[:File:Example.png]]
     *  * [[Cape Town#Economy]]
     *  * [[Link|with description]]
     *  </pre>
     *
     *  in which case <samp>{ "Main Page", "Wikipedia:Featured picture
     *  candidates", "File:Example.png", "Cape Town#Economy", "Link" }</samp> is
     *  the return value. Numbered lists are allowed. Nested lists are 
     *  flattened. Link descriptions are removed.
     *
     *  @param wikitext a wikitext list of pages as described above
     *  @see #toWikitextList(Iterable, boolean)
     *  @return a list of parsed titles
     *  @since Wiki.java 0.11
     */
    public static List<String> parseWikitextList(String wikitext)
    {
        String[] lines = wikitext.split("\n");
        List<String> titles = new ArrayList<>();
        for (String line : lines)
        {
            int wikilinkstart = line.indexOf("[[");
            int wikilinkend = line.indexOf("]]");
            if (wikilinkstart < 0 || wikilinkend < 0)
                continue;
            titles.add(ParserUtils.parseWikilink(line.substring(wikilinkstart, wikilinkend + 2)).get(0));
        }
        return titles;
    }

    /**
     *  Exports a list of pages, say, generated from one of the query methods to
     *  wikitext. Does the exact opposite of {@link #parseWikitextList(String)},
     *  i.e. {@code { "Main Page", "Wikipedia:Featured picture candidates",
     *  "File:Example.png" }} becomes the string:
     *
     *  <pre>
     *  *[[:Main Page]]
     *  *[[:Wikipedia:Featured picture candidates]]
     *  *[[:File:Example.png]]
     *  </pre>
     * 
     *  If a <var>numbered</var> list is desired, the output is:
     * 
     *  <pre>
     *  #[[:Main Page]]
     *  #[[:Wikipedia:Featured picture candidates]]
     *  #[[:File:Example.png]]
     *  </pre>
     *
     *  @param pages a list of page titles
     *  @param numbered whether this is a numbered list
     *  @return the list, exported as wikitext
     *  @see #parseWikitextList(String)
     *  @since Wiki.java 0.14
     */
    public static String toWikitextList(Iterable<String> pages, boolean numbered)
    {
        StringBuilder buffer = new StringBuilder(10000);
        for (String page : pages)
        {
            buffer.append(numbered ? "#[[:" : "*[[:");
            buffer.append(page);
            buffer.append("]]\n");
        }
        return buffer.toString();
    }
    
    /**
     *  For a given list of pages, determine whether the supplied external links
     *  are present in the page.
     *  @param data a Map of title &#8594; list of links to check
     *  @return a Map of title &#8594; link checked &#8594; whether it is in 
     *  that page
     *  @throws IOException if a network error occurs
     */
    public Map<String, Map<String, Boolean>> containExternalLinks(Map<String, List<String>> data) throws IOException
    {
        List<List<String>> pagelinks = wiki.getExternalLinksOnPage(new ArrayList<>(data.keySet()));
        int counter = 0;
        Map<String, Map<String, Boolean>> ret = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : data.entrySet())
        {        
            List<String> addedlinks = entry.getValue();
            List<String> currentlinks = pagelinks.get(counter);
            Map<String, Boolean> stillthere = new HashMap<>();
            for (int i = 0; i < addedlinks.size(); i++)
            {
                String url = addedlinks.get(i);
                stillthere.put(url, currentlinks.contains(url));
            }
            ret.put(entry.getKey(), stillthere);
            counter++;
        }
        return ret;
    }
    
    /**
     *  Generates summary page links of the form Page (edit | talk | history | logs)
     *  as HTML. Doesn't support talk pages yet.
     *  @param page the page to generate links for
     *  @return generated HTML
     */
    public String generateSummaryLinks(String page)
    {
        if (wiki.namespace(page) % 2 == 1)
            return ""; // no talk pages yet
        try
        {
            String indexPHPURL = wiki.getIndexPHPURL();
            String pageenc = URLEncoder.encode(page, "UTF-8");
            
            return "<a href=\"" + wiki.getPageURL(page) + "\">" + page + "</a> ("
                + "<a href=\"" + indexPHPURL + "?title=" + pageenc + "&action=edit\">edit</a> | "
                + "<a href=\"" + wiki.getPageURL(wiki.getTalkPage(page)) + "\">talk</a> | "
                + "<a href=\"" + indexPHPURL + "?title=" + pageenc + "&action=history\">history</a> | "
                + "<a href=\"" + indexPHPURL + "?title=Special:Log&page=" + pageenc + "\">logs</a>)";
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex); // seriously?
        }
    }
    
    /**
     *  Deletes all supplied <var>pages</var> and their associated talk pages.
     *  Requires admin privileges.
     * 
     *  @param pages a list of pages to delete
     *  @param reason the reason for deletion
     *  @param talkReason the reason to use when deleting the relevant talk pages
     *  Does not delete talk pages if {@code null}.
     *  @throws LoginException if one does not possess credentials to delete
     *  @return an array containing pages we were unable to delete
     *  @author Fastily
     */
    public List<String> massDelete(Iterable<String> pages, String reason, String talkReason) throws LoginException
    {
        ArrayList<String> cantdelete = new ArrayList<>();
        for (String page : pages)
        {
            try
            {
                wiki.delete(page, reason);
            }
            catch (IOException | UncheckedIOException ex)
            {
                cantdelete.add(page);
                continue;
            }

            if (talkReason != null)
            {
                try
                {
                    wiki.delete(wiki.getTalkPage(page), talkReason);
                }
                catch (IOException | UncheckedIOException ex)
                {
                    cantdelete.add(page);
                }
            }
        }
        return cantdelete;
    }
}
