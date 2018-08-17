/**
 *  @(#)ContributionSurveyorUnitTest.java 0.04 25/01/2018
 *  Copyright (C) 2011-2018 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.wikipedia.tools;

import java.util.*;
import java.time.OffsetDateTime;
import org.junit.*;
import static org.junit.Assert.*;
import org.wikipedia.Wiki;

/**
 *  Unit tests for {@link ContributionSurveyor}.
 *  @author MER-C
 */
public class ContributionSurveyorTest 
{
    private final Wiki enWiki;
    private final ContributionSurveyor surveyor;
    
    /**
     *  Constructs a tool object and wiki connection for every test so that 
     *  tests are independent.
     */
    public ContributionSurveyorTest()
    {
        enWiki = Wiki.createInstance("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        surveyor = new ContributionSurveyor(enWiki);
    }

    @Test
    public void getWiki()
    {
        assertEquals("getWiki", "en.wikipedia.org", surveyor.getWiki().getDomain());
    }
    
    @Test
    public void contributionSurvey() throws Exception
    {
        List<String> users = Arrays.asList(
            "HilStev", // no edits: https://en.wikipedia.org/wiki/Special:Contributions/HilStev
            "OfficialPankajPatidar", // no mainspace edits: https://en.wikipedia.org/wiki/Special:Contributions/OfficialPankajPatidar
            "Rt11642" // mainspace edits all revisiondeleted: https://en.wikipedia.org/wiki/Special:Contributions/Rt11642
        );
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue("User with no edits", results.get(users.get(0)).isEmpty());
        assertTrue("Check namespace filter", results.get(users.get(1)).isEmpty());
        assertTrue("Check revision deletion", results.get(users.get(2)).isEmpty());
    }
    
    @Test
    public void setDateRange() throws Exception
    {
        // first, verify get/set works
        OffsetDateTime earliest = OffsetDateTime.parse("2017-12-07T00:00:00Z");
        surveyor.setEarliestDateTime(earliest);
        assertEquals("getEarliestDateTime", earliest, surveyor.getEarliestDateTime());
        OffsetDateTime latest = OffsetDateTime.parse("2018-01-23T00:00:00Z");
        surveyor.setLatestDateTime(latest);
        assertEquals("getLatestDateTime", latest, surveyor.getLatestDateTime());
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-01&end=2018-01-24
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-07&end=2018-01-17
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(Arrays.asList("Jimbo Wales"), Wiki.MAIN_NAMESPACE);
        assertTrue("Check date/time bounds", results.get("Jimbo Wales").isEmpty());
    }
    
    @Test
    public void setIgnoreMinorEdits() throws Exception
    {
        // first, verify get/set works
        assertTrue(surveyor.isIgnoringMinorEdits()); 
        surveyor.setIgnoringMinorEdits(false);
        assertFalse(surveyor.isIgnoringMinorEdits()); 
        
        // check functionality
        List<String> users = Arrays.asList(
            "Jjdevine2" // https://en.wikipedia.org/wiki/Special:Contributions/Jjdevine2
        );
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals("User with nearly only minor edits", 2, results.get(users.get(0)).size());
    }
}
