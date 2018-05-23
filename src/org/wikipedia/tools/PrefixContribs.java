/**
 *  @(#)PrefixContribs.java 0.01 15/10/2012
 *  Copyright (C) 2012 - 2016 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.wikipedia.tools;

import java.io.*;
import java.time.*;
import java.util.*;
import javax.swing.JOptionPane;
import org.wikipedia.*;

/**
 *  Fetches contributions from an IP address range or a group of users that
 *  have a common prefix. Offline version of org/wikipedia/servlets/prefixcontribs.jsp.
 *  @author MER-C
 *  @version 0.01
 */
public class PrefixContribs
{
    /**
     *  Pipe the output to a HTML file.
     *  @param args the command line arguments (none expected)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        String prefix = JOptionPane.showInputDialog(null, "Enter query string");
        if (prefix == null)
            System.exit(0);
        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .withinDateRange(OffsetDateTime.now(ZoneOffset.UTC).minusDays(7), null);
        List<Wiki.Revision> revisions = enWiki.prefixContribs(prefix, rh);
        if (revisions.isEmpty())
            System.out.println("No contributions found.");
        else
            System.out.println(ParserUtils.revisionsToHTML(enWiki, revisions.toArray(new Wiki.Revision[0])));
    }
}
