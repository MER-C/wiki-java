/**
 *  @(#)NPPCheckTest.java 0.01 20/06/2019
 *  Copyright (C) 2019 - 20xx MER-C
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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.wikipedia.*;

/**
 *  Unit tests for {@link NPPCheck}.
 *  @author MER-C
 */
public class NPPCheckTest
{
    private final NPPCheck check;
    private final WMFWiki enWiki;
    
    public NPPCheckTest()
    {
        enWiki = WMFWiki.newSession("en.wikipedia.org");
        check = new NPPCheck(enWiki);
    }
    
    @Test
    public void setUser()
    {
        check.setReviewer("Blah");        
        check.setReviewer(null);
        assertNull(check.getUser());
        check.setReviewer("Blah");
        assertEquals("Blah", check.getUser());
        // empty string = no user (for servlets)
        check.setReviewer("");
        assertNull(check.getUser());
    }

    @Test
    public void outputTableHeader()
    {
        String wikitableheader = "{| class=\"wikitable sortable\"\n";
        
        // unpatrolled content (no users)
        String expected = "! Title !! Create timestamp !! Size !! Author !! " +
            "Author registration timestamp !! Author edit count !! Author age at creation !! " +
            "Author blocked !! Snippet\n";
        check.setMode(NPPCheck.Mode.UNPATROLLED);
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        check.setMode(NPPCheck.Mode.REDIRECTS);
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        
        // setting user should be irrelevant for patrolled content
        check.setMode(NPPCheck.Mode.UNPATROLLED);
        check.setReviewer("MER-C");
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        check.setMode(NPPCheck.Mode.REDIRECTS);
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        
        // mainspace patrolled content for all reviewers
        expected = "! Title !! Create timestamp !! Review timestamp !! Age at review !! Size !! " +
            "Author !! Author registration timestamp !! Author edit count !! Author age at creation !! " +
            "Author blocked !! Reviewer !! Reviewer edit count !! Snippet\n";
        check.setMode(NPPCheck.Mode.PATROLS);
        check.setReviewer(null);
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        // mainspace patrolled content for a single reviewers
        expected = "! Title !! Create timestamp !! Review timestamp !! Age at review !! " +
            "Time between reviews !! Size !! Author !! Author registration timestamp !! Author edit count !! " +
            "Author age at creation !! Author blocked !! Snippet\n";
        check.setReviewer("MER-C");
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        
        // patrolled content in other namespaces for all reviewers
        expected = "! Draft !! Title !! Create timestamp !! Review timestamp !! Age at review !! Size !! " +
            "Author !! Author registration timestamp !! Author edit count !! Author age at creation !! " +
            "Author blocked !! Reviewer !! Reviewer edit count !! Snippet\n";
        check.setMode(NPPCheck.Mode.DRAFTS);
        check.setReviewer(null);
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        check.setMode(NPPCheck.Mode.USERSPACE);
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        
        // patrolled content in other namespaces for a given reviewer
        expected = "! Draft !! Title !! Create timestamp !! Review timestamp !! Age at review !! " +
            "Time between reviews !! Size !! Author !! Author registration timestamp !! Author edit count !! " +
            "Author age at creation !! Author blocked !! Snippet\n";
        check.setMode(NPPCheck.Mode.DRAFTS);
        check.setReviewer("MER-C");
        assertEquals(wikitableheader + expected, check.outputTableHeader());
        check.setMode(NPPCheck.Mode.USERSPACE);
        assertEquals(wikitableheader + expected, check.outputTableHeader());        
    }
}
