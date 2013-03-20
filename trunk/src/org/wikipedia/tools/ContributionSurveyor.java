/**
 *  @(#)ContributionSurveyor.java 0.02 01/03/2011
 *  Copyright (C) 2011-2013 MER-C
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

package org.wikipedia.tools;

import java.io.*;
import java.util.*;

import org.wikipedia.*;

/**
 *  Mass contribution surveyor for use at [[WP:CCI]]. Please use the dedicated
 *  contribution surveyors when possible!
 *
 *  @author MER-C
 *  @version 0.02
 */
public class ContributionSurveyor
{
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        Wiki commons = new Wiki("commons.wikimedia.org");

        // file I/O
        // file must contain list of users, one per line
        ArrayList<String> users = new ArrayList<String>(1500);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            ContributionSurveyor.class.getResourceAsStream("users.txt")));
        String line;
        while ((line = in.readLine()) != null)
            users.add(line.substring(5)); // lop off the User: prefix
        FileWriter out = new FileWriter("masscci.txt");

        for (String user : users)
        {
            // determine if user exists; if so, stats
            Wiki.User wpuser = enWiki.getUser(user);
            Wiki.User comuser = commons.getUser(user);
            int editcount = wpuser.countEdits();
            if (wpuser == null)
            {
                System.out.println(user + " is not a registered user.");
                continue;
            }
            Wiki.Revision[] contribs = enWiki.contribs(user);
            out.write("===" + user + "===\n");
            out.write("*{{user5|" + user + "}}\n");
            out.write("*Total edits: " + editcount + ", Live edits: " + contribs.length +
                ", Deleted edits: " + (editcount - contribs.length) + "\n\n");
            out.write(";Mainspace edits");

            // survey mainspace edits
            HashMap<String, StringBuilder> diffs = new HashMap<String, StringBuilder>(60);
            for (Wiki.Revision revision : contribs)
            {
                String title = revision.getPage();
                // check only mainspace edits
                int ns = enWiki.namespace(title);
                if (ns != Wiki.MAIN_NAMESPACE)
                    continue;
                // compute diff size; too small => skip
                int size = revision.getSizeDiff();
                if (size < 150)
                    continue;
                // place to dump diffs
                if (!diffs.containsKey(title))
                {
                    StringBuilder temp = new StringBuilder(500);
                    temp.append("\n*[[:");
                    temp.append(title);
                    temp.append("]]: ");
                    diffs.put(title, temp);
                }
                StringBuilder temp = diffs.get(title);
                temp.append("{{dif|");
                temp.append(revision.getRevid());
                temp.append("|(+");
                temp.append(size);
                temp.append(")}}");
                diffs.put(title, temp);
            }
            // spit out the results of the survey
            for (Map.Entry<String, StringBuilder> entry : diffs.entrySet())
                out.write(entry.getValue().toString());
            if (diffs.isEmpty())
                out.write("\nNo major mainspace contributions.");
            out.write("\n\n");

            // survey userspace
            out.write(";Userspace edits\n");
            HashSet<String> temp = new HashSet(50);
            for (Wiki.Revision revision : contribs)
            {
                String title = revision.getPage();
                // check only userspace edits
                int ns = enWiki.namespace(title);
                if (ns != Wiki.USER_NAMESPACE)
                    continue;
                temp.add(title);
            }
            if (temp.isEmpty())
                out.write("No userspace edits.\n");
            else
                out.write(ParserUtils.formatList(temp.toArray(new String[0])));
            out.write("\n");

            // survey images
            Wiki.LogEntry[] uploads = enWiki.getUploads(wpuser);
            if (uploads.length > 0)
            {
                out.write(";Local uploads\n");
                HashSet<String> list = new HashSet<String>(10000);
                for (int i = 0; i < uploads.length; i++)
                    list.add((String)uploads[i].getTarget());
                out.write(ParserUtils.formatList(list.toArray(new String[0])));
                out.write("\n");
            }
            else
                out.write("No local uploads.\n");

            // commons
            uploads = commons.getUploads(comuser);
            if (uploads.length > 0)
            {
                out.write(";Commons uploads\n");
                HashSet<String> list = new HashSet<String>(10000);
                for (int i = 0; i < uploads.length; i++)
                    list.add((String)uploads[i].getTarget());
                out.write(ParserUtils.formatList(list.toArray(new String[0])));
                out.write("\n");
            }
            else
                out.write("No Commons uploads.\n");
        }
        out.flush();
        out.close();
    }
}
