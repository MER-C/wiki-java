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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
// import static org.junit.jupiter.api.Assumptions.*;

/**
 *  Unit tests for Wiki.java requiring administrator access.
 *  @author MER-C
 */
public class AdminUnitTests
{
    private static Wiki testWiki;
    
    @BeforeAll
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.newSession("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
        testWiki.setThrottle(1000);
//        assumeTrue(testWiki.getCurrentUser().isA("sysop"));
    }
    
    @Test
    public void getLogEntries() throws Exception
    {
        // https://test.wikipedia.org/w/index.php?title=Special%3ALog&page=User%3AMER-C%2FTest
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .byTitle("User:MER-C/Test")
            .limitedTo(100);
        List<Wiki.LogEntry> le = testWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        assertEquals("create a log entry for testing RevisionDelete on", le.get(0).getComment(),
            "RevisionDeleted reason, can access");
        assertEquals("MER-C", le.get(0).getUser(), "RevisionDeleted user, can access");
    }
    
    @Test
    public void getDeletedText() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/Delete
        assertEquals("This revision is also deleted!", testWiki.getDeletedText("User:MER-C/UnitTests/Delete"), "functionality");
        // https://test.wikipedia.org/wiki/Special:Undelete/Tfs;hojfsdhp;osjfeas;lioejg
        assertNull(testWiki.getDeletedText("Tfs;hojfsdhp;osjfeas;lioejg"), "page never deleted");
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/EmptyDelete
        assertEquals("", testWiki.getDeletedText("User:MER-C/UnitTests/EmptyDelete"), "deleted, empty page");
    }
    
    @Test
    public void getDeletedHistory() throws Exception
    {
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .withinDateRange(OffsetDateTime.parse("2019-05-03T00:00:00Z"), OffsetDateTime.parse("2019-05-04T00:00:00Z"));
        List<Wiki.Revision> revisions = testWiki.getDeletedHistory("User:MER-C/UnitTests/Delete2", rh);
        assertEquals(3, revisions.size());
        Wiki.Revision first = revisions.get(0);
        assertEquals(OffsetDateTime.parse("2019-05-03T16:23:32Z"), first.getTimestamp());
        assertEquals("MER-C", first.getUser());
        assertEquals("User:MER-C/UnitTests/Delete2", first.getTitle());
        assertTrue(first.isPageDeleted());
    }
    
    /**
     *  Fetches revisions of deleted pages.
     *  @throws Exception if something goes wrong
     */
    @Test
    @Disabled("See comments")
    public void revisionGetText() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/EmptyDelete
        // currently broken, needs getDeletedRevisions see 
        // https://test.wikipedia.org/w/api.php?action=query&prop=revisions&revids=323866
        Wiki.Revision rev = testWiki.getRevision(323866L);
        assertEquals("", rev.getText(), "deleted, empty revision");
        
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/Delete
        // most recent deleted revision
        // page exists, but has deleted revisions (currently broken)
        rev = testWiki.getRevision(217079L);
        assertEquals("This revision is also deleted!", rev.getText(), "page exists, but has deleted revisions");
    }
    
    @Test
    public void delete() throws Exception
    {
        // try to delete a page that doesn't exist
        // returns silently with log message, which is intended behavior
        testWiki.delete("FakePage" + (int)(Math.random() * 20000), "Fake reason", false);
        
        // try to delete an actual page
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete2
        // setup
        String pagename = "User:MER-C/UnitTests/Delete2";
        String talkpagename = testWiki.getTalkPage(pagename);
        List<String> pages = List.of(pagename, talkpagename);
        testWiki.edit(pagename, "Dummy text", "A dummy edit summary");
        testWiki.edit(talkpagename, "Dummy text", "A dummy edit summary");
        var info = testWiki.getPageInfo(pages);
        assertTrue((Boolean)info.get(0).get("exists"));
        assertTrue((Boolean)info.get(1).get("exists"));
        
        // no talk deletion
        testWiki.delete(pagename, "Per cabal orders (just testing).", false);
        info = testWiki.getPageInfo(pages);
        assertFalse((Boolean)info.get(0).get("exists"));
        assertTrue((Boolean)info.get(1).get("exists"));
        
        // talk deletion
        testWiki.edit(pagename, "Dummy text", "A dummy edit summary");
        testWiki.delete(pagename, "Per cabal orders (just testing, talk=on).", true);
        info = testWiki.getPageInfo(pages);
        assertFalse((Boolean)info.get(0).get("exists"));
        assertFalse((Boolean)info.get(1).get("exists"));
    }
    
    @Test
    public void undelete() throws Exception
    {
        // try to undelete a page that doesn't exist
        // returns silently with log message, which is intended behavior
        testWiki.undelete("FakePage" + (int)(Math.random() * 20000), "Fake reason", false);
        
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete3
        // setup
        String pagename = "User:MER-C/UnitTests/Delete3";
        String talkpagename = testWiki.getTalkPage(pagename);
        List<String> pages = List.of(pagename, talkpagename);
        testWiki.edit(pagename, "Dummy text", "A dummy edit summary");
        testWiki.edit(pagename, "Dummy text", "A dummy edit summary");
        var info = testWiki.getPageInfo(pages);
        assertTrue((Boolean)info.get(0).get("exists"));
        assertTrue((Boolean)info.get(1).get("exists"));
        testWiki.delete(pagename, "Per cabal orders (just testing).", true);
        info = testWiki.getPageInfo(pages);
        assertFalse((Boolean)info.get(0).get("exists"));
        assertFalse((Boolean)info.get(1).get("exists"));
        
        // undelete (no talk page)
        testWiki.undelete(pagename, "There is no cabal.", false);
        info = testWiki.getPageInfo(pages);
        assertTrue((Boolean)info.get(0).get("exists"));
        assertFalse((Boolean)info.get(1).get("exists"));
        
        // undelete with talk page
        testWiki.delete(pagename, "Per cabal orders (just testing).", true);
        testWiki.undelete(pagename, "There is no cabal.", true);
        info = testWiki.getPageInfo(pages);
        assertTrue((Boolean)info.get(0).get("exists"));
        assertTrue((Boolean)info.get(1).get("exists"));
    }
    
    @Test
    public void revisionDelete() throws Exception
    {
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/revdel
        Wiki.Revision revision = testWiki.getRevision(349877L);
        testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Unit testing", 
            Boolean.FALSE, List.of(revision));
        assertTrue(revision.isCommentDeleted());
        assertTrue(revision.isContentDeleted());
        assertTrue(revision.isUserDeleted());
        // check whether ths state was actually changed on-wiki
        revision = testWiki.getRevision(349877L);
        assertTrue(revision.isCommentDeleted(), "check commentDeleted on-wiki");
        assertTrue(revision.isContentDeleted(), "check contentDeleted on-wiki");
        assertTrue(revision.isUserDeleted(), "check userDeleted on-wiki");
        
        // https://test.wikipedia.org/w/index.php?title=Special%3ALog&type=delete&user=&page=File%3AWiki.java+test5.jpg&year=2018&month=3
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .byUser("MER-C")
            .byTitle("File:Wiki.java test5.jpg")
            .withinDateRange(OffsetDateTime.parse("2018-03-16T00:00:00Z"), OffsetDateTime.parse("2018-03-18T00:00:00Z"));
        List<Wiki.LogEntry> le = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", rh);
        testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, 
            "Unit testing WOOP WOOP WOOP", Boolean.FALSE, le.subList(0, 1));
        // local state is controlled by a superclass, no need to test again
        // still need to check whether ths state was actually changed on-wiki
        le = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", rh);
        assertTrue(le.get(0).isCommentDeleted(), "check commentDeleted on-wiki");
        assertTrue(le.get(0).isContentDeleted(), "check contentDeleted on-wiki");
        assertTrue(le.get(0).isUserDeleted(), "check userDeleted on-wiki");
    }
    
    @AfterAll
    public static void cleanup() throws Exception
    {
        // undo RevisionDelete test on Revision
        Wiki.Revision revision = testWiki.getRevision(349877L);
        testWiki.revisionDelete(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "reset test", 
            Boolean.FALSE, List.of(revision));
        
        // undo RevisionDelete test on LogEntry
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .byUser("MER-C")
            .byTitle("File:Wiki.java test5.jpg")
            .withinDateRange(OffsetDateTime.parse("2018-03-16T00:00:00Z"), OffsetDateTime.parse("2018-03-18T00:00:00Z"));
        List<Wiki.LogEntry> le = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", rh);
        
        testWiki.revisionDelete(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, "reset test", 
            Boolean.FALSE, le.subList(0, 1));
    }
}
