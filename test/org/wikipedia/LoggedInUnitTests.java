/**
 *  @(#)LoggedInUnitTest.java 
 *  Copyright (C) 2016 - 2018 MER-C
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

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInUnitTests
{
    private static Wiki testWiki;
    
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.createInstance("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void upload() throws Exception
    {
        // ordinary upload
        // target: https://test.wikipedia.org/wiki/File:Wiki.java_test4.jpg
        // image: https://commons.wikimedia.org/wiki/File:PIA21465_-_North_Polar_Layers.jpg (6.2 MB)
        // Note file size < 4 GB, so can read into an array and is large enough
        // to require two chunks on default settings.
        File expected = File.createTempFile("wikijava_upload1", null);
        testWiki.getImage("PIA21465 - North Polar Layers.jpg", expected);
        String description = "Test image. Source (PD-NASA): [[:File:PIA21465 - North Polar Layers.jpg]]. ∑∑ƒ∂ß";
        String reason = "Testing upload. ∑∑ƒ∂ß";
        String uploadDest = "Wiki.java test4.jpg";
        testWiki.upload(expected, uploadDest, description, reason);
        // verify file uploaded is identical to image
        File actual = File.createTempFile("wikijava_upload2", null);
        testWiki.getImage(uploadDest, actual);
        assertArrayEquals("upload: image", Files.readAllBytes(expected.toPath()), 
            Files.readAllBytes(actual.toPath()));
        assertEquals("upload: description", description, testWiki.getPageText("File:" + uploadDest));
        assertEquals("upload: reason", reason, testWiki.getTopRevision("File:" + uploadDest).getComment());
        
        // upload via URL
        // target: https://test.wikipedia.org/wiki/File:Wiki.java_test5.jpg
        // image: https://commons.wikimedia.org/wiki/File:(Tsander)_Large_Impact_Crater,_Lunar_Surface.jpg (1.55 MB)
        description = "Test image. Source (PD-NASA): [[:File:(Tsander) Large Impact Crater, Lunar Surface.jpg]]";
        uploadDest = "Wiki.java test5.jpg";
        reason = "Testing upload via URL";
        testWiki.upload(new URL("https://upload.wikimedia.org/wikipedia/commons/b/bc/%28Tsander%29_Large_Impact_Crater%2C_Lunar_Surface.jpg"), 
            uploadDest, description, reason);
        // verify file uploaded is identical to copied image
        expected = File.createTempFile("wikijava_upload3", null);
        testWiki.getImage("(Tsander) Large Impact Crater, Lunar Surface.jpg", expected);
        actual = File.createTempFile("wikijava_upload4", null);
        testWiki.getImage(uploadDest, actual);
        // 1.55 MB file
        assertArrayEquals("upload via url: image", Files.readAllBytes(expected.toPath()), 
            Files.readAllBytes(actual.toPath()));
        assertEquals("upload via url: description", description, testWiki.getPageText("File:" + uploadDest));
        assertEquals("upload via url: reason", reason, testWiki.getTopRevision("File:" + uploadDest).getComment());
    }
    
    @Test
    public void edit() throws Exception
    {
        String text = "Testing " + Math.random();
        String page = "User:MER-C/BotSandbox";
        String summary = "Test edit " + Math.random();
        testWiki.edit(page, text, summary);
        assertEquals("edit: page text", text, testWiki.getPageText(page));
        assertEquals("edit: summary", summary, testWiki.getTopRevision(page).getComment());
    }
    
    @AfterClass
    public static void cleanup() throws Exception
    {
        // Requires admin rights... otherwise find an admin to delete manually.
        testWiki.delete("File:Wiki.java test4.jpg", "Test cleanup");
        testWiki.delete("File:Wiki.java test5.jpg", "Test cleanup");
    }
}
