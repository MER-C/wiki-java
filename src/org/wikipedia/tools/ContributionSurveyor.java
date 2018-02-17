/**
 *  @(#)ContributionSurveyor.java 0.04 25/01/2018
 *  Copyright (C) 2011-2018 MER-C
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
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import javax.security.auth.login.CredentialNotFoundException;
import javax.swing.JFileChooser;

import org.wikipedia.*;

/**
 *  This tool surveys the major edits and image uploads of a list of users so
 *  that they may be triaged for problems.
 *
 *  @author MER-C
 *  @see <a href="https://wikipediatools.appspot.com/contributionsurveyor.jsp">
 *  Text contribution surveyor (online version)</a>
 *  @see <a href="https://wikipediatools.appspot.com/imagecci.jsp">Image
 *  contribution surveyor (online version)</a>
 *  @see <a href="https://en.wikipedia.org/wiki/WP:CCI">Contributor Copyright
 *  Investigations</a>
 *  @version 0.04
 */
public class ContributionSurveyor
{
    private final Wiki wiki;
    private OffsetDateTime earliestdate, latestdate;
    private boolean nominor = true;
    private int minsizediff = 150;
    
    /**
     *  Runs this program.
     *  @param args command line arguments (see --help below)
     *  @throws IOException 
     */
    public static void main(String[] args) throws IOException
    {
        // placeholders
        boolean images = false, userspace = false, nominor = true;
        int minsize = 150;
        OffsetDateTime start = null, end = null;
        Wiki homewiki = Wiki.createInstance("en.wikipedia.org");
        File out = null;
        String wikipage = null;
        String infile = null;
        String category = null;

        // parse arguments
        List<String> users = new ArrayList<>(1500);
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
                        + "\t--category 'A category'\n\t\tFetch a list of users from the given category (recursive).\n"
                        + "\t--user user\n\t\tSurvey the given user.\n"
                        + "\t--userspace\n\t\tSurvey userspace as well.\n"
                        + "\t--includeminor\n\t\tInclude minor edits.\n"
                        + "\t--minsize size\n\t\tOnly includes edits that add more than size bytes.\n\t\t"
                            + "Default: 150\n"
                        + "\t--start date\n\t\tInclude edits made after this date (ISO format)\n"
                        + "\t--end date\n\t\tInclude edits made before this date (ISO format)\n");
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
                case "--includeminor":
                    nominor = false;
                    break;
                case "--minsize":
                    minsize = Integer.parseInt(args[++i]);
                    break;
                case "--start":
                    start = OffsetDateTime.parse(args[++i]);
                    break;
                case "--end":
                    end = OffsetDateTime.parse(args[++i]);
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
                if (homewiki.namespace(temp) == Wiki.USER_NAMESPACE)
                    users.add(homewiki.removeNamespace(temp));
        }
        else // file IO
        {
            Path path = null;
            if (infile == null)
            {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Select user list");
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                    path = fc.getSelectedFile().toPath();
                else
                {
                    System.out.println("Error: No input file selected.");
                    System.exit(0);
                }
            }
            else
                path = Paths.get(infile);
            List<String> templist = Files.readAllLines(path, Charset.forName("UTF-8"));
            for (String line : templist)
            {
                if (homewiki.namespace(line) == Wiki.USER_NAMESPACE)
                {
                    // line = line.replace("[*#]\\s?\\[\\[:?", "");
                    // line = line.replace("\\]\\]", "");
                    users.add(homewiki.removeNamespace(line));
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
        
        int[] ns = userspace ? (new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE }) : (new int[] { Wiki.MAIN_NAMESPACE });
        
        ContributionSurveyor surveyor = new ContributionSurveyor(homewiki);
        surveyor.setMinimumSizeDiff(minsize);
        surveyor.setEarliestDateTime(start);
        surveyor.setLatestDateTime(end);
        surveyor.setIgnoringMinorEdits(nominor);
        try (FileWriter outwriter = new FileWriter(out))
        {
            outwriter.write(surveyor.massContributionSurvey(users.toArray(new String[users.size()]), images, ns));
        }
    }
    
    /**
     *  Constructs a new ContributionSurveyor instance.
     *  @param homewiki the wiki that has users we want to survey
     *  @since 0.03
     */
    public ContributionSurveyor(Wiki homewiki)
    {
        this.wiki = homewiki;
    }
    
    /**
     *  Returns the home wiki of users that can be surveyed by this instance.
     *  @return (see above)
     *  @since 0.03
     */
    public Wiki getWiki()
    {
        return wiki;
    }
    
    /**
     *  Sets whether surveys ignore minor edits. Default = true.
     *  @param ignoreminor (see above)
     *  @see #isIgnoringMinorEdits() 
     *  @since 0.04
     */
    public void setIgnoringMinorEdits(boolean ignoreminor)
    {
        nominor = ignoreminor;
    }
    
    /**
     *  Gets whether surveys ignore minor edits. Default = true.
     *  @return (see above)
     *  @see #setIgnoringMinorEdits(boolean) 
     *  @since 0.04
     */
    public boolean isIgnoringMinorEdits()
    {
        return nominor;
    }
    
    /**
     *  Sets the date/time at which surveys start; no edits will be returned
     *  before then. Defaults to {@code null}, i.e. no lower bound.
     *  @param earliest the desired start date/time
     *  @see #getEarliestDateTime() 
     *  @since 0.04
     */
    public void setEarliestDateTime(OffsetDateTime earliest)
    {
        earliestdate = earliest;
    }
    
    /**
     *  Gets the date/time at which surveys start; no edits will be returned 
     *  before then.
     *  @return (see above)
     *  @see #setEarliestDateTime(java.time.OffsetDateTime)  
     *  @since 0.04
     */
    public OffsetDateTime getEarliestDateTime()
    {
        return earliestdate;
    }
    
    /**
     *  Sets the date/time at which surveys finish; no edits will be returned
     *  after then. Defaults to {@code null}, i.e. when the survey is performed
     *  @param latest the desired end date/time
     *  @see #getLatestDateTime()
     *  @since 0.04
     */
    public void setLatestDateTime(OffsetDateTime latest)
    {
        latestdate = latest;
    }
    
    /**
     *  Gets the date at which surveys finish. 
     *  @return (see above)
     *  @see #setLatestDateTime(java.time.OffsetDateTime)  
     *  @since 0.04
     */
    public OffsetDateTime getLatestDateTime()
    {
        return latestdate;
    }
    
    /**
     *  Sets the minimum change size (in bytes added) to include in surveys.  
     *  Default is 150, set to {@code Integer.MIN_VALUE} to disable.
     *  @param sizediff the minimum change size, in bytes added
     *  @see #getMinimumSizeDiff() 
     *  @since 0.04
     */
    public void setMinimumSizeDiff(int sizediff)
    {
        minsizediff = sizediff;
    }
    
    /**
     *  Gets the minimum change size to include in surveys. 
     *  @return the minimum change size, in bytes
     *  @see #setMinimumSizeDiff(int) 
     *  @since 0.04
     */
    public int getMinimumSizeDiff()
    {
        return minsizediff;
    }
    
    /**
     *  Conducts a survey of edits by the given users. The output is in the form
     *  username &#8594; page &#8594; edits, where usernames are in the same
     *  order as <var>users</var>, edits are sorted to place the largest addition
     *  first and pages are sorted by their largest addition. <var>ns</var> 
     *  containing {@link org.wikipedia.Wiki#FILE_NAMESPACE Wiki.FILE_NAMESPACE} 
     *  looks at additions of text to image description pages.
     * 
     *  @param users the list of users to survey
     *  @param ns the namespaces to survey (not specified = all namespaces,)
     *  @return the survey in the form username &#8594; page &#8594; edits
     *  @throws IOException if a network error occurs
     *  @since 0.04
     */
    public Map<String, Map<String, List<Wiki.Revision>>> contributionSurvey(String[] users, int... ns) throws IOException
    {
        Map<String, Boolean> options = new HashMap<>();
        if (nominor)
            options.put("minor", Boolean.FALSE);
        List<Wiki.Revision>[] edits = wiki.contribs(users, "", earliestdate, latestdate, options, ns);
        Map<String, Map<String, List<Wiki.Revision>>> ret = new LinkedHashMap<>();
        Comparator<Wiki.Revision> diffsorter = (rev1, rev2) -> rev2.getSizeDiff() - rev1.getSizeDiff();
        for (int i = 0; i < users.length; i++)
        {
            Map<String, List<Wiki.Revision>> results = edits[i].stream()
                .filter(rev -> rev.getSizeDiff() >= minsizediff)
                .sorted(diffsorter)
                .collect(Collectors.groupingBy(Wiki.Revision::getPage, LinkedHashMap::new, Collectors.toList()));
            ret.put(users[i], results);
        }
        return ret;
    }
    
    /**
     *  Performs a survey of a user's deleted contributions. Requires 
     *  administrator access to the relevant wiki. (Note: due to MediaWiki
     *  limitations, it is not possible to filter by bytes added or whether an
     *  edit created a new page.)
     * 
     *  @param username the user to survey
     *  @param ns the namespaces to survey (not specified = all namespaces)
     *  @throws IOException if a network error occurs
     *  @throws CredentialNotFoundException if one cannot view deleted pages
     *  @since 0.03
     */
    public LinkedHashMap<String, ArrayList<Wiki.Revision>> deletedContributionSurvey(String username, 
        int... ns) throws IOException, CredentialNotFoundException
    {
        // this looks a lot like ArticleEditorIntersector.intersectEditors()...
        Wiki.Revision[] delcontribs = wiki.deletedContribs(username, latestdate, 
            earliestdate, false, ns);
        LinkedHashMap<String, ArrayList<Wiki.Revision>> ret = new LinkedHashMap<>();
        
        // group contributions by page
        for (Wiki.Revision rev : delcontribs)
        {
            if (nominor && rev.isMinor())
                continue;
            String page = rev.getPage();
            if (!ret.containsKey(page))
                ret.put(page, new ArrayList<Wiki.Revision>());
            ret.get(page).add(rev);
        }
        return ret;
    }
    
    /**
     *  Performs an image contribution survey on a user. (Date/time limits do
     *  not apply for transferred images.)
     *  @param user a user on the wiki
     *  @return first element = local uploads, second element = uploads on Wikimedia
     *  Commons by the user, third element = images transferred to Commons (may
     *  be inaccurate depending on username).
     *  @throws IOException if a network error occurs
     */
    public String[][] imageContributionSurvey(Wiki.User user) throws IOException
    {
        // fetch local uploads
        HashSet<String> localuploads = new HashSet<>(10000);
        for (Wiki.LogEntry upload : wiki.getUploads(user, earliestdate, latestdate))
            localuploads.add(upload.getTarget());
        
        // fetch commons uploads
        Wiki commons = Wiki.createInstance("commons.wikimedia.org");
        Wiki.User comuser = commons.getUser(user.getUsername());
        HashSet<String> comuploads = new HashSet<>(10000);
        if (comuser != null)
            for (Wiki.LogEntry upload : commons.getUploads(user, earliestdate, latestdate))
                comuploads.add(upload.getTarget());
        
        // fetch transferred commons uploads
        HashSet<String> commonsTransfer = new HashSet<>(10000);
        String[][] temp = commons.search("\"" + user.getUsername() + "\"", Wiki.FILE_NAMESPACE);
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
     *  Formats a contribution survey for a single user as wikitext.
     *  @param username the relevant username (use null to omit)
     *  @param survey the survey, in form of page &#8594; edits
     *  @return the formatted survey in wikitext
     *  @see #contributionSurvey(java.lang.String[], int...) 
     *  @since 0.04
     */
    public String formatTextSurveyAsWikitext(String username, Map<String, List<Wiki.Revision>> survey)
    {
        StringBuilder out = new StringBuilder();
        Iterator<Map.Entry<String, List<Wiki.Revision>>> iter = survey.entrySet().iterator();
        int numarticles = 0;
        int totalarticles = survey.size();
        
        while (iter.hasNext())
        {
            // add convenience breaks
            numarticles++;
            if (numarticles % 20 == 1)
            {
                if (username == null)
                    out.append(String.format("\n=== Pages %d through %d ===\n", numarticles, Math.min(numarticles + 19, totalarticles)));
                else
                    out.append(String.format("\n=== %s: Pages %d through %d ===\n", username, numarticles, Math.min(numarticles + 19, totalarticles)));
            }
            
            Map.Entry<String, List<Wiki.Revision>> entry = iter.next();
            List<Wiki.Revision> edits = entry.getValue();
            out.append("*");
            
            StringBuilder temp = new StringBuilder();
            boolean newpage = false;
            for (Wiki.Revision edit : edits)
            {
                // is this a new page?
                if (edit.isNew() && !newpage)
                {
                    out.append("'''N''' ");
                    newpage = true;
                }
                // generate the diff strings now to avoid a second iteration
                temp.append(String.format("[[Special:Diff/%d|(%+d)]]", edit.getRevid(), edit.getSizeDiff()));
            }
            int numedits = edits.size();
            if (numedits == 1)
                out.append(String.format("[[:%s]] (1 edit): ", entry.getKey()));
            else
                out.append(String.format("[[:%s]] (%d edits): ", entry.getKey(), numedits));
            out.append(temp);
            out.append("\n");
        }
        return out.toString();
    }
    
    /**
     *  Formats an image contribution survey for a single user as wikitext.
     *  @param username the relevant username (use null to omit)
     *  @param survey the survey
     *  @return the formatted survey in wikitext
     *  @see #imageContributionSurvey(org.wikipedia.Wiki.User) 
     *  @since 0.04
     */
    public String formatImageSurveyAsWikitext(String username, String[][] survey)
    {
        StringBuilder out = new StringBuilder();
        int numfiles = 0;
        int totalfiles = survey[0].length;
        for (String entry : survey[0])
        {
            numfiles++;
            if (numfiles % 20 == 1)
            {
                if (username == null)
                    out.append(String.format("\n=== Local files %d through %d ===\n", numfiles, Math.min(numfiles + 19, totalfiles)));
                else
                    out.append(String.format("\n=== %s: Local files %d through %d ===\n", username, numfiles, Math.min(numfiles + 19, totalfiles)));
            }
            out.append(String.format("*[[:%s]]\n", entry));
        }

        numfiles = 0;
        totalfiles = survey[1].length;
        for (String entry : survey[1])
        {
            numfiles++;
            if (numfiles % 20 == 1)
            {
                if (username == null)
                    out.append(String.format("\n=== Commons files %d through %d ===\n", numfiles, Math.min(numfiles + 19, totalfiles)));
                else
                    out.append(String.format("\n=== %s: Commons files %d through %d ===\n", username, numfiles, Math.min(numfiles + 19, totalfiles)));
            }
            out.append(String.format("*[[:%s]]\n", entry));
        }

        numfiles = 0;
        totalfiles = survey[2].length;
        for (String entry : survey[2])
        {
            numfiles++;
            if (numfiles % 20 == 1)
            {
                if (username == null)
                    out.append(String.format("\n=== Transferred files %d through %d ===\n", numfiles, Math.min(numfiles + 19, totalfiles)));
                else
                    out.append(String.format("\n=== %s: Transferred files %d through %d ===\n", username, numfiles, Math.min(numfiles + 19, totalfiles)));
            }
            out.append(String.format("*[[:%s]]\n", entry));
        }
        return out.toString();
    }
    
    /**
     *  Performs a mass contribution survey and returns wikitext output.
     *  @param users the users to survey
     *  @param ns the namespaces to survey
     *  @param images whether to survey images (searches Commons as well)
     *  @return the survey results as wikitext
     *  @throws IOException if a network error occurs
     *  @since 0.02
     */
    public String massContributionSurvey(String[] users, boolean images, int... ns) throws IOException
    {
        StringBuilder out = new StringBuilder();
        Map<String, Map<String, List<Wiki.Revision>>> results = contributionSurvey(users, ns);
        Map<String, Object>[] userinfo = wiki.getUserInfo(users);
        
        Iterator<Map.Entry<String, Map<String, List<Wiki.Revision>>>> iter = results.entrySet().iterator();
        int userindex = 0;
        
        while (iter.hasNext())
        {
            Map.Entry<String, Map<String, List<Wiki.Revision>>> entry = iter.next();
            String username = entry.getKey();
            Map<String, List<Wiki.Revision>> survey = entry.getValue();
            
            out.append(String.format("== %s ==\n", username));
            out.append(ParserUtils.generateUserLinksAsWikitext(username));
            out.append(formatTextSurveyAsWikitext(username, survey));
            
            // survey images
            if (images && userinfo[userindex] != null)
            {
                String[][] imagesurvey = imageContributionSurvey((Wiki.User)userinfo[userindex].get("user"));
                out.append(formatImageSurveyAsWikitext(username, imagesurvey));
            }
            userindex++;
        }
        out.append(generateWikitextFooter());
        return out.toString();
    }
    
    /**
     *  Generates a wikitext footer for contribution surveys.
     *  @return (see above)
     *  @since 0.04
     */
    public String generateWikitextFooter()
    {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        return "\nThis report generated by [https://github.com/MER-C/wiki-java ContributionSurveyor.java] at "
            + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + ".";
    }
}
