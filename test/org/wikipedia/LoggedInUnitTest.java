/**
 *  @(#)LoggedInUnitTest.java 
 *  Copyright (C) 2016 - 2017 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
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

import java.net.URL;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInUnitTest
{
    private static Wiki testWiki;
    
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        testWiki = new Wiki("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void upload() throws Exception
    {
        // upload via URL
        // target: https://test.wikipedia.org/wiki/File:Wiki.java_test5.jpg
        String description = "Test image. Source (PD-NASA): [[:File:(Tsander) Large Impact Crater, Lunar Surface.jpg]]";
        String uploadDest = "Wiki.java test5.jpg";
        String reason = "Testing upload via URL";
        testWiki.upload(new URL("https://upload.wikimedia.org/wikipedia/commons/b/bc/%28Tsander%29_Large_Impact_Crater%2C_Lunar_Surface.jpg"), 
            uploadDest, description, reason);
        byte[] expected = testWiki.getImage("(Tsander) Large Impact Crater, Lunar Surface.jpg");
        byte[] actual = testWiki.getImage(uploadDest);
        assertArrayEquals("upload via url: image", expected, actual);
        assertEquals("upload via url: description", description, testWiki.getPageText("File:" + uploadDest));
        assertEquals("upload via url: reason", reason, testWiki.getTopRevision("File:" + uploadDest).getSummary());
        // WARNING: delete the image afterwards or supply a new target
    }
    
    @Test
    public void edit() throws Exception
    {
        String text = "Testing " + Math.random();
        String page = "User:MER-C/BotSandbox";
        String summary = "Test edit " + Math.random();
        testWiki.edit(page, text, summary);
        assertEquals("edit: page text", text, testWiki.getPageText(page));
        assertEquals("edit: summary", summary, testWiki.getTopRevision(page).getSummary());
    }
}
