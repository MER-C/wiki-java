/**
 *  @(#)ContributionSurveyor.java 0.09 10/02/2024
 *  Copyright (C) 2011-2024 MER-C
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
import java.util.zip.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import javax.security.auth.login.*;

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
 *  @version 0.09
 */
public class ContributionSurveyor
{
    private final Wiki wiki;
    private OffsetDateTime earliestdate, latestdate;
    private String footer;
    private boolean nominor = true, noreverts = true, newonly = false;
    private boolean comingle;
    private boolean transferredfiles;
    private int minsizediff = 150;
    private final int articlesperpage = 1000;
    private final int articlespersection = 20;

    /**
     *  Runs this program.
     *  @param args command line arguments (see code for documentation)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        // parse arguments
        CommandLineParser clp = new CommandLineParser()
            .synopsis("org.wikipedia.tools.ContributionSurveyor", "[options]")
            .description("Survey the contributions of a large number of wiki editors.")
            .addVersion("ContributionSurveyor v0.08\n" + CommandLineParser.GPL_VERSION_STRING)
            .addSingleArgumentFlag("--infile", "file", "Use file as the list of users. "
                + "Shows a filechooser if not specified.")
            .addSingleArgumentFlag("--outfile", "file", "Save results to file(s). "
                + "Shows a filechooser if not specified. If multiple files output, use this as a prefix.")
            .addBooleanFlag("--zip", "Write a zip file instead of individual file(s).")
            .addSection("Users to scan:")
            .addSingleArgumentFlag("--wiki", "example.org", "Use example.org as the home wiki (default: en.wikipedia.org).")
            .addBooleanFlag("--login", "Shows a CLI login prompt (use for high limits).")
            .addSingleArgumentFlag("--sourcewiki", "example.com", "Use a different wiki than --wiki as a source of users.")
            .addSingleArgumentFlag("--wikipage", "'Main Page'", "Fetch a list of users from the source wiki page [[Main Page]].")
            .addSingleArgumentFlag("--blockedafter", "date", "Only survey unblocked users or those blocked on the target wiki after a certain date.");
        clp = addSharedOptions(clp);
        Map<String, String> parsedargs = clp
            .addBooleanFlag("--images", "Survey images both on the home wiki and Commons.")
            .addBooleanFlag("--notransfer", "Do not include transferred files to Commons.")
            .addBooleanFlag("--deleted", "Survey deleted edits (requires admin privileges)")
            .addBooleanFlag("--skiplive", "Don't survey live edits (for image/deleted only surveys)")
            .parse(args);

        Wiki homewiki = Wiki.newSession(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        Wiki sourcewiki = homewiki;
        if (parsedargs.containsKey("--sourcewiki"))
            sourcewiki = Wiki.newSession(parsedargs.get("--sourcewiki"));
        if (parsedargs.containsKey("--login"))
            Users.of(homewiki).cliLogin();
        String blockedafterstring = parsedargs.get("--blockedafter");
        OffsetDateTime blockedafter = (blockedafterstring == null) ? null : OffsetDateTime.parse(blockedafterstring);

        // fetch user list
        List<String> users = CommandLineParser.parseUserOptions(parsedargs, sourcewiki);
        if (users.isEmpty()) // file IO
        {
            Path path = CommandLineParser.parseFileOption(parsedargs, "--infile", "Select user list", 
                "Error: No input file selected.", false);
            List<String> templist = Files.readAllLines(path);
            for (String line : templist)
                if (sourcewiki.namespace(line) == Wiki.USER_NAMESPACE)
                    users.add(sourcewiki.removeNamespace(line));
        }
        
        // filter for users blocked after __ (for persistent sockfarms)
        if (blockedafter != null)
        {
            List<Wiki.User> userobjs = homewiki.getUsers(users);
            users.clear();
            for (Wiki.User user : userobjs)
            {
                Wiki.LogEntry block = user.getBlockDetails();
                if (block == null || block.getTimestamp().isAfter(blockedafter))
                    users.add(user.getUsername());
            }
        }

        int[] ns;
        if (parsedargs.containsKey("--userspace"))
            ns = new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE };
        else
            ns = new int[] { Wiki.MAIN_NAMESPACE };
        
        ContributionSurveyor surveyor = makeContributionSurveyor(homewiki, parsedargs);
        StringBuilder temp = new StringBuilder("Command line: <kbd>java org.wikipedia.tools.ContributionSurveyor");
        for (String arg : args)
        {
            temp.append(" ");
            temp.append(arg);
        }
        temp.append("</kbd>");
        surveyor.setFooter(temp.toString());
       
        List<String> output = surveyor.outputContributionSurvey(users, !parsedargs.containsKey("--skiplive"),
            parsedargs.containsKey("--deleted"), parsedargs.containsKey("--images"), ns);

        String outfile = parsedargs.get("--outfile");
        Path path = CommandLineParser.parseFileOption(parsedargs, "--outfile", "Select output file", 
            "Error: No output file selected.", true);
        if (parsedargs.containsKey("--zip"))
        {
            String fname = outfile.replace(".zip", ".txt");
            Map<String, byte[]> zip = new LinkedHashMap<>();
            for (int i = 0; i < output.size(); i++)
                zip.put(fname + (i == 0 ? "" : ".%03d".formatted(i)), output.get(i).getBytes());
            try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(path.toFile())))
            {
                ContributionSurveyor.outputZipFile(zout, zip);
            }
        }
        else
        {
            try (BufferedWriter outwriter = Files.newBufferedWriter(path))
            {
                outwriter.write(output.get(0));
            }
            for (int i = 1; i < output.size(); i++)
            {
                path = path.resolveSibling("%s.%03d".formatted(outfile, i));
                try (BufferedWriter outwriter = Files.newBufferedWriter(path))
                {
                    outwriter.write(output.get(i));
                }
            }
        }
    }
    
    /**
     *  Returns command line arguments that can be shared with other tools.
     *  @param parser a CommandLineParser
     *  @return the CommandLineParser with options added
     *  @since 0.08
     *  @see org.wikipedia.tools.CommandLineParser
     *  @see #makeContributionSurveyor(org.wikipedia.Wiki, java.util.Map)
     */
    static CommandLineParser addSharedOptions(CommandLineParser parser)
    {
        return parser.addHelp()
            .addSingleArgumentFlag("--user", "user", "Survey the given user.")
            .addSingleArgumentFlag("--category", "category", "Fetch a list of users from the given category (recursive).")
            .addBooleanFlag("--comingle", "If there are multiple users, combine their edits into the one survey (edits/uploads only).")
            .addSection("Survey options:")
            .addBooleanFlag("--includeminor", "Include minor edits.")
            .addBooleanFlag("--includereverts", "Include rollbacks.")
            .addBooleanFlag("--newonly", "Survey only page creations.")
            .addSingleArgumentFlag("--minsize", "size", "Only includes edits that add more than size bytes (default: 150).")
            .addSingleArgumentFlag("--editsafter", "date", "Include edits made after this date (ISO format).")
            .addSingleArgumentFlag("--editsbefore", "date", "Include edits made before this date (ISO format).")
            .addBooleanFlag("--userspace", "Survey userspace as well.");
    }
    
    /**
     *  Makes a new ContributionSurveyor for the given wiki based on parsed 
     *  command line arguments. Currently supports date, minsize, includeminor, 
     *  includereverts, newonly, and notransfer.
     *  @param wiki a wiki
     *  @param parsedargs parsed command line arguments
     *  @return a ContributionSurveyor object
     *  @see org.wikipedia.tools.CommandLineParser
     *  @see #addSharedOptions(org.wikipedia.tools.CommandLineParser) 
     *  @since 0.08
     */
    static ContributionSurveyor makeContributionSurveyor(Wiki wiki, Map<String, String> parsedargs)
    {
        List<OffsetDateTime> daterange = CommandLineParser.parseDateRange(parsedargs, "--editsafter", "--editsbefore");
        ContributionSurveyor cs = new ContributionSurveyor(wiki);
        cs.setNewOnly(parsedargs.containsKey("--newonly"));
        cs.setDateRange(daterange.get(0), daterange.get(1));
        cs.setMinimumSizeDiff(Integer.parseInt(parsedargs.getOrDefault("--minsize", "150")));
        cs.setIgnoringMinorEdits(!parsedargs.containsKey("--includeminor"));
        cs.setIgnoringReverts(!parsedargs.containsKey("--includereverts"));
        cs.setComingled(parsedargs.containsKey("--comingle"));
        cs.setSurveyingTransferredFiles(!parsedargs.containsKey("--notransfer"));
        return cs;
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
     *  If there are multiple users in a particular survey, treat them as one
     *  user as part of the output.
     *  @param comingle whether to combine listings for users into one
     *  @see #isComingled
     *  @since 0.07
     */
    public void setComingled(boolean comingle)
    {
        this.comingle = comingle;
    }
    
    /**
     *  If there are multiple users in a particular survey, fetches whether they 
     *  treated as one user as part of the output.
     *  @return (see above)
     *  @see #setComingled(boolean)
     *  @since 0.07
     */
    public boolean isComingled()
    {
        return comingle;
    }
    
    /**
     *  Survey page creations only.
     *  @param newonly only output new pages
     *  @see #newOnly
     *  @since 0.07
     */
    public void setNewOnly(boolean newonly)
    {
        this.newonly = newonly;
    }
    
    /**
     *  Returns whether this surveyor surveys page creations only.
     *  @return (see above)
     *  @see #setNewOnly(boolean)
     *  @since 0.07
     */
    public boolean newOnly()
    {
        return newonly;
    }
    
    /**
     *  Include (or not) files uploaded by the user on a local wiki but later 
     *  transferred to in image contribution surveys. This can be prone to
     *  inaccuracies because it performs a search of file namespace for the 
     *  username of the surveyed user.
     *  @param transferredfiles (see above)
     *  @since 0.09
     */
    public void setSurveyingTransferredFiles(boolean transferredfiles)
    {
        this.transferredfiles = transferredfiles;
    }
    
    /**
     *  Returns whether this surveyor includes files uploaded by the user on a 
     *  local wiki but later transferred to in image contribution surveys. This 
     *  can be prone to inaccuracies because it performs a search of file 
     *  namespace for the  username of the surveyed user.
     *  @return (see above)
     *  @since 0.09
     */
    public boolean isSurveyingTransferredFiles()
    {
        return transferredfiles;
    }
    
    /**
     *  Sets a custom footer to appear after the time the survey was generated.
     *  @param footer a footer
     *  @since 0.08
     *  @see #generateWikitextFooter()
     *  @see #generateHTMLFooter()
     */
    public void setFooter(String footer)
    {
        this.footer = footer;
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
        if (newonly)
            options.put("new", Boolean.TRUE);
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .inNamespaces(ns)
            .withinDateRange(earliestdate, latestdate)
            .filterBy(options);
        List<List<Wiki.Revision>> edits = wiki.contribs(users, null, rh);
        List<Wiki.Revision> comingled = new ArrayList<>();
        // filter
        for (int i = 0; i < users.size(); i++)
        {
            List<Wiki.Revision> useredits = edits.get(i);
            // RevisionDelete... should check for content AND no access, but with no SHA-1 that is impossible
            useredits.removeIf(rev -> rev.isContentDeleted() || rev.getSizeDiff() < minsizediff);
            if (noreverts)
            {
                useredits.removeIf(edit -> 
                {
                    List<String> tags = edit.getTags();
                    return tags.contains("mw-rollback") || tags.contains("mw-manual-revert");
                });
                // useredits = Revisions.removeReverts(useredits);
            }
            if (comingle)
                comingled.addAll(useredits);
        }
        
        if (comingle)
        {
            edits = List.of(comingled);
            users = List.of("");
        }
        Map<String, Map<String, List<Wiki.Revision>>> ret = new LinkedHashMap<>();
        for (int i = 0; i < users.size(); i++)
        {
            Map<String, List<Wiki.Revision>> results = edits.get(i).stream()
                .sorted(Comparator.comparingInt(Wiki.Revision::getSizeDiff).reversed())
                .collect(Collectors.groupingBy(Wiki.Revision::getTitle, LinkedHashMap::new, Collectors.toList()));
            ret.put(users.get(i), results);
        }
        return ret;
    }

    /**
     *  Performs a survey of some users' deleted contributions. Requires
     *  administrator access to the relevant wiki. (Note: due to MediaWiki
     *  limitations, it is not possible to filter by bytes added or whether an
     *  edit created a new page.)
     *
     *  @param users the users to survey
     *  @param ns the namespaces to survey (not specified = all namespaces)
     *  @return the survey, in the form username &#8594; deleted page &#8594; edits
     *  @throws IOException if a network error occurs
     *  @throws CredentialNotFoundException if one cannot view deleted pages
     *  @since 0.03
     */
    public Map<String, Map<String, List<Wiki.Revision>>> deletedContributionSurvey(Iterable<String> users,
        int... ns) throws IOException, CredentialNotFoundException
    {
        // this looks a lot like ArticleEditorIntersector.intersectEditors()...
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .withinDateRange(earliestdate, latestdate)
            .inNamespaces(ns);
        
        Map<String, Map<String, List<Wiki.Revision>>> ret = new LinkedHashMap<>();
        for (String username : users)
        {
            List<Wiki.Revision> delcontribs = wiki.deletedContribs(username, rh);
            if (noreverts)
                delcontribs.removeIf(edit -> edit.getTags().contains("mw-rollback"));
                // delcontribs = Revisions.removeReverts(delcontribs);
            LinkedHashMap<String, List<Wiki.Revision>> imap = new LinkedHashMap<>();

            // group contributions by page
            for (Wiki.Revision rev : delcontribs)
            {
                if (nominor && rev.isMinor())
                    continue;
                String page = rev.getTitle();
                if (!imap.containsKey(page))
                    imap.put(page, new ArrayList<>());
                imap.get(page).add(rev);
            }
            ret.put(username, imap);
        }
        return ret;
    }

    /**
     *  Performs an image contribution survey on a list of users. (Date/time 
     *  limits do not apply to, nor do they make sense for transferred images.)
     *  @param users a list of users on the wiki
     *  @return for each user: first element = local uploads, second element = 
     *  uploads on Wikimedia Commons by the user, third element = images 
     *  transferred to Commons (may be inaccurate depending on username or empty
     *  if disabled).
     *  @throws IOException if a network error occurs
     */
    public Map<String, Map<String, List<String>>> imageContributionSurvey(Iterable<String> users) throws IOException
    {
        // TODO: expose common repository somewhere
        Wiki commons = Wiki.newSession("commons.wikimedia.org");
        Wiki.RequestHelper rh = wiki.new RequestHelper().withinDateRange(earliestdate, latestdate);
        Map<String, Map<String, List<String>>> ret = new HashMap<>();
        
        for (String user : users)
        {
            // fetch local uploads
            HashSet<String> localuploads = new HashSet<>(10000);
            for (Wiki.LogEntry upload : wiki.getUploads(user, rh))
                localuploads.add(upload.getTitle());

            // fetch commons uploads
            HashSet<String> comuploads = new HashSet<>(10000);
            for (Wiki.LogEntry upload : commons.getUploads(user, rh))
                comuploads.add(upload.getTitle());

            // fetch transferred commons uploads
            HashSet<String> commonsTransfer = new HashSet<>(10000);
            if (transferredfiles)
            {
                List<Map<String, Object>> temp = commons.search("\"" + user + "\"", Wiki.FILE_NAMESPACE);
                for (Map<String, Object> x : temp)
                    commonsTransfer.add((String)x.get("title"));
            }

            // remove all files that have been reuploaded to Commons
            localuploads.removeAll(comuploads);
            localuploads.removeAll(commonsTransfer);
            commonsTransfer.removeAll(comuploads);
            
            if (comingle)
            {
                if (ret.isEmpty())
                    ret.put("", Map.of(
                        "local", new ArrayList<>(localuploads), 
                        "commons", new ArrayList<>(comuploads),
                        "transferred", new ArrayList<>(commonsTransfer)));
                else
                {
                    var comingled = ret.get("");
                    comingled.get("local").addAll(localuploads);
                    comingled.get("commons").addAll(comuploads);
                    comingled.get("transferred").addAll(commonsTransfer);
                }
            }
            else
                ret.put(user, Map.of(
                    "local", new ArrayList<>(localuploads), 
                    "commons", new ArrayList<>(comuploads),
                    "transferred", new ArrayList<>(commonsTransfer)));
        }
        return ret;
    }

    /**
     *  Generates a survey listing for a given article in a CCI.
     *  @param user_survey a particular user's text contribution survey
     *  @param article the article to render
     *  @return the survey listing for that article in wikitext
     *  @see #contributionSurvey(List, int...)
     *  @since 0.04
     */
    public String outputNextPage(Map<String, List<Wiki.Revision>> user_survey, String article)
    {
        StringBuilder out = new StringBuilder(10000);
        List<Wiki.Revision> edits = user_survey.get(article);

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
            temp.append("[[Special:Diff/%d|(%+d)]]".formatted(edit.getID(), edit.getSizeDiff()));
        }
        int numedits = edits.size();
        out.append("[[:");
        out.append(article);
        out.append("]] (");
        if (numedits == 1)
            out.append("1 edit): ");
        else
        {
            out.append(numedits);
            out.append(" edits): ");
        }
        out.append(temp);
        return out.toString();
    }

    /**
     *  Performs a mass contribution survey and returns wikitext output.
     *  @param usernames the users to survey
     *  @param ns the namespaces to survey
     *  @param contribs include live edits
     *  @param deleted include deleted edits (requires admin login)
     *  @param images whether to survey images (searches Commons as well)
     *  @return the survey results as wikitext
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if fetching deleted edits without admin
     *  privileges 
     *  @since 0.02
     */
    public List<String> outputContributionSurvey(List<String> usernames, boolean contribs, 
        boolean deleted, boolean images, int... ns) throws IOException, SecurityException
    {
        List<String> sections = new ArrayList<>();
        int sectionsperpage = articlesperpage / articlespersection;  
        Map<String, Map<String, List<Wiki.Revision>>> results = null, delresults = null;
        Map<String, Map<String, List<String>>> imagesurvey = null;
        
        if (contribs)
            results = contributionSurvey(usernames, ns);
        if (deleted)
        {
            try
            {
                delresults = deletedContributionSurvey(usernames, ns);
            }
            catch (CredentialNotFoundException ex)
            {
                throw new SecurityException(ex);
            }
        }
        if (images)
            imagesurvey = imageContributionSurvey(usernames);
        
        if (comingle)
            usernames = List.of("");
        int count = usernames.size();
        
        for (String username : usernames)
        {
            // output text results
            int sizebefore = sections.size();
            String username_hdr = count == 1 ? "" : (username + ":");

            if (results != null)
            {
                Map<String, List<Wiki.Revision>> user_survey = results.get(username);
                sections.addAll(Pages.toWikitextPaginatedList(user_survey.keySet(), page -> outputNextPage(user_survey, page), 
                    (start, end) -> "===" + username_hdr + " Pages " + start + " to " + end + "===", 
                    articlespersection, false));
            }
            
            // output deleted results
            if (delresults != null)
            {
                Map<String, List<Wiki.Revision>> user_survey = delresults.get(username);
                sections.addAll(Pages.toWikitextPaginatedList(user_survey.keySet(), page -> outputNextPage(user_survey, page), 
                    (start, end) -> "===" + username_hdr + " Deleted pages " + start + " to " + end + "===", 
                    articlespersection, false));
            }
            
            // output image contribution survey for this user
            if (imagesurvey != null && imagesurvey.containsKey(username))
            {
                Map<String, List<String>> imagesurvey2 = imagesurvey.get(username);
                sections.addAll(Pages.toWikitextPaginatedList(imagesurvey2.get("local"), Pages.LIST_OF_LINKS, 
                    (start, end) -> "===" + username_hdr + " Local files " + start + " to " + end + "===", 
                    articlespersection, false));
                sections.addAll(Pages.toWikitextPaginatedList(imagesurvey2.get("commons"), Pages.LIST_OF_LINKS, 
                    (start, end) -> "===" + username_hdr + " Commons files " + start + " to " + end + "===", 
                    articlespersection, false));
                sections.addAll(Pages.toWikitextPaginatedList(imagesurvey2.get("transferred"), Pages.LIST_OF_LINKS, 
                    (start, end) -> "===" + username_hdr + " Transferred files " + start + " to " + end + "===", 
                    articlespersection, false));
            }
            
            // insert header if there were results for this user and at the 
            // start of every new page
            String header = "";
            if (!comingle)
                header = "== " + username + " ==\n" + 
                    Users.generateWikitextSummaryLinks(username) + "\n";
            for (int i = sizebefore; i < sections.size(); i++)
            {
                if (i == sizebefore || i % sectionsperpage == 0)
                {
                    String toreplace = sections.get(i);
                    sections.set(i, header + toreplace);
                }
            }
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
     *  Writes a set of text files to a zip archive.
     *  @param zipper a zip output stream
     *  @param output a map, filename to contents
     *  @throws IOException if a I/O error occurs
     *  @since 0.08
     */
    public static void outputZipFile(ZipOutputStream zipper, Map<String, byte[]> output) throws IOException
    {
        for (Map.Entry<String, byte[]> e : output.entrySet())
        {
            ZipEntry ze = new ZipEntry(e.getKey());
            zipper.putNextEntry(ze);
            byte[] b = e.getValue();
            zipper.write(b, 0, b.length);
            zipper.closeEntry();
        }
    }

    /**
     *  Generates a wikitext footer for contribution surveys.
     *  @return (see above)
     *  @since 0.04
     *  @see #setFooter(java.lang.String)
     *  @see #generateHTMLFooter()
     */
    public String generateWikitextFooter()
    {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return "This report generated by [https://github.com/MER-C/wiki-java ContributionSurveyor.java] at "
            + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + ". " + footer;
    }
    
    /**
     *  Generates a HTML footer for contribution surveys.
     *  @return (see above)
     *  @since 0.08
     *  @see #setFooter(java.lang.String) 
     *  @see #generateWikitextFooter() 
     */
    public String generateHTMLFooter()
    {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return "This report generated by <a href=\"https://github.com/MER-C/wiki-java\">ContributionSurveyor.java</a> at "
            + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + ". " + footer;
    }
}
