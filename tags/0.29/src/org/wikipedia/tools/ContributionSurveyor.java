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
import java.text.SimpleDateFormat;
import javax.swing.JFileChooser;

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
        // placeholders
        boolean images = false, userspace = false;
        Wiki homewiki = new Wiki("en.wikipedia.org");
        File out = null;
        String wikipage = null;
        String infile = null;

        // parse arguments
        ArrayList<String> users = new ArrayList<String>(1500);
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            switch (arg)
            {
                case "--help":
                    System.out.println("SYNOPSIS:\n\t java org.wikipedia.tools.ContributionSurveyor [options]\n\n"
                        + "DESCRIPTION:\n\tSurvey the contributions of a large number of wiki editors.\n\n"
                        + "\t--help\n\t\tPrints this screen and exits.\n"
                        + "\t--images\n\t\tSurvey images both on the home wiki and Commons.\n"
                        + "\t--infile file\n\t\tUse file as the list of users, shows a filechooser if not "
                            + "specified.\n"
                        + "\t--wiki example.wikipedia.org\n\t\tUse example.wikipedia.org as the home wiki. \n\t\t"
                            + "Default: en.wikipedia.org.\n"
                        + "\t--outfile file\n\t\tSave results to file, shows a filechooser if not specified.\n"
                        + "\t--wikipage 'Main Page'\n\t\tFetch a list of users at the wiki page Main Page.\n"
                        + "\t--user user\n\t\tSurvey the given user.\n"
                        + "\t--userspace\n\t\tSurvey userspace as well.\n");

                    System.exit(0);
                case "--images":
                    images = true;
                    break;
                case "--infile":
                    infile = args[++i];
                    break;
                case "--user":
                    users.add(args[++i]);
                    break;
                case "--userspace":
                    userspace = true;
                    break;
                case "--wiki":
                    homewiki = new Wiki(args[++i]);
                    break;
                case "--outfile":
                    out = new File(args[++i]);
                    break;
                case "--wikipage":
                    wikipage = args[++i];
                    break;
            }
        }
        // file I/O
        // file must contain list of users, one per line
        if (users.isEmpty())
        {
            if (wikipage == null)
            {
                BufferedReader in = null;
                if (infile == null)
                {
                    JFileChooser fc = new JFileChooser();
                    fc.setDialogTitle("Select user list");
                    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                        in = new BufferedReader(new FileReader(fc.getSelectedFile()));
                    else
                    {
                        System.out.println("Error: No input file selected.");
                        System.exit(0);
                    }
                }
                else
                    in = new BufferedReader(new FileReader(infile));
                String line;
                while ((line = in.readLine()) != null)
                {
                    line = line.replaceFirst("^" + homewiki.namespaceIdentifier(Wiki.USER_NAMESPACE) + ":", "");
                    users.add(line);
                }
            }
            else
            {
                String[] list = ParserUtils.parseList(homewiki.getPageText(wikipage));
                for (String temp : list)
                {
                    temp = temp.replaceFirst("^" + homewiki.namespaceIdentifier(Wiki.USER_NAMESPACE) + ":", "");
                    users.add(temp);
                }
            }
        }
        if (out == null)
        {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select output file");
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
                out = fc.getSelectedFile();
            else
            {
                System.out.println("Error: No output file selected.");
                System.exit(0);
            }
        }
        contributionSurvey(homewiki, users.toArray(new String[users.size()]), out, userspace, images);
    }
    
    /**
     *  Performs a mass contribution survey.
     *  @param homewiki the wiki to survey on
     *  @param users the users to survey
     *  @param output the output file to write to
     *  @param userspace whether to survey output
     *  @param images whether to survey images (searches Commons as well)
     *  @throws IOException if a network error occurs
     *  @since 0.02
     */
    public static void contributionSurvey(Wiki homewiki, String[] users, File output, boolean userspace, boolean images) throws IOException
    {
        FileWriter out = new FileWriter(output);
        Wiki commons = new Wiki("commons.wikimedia.org");
        for (String user : users)
        {
            // determine if user exists; if so, stats
            Wiki.Revision[] contribs = homewiki.contribs(user);
            out.write("===" + user + "===\n");
            out.write("*{{user5|" + user + "}}\n");
            Wiki.User wpuser = homewiki.getUser(user);
            if (wpuser != null)
            {
                int editcount = wpuser.countEdits();
                out.write("*Total edits: " + editcount + ", Live edits: " + contribs.length +
                ", Deleted edits: " + (editcount - contribs.length) + "\n\n");
            }
            else
                System.out.println(user + " is not a registered user.");

            // survey mainspace edits
            out.write("====Mainspace edits (" + user + ")====");
            HashMap<String, StringBuilder> diffs = new HashMap<String, StringBuilder>(60);
            for (Wiki.Revision revision : contribs)
            {
                String title = revision.getPage();
                // check only mainspace edits
                int ns = homewiki.namespace(title);
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
            if (userspace)
            {
                out.write("====Userspace edits (" + user + ")====\n");
                HashSet<String> temp = new HashSet(50);
                for (Wiki.Revision revision : contribs)
                {
                    String title = revision.getPage();
                    // check only userspace edits
                    int ns = homewiki.namespace(title);
                    if (ns != Wiki.USER_NAMESPACE)
                        continue;
                    temp.add(title);
                }
                if (temp.isEmpty())
                    out.write("No userspace edits.\n");
                else
                    out.write(ParserUtils.formatList(temp.toArray(new String[temp.size()])));
                out.write("\n");
            }

            // survey images
            if (images && wpuser != null)
            {
                Wiki.User comuser = commons.getUser(user);
                Wiki.LogEntry[] uploads = homewiki.getUploads(wpuser);
                if (uploads.length > 0)
                {
                    out.write("====Local uploads (" + user + ")====\n");
                    HashSet<String> list = new HashSet<String>(10000);
                    for (Wiki.LogEntry upload : uploads)
                        list.add(upload.getTarget());
                    out.write(ParserUtils.formatList(list.toArray(new String[list.size()])));
                    out.write("\n");
                }

                // commons
                uploads = commons.getUploads(comuser);
                if (uploads.length > 0)
                {
                    out.write("====Commons uploads (" + user + ")====\n");
                    HashSet<String> list = new HashSet<String>(10000);
                    for (Wiki.LogEntry upload : uploads)
                        list.add(upload.getTarget());
                    out.write(ParserUtils.formatList(list.toArray(new String[list.size()])));
                    out.write("\n");
                }
            }
        }
        // timestamp
        Date date = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss dd MMMM yyyy");
        out.write("This report generated by [https://code.google.com/p/wiki-java ContributionSurveyor.java] on "
            + df.format(date) + " (UTC).");
        out.flush();
        out.close();
    }
}
