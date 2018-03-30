/**
 *  @(#)UserLinkAdditionFinderUnitTest.java 0.01 17/10/2015
 *  Copyright (C) 2015 MER-C
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
import org.junit.*;
import org.wikipedia.Wiki;
import static org.junit.Assert.*;

/**
 *  Diff parsing unit tests for the UserLinkAdditionFinder.
 *  @author MER-C
 */
public class UserLinkAdditionFinderUnitTest
{
    private static Wiki testWiki;
    
    /**
     *  Initialize wiki objects.
     *  @throws Exception if a network error occurs
     */
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.createInstance("test.wikipedia.org");
    }
    
    @Test
    public void parseDiff() throws IOException
    {
        Wiki.Revision[] revs = testWiki.getRevisions(new long[]
        { 
            244169L, 244170L, 244171L, 320307L, 350372L
        });
        
        // https://test.wikipedia.org/w/index.php?oldid=244169&diff=prev
        Map<Wiki.Revision, List<String>> results = UserLinkAdditionFinder.parseDiff(revs[0]);
        List<String> links = results.get(revs[0]);
        assertNotNull(links);
        assertEquals(1, links.size());
        assertEquals("http://spam.example.com", links.get(0));
                
        // https://test.wikipedia.org/w/index.php?oldid=244170&diff=prev
        results = UserLinkAdditionFinder.parseDiff(revs[1]);
        links = results.get(revs[1]);
        assertEquals("https://en.wikipedia.org", links.get(0));
        
        // https://test.wikipedia.org/w/index.php?oldid=244171&diff=prev
        results = UserLinkAdditionFinder.parseDiff(revs[2]);
        links = results.get(revs[2]);
        assertEquals("http://www.example.net", links.get(0));
        
        // dummy edit
        // https://test.wikipedia.org/w/index.php?oldid=320307&diff=prev
        results = UserLinkAdditionFinder.parseDiff(revs[3]);
        links = results.get(revs[3]);
        assertEquals(0, links.size());
        
        // https://test.wikipedia.org/w/index.php?oldid=350372&diff=prev
        results = UserLinkAdditionFinder.parseDiff(revs[4]);
        links = results.get(revs[4]);
        assertEquals("http://template.example.com", links.get(0));
    }
}
