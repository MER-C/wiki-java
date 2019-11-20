/**
 *  @(#)ContributionSurveyor.java 0.06 20/11/2019
 *  Copyright (C) 2011-2019 MER-C
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
import javax.security.auth.login.*;
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
 *  @version 0.06
 */
public class ContributionSurveyor
{
    private final Wiki wiki;
    private OffsetDateTime earliestdate, latestdate;
    private boolean nominor = true, noreverts = true;
    private int minsizediff = 150;
    private int articlesperpage = 1000;
    private int articlespersection = 20;

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
            .addSingleArgumentFlag("--infile", "file", "Use file as the list of users. "
                + "Shows a filechooser if not specified.")
            .addSingleArgumentFlag("--outfile", "file", "Save results to file(s). "
                + "Shows a filechooser if not specified. If multiple files output, use this as a prefix.")
            .addSection("Users to scan:")
            .addSingleArgumentFlag("--wiki", "example.org", "Use example.org as the home wiki (default: en.wikipedia.org).")
            .addBooleanFlag("--login", "Shows a CLI login prompt (use for high limits)s.")
            .addSingleArgumentFlag("--wikipage", "'Main Page'", "Fetch a list of users from the wiki page [[Main Page]].")
            .addSingleArgumentFlag("--category", "category", "Fetch a list of users from the given category (recursive).")
            .addSingleArgumentFlag("--user", "user", "Survey the given user.")
            .addSection("Survey options:")
            .addBooleanFlag("--images", "Survey images both on the home wiki and Commons.")
            .addBooleanFlag("--userspace", "Survey userspace as well.")
            .addBooleanFlag("--includeminor", "Include minor edits.")
            .addBooleanFlag("--includereverts", "Include rollbacks.")
            .addSingleArgumentFlag("--minsize", "size", "Only includes edits that add more than size bytes (default: 150).")
            .addSingleArgumentFlag("--editsafter", "date", "Include edits made after this date (ISO format).")
            .addSingleArgumentFlag("--editsbefore", "date", "Include edits made before this date (ISO format).")
            .parse(args);

        Wiki homewiki = Wiki.newSession(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        if (parsedargs.containsKey("--login"))
        {
            // CLI login
            try
            {
                Console console = System.console();
                homewiki.login(console.readLine("Username: "), console.readPassword("Password: "));
            }
            catch (FailedLoginException ex)
            {
                System.err.println("Invalid username or password.");
                System.exit(1);
            }
        }
        String infile = parsedargs.get("--infile");
        String outfile = parsedargs.get("--outfile");
        String wikipage = parsedargs.get("--wikipage");
        String category = parsedargs.get("--category");
        String user = parsedargs.get("--user");
        boolean images = parsedargs.containsKey("--images");
        boolean userspace = parsedargs.containsKey("--userspace");
        boolean nominor = !parsedargs.containsKey("--includeminor");
        boolean noreverts = !parsedargs.containsKey("--includereverts");
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
            String text = homewiki.getPageText(List.of(wikipage)).get(0);
            List<String> list = Pages.parseWikitextList(text);
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
        if (outfile == null)
        {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select output file");
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
                outfile = fc.getSelectedFile().getPath();
        }
        if (outfile == null)
        {
            System.out.println("Error: No output file selected.");
            System.exit(0);
        }

        int[] ns = userspace ? (new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE }) : (new int[] { Wiki.MAIN_NAMESPACE });

        ContributionSurveyor surveyor = new ContributionSurveyor(homewiki);
        surveyor.setMinimumSizeDiff(minsize);
        surveyor.setDateRange(editsafter, editsbefore);
        surveyor.setIgnoringMinorEdits(nominor);
        surveyor.setIgnoringReverts(noreverts);
        List<String> output = surveyor.outputContributionSurvey(users, images, ns);
        
        Path path = Paths.get(outfile);
        try (BufferedWriter outwriter = Files.newBufferedWriter(path))
        {
            outwriter.write(output.get(0));
        }
        for (int i = 1; i < output.size(); i++)
        {
            path = Paths.get(outfile + String.format(".%03d", i));
            try (BufferedWriter outwriter = Files.newBufferedWriter(path))
            {
                outwriter.write(output.get(i));
            }
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

    // Partially effective because of https://phabricator.wikimedia.org/T185809 
    
    /**
     *  Sets whether surveys ignore reverts. Currently ignores rollbacks only. 
     *  Default = true.
     *  @param ignorereverts (see above)
     *  @see #isIgnoringReverts()
     *  @see Revisions#removeReverts
     *  @since 0.05
     */
    public void setIgnoringReverts(boolean ignorereverts)
    {
        noreverts = ignorereverts;
    }
    
    /**
     *  Gets whether surveys ignore reverts. Currently ignores rollbacks only. 
     *  Default = true.
     *  @return (see above)
     *  @see #setIgnoringReverts(boolean)
     *  @see Revisions#removeReverts
     *  @since 0.05
     */
    public boolean isIgnoringReverts()
    {
        return noreverts;
    }
    
    /**
     *  Sets the dates/times at which surveys start and finish; no edits will be 
     *  returned outside this range. The default, {@code null}, indicates no
     *  bound.
     *  @param earliest the desired start date/time
     *  @param latest the desired end date/time
     *  @throws IllegalArgumentException if <var>earliest</var> is after
     *  <var>latest</var>
     *  @see #getEarliestDateTime() 
     *  @see #getLatestDateTime() 
     *  @since 0.04
     */
    public void setDateRange(OffsetDateTime earliest, OffsetDateTime latest)
    {
        if (earliest != null && latest != null && earliest.isAfter(latest))
            throw new IllegalArgumentException("Date range is reversed.");
        earliestdate = earliest;
        latestdate = latest;
    }

    /**
     *  Gets the date/time at which surveys start; no edits will be returned
     *  before then.
     *  @return (see above)
     *  @see #setDateRange(OffsetDateTime, OffsetDateTime)
     *  @since 0.04
     */
    public OffsetDateTime getEarliestDateTime()
    {
        return earliestdate;
    }

    /**
     *  Gets the date at which surveys finish.
     *  @return (see above)
     *  @see #setDateRange(OffsetDateTime, OffsetDateTime)
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
            .withinDateRange(earliestdate, latestdate)
            .filterBy(options);
        List<List<Wiki.Revision>> edits = wiki.contribs(users, null, rh);
        Map<String, Map<String, List<Wiki.Revision>>> ret = new LinkedHashMap<>();
        for (int i = 0; i < users.size(); i++)
        {
            List<Wiki.Revision> useredits = edits.get(i);
            if (noreverts)
                useredits.removeIf(edit -> edit.getTags().contains("mw-rollback"));
                // useredits = Revisions.removeReverts(useredits);
            Map<String, List<Wiki.Revision>> results = useredits.stream()
            // RevisionDelete... should check for content AND no access, but with no SHA-1 that is impossible
                .filter(rev -> !rev.isContentDeleted()) 
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
        if (noreverts)
            delcontribs.removeIf(edit -> edit.getTags().contains("mw-rollback"));
            // delcontribs = Revisions.removeReverts(delcontribs);
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
        Wiki commons = Wiki.newSession("commons.wikimedia.org");
        Wiki.User comuser = commons.getUsers(List.of(user.getUsername())).get(0);
        HashSet<String> comuploads = new HashSet<>(10000);
        if (comuser != null)
            for (Wiki.LogEntry upload : commons.getUploads(user, rh))
                comuploads.add(upload.getTitle());

        // fetch transferred commons uploads
        HashSet<String> commonsTransfer = new HashSet<>(10000);
        List<Map<String, Object>> temp = commons.search("\"" + user.getUsername() + "\"", Wiki.FILE_NAMESPACE);
        for (Map<String, Object> x : temp)
            commonsTransfer.add((String)x.get("title"));

        // remove all files that have been reuploaded to Commons
        localuploads.removeAll(comuploads);
        localuploads.removeAll(commonsTransfer);
        commonsTransfer.removeAll(comuploads);

        return new String[][] {
            localuploads.toArray(String[]::new),
            comuploads.toArray(String[]::new),
            commonsTransfer.toArray(String[]::new)
        };
    }

    /**
     *  Formats a section in a text contribution survey as wikitext.
     *  @param username the relevant username (use {@code null} to omit)
     *  @param survey the survey, in form of page &#8594; edits
     *  @param offset how many articles have been output via this method for this 
     *  user so far
     *  @return the formatted section of the survey in wikitext
     *  @see #contributionSurvey(List, int...)
     *  @since 0.04
     */
    public String outputNextSection(String username, List<Map.Entry<String, List<Wiki.Revision>>> survey, int offset)
    {
        // output header
        int maxarticles = Math.min(offset + articlespersection, survey.size());
        StringBuilder out = new StringBuilder("=== ");
        if (username != null)
        {
            out.append(username);
            out.append(": ");
        }
        out.append("Pages ");
        out.append(offset + 1);
        out.append(" through ");
        out.append(maxarticles);
        out.append(" ===\n");
        
        // output results
        for (int i = offset; i < maxarticles; i++)
        {
            var entry = survey.get(i);
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
        }
        out.append("\n");
        return out.toString();
    }
    
    /**
     *  Formats a section in an image contribution survey as wikitext.
     *  @param username the relevant username (use null to omit)
     *  @param source the source of the images, typically one of "Local files", 
     *  "Commons files", or "Transferred files"
     *  @param survey the survey corresponding to the source of the image
     *  @param offset how many images have been output via this method for this 
     *  user and source of images so far
     *  @return the formatted survey in wikitext
     *  @see #imageContributionSurvey(Wiki.User)
     *  @since 0.04
     */
    public String outputNextSection(String username, String source, String[] survey, int offset)
    {
        // output header
        int maxfiles = Math.min(offset + articlespersection, survey.length);
        StringBuilder out = new StringBuilder("=== ");
        if (username != null)
        {
            out.append(username);
            out.append(": ");
        }
        out.append(source);
        out.append(" ");
        out.append(offset + 1);
        out.append(" through ");
        out.append(maxfiles);
        out.append(" ===\n");
        
        // output results
        for (int i = offset; i < maxfiles; i++)
        {            
            out.append("*[[:");
            out.append(survey[i]);
            out.append("]]\n");
        }
        out.append("\n");
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
    public List<String> outputContributionSurvey(List<String> usernames, boolean images, int... ns) throws IOException
    {
        // TODO: output image only CCIs via this method
        List<String> sections = new ArrayList<>();
        List<Wiki.User> userinfo = wiki.getUsers(usernames);
        int sectionsperpage = articlesperpage / articlespersection;  
        var results = contributionSurvey(usernames, ns);
        var iter = results.entrySet().iterator();
        int userindex = 0;

        while (iter.hasNext())
        {
            // fetch text contribution survey for this user
            // note List so we don't have to iterate over it many times (slow)
            var entry = iter.next();
            var tempmap = entry.getValue();
            var survey = new ArrayList<>(tempmap.entrySet());
            String username = entry.getKey();
            // null suppresses output of usernames in headers
            String username_out = results.size() == 1 ? null : username;
            int sizebefore = sections.size();

            // populate image contribution survey for this user
            // userinfo required because there may be IP addresses
            String[][] imagesurvey = new String[3][0];
            if (images && userinfo.get(userindex) != null)
                imagesurvey = imageContributionSurvey(userinfo.get(userindex));

            // output text results
            for (int i = 0; i < survey.size(); i += 20)
                sections.add(outputNextSection(username_out, survey, i));
            
            // output image results
            if (images && userinfo.get(userindex) != null)
            {
                for (int i = 0; i < imagesurvey[0].length; i += 20)
                    sections.add(outputNextSection(username_out, "Local files", imagesurvey[0], i));
                for (int i = 0; i < imagesurvey[1].length; i += 20)
                    sections.add(outputNextSection(username_out, "Commons files", imagesurvey[1], i));
                for (int i = 0; i < imagesurvey[2].length; i += 20)
                    sections.add(outputNextSection(username_out, "Transferred files", imagesurvey[2], i));
            }
            
            // insert header if there were results for this user and at the 
            // start of every new page
            String header = "== " + username + " ==\n" + 
                Users.generateWikitextSummaryLinks(username) + "\n";
            for (int i = sizebefore; i < sections.size(); i++)
            {
                if (i == sizebefore || i % sectionsperpage == 0)
                {
                    String toreplace = sections.get(i);
                    sections.set(i, header + toreplace);
                }
            }
            userindex++;
        }
        
        // segment sections into pages
        StringBuilder out = new StringBuilder();
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++)
        {
            out.append(sections.get(i));
            if (i == sections.size() - 1 || i % sectionsperpage == sectionsperpage - 1)
            {
                out.append(generateWikitextFooter());
                ret.add(out.toString());
                out.setLength(0);
            }
        }
        return ret;
    }

    /**
     *  Generates a wikitext footer for contribution surveys.
     *  @return (see above)
     *  @since 0.04
     */
    public String generateWikitextFooter()
    {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return "This report generated by [https://github.com/MER-C/wiki-java ContributionSurveyor.java] at "
            + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + ". ";
    }
}
