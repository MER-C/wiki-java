/**
 *  @(#)Pages.java 0.01 31/03/2018
 *  Copyright (C) 2018-20XX MER-C
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

import java.util.*;
import java.util.StringTokenizer;

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
     *  for methods that make queries).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Pages bindTo(Wiki wiki)
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
     *  </pre>
     *
     *  in which case <samp>{ "Main Page", "Wikipedia:Featured picture
     *  candidates", "File:Example.png", "Cape Town#Economy" }</samp> is the
     *  return value. Numbered lists are allowed. Nested lists are flattened.
     *
     *  @param wikitext a wikitext list of pages as described above
     *  @see #toWikitextList(Iterable, boolean)
     *  @return an array of the page titles
     *  @since Wiki.java 0.11
     */
    public static List<String> parseWikitextList(String wikitext)
    {
        StringTokenizer tokenizer = new StringTokenizer(wikitext, "[]");
        ArrayList<String> titles = new ArrayList<>(667);
        tokenizer.nextToken(); // skip the first token
        while (tokenizer.hasMoreTokens()) 
        {
            String token = tokenizer.nextToken();
            // skip any containing new lines or double letters
            if (token.contains("\n") || token.isEmpty())
                continue;
            // trim the starting colon, if present
            if (token.startsWith(":"))
                token = token.substring(1);
            titles.add(token);
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
}
