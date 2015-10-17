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
    public static void setUpClass()
    {
        testWiki = new Wiki("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
    }
    

    /**
     *  See https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
     *  @throws Exception if something goes wrong
     */
    @Test
    public void getDeletedText() throws Exception
    {
        String text = testWiki.getDeletedText("User:MER-C/UnitTests/Delete");
        assertEquals("getDeletedText", text, "This revision is also deleted!");
        assertNull("getDeletedText: page never deleted", testWiki.getDeletedText("Tfs;hojfsdhp;osjfeas;lioejg"));
    }
    
    /**
     *  See https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
     *  @throws Exception if something goes wrong
     */
    @Test
    public void RevisionGetText() throws Exception
    {
        Wiki.Revision deleted = testWiki.getRevision(217078L);
        assertEquals(deleted.getText(), "This revision is deleted!");
    }
}
