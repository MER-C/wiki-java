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
     *  @param args command line arguments (see code for documentation)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        // parse arguments
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.ContributionSurveyor", "[options]")
            .description("Survey the contributions of a large number of wiki editors.")
            .addHelp()
            .addVersion("ContributionSurveyor v0.04\n" + CommandLineParser.GPL_VERSION_STRING)
            .addSingleArgumentFlag("--infile", "file", "Use file as the list of users, shows a filechooser if not specified.")
            .addSingleArgumentFlag("--outfile", "file", "Save results to file, shows a filechooser if not specified.")
            .addSection("Users to scan:")
            .addSingleArgumentFlag("--wiki", "example.org", "Use example.org as the home wiki (default: en.wikipedia.org).")
            .addSingleArgumentFlag("--wikipage", "'Main Page'", "Fetch a list of users from the wiki page [[Main Page]].")
            .addSingleArgumentFlag("--category", "category", "Fetch a list of users from the given category (recursive).")
            .addSingleArgumentFlag("--user", "user", "Survey the given user.")
            .addSection("Survey options:")
            .addBooleanFlag("--images", "Survey images both on the home wiki and Commons.")
            .addBooleanFlag("--userspace", "Survey userspace as well.")
            .addBooleanFlag("--includeminor", "Include minor edits.")
            .addSingleArgumentFlag("--minsize", "size", "Only includes edits that add more than size bytes (default: 150).")
            .addSingleArgumentFlag("--editsafter", "date", "Include edits made after this date (ISO format).")
            .addSingleArgumentFlag("--editsbefore", "date", "Include edits made before this date (ISO format).")
            .parse(args);

        Wiki homewiki = Wiki.createInstance(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        String infile = parsedargs.get("--infile");
        String outfile = parsedargs.get("--outfile");
        String wikipage = parsedargs.get("--wikipage");
        String category = parsedargs.get("--category");
        String user = parsedargs.get("--user");
        boolean images = parsedargs.containsKey("--images");
        boolean userspace = parsedargs.containsKey("--userspace");
        boolean nominor = !parsedargs.containsKey("--includeminor");
        int minsize = Integer.parseInt(parsedargs.getOrDefault("--minsize", "150"));
        String earliestdatestring = parsedargs.get("--editsafter");
        String latestdatestring = parsedargs.get("--editsbefore");

        List<String> users = new ArrayList<>(1500);
        OffsetDateTime editsafter = (earliestdatestring == null) ? null : OffsetDateTime.parse(earliestdatestring);
        OffsetDateTime editsbefore = (latestdatestring == null) ? null : OffsetDateTime.parse(latestdatestring);

        // fetch user list
        if (user != null)
            users.add(user);
        else if (category != null)
        {
            for (String member : homewiki.getCategoryMembers(category, true, Wiki.USER_NAMESPACE))
                users.add(homewiki.removeNamespace(member));
        }
        else if (wikipage != null)
        {
            List<String> list = Pages.parseWikitextList(homewiki.getPageText(wikipage));
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
            }
            else
                path = Paths.get(infile);
            if (path == null)
            {
                System.out.println("Error: No input file selected.");
                System.exit(0);
            }
            List<String> templist = Files.readAllLines(path);
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
        Path out = null;
        if (outfile == null)
        {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select output file");
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
                out = fc.getSelectedFile().toPath();
        }
        else
            out = Paths.get(outfile);
        if (out == null)
        {
            System.out.println("Error: No output file selected.");
            System.exit(0);
        }

        int[] ns = userspace ? (new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE }) : (new int[] { Wiki.MAIN_NAMESPACE });

        ContributionSurveyor surveyor = new ContributionSurveyor(homewiki);
        surveyor.setMinimumSizeDiff(minsize);
        surveyor.setEarliestDateTime(editsafter);
        surveyor.setLatestDateTime(editsbefore);
        surveyor.setIgnoringMinorEdits(nominor);
        try (BufferedWriter outwriter = Files.newBufferedWriter(out))
        {
            outwriter.write(surveyor.massContributionSurvey(users, images, ns));
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
     *  @see #setEarliestDateTime(OffsetDateTime)
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
     *  @see #setLatestDateTime(OffsetDateTime)
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
     *  containing {@link Wiki#FILE_NAMESPACE} looks at additions of text to
     *  image description pages.
     *
     *  @param users the list of users to survey (without User: prefix)
     *  @param ns the namespaces to survey (not specified = all namespaces,)
     *  @return the survey in the form username &#8594; page &#8594; edits
     *  @throws IOException if a network error occurs
     *  @since 0.04
     */
    public Map<String, Map<String, List<Wiki.Revision>>> contributionSurvey(List<String> users, int... ns) throws IOException
    {
        Map<String, Boolean> options = new HashMap<>();
        if (nominor)
            options.put("minor", Boolean.FALSE);
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .inNamespaces(ns)
            .withinDateRange(earliestdate, latestdate);
        List<List<Wiki.Revision>> edits = wiki.contribs(users, null, rh, options);
        Map<String, Map<String, List<Wiki.Revision>>> ret = new LinkedHashMap<>();
        for (int i = 0; i < users.size(); i++)
        {
            Map<String, List<Wiki.Revision>> results = edits.get(i).stream()
                .filter(rev -> rev.getSizeDiff() >= minsizediff)
                .sorted(Comparator.comparingInt(Wiki.Revision::getSizeDiff).reversed())
                .collect(Collectors.groupingBy(Wiki.Revision::getTitle, LinkedHashMap::new, Collectors.toList()));
            ret.put(users.get(i), results);
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
     *  @return the survey, in the form deleted page &#8594; revisions
     *  @throws IOException if a network error occurs
     *  @throws CredentialNotFoundException if one cannot view deleted pages
     *  @since 0.03
     */
    public LinkedHashMap<String, List<Wiki.Revision>> deletedContributionSurvey(String username,
        int... ns) throws IOException, CredentialNotFoundException
    {
        // this looks a lot like ArticleEditorIntersector.intersectEditors()...
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .withinDateRange(earliestdate, latestdate)
            .inNamespaces(ns);
        List<Wiki.Revision> delcontribs = wiki.deletedContribs(username, rh);
        LinkedHashMap<String, List<Wiki.Revision>> ret = new LinkedHashMap<>();

        // group contributions by page
        for (Wiki.Revision rev : delcontribs)
        {
            if (nominor && rev.isMinor())
                continue;
            String page = rev.getTitle();
            if (!ret.containsKey(page))
                ret.put(page, new ArrayList<>());
            ret.get(page).add(rev);
        }
        return ret;
    }

    /**
     *  Performs an image contribution survey on a user. (Date/time limits do
     *  not apply to, nor do they make sense for transferred images.)
     *  @param user a user on the wiki
     *  @return first element = local uploads, second element = uploads on Wikimedia
     *  Commons by the user, third element = images transferred to Commons (may
     *  be inaccurate depending on username).
     *  @throws IOException if a network error occurs
     */
    public String[][] imageContributionSurvey(Wiki.User user) throws IOException
    {
        // fetch local uploads
        Wiki.RequestHelper rh = wiki.new RequestHelper().withinDateRange(earliestdate, latestdate);
        HashSet<String> localuploads = new HashSet<>(10000);
        for (Wiki.LogEntry upload : wiki.getUploads(user, rh))
            localuploads.add(upload.getTitle());

        // fetch commons uploads
        Wiki commons = Wiki.createInstance("commons.wikimedia.org");
        Wiki.User comuser = commons.getUser(user.getUsername());
        HashSet<String> comuploads = new HashSet<>(10000);
        if (comuser != null)
            for (Wiki.LogEntry upload : commons.getUploads(user, rh))
                comuploads.add(upload.getTitle());

        // fetch transferred commons uploads
        HashSet<String> commonsTransfer = new HashSet<>(10000);
        Map<String, Object>[] temp = commons.search("\"" + user.getUsername() + "\"", Wiki.FILE_NAMESPACE);
        for (Map<String, Object> x : temp)
            commonsTransfer.add((String)x.get("title"));

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
     *  @param username the relevant username (use {@code null} to omit)
     *  @param survey the survey, in form of page &#8594; edits
     *  @return the formatted survey in wikitext
     *  @see #contributionSurvey(String[], int...)
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
            // add convenience breaks for long surveys
            numarticles++;
            if (numarticles % 20 == 1 && totalarticles > 20)
            {
                out.append("=== ");
                if (username != null)
                {
                    out.append(username);
                    out.append(": ");
                }
                out.append("Pages ");
                out.append(numarticles);
                out.append(" through ");
                out.append(Math.min(numarticles + 19, totalarticles));
                out.append(" ===\n");
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
                temp.append(String.format("[[Special:Diff/%d|(%+d)]]", edit.getID(), edit.getSizeDiff()));
            }
            int numedits = edits.size();
            out.append("[[:");
            out.append(entry.getKey());
            out.append("]] (");
            if (numedits == 1)
                out.append("1 edit): ");
            else
            {
                out.append(numedits);
                out.append(" edits): ");
            }
            out.append(temp);
            out.append("\n");
            if (numarticles % 20 == 0)
                out.append("\n");
        }
        return out.toString();
    }

    /**
     *  Formats an image contribution survey for a single user as wikitext.
     *  @param username the relevant username (use null to omit)
     *  @param survey the survey
     *  @return the formatted survey in wikitext
     *  @see #imageContributionSurvey(Wiki.User)
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
                out.append("\n=== ");
                if (username != null)
                {
                    out.append(username);
                    out.append(": ");
                }
                out.append("Local files ");
                out.append(numfiles);
                out.append(" through ");
                out.append(Math.min(numfiles + 19, totalfiles));
                out.append(" ===\n");
            }
            out.append("*[[:");
            out.append(entry);
            out.append("]]\n");
        }

        numfiles = 0;
        totalfiles = survey[1].length;
        for (String entry : survey[1])
        {
            numfiles++;
            if (numfiles % 20 == 1)
            {
                out.append("\n=== ");
                if (username != null)
                {
                    out.append(username);
                    out.append(": ");
                }
                out.append("Commons files ");
                out.append(numfiles);
                out.append(" through ");
                out.append(Math.min(numfiles + 19, totalfiles));
                out.append(" ===\n");
            }
            out.append("*[[:");
            out.append(entry);
            out.append("]]\n");
        }

        numfiles = 0;
        totalfiles = survey[2].length;
        for (String entry : survey[2])
        {
            numfiles++;
            if (numfiles % 20 == 1)
            {
                out.append("\n=== ");
                if (username != null)
                {
                    out.append(username);
                    out.append(": ");
                }
                out.append("Transferred files ");
                out.append(numfiles);
                out.append(" through ");
                out.append(Math.min(numfiles + 19, totalfiles));
                out.append(" ===\n");
            }
            out.append("*[[:");
            out.append(entry);
            out.append("]]\n");
        }
        return out.toString();
    }

    /**
     *  Performs a mass contribution survey and returns wikitext output.
     *  @param usernames the users to survey
     *  @param ns the namespaces to survey
     *  @param images whether to survey images (searches Commons as well)
     *  @return the survey results as wikitext
     *  @throws IOException if a network error occurs
     *  @since 0.02
     */
    public String massContributionSurvey(List<String> usernames, boolean images, int... ns) throws IOException
    {
        StringBuilder out = new StringBuilder();
        Map<String, Map<String, List<Wiki.Revision>>> results = contributionSurvey(usernames, ns);
        Wiki.User[] userinfo = wiki.getUsers(usernames.toArray(new String[0]));

        Iterator<Map.Entry<String, Map<String, List<Wiki.Revision>>>> iter = results.entrySet().iterator();
        int userindex = 0;

        while (iter.hasNext())
        {
            Map.Entry<String, Map<String, List<Wiki.Revision>>> entry = iter.next();
            String username = entry.getKey();
            Map<String, List<Wiki.Revision>> survey = entry.getValue();
            // skip no results users
            if (survey.isEmpty())
            {
                userindex++;
                continue;
            }

            out.append("== ");
            out.append(username);
            out.append(" ==\n");
            out.append(Users.generateWikitextSummaryLinks(username));
            out.append("\n");
            out.append(formatTextSurveyAsWikitext(username, survey));

            // survey images
            if (images && userinfo[userindex] != null)
            {
                String[][] imagesurvey = imageContributionSurvey(userinfo[userindex]);
                out.append(formatImageSurveyAsWikitext(username, imagesurvey));
            }
            out.append("\n");
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
