/**
 *  @(#)AdminUnitTest.java 0.31 29/08/2015
 *  Copyright (C) 2015 - 2018 MER-C
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

import java.time.OffsetDateTime;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java requiring administrator access.
 *  @author MER-C
 */

public class AdminUnitTest
{
    private static Wiki testWiki;
    
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.createInstance("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
        testWiki.setThrottle(1000);
    }
    
    @Test
    public void getLogEntries() throws Exception
    {
        // https://test.wikipedia.org/w/index.php?title=Special%3ALog&page=User%3AMER-C%2FTest
        Wiki.LogEntry[] le = testWiki.getLogEntries(Wiki.ALL_LOGS, null, null, "User:MER-C/Test");
        assertEquals("getLogEntries: RevisionDeleted reason, can access", 
            "create a log entry for testing RevisionDelete on", le[0].getComment());
        assertEquals("getLogEntries: RevisionDeleted user, can access", "MER-C", 
            le[0].getUser());
    }
    
    @Test
    public void getDeletedText() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/Delete
        assertEquals("getDeletedText", "This revision is also deleted!", 
            testWiki.getDeletedText("User:MER-C/UnitTests/Delete"));
        // https://test.wikipedia.org/wiki/Special:Undelete/Tfs;hojfsdhp;osjfeas;lioejg
        assertNull("getDeletedText: page never deleted", testWiki.getDeletedText("Tfs;hojfsdhp;osjfeas;lioejg"));
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/EmptyDelete
        assertEquals("getDeletedText: empty", "", 
            testWiki.getDeletedText("User:MER-C/UnitTests/EmptyDelete"));
    }
    
    /**
     *  Fetching revisions of deleted pages.
     *  @throws Exception if something goes wrong
     */
    @Test
    public void revisionGetText() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/EmptyDelete
        // Wiki.Revision rev = testWiki.getRevision(323866L);
        // assertEquals("Revision.getText: empty", rev.getText(), "");
        // currently broken, needs getDeletedRevisions see 
        // https://test.wikipedia.org/w/api.php?action=query&prop=revisions&revids=323866
        
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/Delete
        // most recent deleted revision
        // page exists, but has deleted revisions (currently broken)
        // rev = testWiki.getRevision(217079L);
        // assertEquals("Revision.getText", rev.getText(), "This revision is also deleted!");
    }
    
    @Test
    public void revisionDelete() throws Exception
    {
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/revdel
        Wiki.Revision revision = testWiki.getRevision(349877L);
        testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Unit testing", 
            Boolean.FALSE, Arrays.asList(revision));
        assertTrue("RevisionDelete: set commentDeleted flag", revision.isCommentDeleted());
        assertTrue("RevisionDelete: set contentDeleted flag", revision.isContentDeleted());
        assertTrue("RevisionDelete: set userDeleted flag", revision.isUserDeleted());
        // check whether ths state was actually changed on-wiki
        revision = testWiki.getRevision(349877L);
        assertTrue("RevisionDelete: check commentDeleted on-wiki", revision.isCommentDeleted());
        assertTrue("RevisionDelete: check contentDeleted on-wiki", revision.isContentDeleted());
        assertTrue("RevisionDelete: check userDeleted on-wiki", revision.isUserDeleted());
        
        // https://test.wikipedia.org/w/index.php?title=Special%3ALog&type=delete&user=&page=File%3AWiki.java+test5.jpg&year=2018&month=3
        Wiki.LogEntry[] le = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", "MER-C", 
            "File:Wiki.java test5.jpg", OffsetDateTime.parse("2018-03-18T00:00:00Z"),
            OffsetDateTime.parse("2018-03-16T00:00:00Z"), 50, Wiki.ALL_NAMESPACES);
        testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, 
            "Unit testing WOOP WOOP WOOP", Boolean.FALSE, Arrays.asList(le[0]));
        // local state is controlled by a superclass, no need to test again
        // still need to check whether ths state was actually changed on-wiki
        le = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", "MER-C", 
            "File:Wiki.java test5.jpg", OffsetDateTime.parse("2018-03-18T00:00:00Z"),
            OffsetDateTime.parse("2018-03-16T00:00:00Z"), 50, Wiki.ALL_NAMESPACES);
        assertTrue("RevisionDelete: check commentDeleted on-wiki", le[0].isCommentDeleted());
        assertTrue("RevisionDelete: check contentDeleted on-wiki", le[0].isContentDeleted());
        assertTrue("RevisionDelete: check userDeleted on-wiki", le[0].isUserDeleted());
    }
    
    @AfterClass
    public static void cleanup() throws Exception
    {
        // undo RevisionDelete test on Revision
        Wiki.Revision revision = testWiki.getRevision(349877L);
        testWiki.revisionDelete(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "reset test", 
            Boolean.FALSE, Arrays.asList(revision));
        
        // undo RevisionDelete test on LogEntry
        Wiki.LogEntry[] le = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", "MER-C", 
            "File:Wiki.java test5.jpg", OffsetDateTime.parse("2018-03-18T00:00:00Z"),
            OffsetDateTime.parse("2018-03-16T00:00:00Z"), 50, Wiki.ALL_NAMESPACES);
        testWiki.revisionDelete(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "reset test", 
            Boolean.FALSE, Arrays.asList(le[0]));
    }
}
