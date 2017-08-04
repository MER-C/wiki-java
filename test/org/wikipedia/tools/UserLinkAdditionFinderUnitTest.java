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
import org.wikipedia.Wiki;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Diff parsing unit tests for the UserLinkAdditionFinder.
 *  @author MER-C
 */
public class UserLinkAdditionFinderUnitTest
{
    private final Wiki testWiki = new Wiki("test.wikipedia.org");
    
    @Test
    public void parseDiff() throws IOException
    {
        Wiki.Revision[] revs = testWiki.getRevisions(new long[]
        { 
            244169L, 244170L, 244171L, 320307L 
        });
        
        // https://test.wikipedia.org/w/index.php?oldid=244169&diff=prev
        String[] links = UserLinkAdditionFinder.parseDiff(revs[0]);
        assertEquals("244169", links[0]);
        assertEquals(3, links.length);
        assertEquals("MER-C", links[1]);
        assertEquals("http://spam.example.com", links[2]);
                
        // https://test.wikipedia.org/w/index.php?oldid=244170&diff=prev
        links = UserLinkAdditionFinder.parseDiff(revs[1]);
        assertEquals("https://en.wikipedia.org", links[2]);
        
        // https://test.wikipedia.org/w/index.php?oldid=244171&diff=prev
        links = UserLinkAdditionFinder.parseDiff(revs[2]);
        assertEquals("http://www.example.net", links[2]);
        
        // dummy edit
        // https://test.wikipedia.org/w/index.php?oldid=320307&diff=prev
        links = UserLinkAdditionFinder.parseDiff(revs[3]);
        assertEquals(2, links.length);
        assertEquals("320307", links[0]);
        assertEquals("AlvaroMolina", links[1]);
    }
}
