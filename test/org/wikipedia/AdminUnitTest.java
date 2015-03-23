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
    private static Wiki testWiki, enWiki;
    
    @BeforeClass
    public static void setUpClass()
    {
        testWiki = new Wiki("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
        enWiki = new Wiki("en.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(enWiki);
        enWiki.setMaxLag(-1);
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
