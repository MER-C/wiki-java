/**
 *  @(#)ExternalLinkPopularityTest.java 0.01 24/04/2018
 *  Copyright (C) 2018 - 20xx MER-C
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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.wikipedia.Wiki;

/**
 *  Unit tests for {@link ExternalLinkPopularity}.
 *  @author MER-C
 */
public class ExternalLinkPopularityTest
{
    private final Wiki testWiki;
    private final ExternalLinkPopularity elp;
    
    /**
     *  Construct a tool object and wiki connection for every test so that tests
     *  are independent.
     */
    public ExternalLinkPopularityTest()
    {
        testWiki = Wiki.createInstance("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        elp = new ExternalLinkPopularity(testWiki);
    }
    
    @Test
    public void getWiki() throws Exception
    {
        assertEquals("test.wikipedia.org", elp.getWiki().getDomain());
    }
    
    @Test
    public void setMaxLinks() throws Exception
    {
        int limit = 250;
        elp.setMaxLinks(limit);
        assertEquals(limit, elp.getMaxLinks());
        
        // check that the limit actually does anything
        Map<String, Integer> results = elp.determineLinkPopularity(Arrays.asList("wikipedia.org"));
        assertEquals(1, results.size());
        assertEquals(limit, results.get("wikipedia.org").intValue());
    }
    
    @Test
    public void getExcludeList() throws Exception
    {
        elp.getExcludeList().addAll(Arrays.asList("wikipedia.org", "example."));
        String article = "User:MER-C/UnitTests/Linkfinder";
        Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(Arrays.asList(article));
        assertTrue(results.get(article).isEmpty());
    }

    @Test
    public void fetchExternalLinks() throws Exception
    {
        String article = "User:MER-C/UnitTests/Linkfinder";
        Map<String, Map<String, List<String>>> results = elp.fetchExternalLinks(Arrays.asList(article));
        Map<String, List<String>> urlsbydomain = results.get(article);
        Set<String> keyset = urlsbydomain.keySet();
        assertEquals(3, keyset.size());
        assertTrue(keyset.containsAll(Arrays.asList("example.net", "wikipedia.org", "example.com")));
        assertEquals(Arrays.asList("http://www.example.net"), urlsbydomain.get("example.net"));
        assertEquals(Arrays.asList("https://en.wikipedia.org"), urlsbydomain.get("wikipedia.org"));
        assertEquals(Arrays.asList("http://spam.example.com"), urlsbydomain.get("example.com"));
    }
    
    @Test
    public void determineLinkPopularity() throws Exception
    {
        List<String> data = Arrays.asList("wikipedia.org", "obviously.invalid", "wikimedia.org");
        Map<String, Integer> results = elp.determineLinkPopularity(data);
        assertEquals(3, results.size());
        assertEquals(500, results.get("wikipedia.org").intValue());
        assertEquals(0, results.get("obviously.invalid").intValue());
        assertEquals(500, results.get("wikimedia.org").intValue());        
    }
}
