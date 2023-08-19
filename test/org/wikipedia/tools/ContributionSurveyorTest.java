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
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty(), "User with no edits");
        assertTrue(results.get(users.get(1)).isEmpty(), "Check namespace filter");
        assertTrue(results.get(users.get(2)).isEmpty(), "Check revision deletion");
    }
    
    @Test
    public void imageContributionSurvey() throws Exception
    {
        // https://meta.wikimedia.org/wiki/Special:CentralAuth/Helga_The_Great_Kyiv - one deleted upload
        // https://meta.wikimedia.org/wiki/Special:CentralAuth/Lozouhg - one image, PD text (so probably not going away)
        // note search for transferred uploads is not stable enough for testing
        List<String> users = List.of("Helga The Great Kyiv", "Lozouhg");
        var results = surveyor.imageContributionSurvey(users);
        assertTrue(results.get(users.get(0)).get("local").isEmpty(), "User with no uploads (local)");
        assertTrue(results.get(users.get(0)).get("commons").isEmpty(), "User with no uploads (commons)");
        assertTrue(results.get(users.get(1)).get("local").isEmpty(), "User with only commons uploads");
        assertEquals(List.of("File:Infinum logo.jpg"), results.get(users.get(1)).get("commons"));
    }
    
    @Test
    public void outputContributionSurvey() throws Exception
    {
        // same use case as above: all three users have no surveyable edits
        List<String> users = List.of("HilStev", "OfficialPankajPatidar", "Rt11642");
        List<String> results = surveyor.outputContributionSurvey(users, true, false, false, Wiki.MAIN_NAMESPACE);
        assertTrue(results.isEmpty());
    }
    
    @Test
    public void setDateRange() throws Exception
    {        
        // verify get/set works
        assertThrows(IllegalArgumentException.class,
            () -> surveyor.setDateRange(OffsetDateTime.now(), OffsetDateTime.MIN));
        assertThrows(IllegalArgumentException.class,
            () -> surveyor.setDateRange(OffsetDateTime.MAX, OffsetDateTime.now()));
        OffsetDateTime earliest = OffsetDateTime.parse("2017-12-07T00:00:00Z");
        OffsetDateTime latest = OffsetDateTime.parse("2018-01-23T00:00:00Z");
        surveyor.setDateRange(earliest, latest);
        assertEquals(earliest, surveyor.getEarliestDateTime());
        assertEquals(latest, surveyor.getLatestDateTime());
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-01&end=2018-01-24
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-07&end=2018-01-17
        List<String> users = List.of("Jimbo Wales");
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty(), "Check date range functionality (text)");
        
        // images
        users = List.of("Lozouhg");
        var results2 = surveyor.imageContributionSurvey(users);
        assertTrue(results2.get(users.get(0)).get("commons").isEmpty(), "Check date range functionality (images)");
    }
    
    @Test
    public void setIgnoreMinorEdits() throws Exception
    {
        // minor edits are ignored by default
        assertTrue(surveyor.isIgnoringMinorEdits()); 

        // https://en.wikipedia.org/wiki/Special:Contributions/Jjdevine2
        List<String> users = List.of("Jjdevine2");
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
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
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
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
        surveyor.setMinimumSizeDiff(0);
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
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
        
        // verify get/set works
        surveyor.setMinimumSizeDiff(0);
        assertEquals(0, surveyor.getMinimumSizeDiff());
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
    }
    
    @Test
    public void setNewOnly() throws Exception
    {
        // default is false
        assertFalse(surveyor.newOnly());
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&target=GarciaB&start=2005-03-14&end=2005-03-15
        List<String> users = List.of("GarciaB");
        surveyor.setDateRange(OffsetDateTime.parse("2005-03-14T00:00:00Z"), OffsetDateTime.parse("2005-03-15T00:00:00Z"));
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        Map<String, List<Wiki.Revision>> results2 = results.get(users.get(0));
        assertEquals(2, results2.size());
        assertTrue(results2.keySet().containsAll(List.of("Akan people", "Lists of volcanoes")));
        
        // verify get/set works
        surveyor.setNewOnly(true);
        assertTrue(surveyor.newOnly());
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        results2 = results.get(users.get(0));
        assertEquals(1, results2.size());
        assertTrue(results2.containsKey("Akan people"));
    }
    
    @Test
    public void setComingled() throws Exception
    {
        // default is false
        assertFalse(surveyor.isComingled());
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&target=Dhouston45&start=2022-07-06&end=2022-07-07
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&target=Dhouston17&start=2022-07-06&end=2022-07-07
        List<String> users = List.of("Dhouston17", "Dhouston45");
        surveyor.setDateRange(OffsetDateTime.parse("2022-07-06T00:00:00Z"), OffsetDateTime.parse("2022-07-07T00:00:00Z"));
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(2, results.size());
        assertTrue(results.keySet().containsAll(users));
        Map<String, List<Wiki.Revision>> results2 = results.get(users.get(0));
        assertEquals(1, results2.size());
        assertTrue(results2.containsKey("NHL on ESPN2"));
        results2 = results.get(users.get(1));
        assertTrue(results2.containsKey("NHL on ESPN"));
        assertTrue(results2.containsKey("NHL on ESPN2"));
        
        // verify get/set works
        surveyor.setComingled(true);
        assertTrue(surveyor.isComingled());
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.size());
        results2 = results.get("");
        assertEquals(2, results2.size());
        assertTrue(results2.containsKey("NHL on ESPN"));
        assertTrue(results2.containsKey("NHL on ESPN2"));
    }
    
    @Test
    public void setFooter()
    {
        String f = "Test123";
        surveyor.setFooter(f);
        assertTrue(surveyor.generateHTMLFooter().endsWith(f));
        assertTrue(surveyor.generateWikitextFooter().endsWith(f));
    }
}
