/**
 *  @(#)XWikiLinksearch.java 0.01 14/02/2011
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

import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 *  A crude replacement for Eagle's cross-wiki linksearch tool. The results
 *  are found in results.html.
 *  @author MER-C
 */
public class XWikiLinksearch
{
    public static void main(String[] args) throws IOException
    {
        String[] wikis = { "en", "de", "fr", "pl", "it", "ja", "es", "nl", "pt", "ru",
            "sv", "zh", "ca", "no", "fi", "uk", "hu", "cs", "ro" };
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("results.html"), "UTF-8");
        String domain = JOptionPane.showInputDialog(null, "Enter domain to search");
        for (int i = 0; i < wikis.length; i++)
        {
            Wiki wiki = new Wiki(wikis[i] + ".wikipedia.org");
            wiki.setMaxLag(0);
            ArrayList[] temp = wiki.spamsearch("*." + domain);
            out.write("Results for " + wikis[i] + ".wikipedia.org:\n<ol>");
            for (int j = 0; j < temp[0].size(); j++)
            {
                out.write("<li><a href=\"http://");
                out.write(wikis[i]);
                out.write(".wikipedia.org/wiki/");
                out.write((String)temp[0].get(j));
                out.write("\">");
                out.write((String)temp[0].get(j));
                out.write("</a> uses link <a href=\"");
                out.write(temp[1].get(j).toString());
                out.write("\">");
                out.write(temp[1].get(j).toString());
                out.write("</a><br>");
            }
            out.write("</ol>\n");
        }
        out.close();
        System.exit(0);
    }
}