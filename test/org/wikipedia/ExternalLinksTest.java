/**
 *  @(#)ExternalLinksTest.java 0.01 03/04/2018
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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for {@link org.wikipedia.ExternalLinks}.
 *  @author MER-C
 */
public class ExternalLinksTest
{
    private Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    private ExternalLinks el = ExternalLinks.of(enWiki);
    
    @Test
    public void extractDomain()
    {
        assertEquals("example.com.au", ExternalLinks.extractDomain("http://example.com.au"), "just domain");
        assertEquals("example.com", ExternalLinks.extractDomain("http://example.com/index.html"), "plain URL");
        assertEquals("example.com", ExternalLinks.extractDomain("https://www.example.com:443"), "port");
        assertEquals("example.com", ExternalLinks.extractDomain("https://www.example.com"), "www");
        assertEquals("example.com", ExternalLinks.extractDomain("//www.example.com/test.jsp?param=yes"),
            "protocol relative");
        // unfortunate, but necessary
        assertEquals("test.example.com", ExternalLinks.extractDomain("http://test.example.com/index.html"), 
            "other subdomains not stripped");
        // failures
        assertNull(ExternalLinks.extractDomain("gkskdgds"), "nonsense");
        assertNull(ExternalLinks.extractDomain("http://example.com,"), "ending comma");
        // documenting this common form of broken wikimarkup 
        assertEquals("http", ExternalLinks.extractDomain("http://http://example.com"), "duplicated http");
    }
    
    @Test
    public void isSpamBlacklisted() throws Exception
    {
        assertFalse(el.isSpamBlacklisted("example.com"));
        assertTrue(el.isSpamBlacklisted("youtu.be"));
    }
}
