/**
 *  @(#)IndianEducationCCI.java 0.01 04/11/2011
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

package org.wikipedia.tools;

import java.io.*;
import java.util.*;

import org.wikipedia.*;

/**
 *  Instructions:
 *  # Compile. You need my bot framework to do so.
 *  # Place a duplicate free IEP student list in the same directory as
 *    IndianEducationCCI.class with filename "iepstudents.txt".
 *  # Run and wait.
 *
 *  @author MER-C
 *  @version 0.01
 */
public class IndianEducationCCI
{
    public static void main(String[] args) throws IOException
    {
        Wiki enWiki = new Wiki("en.wikipedia.org");
        Wiki commons = new Wiki("commons.wikimedia.org");
        // this program started in June
        GregorianCalendar cal = new GregorianCalendar(2011, 05, 01);

        // file I/O
        ArrayList<String> users = new ArrayList<String>(1500);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            IndianEducationCCI.class.getResourceAsStream("iepstudents.txt")));
        String line;
        while ((line = in.readLine()) != null)
            users.add(line.substring(5)); // lop off the User: prefix
        FileWriter out = new FileWriter("iep.txt");

        for (String user : users)
        {
            // determine if user exists; if so, stats
            Wiki.User u = enWiki.getUser(user);
            int editcount = 0;
            if (u != null)
                editcount = u.countEdits();
            else
            {
                System.out.println(user + " is not a registered user.");
                continue;
            }
            Wiki.Revision[] contribs = enWiki.contribs(user);
            out.write("===" + user + "===\n");
            out.write("*{{user5|" + user + "}}\n");
            out.write("*Total edits: " + editcount + ", Live edits: " + contribs.length +
                ", Deleted edits: " + (editcount - contribs.length) + "\n\n");
            // any user with over 200 live edits should be handled by the dedicated
            // contribution surveyor
            if (contribs.length >= 200)
            {
                out.write("User has too many live edits for this hack. Use the [http://"
                    + " toolserver.org/~dcoetzee/contributionsurveyor/index.php Contribution Surveyor].\n\n");
                continue;
            }
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
                Wiki.Revision[] history = enWiki.getPageHistory(title, revision.getTimestamp(), cal);
                if (history.length == 0)
                {
                    System.out.println(user + " has contributions prior to the IEP.");
                    continue;
                }
                int size = history.length == 1 ? revision.getSize() : revision.getSize() - history[1].getSize();
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
            out.write(";Local uploads\n");
            Wiki.LogEntry[] uploads = enWiki.getLogEntries(null, null, Integer.MAX_VALUE, Wiki.UPLOAD_LOG, "", u, "", Wiki.ALL_NAMESPACES);
            HashSet<String> list = new HashSet<String>(10000);
            for (int i = 0; i < uploads.length; i++)
                list.add((String)uploads[i].getTarget());
            if (uploads.length == 0)
                out.write("No local uploads.\n");
            else
                out.write(ParserUtils.formatList(list.toArray(new String[0])));
            out.write("\n");

            // commons
            out.write(";Commons uploads\n");
            uploads = commons.getLogEntries(null, null, Integer.MAX_VALUE, Wiki.UPLOAD_LOG, "", u, "", Wiki.ALL_NAMESPACES);
            list.clear();
            for (int i = 0; i < uploads.length; i++)
                list.add((String)uploads[i].getTarget());
            if (uploads.length == 0)
                out.write("No Commons uploads.\n");
            else
                out.write(ParserUtils.formatList(list.toArray(new String[0])));
            out.write("\n");
        }
        out.flush();
        out.close();
    }
}
