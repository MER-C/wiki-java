/**
 *  @(#)RevisionsTest.java 0.01 09/08/2018
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
package org.wikipedia;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for {@link org.wikipedia.Revisions}.
 *  @author MER-C
 */
public class RevisionsTest
{
    private final Wiki enWiki;
    
    /**
     *  Construct wiki objects for each test so that tests are independent.
     */
    public RevisionsTest()
    {
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
    }
    
    @Test
    public void removeReverts() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Azerbaijan&offset=20180125204500&action=history
        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .withinDateRange(OffsetDateTime.parse("2018-01-16T00:00:00Z"), OffsetDateTime.parse("2018-01-25T20:45:00Z"));
        List<Wiki.Revision> revisions = enWiki.getPageHistory("Azerbaijan", rh);
        long[] oldids = new long[] 
        {
            822342083L, //  0: state  3, reverts 822341440L
            822341440L, //  1: state  7, reverts 822339163L
            822339163L, //  2: state  3, reverts 822338691L and 822338419L
            822338691L, //  3: state  7
            822338419L, //  4: state  6
            821798528L, //  5: state  3, reverts 821778837L
            821778837L, //  6: state  5
            821776884L, //  7: state  3, reverts 821776856L
            821776856L, //  8: state  4
            821575229L, //  9: state  3
            821353780L, // 10: state  0, reverts 821353728L
            821353728L, // 11: state NA, content RevisionDeleted
            821171566L, // 12: state  0, reverts 821170526L. Should not be removed, this state does not appear before this revision.
            821171526L, // 13: state  1, reverts 821171155L
            821171155L, // 14: state  2
            821170526L  // 15: state  1
        };
        assertArrayEquals(oldids, revisions.stream().mapToLong(Wiki.Revision::getID).toArray(), "test setup");
        long[] expected = new long[] 
        {
            821170526L, // 14: state  1
            821171155L, // 13: state  2
            821171566L, // 12: state  0, reverts 821170526L. Should not be removed, this is unique.
            821353728L, // 11: state NA, content RevisionDeleted
            821575229L, //  9: state  3
            821776856L, //  8: state  4
            821778837L, //  6: state  5
            822338419L, //  4: state  6
            822338691L  //  3: state  7
        };
        List<Wiki.Revision> revertsremoved = Revisions.removeReverts(revisions);
        assertArrayEquals(expected, revertsremoved.stream().mapToLong(Wiki.Revision::getID).toArray());
        // two identical edits on different pages are not reverts
        oldids = new long[]
        {
            823352879L, // https://en.wikipedia.org/w/index.php?oldid=823352879
            823352525L  // https://en.wikipedia.org/w/index.php?oldid=823352525
        };
        revisions = enWiki.getRevisions(oldids);
        assertEquals(revisions, Revisions.removeReverts(revisions), "different pages, same content");
    }
    
    @Test
    public void toWikitext() throws Exception
    {
        // note for this means of getting revisions the new page flag and sizediffs aren't available
        // https://en.wikipedia.org/w/index.php?title=Phoenician_sanctuary_of_Kharayeb&oldid=1165014330
        // https://en.wikipedia.org/w/index.php?title=Hun_Manet&oldid=1171939466
        // https://en.wikipedia.org/w/index.php?title=Hun_Manet&oldid=1171939631
        List<Wiki.Revision> revisions = enWiki.getRevisions(new long[] { 1165014330L, 1171939631L, 1171939466L });
        String begin = "<div style=\"font-family: monospace; font-size: 120%\">\n";
        String end = "</div>";
        
        // Test each for functionality
        String actual_0 = Revisions.toWikitext(revisions.subList(0, 1));
        String expected_0 = """
            *[[Special:Permanentlink/1165014330|2023-07-12T13:04:44Z]] ([[Special:Diff/1165014330|prev]]) \
            . . . [[Phoenician sanctuary of Kharayeb]] .. [[User:Elias Ziade|Elias Ziade]] \
            ([[User talk:Elias Ziade|talk]] &middot; [[Special:Contributions/Elias Ziade|contribs]]) \
            .. (27768 bytes) (0) .. (<nowiki>[[WP:AES|←]]Created page with '{{Infobox historic site \
            | name = Phoenician sanctuary of Kharayeb | native_name = معبد الخرايب الفينيقي \
            | native_language = ar | image =  | caption =  \
            | built_for = Unidentified Phoenician deity, presumably a healing god/godess \
            | architecture = [[Phoenicia]]n, [[Achaemenid Empire|Achaemenid]], [[Hellenistic period|Hellenistic]] \
            | governing_body =  | designation1 =  | designation1_date =  | designation1_parent =  \
            | designation1_number =  |...'</nowiki>)
            """;
        assertEquals(begin + expected_0 + end, actual_0);
        // revision deleted edit summary + minor edit
        String actual_1 = Revisions.toWikitext(revisions.subList(1, 2));
        String expected_1 = """
            *[[Special:Permanentlink/1171939631|2023-08-24T01:56:14Z]] ([[Special:Diff/1171939631|prev]]) \
            . '''m''' . [[Hun Manet]] .. [[User:Gobonobo|Gobonobo]] \
            ([[User talk:Gobonobo|talk]] &middot; [[Special:Contributions/Gobonobo|contribs]]) \
            .. (19234 bytes) (0) .. (<span class="history-deleted">deleted</span>)              
            """;
        assertEquals(begin + expected_1 + end, actual_1);
        // revision deleted user + minor edit
        String actual_2 = Revisions.toWikitext(revisions.subList(2, 3));
        String expected_2 = """
            *[[Special:Permanentlink/1171939466|2023-08-24T01:55:09Z]] ([[Special:Diff/1171939466|prev]]) \
            . '''m''' . [[Hun Manet]] .. <span class="history-deleted">deleted</span> \
            .. (107696 bytes) (0) .. (<nowiki>becuase i feel like it</nowiki>)              
            """;
        assertEquals(begin + expected_2 + end, actual_2);
        
        // Combined
        String actual = Revisions.toWikitext(revisions);
        assertEquals(begin + expected_0 + expected_1 + expected_2 + end, actual);
    }
}
