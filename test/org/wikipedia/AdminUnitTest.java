/**
 *  @(#)AdminUnitTest.java 0.31 29/08/2015
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

package org.wikipedia;

import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
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
    }
    
    /**
     *  See https://test.wikipedia.org/w/index.php?title=Special%3ALog&page=User%3AMER-C%2FTest.
     *  @throws Exception if something goes wrong
     */
    @Test
    public void getLogEntries() throws Exception
    {
        Wiki.LogEntry[] le = testWiki.getLogEntries(null, null, null, "User:MER-C/Test");
        assertEquals("getLogEntries: RevisionDeleted reason, can access", 
            "create a log entry for testing RevisionDelete on", le[0].getReason());
        assertEquals("getLogEntries: RevisionDeleted user, can access", "MER-C", 
            le[0].getUser().getUsername());
    }
    
    @Test
    public void getDeletedText() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/Delete
        String text = testWiki.getDeletedText("User:MER-C/UnitTests/Delete");
        assertEquals("getDeletedText", text, "This revision is also deleted!");
        
        // https://test.wikipedia.org/wiki/Special:Undelete/Tfs;hojfsdhp;osjfeas;lioejg
        assertNull("getDeletedText: page never deleted", testWiki.getDeletedText("Tfs;hojfsdhp;osjfeas;lioejg"));
        
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/EmptyDelete
        text = testWiki.getDeletedText("User:MER-C/UnitTests/EmptyDelete");
        assertEquals("getDeletedText: empty", testWiki.getDeletedText("User:MER-C/UnitTests/EmptyDelete"), "");
    }
    
    /**
     *  Fetching revisions of deleted pages.
     *  @throws Exception if something goes wrong
     */
    @Test
    public void revisionGetText() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/EmptyDelete
        Wiki.Revision rev = testWiki.getRevision(323866L);
        assertEquals("Revision.getText: empty", rev.getText(), "");
        
        // https://test.wikipedia.org/wiki/Special:Undelete/User:MER-C/UnitTests/Delete
        // most recent deleted revision
        // page exists, but has deleted revisions (currently broken)
        // rev = testWiki.getRevision(217079L);
        // assertEquals("Revision.getText", rev.getText(), "This revision is also deleted!");
    }
}
