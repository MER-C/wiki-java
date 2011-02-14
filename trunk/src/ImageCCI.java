/**
 *  @(#)ImageCCI.java 0.01 22/01/2011
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
 *  Crude tool for generating image CCIs for users. See [[WP:CCI]].
 */
public class ImageCCI
{
    private static HashSet list = new HashSet(1000);

    public static void main(String[] args) throws IOException
    {
        String user = JOptionPane.showInputDialog(null, "Enter user to survey");
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(user + ".txt"), "UTF-8");
        conductCCI("en.wikipedia.org", user, out);
        conductCCI("commons.wikimedia.org", user, out);
        out.close();
        System.exit(0);
    }

    public static void conductCCI(String w, String u, OutputStreamWriter out) throws IOException
    {
        Wiki wiki = new Wiki(w);
        wiki.setMaxLag(0);
        Wiki.User user = wiki.getUser(u);

        Wiki.LogEntry[] entries = wiki.getLogEntries(null, null, Integer.MAX_VALUE, Wiki.UPLOAD_LOG, user, "", Wiki.ALL_NAMESPACES);
        for (int i = 0; i < entries.length; i++)
        {
            if (i == 0)
                out.write("===Uploads on " + w + " ===\n");
            int size = list.size();
            String page = entries[i].getTarget();
            if (size % 20 == 0 && !list.contains(page))
                out.write("\n====Files " + (size + 1) + " to " + (size + 21) + " ====\n");
            // remove duplicates
            if (!list.contains(page))
            {
                out.write("*[[:" + page + "]]\n");
                list.add(page);
            }
        }
        list.clear();
        out.write("\n");
    }
}
