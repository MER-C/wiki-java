/**
 *  @(#)ParserUtils.java 0.02 23/12/2016
 *  Copyright (C) 2012-2018 MER-C
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

/**
 *  Utility methods for generating and parsing wikitext that don't belong in
 *  any of the specialist utility classes.
 *  @author MER-C
 *  @version 0.02
 */
public class WikitextUtils
{
    /**
     *  Parses a wikilink into its target and linked text. Example: <kbd>[[Link]]</kbd>
     *  returns {@code { "Link", "Link" }} and <kbd>[[Link|Test]]</kbd> returns
     *  {@code { "Link", "Test" }}. Can also be used to get sortkeys from 
     *  categorizations. Use with caution on file uses because they can
     *  contain their own wikilinks.
     *  @param wikitext the wikitext to parse
     *  @return first element = the target of the link, the second being the
     *  description
     *  @throws IllegalArgumentException if wikitext is not a valid wikilink
     */
    public static List<String> parseWikilink(String wikitext)
    {
        int wikilinkstart = wikitext.indexOf("[[");
        int wikilinkend = wikitext.indexOf("]]", wikilinkstart);
        if (wikilinkstart < 0 || wikilinkend < 0)
            throw new IllegalArgumentException("\"" + wikitext + "\" is not a valid wikilink.");
        // strip escaping of categories and files
        String linktext = wikitext.substring(wikilinkstart + 2, wikilinkend).trim();
        if (linktext.startsWith(":"))
            linktext = linktext.substring(1);
        // check for description, if not there then set it to the target
        int pipe = linktext.indexOf('|');
        if (pipe >= 0)
            return List.of(linktext.substring(0, pipe).trim(), linktext.substring(pipe + 1).trim());
        else
        {
            String temp = linktext.trim();
            return List.of(temp, temp);
        }        
    }
    
    /**
     *  Outputs a row of a wikitext table.
     *  @param entries the individual cells
     *  @return wikitext for that row
     */
    public static String addTableRow(List<String> entries)
    {
        StringBuilder sb = new StringBuilder("|-\n");
        sb.append("| ");
        for (int i = 0; i < entries.size() - 1; i++)
        {
            sb.append(entries.get(i));
            sb.append(" || ");
        }
        sb.append(entries.get(entries.size() - 1));
        sb.append("\n");
        return sb.toString();
    }
        
    /**
     *  Reverse of Wiki.decode()
     *  @param in input string
     *  @return recoded input string
     */
    public static String recode(String in)
    {
        in = in.replace("&", "&amp;");
        in = in.replace("<", "&lt;").replace(">", "&gt;"); // html tags
        in = in.replace("\"", "&quot;");
        in = in.replace("'", "&#039;");
        return in;
    }
    
    /**
     *  Removes HTML comments from the supplied string. 
     *  @param delta the string to strip HTML comments from
     *  @return the string minus HTML comments
     *  @since 0.02
     */
    public static String removeComments(String delta)
    {
        while (delta.contains("<!--"))
        {
            int a = delta.indexOf("<!--");
            int b = delta.indexOf("-->", a);
            if (b < 0)
                delta = delta.substring(0, a);
            else
                delta = delta.substring(0, a) + delta.substring(b + 3);
        }
        return delta;
    }
}
