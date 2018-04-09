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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Tests for {@link org.wikipedia.ExternalLinks}.
 *  @author MER-C
 */
public class ExternalLinksTest
{
    @Test
    public void extractDomain()
    {
        assertEquals("extractDomain: just domain", "example.com.au", 
            ExternalLinks.extractDomain("http://example.com.au"));
        assertEquals("extractDomain: plain URL", "example.com", 
            ExternalLinks.extractDomain("http://example.com/index.html"));
        assertEquals("extractDomain: port", "example.com", 
            ExternalLinks.extractDomain("https://www.example.com:443"));
        assertEquals("extractDomain: www", "example.com", 
            ExternalLinks.extractDomain("https://www.example.com"));
        assertEquals("extractDomain: protocol relative", "example.com",
            ExternalLinks.extractDomain("//www.example.com/test.jsp?param=yes"));
        // unfortunate, but necessary
        assertEquals("extractDomain: other subdomains not stripped", 
            "test.example.com", ExternalLinks.extractDomain("http://test.example.com/index.html"));
        // failures
        assertNull("extractDomain: nonsense", ExternalLinks.extractDomain("gkskdgds"));
        assertNull("extractDomain: not quite right", ExternalLinks.extractDomain("http://example.com,"));
        // documenting this common form of broken wikimarkup 
        assertEquals("extractDomain: http", "http", ExternalLinks.extractDomain("http://http://example.com"));
    }
}
