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
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.wikipedia.*;
import static org.junit.jupiter.api.Assertions.*;

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
        testWiki = WMFWiki.newSession("test.wikipedia.org");
        enWiki = WMFWiki.newSession("en.wikipedia.org");
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
    public void canSkipDomain() throws Exception
    {
        List<String> domains = List.of("en.wikipedia.org", "www.fda.gov", "nasa.gov",
            "army.mil", "www.consilium.europa.eu", "gov.uk", "wa.gov.au", "europa.eu",
            "govt.nz", "www.govt.nz", "bl.uk", "parliament.uk", "un.int");
        for (String domain : domains)
            assertTrue(finder_en.canSkipDomain(domain, UserLinkAdditionFinder.RemovalMode.NO_BLACKLISTS), "domain: " + domain);
        
        domains = List.of("www.example.com", "blah.gov.invalid", "fakegov.uk", "blahbl.uk",
            "fake-gov.uk");
        for (String domain : domains)
            assertFalse(finder_en.canSkipDomain(domain, UserLinkAdditionFinder.RemovalMode.NO_BLACKLISTS), "domain: " + domain);
        
        domains = List.of("youtu.be", "goo.gl");
        for (String domain : domains)
        {
            assertFalse(finder_en.canSkipDomain(domain, UserLinkAdditionFinder.RemovalMode.NO_BLACKLISTS), "domain: " + domain);
            assertTrue(finder_en.canSkipDomain(domain, UserLinkAdditionFinder.RemovalMode.GLOBAL_BLACKLIST), "domain: " + domain);
            assertTrue(finder_en.canSkipDomain(domain, UserLinkAdditionFinder.RemovalMode.ALL_BLACKLISTS), "domain: " + domain);
        }
    }

    @Test
    public void getLinksAdded() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Contributions/EDPerfect (no edits)
        // https://en.wikipedia.org/wiki/Special:Contributions/Shittipa (2 edits, only one adds links)
        // https://en.wikipedia.org/wiki/Special:Contributions/EmanningKBRA (all link adding edits revision deleted)
        // remainder of users with one edit, one link

        List<String> users = List.of("Helonty", "ReteNsep", "Reyeilint", "Shittipa", "EDPerfect");
        Map<Wiki.Revision, List<String>> linksadded = finder_en.getLinksAdded(users, null, null);
        Set<String> actual = linksadded.keySet().stream().map(rev -> String.valueOf(rev.getID())).collect(Collectors.toSet());
        assertEquals(4, actual.size());
        assertTrue(actual.containsAll(List.of("834061933", "823758919", "833871994", "834097191")));
        for (Map.Entry<Wiki.Revision, List<String>> entry : linksadded.entrySet())
        {
            Wiki.Revision revision = entry.getKey();
            List<String> links = entry.getValue();
            long id = revision.getID();

            // https://en.wikipedia.org/wiki/Special:Diff/823758919
            if (id == 823758919L)
                assertEquals(List.of("http://gastroinflorida.com/"), links);
            // https://en.wikipedia.org/wiki/Special:Diff/833871994
            else if (id == 833871994L)
                assertEquals(List.of("http://www.insurancepanda.com/"), links);
            // https://en.wikipedia.org/wiki/Special:Diff/834097191
            else if (id == 834097191L)
                assertEquals(List.of("https://www.sfspa.com/"), links);
            // https://en.wikipedia.org/wiki/Special:Diff/834061933
            else if (id == 834061933L)
                assertEquals(List.of("http://www.drgoldman.com/"), links);
        }
        
        // check date cutoff (all users indeffed prior to this date, therefore empty)
        linksadded = finder_en.getLinksAdded(users, OffsetDateTime.parse("2018-06-01T00:00:00Z"), null);
        assertTrue(linksadded.isEmpty());
    }

    @Test
    public void parseDiff() throws IOException
    {
        List<Wiki.Revision> revs = testWiki.getRevisions(new long[]
        {
            244169L, 244170L, 244171L, 320307L, 350372L
        });

        // https://test.wikipedia.org/wiki/Special:Diff/244169
        List<String> links = finder_test.parseDiff(revs.get(0));
        assertNotNull(links);
        assertEquals(1, links.size());
        assertEquals("http://spam.example.com", links.get(0));

        // https://test.wikipedia.org/wiki/Special:Diff/244170
        links = finder_test.parseDiff(revs.get(1));
        assertEquals("https://en.wikipedia.org", links.get(0));

        // https://test.wikipedia.org/wiki/Special:Diff/244171
        links = finder_test.parseDiff(revs.get(2));
        assertEquals("http://www.example.net", links.get(0));

        // dummy edit
        // https://test.wikipedia.org/wiki/Special:Diff/320307
        links = finder_test.parseDiff(revs.get(3));
        assertTrue(links.isEmpty());

        // https://test.wikipedia.org/wiki/Special:Diff/350372
        links = finder_test.parseDiff(revs.get(4));
        assertEquals("http://template.example.com", links.get(0));
    }
}
