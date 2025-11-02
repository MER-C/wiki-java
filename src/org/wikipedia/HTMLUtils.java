/**
 *  @(#)HTMLUtils.java 0.01 26/10/2025
 *  Copyright (C) 2025 - 20xx MER-C
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

/**
 *  Utility methods for generating and parsing HTML that don't belong in any of
 *  the specialist utility classes.
 *  @author MER-C
 *  @version 0.01
 */
public class HTMLUtils
{
    /**
     *  Sanitizes untrusted input for XSS destined for inclusion in the HTML
     *  body.
     *  @param input an input string
     *  @see <a href="https://www.owasp.org/index.php/XSS_Prevention">OWASP XSS
     *  Prevention Cheat Sheet Rule 1</a>
     *  @return the sanitized input or the empty string if input is null
     */
    public static String sanitizeForHTML(String input)
    {
        if (input == null)
            return "";
        return input.replaceAll("&", "&amp;")
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;")
                    .replaceAll("'", "&#x27;")
                    .replaceAll("\"", "&quot;")
                    .replaceAll("/", "&#x2F;");
    }
}
