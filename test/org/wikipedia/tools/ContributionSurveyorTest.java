/**
 *  @(#)ContributionSurveyorUnitTest.java 0.04 25/01/2018
 *  Copyright (C) 2011-20xx MER-C
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
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
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
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        surveyor = new ContributionSurveyor(enWiki);
    }

    @Test
    public void getWiki()
    {
        assertEquals("en.wikipedia.org", surveyor.getWiki().getDomain());
    }
    
    @Test
    public void contributionSurvey() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Contributions/HilStev               - no edits
        // https://en.wikipedia.org/wiki/Special:Contributions/OfficialPankajPatidar - no mainspace edits
        // https://en.wikipedia.org/wiki/Special:Contributions/Rt11642               - mainspace edits all revisiondeleted
        List<String> users = List.of("HilStev", "OfficialPankajPatidar", "Rt11642");
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty(), "User with no edits");
        assertTrue(results.get(users.get(1)).isEmpty(), "Check namespace filter");
        assertTrue(results.get(users.get(2)).isEmpty(), "Check revision deletion");
    }
    
    @Test
    public void outputContributionSurvey() throws Exception
    {
        // same use case as above: all three users have no surveyable edits
        List<String> users = List.of("HilStev", "OfficialPankajPatidar", "Rt11642");
        List<String> results = surveyor.outputContributionSurvey(users, false, Wiki.MAIN_NAMESPACE);
        assertTrue(results.isEmpty());
    }
    
    @Test
    public void setDateRange() throws Exception
    {        
        // verify get/set works
        assertThrows(IllegalArgumentException.class,
            () -> surveyor.setDateRange(OffsetDateTime.now(), OffsetDateTime.MIN));
        OffsetDateTime earliest = OffsetDateTime.parse("2017-12-07T00:00:00Z");
        OffsetDateTime latest = OffsetDateTime.parse("2018-01-23T00:00:00Z");
        surveyor.setDateRange(earliest, latest);
        assertEquals(earliest, surveyor.getEarliestDateTime());
        assertEquals(latest, surveyor.getLatestDateTime());
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-01&end=2018-01-24
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-07&end=2018-01-17
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(List.of("Jimbo Wales"), Wiki.MAIN_NAMESPACE);
        assertTrue(results.get("Jimbo Wales").isEmpty(), "check date range functionality");
    }
    
    @Test
    public void setIgnoreMinorEdits() throws Exception
    {
        // minor edits are ignored by default
        assertTrue(surveyor.isIgnoringMinorEdits()); 

        // https://en.wikipedia.org/wiki/Special:Contributions/Jjdevine2
        List<String> users = List.of("Jjdevine2");
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
        
        // verify get/set works
        surveyor.setIgnoringMinorEdits(false);
        assertFalse(surveyor.isIgnoringMinorEdits()); 
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(2, results.get(users.get(0)).size(), "User with nearly only minor edits");
    }
    
    @Test
    public void setIgnoringReverts() throws Exception
    {
        // reverts are ignored by default
        assertTrue(surveyor.isIgnoringReverts()); 
        
        // rollbacks with tag mw-rollback
        // https://en.wikipedia.org/w/index.php?title=Special:Contributions&dir=prev&offset=20191109040135&target=Dl2000
        List<String> users = List.of("Dl2000");
        surveyor.setIgnoringMinorEdits(false);
        surveyor.setDateRange(OffsetDateTime.parse("2019-11-09T16:00:00Z"), OffsetDateTime.parse("2019-11-09T16:21:00Z"));
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
        
        // verify get/set works
        surveyor.setIgnoringReverts(false);
        assertFalse(surveyor.isIgnoringReverts());
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
        
        // reverts with tag mw-manual-revert
        // https://en.wikipedia.org/w/index.php?title=Special:Contributions&offset=20200808093000&target=SouthAfricanCitizen
        users = List.of("SouthAfricanCitizen");
        surveyor.setDateRange(OffsetDateTime.parse("2020-08-08T09:00:00Z"), OffsetDateTime.parse("2020-08-08T09:30:00Z"));
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
        surveyor.setIgnoringReverts(true);
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
    }
    
    @Test
    public void setMinimumSizeDiff() throws Exception
    {
        // default is addition of at least 150 bytes
        assertEquals(150, surveyor.getMinimumSizeDiff());
        
        // https://en.wikipedia.org/wiki/Special:Contributions/Cyprumande
        List<String> users = List.of("Cyprumande");
        surveyor.setDateRange(OffsetDateTime.parse("2019-01-01T00:00:00Z"), null);
        Map<String, Map<String, List<Wiki.Revision>>> results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
        
        // verify get/set works
        surveyor.setMinimumSizeDiff(0);
        assertEquals(0, surveyor.getMinimumSizeDiff());
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
    }
}
