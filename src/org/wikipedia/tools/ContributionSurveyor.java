/**
 *  @(#)ContributionSurveyor.java 0.03 11/03/2017
 *  Copyright (C) 2011-2017 MER-C
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
import java.util.*;
import java.text.SimpleDateFormat;
import javax.security.auth.login.CredentialNotFoundException;
import javax.swing.JFileChooser;

import org.wikipedia.*;

/**
 *  Mass contribution surveyor for use at [[WP:CCI]]. Please use the dedicated
 *  contribution surveyors when possible!
 *
 *  @author MER-C
 *  @version 0.03
 */
public class ContributionSurveyor
{
    public static void main(String[] args) throws IOException
    {
        // placeholders
        boolean images = false, userspace = false;
        Wiki homewiki = Wiki.createInstance("en.wikipedia.org");
        File out = null;
        String wikipage = null;
        String infile = null;
        String category = null;

        // parse arguments
        ArrayList<String> users = new ArrayList<>(1500);
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
                        + "\t--category 'A category'\n\t\tFetch a list of users from the given category (recursive)."
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
                    homewiki = Wiki.createInstance(args[++i]);
                    break;
                case "--outfile":
                    out = new File(args[++i]);
                    break;
                case "--wikipage":
                    wikipage = args[++i];
                    break;
                case "--category":
                    category = args[++i];
                    break;
            }
        }
        
        // fetch user list
        if (!users.isEmpty())
        {
        }
        else if (category != null)
            users.addAll(Arrays.asList(homewiki.getCategoryMembers(category, true, Wiki.USER_NAMESPACE)));
        else if (wikipage != null)
        {
            String[] list = ParserUtils.parseList(homewiki.getPageText(wikipage));
            for (String temp : list)
            {
                if (homewiki.namespace(temp) == Wiki.USER_NAMESPACE)
                {
                    temp = temp.replaceFirst("^" + homewiki.namespaceIdentifier(Wiki.USER_NAMESPACE) + ":", "");
                    users.add(temp);
                }
            }
        }
        else // file IO
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
                if (homewiki.namespace(line) == Wiki.USER_NAMESPACE)
                {
                    // line = line.replace("[*#]\\s?\\[\\[:?", "");
                    // line = line.replace("\\]\\]", "");
                    line = line.replaceFirst("^" + homewiki.namespaceIdentifier(Wiki.USER_NAMESPACE) + ":", "");
                    users.add(line);
                }
            }
        }

        // output file
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
            if (images || userspace)
                out.write("====Mainspace edits (" + user + ")====");
            HashMap<String, StringBuilder> diffs = new HashMap<>(60);
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
                temp.append("[[Special:Diff/");
                temp.append(revision.getRevid());
                temp.append("|(+");
                temp.append(size);
                temp.append(")]]");
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
                String[][] survey = imageContributionSurvey(homewiki, wpuser);
                if (survey[0].length > 0)
                {
                    out.write("====Local uploads (" + user + ")====\n");
                    out.write(ParserUtils.formatList(survey[0]));
                    out.write("\n");
                }
                if (survey[1].length > 0)
                {
                    out.write("====Commons uploads (" + user + ")====\n");
                    out.write(ParserUtils.formatList(survey[1]));
                    out.write("\n");
                }
                if (survey[2].length > 0)
                {
                    out.write("====Transferred uploads (" + user + ")====\n");
                    out.write("WARNING: may be inaccurate, depending on username.");
                    out.write(ParserUtils.formatList(survey[2]));
                    out.write("\n");
                }
            }
        }
        // timestamp
        Date date = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss dd MMMM yyyy");
        out.write("This report generated by [https://github.com/MER-C/wiki-java ContributionSurveyor.java] on "
            + df.format(date) + " (UTC).");
        out.flush();
        out.close();
    }
    
    /**
     *  Performs an image contribution survey on a user.
     *  @param homewiki a wiki
     *  @param user a user on that wiki
     *  @return first element = local uploads, second element = uploads on Wikimedia
     *  Commons by the user, third element = images transferred to Commons (may
     *  be inaccurate depending on username).
     *  @throws IOException if a network error occurs
     */
    public static String[][] imageContributionSurvey(Wiki homewiki, Wiki.User user) throws IOException
    {
        // fetch local uploads
        HashSet<String> localuploads = new HashSet<>(10000);
        for (Wiki.LogEntry upload : homewiki.getUploads(user))
            localuploads.add(upload.getTarget());
        
        // fetch commons uploads
        Wiki commons = Wiki.createInstance("commons.wikimedia.org");
        Wiki.User comuser = commons.getUser(user.getUsername());
        HashSet<String> comuploads = new HashSet<>(10000);
        if (comuser != null)
            for (Wiki.LogEntry upload : commons.getUploads(user))
                comuploads.add(upload.getTarget());
        
        // fetch transferred commons uploads
        HashSet<String> commonsTransfer = new HashSet<>(10000);
        String[][] temp = commons.search("\"" + user + "\"", Wiki.FILE_NAMESPACE);
        for (String[] x : temp)
            commonsTransfer.add(x[0]);

        // remove all files that have been reuploaded to Commons
        localuploads.removeAll(comuploads);
        localuploads.removeAll(commonsTransfer);
        commonsTransfer.removeAll(comuploads);
        
        return new String[][] {
            localuploads.toArray(new String[localuploads.size()]),
            comuploads.toArray(new String[comuploads.size()]),
            commonsTransfer.toArray(new String[commonsTransfer.size()])
        };
    }
    
    /**
     *  Performs a survey of a user's deleted contributions. Requires 
     *  administrator access to the relevant wiki. (Note: due to MediaWiki
     *  limitations, it is not possible to filter by bytes added or whether an
     *  edit created a new page.)
     * 
     *  @param homewiki the user's home wiki
     *  @param username the user to survey
     *  @param ns the namespaces to survey (not specified = all namespaces)
     *  @throws IOException if a network error occurs
     *  @throws CredentialNotFoundException if one cannot view deleted pages
     *  @since 0.03
     */
    public static LinkedHashMap<String, ArrayList<Wiki.Revision>> deletedContributionSurvey(Wiki homewiki, 
        String username, int... ns) throws IOException, CredentialNotFoundException
    {
        Wiki.Revision[] delcontribs = homewiki.deletedContribs(username, null, 
            null, false, ns);
        LinkedHashMap<String, ArrayList<Wiki.Revision>> ret = new LinkedHashMap<>();
        
        // group contributions by page
        for (Wiki.Revision rev : delcontribs)
        {
            String page = rev.getPage();
            if (!ret.containsKey(page))
                ret.put(page, new ArrayList<Wiki.Revision>());
            ret.get(page).add(rev);
        }
        return ret;
    }
}
