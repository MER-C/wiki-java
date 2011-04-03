/**
 *  @(#)ServletUtils.java 0.01 22/02/2011
 *  Copyright (C) 2011 MER-C
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

package org.wikipedia.servlets;

/**
 *  Common servlet code so that I can maintain it easier.
 *  @author MER-C
 */
public class ServletUtils
{
    /**
     *  Generates a boilerplate GPLv3 footer given a tool name
     *  @param toolname the name of the tool
     */
    public static String generateFooter(String toolname)
    {
        // don't forget to update timeout.html and index.html
        StringBuilder sb = new StringBuilder(500);
        sb.append("<hr>\n<p>");
        sb.append(toolname);
        sb.append(": Copyright (C) MER-C 2007-2011. This tool is free software: ");
        sb.append("you can redistribute it and/or modify it\nunder the terms of ");
        sb.append("the GNU General Public License as published by the Free ");
        sb.append("Software Foundation, either version 3 of the License, or (at ");
        sb.append("your\noption) any later version.\n<p>Source code is available ");
        sb.append("<a href=\"http://code.google.com/p/wiki-java\">here</a>. Report ");
        sb.append("bugs at <a href=\"http://en.wikipedia.org/wiki/User_talk:MER-C\">my ");
        sb.append("talk page</a>\n(fast) or the tracker associated with the source ");
        sb.append("(slow).\n\n</body>\n</html>");
        return sb.toString();
    }
}
