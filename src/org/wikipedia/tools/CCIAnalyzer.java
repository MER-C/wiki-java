/**
 *  @(#)CCIAnalyzer.java 0.01 16/12/2013
 *  Copyright (C) 2013 - 2014 MER-C
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

import java.io.IOException;
import java.util.*;
import java.net.*;
import org.wikipedia.Wiki;

/**
 *
 */
public class CCIAnalyzer
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        StringBuilder cci = new StringBuilder(enWiki.getPageText("X"));
        
        // parse the list of diffs
        ArrayList<String> minoredits = new ArrayList<String>(500);
        for (int i = cci.indexOf("{{dif|"); i > 0; i = cci.indexOf("{{dif|", ++i))
        {
            int x = cci.indexOf("}}", i);
            String edit = cci.substring(i, x);
            x = edit.indexOf("|");
            int y = edit.indexOf("|", x);
            String oldid = edit.substring(x, y);

            // Fetch diff. No plain text diffs for performance reasons, see
            // https://bugzilla.wikimedia.org/show_bug.cgi?id=13209
            // We don't use the Wiki.java method here, this avoids an extra query.
            String diff = enWiki.fetch("https://en.wikipedia.org/w/api.php?format=xml&action=query&prop=revisions&rvdiffto=prev&revids=" + oldid, "blah");
            diff = enWiki.decode(diff);
            // However, it is easy to strip the HTML.
            boolean major = true;
            for (int j = diff.indexOf("diff-addedlines"); j > 0; j = diff.indexOf("diff-addedlines", j))
            {
                if (diff.contains("diffchange diffchange-inline"))
                {
                    // add the highlighted parts
                    
                }
                else
                {
                    // add the whole line
                }
            }
            if (!major)
                minoredits.add(edit);
        }
        
        // remove all minor edits from the CCI
        for (String minoredit : minoredits)
        {
            int x = cci.indexOf(minoredit);
            cci.replace(x, x + minoredit.length(), "");
        }
    }
    
    /**
     *  Determines whether a given delta is a major edit. A "major edit" is
     *  defined as something that adds more than 6 words.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     */
    public static boolean analyzeDelta(String delta)
    {
        // This treats wikilinks as separate tokens.
        // From what I see, all articles still have 6 words between wikilinks.
        StringTokenizer tk = new StringTokenizer(delta, "<>[]{}|=");
        while (tk.hasMoreTokens())
        {
            String token =  tk.nextToken();
            if (token.split("\\s").length > 6)
                return true;
        }
        return false;
    }
}
