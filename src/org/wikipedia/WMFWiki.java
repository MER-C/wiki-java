/**
 *  @(#)WMFWiki.java 0.01 29/03/2011
 *  Copyright (C) 2011 MER-C
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

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 *  Stuff specific to Wikimedia wikis.
 *  @author MER-C
 *  @version 0.01
 */
public class WMFWiki extends Wiki
{
    private static final Logger logger = Logger.getLogger("wiki");

    /**
     *  Creates a new WMF wiki that represents the English Wikipedia.
     */
    public WMFWiki()
    {
        super("en.wikipedia.org");
    }

    /**
     *  Creates a new WMF wiki that has the given domain name.
     *  @param domain a WMF wiki domain name e.g. en.wikipedia.org
     */
    public WMFWiki(String domain)
    {
        super(domain);
    }

    /**
     *  Returns the list of publicly readable and editable wikis operated by the
     *  Wikimedia Foundation.
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public static WMFWiki[] getSiteMatrix() throws IOException
    {
        WMFWiki wiki = new WMFWiki("en.wikipedia.org");
        wiki.setMaxLag(0);
        String line = wiki.fetch("http://en.wikipedia.org/w/api.php?format=xml&action=sitematrix", "WMFWiki.getSiteMatrix", false);
        ArrayList<WMFWiki> wikis = new ArrayList<WMFWiki>(1000);

        // form: <special url="http://wikimania2007.wikimedia.org" code="wikimania2007" fishbowl="" />
        // <site url="http://ab.wiktionary.org" code="wiktionary" closed="" />
        for (int x = line.indexOf("url=\""); x >= 0; x = line.indexOf("url=\"", x))
        {
            int a = line.indexOf("http://", x) + 7;
            int b = line.indexOf('\"', a);
            int c = line.indexOf("/>", b);
            x = c;
            
            // check for closed/fishbowl/private wikis
            String temp = line.substring(b, c);
            if (temp.contains("closed=\"\"") || temp.contains("private=\"\"") || temp.contains("fishbowl=\"\""))
                continue;
            wikis.add(new WMFWiki(line.substring(a, b)));
        }
        logger.log(Level.INFO, "Successfully retrieved site matrix ({0} wikis).", wikis.size());
        return wikis.toArray(new WMFWiki[0]);
    }
}
