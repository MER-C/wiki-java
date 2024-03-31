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
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInUnitTests
{
    private static Wiki testWiki;
    
    @BeforeAll
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.newSession("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void login() throws Exception
    {
        // one wiki instance = one session
        // https://github.com/MER-C/wiki-java/issues/157
        Wiki enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.getPageText(List.of("Main Page"));
        // should still be logged in (also checks whether the cookies work in
        // in the first place)...
        testWiki.setAssertionMode(Wiki.ASSERT_USER);
        testWiki.getPageText(List.of("Main Page"));
        enWiki.setAssertionMode(Wiki.ASSERT_USER);
        assertThrows(AssertionError.class, () -> enWiki.getPageText(List.of("Main Page")),
            "cross-contamination between sessions");        
    }       
    
    @Test
    public void upload() throws Exception
    {
        try
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
            assertArrayEquals(Files.readAllBytes(expected.toPath()), 
                Files.readAllBytes(actual.toPath()), "upload: image");
            assertEquals(description, testWiki.getPageText(List.of("File:" + uploadDest)).get(0), "upload: description");
            assertEquals(reason, testWiki.getTopRevision("File:" + uploadDest).getComment(), "upload: reason");

            // upload via URL
            // target: https://test.wikipedia.org/wiki/File:Wiki.java_test5.jpg
            // image: https://commons.wikimedia.org/wiki/File:(Tsander)_Large_Impact_Crater,_Lunar_Surface.jpg (1.55 MB)
            description = "Test image. Source (PD-NASA): [[:File:(Tsander) Large Impact Crater, Lunar Surface.jpg]]";
            uploadDest = "Wiki.java test5.jpg";
            reason = "Testing upload via URL";
            testWiki.upload(
                new URI("https://upload.wikimedia.org/wikipedia/commons/b/bc/%28Tsander%29_Large_Impact_Crater%2C_Lunar_Surface.jpg").toURL(),
                uploadDest, description, reason);
            // verify file uploaded is identical to copied image
            expected = File.createTempFile("wikijava_upload3", null);
            testWiki.getImage("(Tsander) Large Impact Crater, Lunar Surface.jpg", expected);
            actual = File.createTempFile("wikijava_upload4", null);
            testWiki.getImage(uploadDest, actual);
            // 1.55 MB file
            assertArrayEquals(Files.readAllBytes(expected.toPath()), 
                Files.readAllBytes(actual.toPath()), "upload via url: image");
            assertEquals(description, testWiki.getPageText(List.of("File:" + uploadDest)).get(0), "upload via url: description");
            assertEquals(reason, testWiki.getTopRevision("File:" + uploadDest).getComment(), "upload via url: reason");
        }
        finally
        {
            // Requires admin rights... otherwise find an admin to delete manually.
            testWiki.delete("File:Wiki.java test4.jpg", "Test cleanup", false);
            testWiki.delete("File:Wiki.java test5.jpg", "Test cleanup", false);
        }
    }
    
    @Test
    public void fileRevert() throws Exception
    {
        String fname = "File:Wiki.java test6.jpg";
        try
        {
            File expected = File.createTempFile("wikijava_filerevert1", null);
            testWiki.upload(
                new URI("https://upload.wikimedia.org/wikipedia/commons/b/bc/%28Tsander%29_Large_Impact_Crater%2C_Lunar_Surface.jpg").toURL(),
                fname, "testing file reversion", "Test image. Source (PD-NASA): [[:File:(Tsander) Large Impact Crater, Lunar Surface.jpg]]");
            testWiki.getImage(fname, expected);
            testWiki.upload(new URI("https://upload.wikimedia.org/wikipedia/commons/0/00/PIA21465_-_North_Polar_Layers.jpg").toURL(),
                fname, "overwriting", "overwriting");
            List<Wiki.LogEntry> le = testWiki.getFileHistory(List.of(fname)).get(0);
            String comment = "test file revert";
            testWiki.fileRevert(fname, le.get(1), comment);
            le = testWiki.getFileHistory(List.of(fname)).get(0);
            
            assertEquals(3, le.size());
            Wiki.LogEntry top = le.get(0);
            assertEquals(comment, top.getComment());
            assertEquals(fname, top.getTitle());
            File actual = File.createTempFile("wikijava_filerevert2", null);
            testWiki.getImage(fname, actual);
            assertArrayEquals(Files.readAllBytes(expected.toPath()), 
                Files.readAllBytes(actual.toPath()), "file revert: image");
        }
        finally
        {
            // Requires admin rights... otherwise find an admin to delete manually.
            testWiki.delete(fname, "Test cleanup", false);
        }
    }
    
    @Test
    public void edit() throws Exception
    {
        String text = "Testing " + Math.random();
        String page = "User:MER-C/BotSandbox";
        String summary = "Test edit " + Math.random();
        testWiki.edit(page, text, summary);
        assertEquals(text, testWiki.getPageText(List.of(page)).get(0), "page text");
        assertEquals(summary, testWiki.getTopRevision(page).getComment(), "edit summary");
    }
}
