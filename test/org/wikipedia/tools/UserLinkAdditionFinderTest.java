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
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;
import org.junit.*;
import org.wikipedia.*;
import static org.junit.Assert.*;

/**
 *  Diff parsing unit tests for the UserLinkAdditionFinder.
 *  @author MER-C
 */
public class UserLinkAdditionFinderTest
{
    private final WMFWiki testWiki, enWiki;
    private UserLinkAdditionFinder finder_test, finder_en;

    /**
     *  Construct a new wiki connection to make sure tests are independent.
     */
    public UserLinkAdditionFinderTest()
    {
        testWiki = WMFWiki.createInstance("test.wikipedia.org");
        enWiki = WMFWiki.createInstance("en.wikipedia.org");
        finder_test = new UserLinkAdditionFinder(testWiki);
        finder_en = new UserLinkAdditionFinder(enWiki);
    }
            
    @Test
    public void getWiki() throws Exception
    {
        assertEquals("test.wikipedia.org", finder_test.getWiki().getDomain());
        assertEquals("en.wikipedia.org", finder_en.getWiki().getDomain());
    }

    @Test
    public void getLinksAdded() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Contributions/EDPerfect (no edits)
        // https://en.wikipedia.org/wiki/Special:Contributions/Shittipa (2 edits, only one adds links)
        // https://en.wikipedia.org/wiki/Special:Contributions/EmanningKBRA (all link adding edits revision deleted)
        // remainder of users with one edit, one link

        List<String> users = Arrays.asList("Helonty", "ReteNsep", "Reyeilint", "Shittipa", "EDPerfect");
        Map<Wiki.Revision, List<String>> linksadded = finder_en.getLinksAdded(users, null);
        Set<String> actual = linksadded.keySet().stream().map(rev -> String.valueOf(rev.getID())).collect(Collectors.toSet());
        assertEquals(4, actual.size());
        assertTrue(actual.containsAll(Arrays.asList("834061933", "823758919", "833871994", "834097191")));
        for (Map.Entry<Wiki.Revision, List<String>> entry : linksadded.entrySet())
        {
            Wiki.Revision revision = entry.getKey();
            List<String> links = entry.getValue();
            long id = revision.getID();

            // https://en.wikipedia.org/wiki/Special:Diff/823758919
            if (id == 823758919L)
                assertEquals(Arrays.asList("http://gastroinflorida.com/"), links);
            // https://en.wikipedia.org/wiki/Special:Diff/833871994
            else if (id == 833871994L)
                assertEquals(Arrays.asList("http://www.insurancepanda.com/"), links);
            // https://en.wikipedia.org/wiki/Special:Diff/834097191
            else if (id == 834097191L)
                assertEquals(Arrays.asList("https://www.sfspa.com/"), links);
            // https://en.wikipedia.org/wiki/Special:Diff/834061933
            else if (id == 834061933L)
                assertEquals(Arrays.asList("http://www.drgoldman.com/"), links);
        }
        
        // check date cutoff (all users indeffed prior to this date, therefore empty)
        linksadded = finder_en.getLinksAdded(users, OffsetDateTime.parse("2018-06-01T00:00:00Z"));
        assertTrue(linksadded.isEmpty());
    }

    @Test
    public void parseDiff() throws IOException
    {
        Wiki.Revision[] revs = testWiki.getRevisions(new long[]
        {
            244169L, 244170L, 244171L, 320307L, 350372L
        });

        // https://test.wikipedia.org/wiki/Special:Diff/244169
        List<String> links = finder_test.parseDiff(revs[0]);
        assertNotNull(links);
        assertEquals(1, links.size());
        assertEquals("http://spam.example.com", links.get(0));

        // https://test.wikipedia.org/wiki/Special:Diff/244170
        links = finder_test.parseDiff(revs[1]);
        assertEquals("https://en.wikipedia.org", links.get(0));

        // https://test.wikipedia.org/wiki/Special:Diff/244171
        links = finder_test.parseDiff(revs[2]);
        assertEquals("http://www.example.net", links.get(0));

        // dummy edit
        // https://test.wikipedia.org/wiki/Special:Diff/320307
        links = finder_test.parseDiff(revs[3]);
        assertEquals(0, links.size());

        // https://test.wikipedia.org/wiki/Special:Diff/350372
        links = finder_test.parseDiff(revs[4]);
        assertEquals("http://template.example.com", links.get(0));
    }
}
