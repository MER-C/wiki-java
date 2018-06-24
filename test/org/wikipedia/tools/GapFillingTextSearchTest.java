/**
 *  @(#)GapFillingTextSearchTest.java 0.01 10/06/2018
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
package org.wikipedia.tools;

import java.util.*;
import java.util.regex.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.wikipedia.*;

/**
 *  Unit tests for {@link GapFillingTextSearch}.
 *  @author MER-C
 */
public class GapFillingTextSearchTest
{
    private final Wiki enWiki;
    private final GapFillingTextSearch gfs;
    
    /**
     *  Construct a tool object and wiki connection for every test so that tests
     *  are independent.
     */
    public GapFillingTextSearchTest()
    {
        enWiki = Wiki.createInstance("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        gfs = new GapFillingTextSearch(enWiki);
    }
    
    @Test
    public void getWiki() throws Exception
    {
        assertEquals("en.wikipedia.org", gfs.getWiki().getDomain());
    }

    @Test
    public void searchAndExtractSnippets() throws Exception
    {
        // needs to handle no results
        assertTrue("searchAndExtractSnippets: no inputs", gfs.searchAndExtractSnippets(Collections.emptyMap(), "Blah", false).isEmpty());
        
        Map<String, String> inputs = new HashMap<>();
        String page = "Wikipedia:Articles for deletion/Dmarket";
        String text = enWiki.getPageText(page);
        inputs.put(page, text);
        assertTrue("searchAndExtractSnippets: no results", gfs.searchAndExtractSnippets(inputs, "NotASearchTerm", false).isEmpty());
        Map<String, String> results = gfs.searchAndExtractSnippets(inputs, "kamikaze", false);
        assertEquals(1, results.size());
        String expected = "talk:CASSIOPEIA|<b style=\"#0000FF\">talk</b>]])</sup> 14:45, 9 June 2018 (UTC)</small> "
            + "*'''Delete''' this highly [[WP:PROMO|promotional]] text, created by a [[WP:SPA|kamikaze account]], "
            + "about a subject lacking notability aside from mentions in a few trade blogs";
        assertEquals("searchAndExtractSnippets: one result", expected, results.get(page));
        assertTrue("searchAndExtractSnippets: case sensitive", gfs.searchAndExtractSnippets(inputs, "Kamikaze", true).isEmpty());
        Map<String, String> results2 = gfs.searchAndExtractSnippets(inputs, "kamikaze", true);
        assertEquals(results, results2);
    }

    @Test
    public void regexMatchAndExtractSnippets()
    {
        // needs to handle no results
        Pattern pattern = Pattern.compile("blah");
        assertTrue("regexMatchAndExtractSnippets: no inputs", 
            gfs.regexMatchAndExtractSnippets(Collections.emptyMap(), pattern).isEmpty());

        
    }

    @Test
    public void extractSnippet()
    {
        int count = 60;
        List<String> inputs = new ArrayList<>();
        for (int i = 0; i < count; i++)
            inputs.add("Test" + i);
        String text = String.join(" ", inputs);
        try
        {
            gfs.extractSnippet(text, -30);
            fail("Tried to extract a snippet at a negative index.");
        }
        catch (StringIndexOutOfBoundsException expected)
        {
        }
        try
        {
            gfs.extractSnippet(text, text.length());
            fail("Tried to extract a snippet over the end of the string.");
        }
        catch (StringIndexOutOfBoundsException expected)
        {
        }
        
        // First 10 items in text = 60 characters, each subsequent 10 = 70 characters
        assertEquals("extractSnippet", String.join(" ", inputs.subList(10, 40)), gfs.extractSnippet(text, 60 + 70 + 38));
        assertEquals("extractSnippet: at start", String.join(" ", inputs.subList(0, 15)), gfs.extractSnippet(text, 0));
        assertEquals("extractSnippet: at end", String.join(" ", inputs.subList(count - 15 - 1, count - 1)), 
            gfs.extractSnippet(text, text.length() - 1));
    }
    
}
