/**
 *  @(#)Wiki.java 0.36 08/02/2019
 *  Copyright (C) 2007 - 2019 MER-C and contributors
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

package org.wikipedia;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;
import java.util.zip.GZIPInputStream;

import javax.security.auth.login.*;

/**
 *  This is a somewhat sketchy bot framework for editing MediaWiki wikis.
 *  Requires JDK 11 or greater. Uses the <a
 *  href="https://mediawiki.org/wiki/API:Main_page">MediaWiki API</a> for most
 *  operations. It is recommended that the server runs the latest version
 *  of MediaWiki (1.31), otherwise some functions may not work. This framework
 *  requires no dependencies outside the core JDK and does not implement any
 *  functionality added by MediaWiki extensions.
 *  <p>
 *  Extended documentation is available
 *  <a href="https://github.com/MER-C/wiki-java/wiki/Extended-documentation">here</a>.
 *  All wikilinks are relative to the English Wikipedia and all timestamps are in
 *  your wiki's time zone.
 *  <p>
 *  Please file bug reports <a href="https://en.wikipedia.org/wiki/User_talk:MER-C">here</a>
 *  or at the <a href="https://github.com/MER-C/wiki-java/issues">Github issue
 *  tracker</a>.
 * 
 *  <h2>Configuration variables</h2>
 *  <p>
 *  Some configuration is available through <code>java.util.Properties</code>. 
 *  Set the system property <code>wiki-java.properties</code> to a file path
 *  where a configuration file is located. The available variables are:
 *  <ul>
 *  <li><b>maxretries</b>: (default 2) the number of attempts to retry a network 
 *      request before stopping
 *  <li><b>connecttimeout</b>: (default 30000) maximum allowed time for a HTTP(s)
 *      connection to be established in milliseconds
 *  <li><b>readtimeout</b>: (default 180000) maximum allowed time for the read 
 *      to take place in milliseconds (needs to be longer, some connections are 
 *      slow and the data volume is large!). 
 *  <li><b>loguploadsize</b>: (default 22, equivalent to 2^22 = 4 MB) controls 
 *      the log2(size) of each chunk in chunked uploads. Disable chunked uploads 
 *      by setting a large value here (50, equivalent to 2^50 = 1 PB will do).
 *      Stuff you actually upload must be no larger than 4 GB.
 * .</ul>
 *
 *  @author MER-C and contributors
 *  @version 0.36
 */
public class Wiki implements Comparable<Wiki>
{
    // NAMESPACES

    /**
     *  Denotes the namespace of images and media, such that there is no
     *  description page. Uses the "Media:" prefix.
     *  @see #FILE_NAMESPACE
     *  @since 0.03
     */
    public static final int MEDIA_NAMESPACE = -2;

    /**
     *  Denotes the namespace of pages with the "Special:" prefix. Note
     *  that many methods dealing with special pages may spew due to
     *  raw content not being available.
     *  @since 0.03
     */
    public static final int SPECIAL_NAMESPACE = -1;

    /**
     *  Denotes the main namespace, with no prefix.
     *  @since 0.03
     */
    public static final int MAIN_NAMESPACE = 0;

    /**
     *  Denotes the namespace for talk pages relating to the main
     *  namespace, denoted by the prefix "Talk:".
     *  @since 0.03
     */
    public static final int TALK_NAMESPACE = 1;

    /**
     *  Denotes the namespace for user pages, given the prefix "User:".
     *  @since 0.03
     */
    public static final int USER_NAMESPACE = 2;

    /**
     *  Denotes the namespace for user talk pages, given the prefix
     *  "User talk:".
     *  @since 0.03
     */
    public static final int USER_TALK_NAMESPACE = 3;

    /**
     *  Denotes the namespace for pages relating to the project,
     *  with prefix "Project:". It also goes by the name of whatever
     *  the project name was.
     *  @since 0.03
     */
    public static final int PROJECT_NAMESPACE = 4;

    /**
     *  Denotes the namespace for talk pages relating to project
     *  pages, with prefix "Project talk:". It also goes by the name
     *  of whatever the project name was, + "talk:".
     *  @since 0.03
     */
    public static final int PROJECT_TALK_NAMESPACE = 5;

    /**
     *  Denotes the namespace for file description pages. Has the prefix
     *  "File:". Do not create these directly, use upload() instead.
     *  @see #MEDIA_NAMESPACE
     *  @since 0.25
     */
    public static final int FILE_NAMESPACE = 6;

    /**
     *  Denotes talk pages for file description pages. Has the prefix
     *  "File talk:".
     *  @since 0.25
     */
    public static final int FILE_TALK_NAMESPACE = 7;

    /**
     *  Denotes the namespace for (wiki) system messages, given the prefix
     *  "MediaWiki:".
     *  @since 0.03
     */
    public static final int MEDIAWIKI_NAMESPACE = 8;

    /**
     *  Denotes the namespace for talk pages relating to system messages,
     *  given the prefix "MediaWiki talk:".
     *  @since 0.03
     */
    public static final int MEDIAWIKI_TALK_NAMESPACE = 9;

    /**
     *  Denotes the namespace for templates, given the prefix "Template:".
     *  @since 0.03
     */
    public static final int TEMPLATE_NAMESPACE = 10;

    /**
     *  Denotes the namespace for talk pages regarding templates, given
     *  the prefix "Template talk:".
     *  @since 0.03
     */
    public static final int TEMPLATE_TALK_NAMESPACE = 11;

    /**
     *  Denotes the namespace for help pages, given the prefix "Help:".
     *  @since 0.03
     */
    public static final int HELP_NAMESPACE = 12;

    /**
     *  Denotes the namespace for talk pages regarding help pages, given
     *  the prefix "Help talk:".
     *  @since 0.03
     */
    public static final int HELP_TALK_NAMESPACE = 13;

    /**
     *  Denotes the namespace for category description pages. Has the
     *  prefix "Category:".
     *  @since 0.03
     */
    public static final int CATEGORY_NAMESPACE = 14;

    /**
     *  Denotes the namespace for talk pages regarding categories. Has the
     *  prefix "Category talk:".
     *  @since 0.03
     */
    public static final int CATEGORY_TALK_NAMESPACE = 15;

    /**
     *  Denotes all namespaces.
     *  @since 0.03
     */
    public static final int ALL_NAMESPACES = 0x09f91102;

    // LOG TYPES

    /**
     *  Denotes all logs.
     *  @since 0.06
     */
    public static final String ALL_LOGS = "";

    /**
     *  Denotes the user creation log.
     *  @since 0.06
     */
    public static final String USER_CREATION_LOG = "newusers";

    /**
     *  Denotes the upload log.
     *  @since 0.06
     */
    public static final String UPLOAD_LOG = "upload";

    /**
     *  Denotes the deletion log.
     *  @since 0.06
     */
    public static final String DELETION_LOG = "delete";

    /**
     *  Denotes the move log.
     *  @since 0.06
     */
    public static final String MOVE_LOG = "move";

    /**
     *  Denotes the block log.
     *  @since 0.06
     */
    public static final String BLOCK_LOG = "block";

    /**
     *  Denotes the protection log.
     *  @since 0.06
     */
    public static final String PROTECTION_LOG = "protect";

    /**
     *  Denotes the user rights log.
     *  @since 0.06
     */
    public static final String USER_RIGHTS_LOG = "rights";

    /**
     *  Denotes the user renaming log.
     *  @since 0.06
     */
    public static final String USER_RENAME_LOG = "renameuser";

    /**
     *  Denotes the page importation log.
     *  @since 0.08
     */
    public static final String IMPORT_LOG = "import";

    /**
     *  Denotes the edit patrol log.
     *  @since 0.08
     */
    public static final String PATROL_LOG = "patrol";

    /**
     *  Denotes the page creation log.
     *  @since 0.36
     */
    public static final String PAGE_CREATION_LOG = "create";

    // PROTECTION LEVELS

    /**
     *  Denotes a non-protected page.
     *  @since 0.09
     */
    public static final String NO_PROTECTION = "all";

    /**
     *  Denotes semi-protection (only autoconfirmed users can perform a
     *  action).
     *  @since 0.09
     */
    public static final String SEMI_PROTECTION = "autoconfirmed";

    /**
     *  Denotes full protection (only admins can perfom a particular action).
     *  @since 0.09
     */
    public static final String FULL_PROTECTION = "sysop";

    // ASSERTION MODES

    /**
     *  Use no assertions.
     *  @see #setAssertionMode
     *  @since 0.11
     */
    public static final int ASSERT_NONE = 0;

    /**
     *  Assert that we are logged in. This is checked every action.
     *  @see #setAssertionMode
     *  @since 0.30
     */
    public static final int ASSERT_USER = 1;

    /**
     *  Assert that we have a bot flag. This is checked every action.
     *  @see #setAssertionMode
     *  @since 0.11
     */
    public static final int ASSERT_BOT = 2;

    /**
     *  Assert that we have no new messages. Not defined officially, but
     *  some bots have this. This is checked intermittently.
     *  @see #setAssertionMode
     *  @since 0.11
     */
    public static final int ASSERT_NO_MESSAGES = 4;

    /**
     *  Assert that we have a sysop flag. This is checked intermittently.
     *  @see #setAssertionMode
     *  @since 0.30
     */
    public static final int ASSERT_SYSOP = 8;

    // REVISION OPTIONS

    /**
     *  In {@link org.wikipedia.Wiki.Revision#diff(long) Revision.diff()},
     *  denotes the next revision.
     *  @see org.wikipedia.Wiki.Revision#diff(long)
     *  @since 0.21
     */
    public static final long NEXT_REVISION = -1L;

    /**
     *  In {@link org.wikipedia.Wiki.Revision#diff(long) Revision.diff()},
     *  denotes the current revision.
     *  @see org.wikipedia.Wiki.Revision#diff(long)
     *  @since 0.21
     */
    public static final long CURRENT_REVISION = -2L;

    /**
     *  In {@link org.wikipedia.Wiki.Revision#diff(long) Revision.diff()},
     *  denotes the previous revision.
     *  @see org.wikipedia.Wiki.Revision#diff(long)
     *  @since 0.21
     */
    public static final long PREVIOUS_REVISION = -3L;

    /**
     *  The list of options the user can specify for his/her gender.
     *  @see User#getGender()
     *  @since 0.24
     */
    public enum Gender
    {
        // These names come from the MW API so we can use valueOf() and
        // toString() without any fidgets whatsoever. Java naming conventions
        // aren't worth another 20 lines of code.

        /**
         *  The user self-identifies as a male.
         *  @since 0.24
         */
        male,

        /**
         *  The user self-identifies as a female.
         *  @since 0.24
         */
        female,

        /**
         *  The user has not specified a gender in preferences.
         *  @since 0.24
         */
        unknown;
    }

    private static final String version = "0.36";

    // fundamental URL strings
    private final String protocol, domain, scriptPath;
    private String base, articleUrl;

    /**
     *  Stores default HTTP parameters for API calls. Contains {@linkplain
     *  #setMaxLag(int) maxlag}, {@linkplain #setResolveRedirects(boolean) redirect
     *  resolution} and {@linkplain #setAssertionMode(int) user and bot assertions}
     *  when wanted by default. Add stuff to this map if you want to add parameters
     *  to every API call.
     *  @see #makeApiCall(Map, Map, String)
     */
    protected ConcurrentHashMap<String, String> defaultApiParams;

    /**
     *  URL entrypoint for the MediaWiki API. (Needs to be accessible to
     *  subclasses.)
     *  @see #initVars()
     *  @see #getApiUrl()
     *  @see <a href="https://mediawiki.org/wiki/Manual:Api.php">MediaWiki
     *  documentation</a>
     */
    protected String apiUrl;

    // wiki properties
    private boolean siteinfofetched = false;
    private boolean wgCapitalLinks = true;
    private String dbname;
    private String mwVersion;
    private ZoneId timezone = ZoneOffset.UTC;
    private Locale locale = Locale.ENGLISH;
    private List<String> extensions = Collections.emptyList();
    private LinkedHashMap<String, Integer> namespaces = null;
    private ArrayList<Integer> ns_subpages = null;

    // user management
    private HttpClient client;
    private final CookieManager cookies;
    private User user;
    private int statuscounter = 0;

    // watchlist cache
    private List<String> watchlist = null;

    // preferences
    private int max = 500;
    private int slowmax = 50;
    private int throttle = 10000;
    private int maxlag = 5;
    private int assertion = ASSERT_NONE; // assertion mode
    private int statusinterval = 100; // status check
    private int querylimit = Integer.MAX_VALUE;
    private String useragent = "Wiki.java/" + version + " (https://github.com/MER-C/wiki-java/)";
    private boolean zipped = true;
    private boolean markminor = false, markbot = false;
    private boolean resolveredirect = false;
    private Level loglevel = Level.ALL;
    private static final Logger logger = Logger.getLogger("wiki");

    // Store time when the last throttled action was executed
    private long lastThrottleActionTime = 0;

    // config via properties
    private final int maxtries;
    private final int read_timeout_msec;
    private final int log2_upload_size;

    // CONSTRUCTORS AND CONFIGURATION

    /**
     *  Creates a new MediaWiki API client for the given wiki with <a
     *  href="https://mediawiki.org/wiki/Manual:$wgScriptPath"><var>
     *  $wgScriptPath</var></a> set to <var>scriptPath</var> and via the
     *  specified protocol.
     *
     *  @param domain the wiki domain name
     *  @param scriptPath the script path
     *  @param protocol a protocol e.g. "http://", "https://" or "file:///"
     *  @since 0.31
     */
    protected Wiki(String domain, String scriptPath, String protocol)
    {
        this.domain = Objects.requireNonNull(domain);
        this.scriptPath = Objects.requireNonNull(scriptPath);
        this.protocol = Objects.requireNonNull(protocol);

        defaultApiParams = new ConcurrentHashMap<>();
        defaultApiParams.put("format", "xml");
        defaultApiParams.put("maxlag", String.valueOf(maxlag));

        logger.setLevel(loglevel);
        logger.log(Level.CONFIG, "[{0}] Using Wiki.java {1}", new Object[] { domain, version });
        
        // read in config
        Properties props = new Properties();
        String filename = System.getProperty("wiki-java.properties");
        if (filename != null)
        {
            try
            {
                InputStream in = new FileInputStream(new File(filename));
                props.load(in);
            }
            catch (IOException ex)
            {
                logger.log(Level.WARNING, "Unable to load properties file " + filename);
            }
        }
        maxtries = Integer.parseInt(props.getProperty("maxretries", "2"));
        log2_upload_size = Integer.parseInt(props.getProperty("loguploadsize", "22")); // 4 MB
        read_timeout_msec = Integer.parseInt(props.getProperty("readtimeout", "180000")); // 180 seconds
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .cookieHandler(cookies)
            .build();
    }

    /**
     *  Creates a new MediaWiki API client for the given wiki using HTTPS.  
     *  Depending on the settings of the wiki, you may need to call {@link 
     *  Wiki#getSiteInfo()} on the returned object after this in order for some 
     *  functionality to work correctly.
     *
     *  @param domain the wiki domain name e.g. en.wikipedia.org (defaults to
     *  en.wikipedia.org)
     *  @return the constructed API client object
     *  @since 0.34
     */
    public static Wiki newSession(String domain)
    {
        return newSession(domain, "/w", "https://");
    }

    /**
     *  Creates a new MediaWiki API client for the given wiki with <a
     *  href="https://mediawiki.org/wiki/Manual:$wgScriptPath"><var>
     *  $wgScriptPath</var></a> set to <var>scriptPath</var> and via the
     *  specified protocol. Depending on the settings of the wiki, you may need
     *  to call {@link Wiki#getSiteInfo()} on the returned object after this in
     *  order for some functionality to work correctly.
     *
     *  <p>All factory methods in subclasses must call {@link #initVars()}.
     *
     *  @param domain the wiki domain name
     *  @param scriptPath the script path
     *  @param protocol a protocol e.g. "http://", "https://" or "file:///"
     *  @return the constructed API client object
     *  @since 0.34
     */
    public static Wiki newSession(String domain, String scriptPath, String protocol)
    {
        // Don't put network requests here. Servlets cannot afford to make
        // unnecessary network requests in initialization.
        Wiki wiki = new Wiki(domain, scriptPath, protocol);
        wiki.initVars();
        return wiki;
    }

    /**
     *  Edit this if you need to change the API and human interface url
     *  configuration of the wiki. One example use is to change the port number.
     *
     *  <p>Contributed by Tedder
     *  @since 0.24
     */
    protected void initVars()
    {
        base = protocol + domain + scriptPath + "/index.php";
        apiUrl = protocol + domain + scriptPath + "/api.php";
        articleUrl = protocol + domain + "/wiki/";
    }

    /**
     *  Gets the domain of the wiki as supplied on construction.
     *  @return the domain of the wiki
     *  @since 0.06
     */
    public final String getDomain()
    {
        return domain;
    }

    /**
     *  Gets the <a href="https://mediawiki.org/wiki/Manual:$wgScriptPath"><var>
     *  $wgScriptPath</var></a> variable as supplied on construction.
     *  @return the script path of the wiki
     *  @since 0.14
     */
    public final String getScriptPath()
    {
        return scriptPath;
    }

    /**
     *  Gets the protocol used to access this MediaWiki instance, as supplied
     *  on construction.
     *  @return (see above)
     *  @since 0.35
     */
    public final String getProtocol()
    {
        return protocol;
    }

    /**
     *  Determines whether this wiki is equal to another object based on the
     *  protocol (not case sensitive), domain (not case sensitive) and
     *  scriptPath (case sensitive). A return value of {@code true} means two
     *  Wiki objects point to the same instance of MediaWiki.
     *  @param obj the object to compare
     *  @return whether the wikis point to the same instance of MediaWiki
     */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Wiki))
            return false;
        Wiki other = (Wiki)obj;
        return domain.equalsIgnoreCase(other.domain)
            && scriptPath.equals(other.scriptPath)
            && protocol.equalsIgnoreCase(other.protocol);
    }

    /**
     *  Returns a hash code of this object based on the protocol, domain and
     *  scriptpath.
     *  @return a hash code
     */
    @Override
    public int hashCode()
    {
        // English locale used here for reproducability and so network requests
        // are not required
        int hc = domain.toLowerCase(Locale.ENGLISH).hashCode();
        hc = 127 * hc + scriptPath.hashCode();
        hc = 127 * hc + protocol.toLowerCase(Locale.ENGLISH).hashCode();
        return hc;
    }

    /**
     *  Allows wikis to be sorted based on their domain (case insensitive), then
     *  their script path (case sensitive). If 0 is returned, it is reasonable
     *  both Wikis point to the same instance of MediaWiki.
     *  @param other the wiki to compare to
     *  @return -1 if this wiki is alphabetically before the other, 1 if after
     *  and 0 if they are likely to be the same instance of MediaWiki
     *  @since 0.35
     */
    @Override
    public int compareTo(Wiki other)
    {
        int result = domain.compareToIgnoreCase(other.domain);
        if (result == 0)
            result = scriptPath.compareTo(other.scriptPath);
        return result;
    }

    /**
     *  Gets the URL of index.php.
     *  @return (see above)
     *  @see <a href="https://mediawiki.org/wiki/Manual:Parameters_to_index.php">
     *  MediaWiki documentation</a>
     *  @since 0.35
     */
    public String getIndexPhpUrl()
    {
        return base;
    }

    /**
     *  Gets the URL of api.php.
     *  @return (see above)
     *  @see <a href="https://mediawiki.org/wiki/Manual:Api.php">MediaWiki
     *  documentation</a>
     *  @since 0.36
     */
    public String getApiUrl()
    {
        return apiUrl;
    }

    /**
     *  Gets the editing throttle.
     *  @return the throttle value in milliseconds
     *  @see #setThrottle
     *  @since 0.09
     */
    public int getThrottle()
    {
        return throttle;
    }

    /**
     *  Sets the throttle, which limits most write requests to no more than one
     *  per wiki instance in the given time across all threads. (As a
     *  consequence, all throttled methods are thread safe.) Read requests are
     *  not throttled or restricted in any way. Default is 10 seconds.
     *
     *  @param throttle the new throttle value in milliseconds
     *  @see #getThrottle
     *  @since 0.09
     */
    public void setThrottle(int throttle)
    {
        this.throttle = throttle;
        log(Level.CONFIG, "setThrottle", "Throttle set to " + throttle + " milliseconds");
    }

    /**
     *  Gets various properties of the wiki and sets the bot framework up to use
     *  them. The return value is cached. This method is thread safe. Returns:
     *  <ul>
     *  <li><b>usingcapitallinks</b>: (Boolean) whether a wiki forces upper case
     *    for the title. Example: en.wikipedia = true, en.wiktionary = false.
     *    Default = true. See <a href="https://mediawiki.org/wiki/Manual:$wgCapitalLinks">
     *    <var>$wgCapitalLinks</var></a>
     *  <li><b>scriptpath</b>: (String) the <a
     *    href="https://mediawiki.org/wiki/Manual:$wgScriptPath"><var>
     *    $wgScriptPath</var> wiki variable</a>. Default = {@code /w}.
     *  <li><b>version</b>: (String) the MediaWiki version used for this wiki
     *  <li><b>timezone</b>: (ZoneId) the timezone the wiki is in, default = UTC
     *  <li><b>locale</b>: (Locale) the locale of the wiki
     *  <li><b>dbname</b>: (String) the internal name of the database
     *  </ul>
     *
     *  @return (see above)
     *  @since 0.30
     *  @throws IOException if a network error occurs
     *  @deprecated This method is likely going to get renamed with the return
     *  type changed to void once I finish cleaning up the site info caching
     *  mechanism. Use the specialized methods instead.
     */
    @Deprecated
    public synchronized Map<String, Object> getSiteInfo() throws IOException
    {
        Map<String, Object> siteinfo = new HashMap<>();
        if (!siteinfofetched)
        {
            Map<String, String> getparams = new HashMap<>();
            getparams.put("action", "query");
            getparams.put("meta", "siteinfo");
            getparams.put("siprop", "namespaces|namespacealiases|general|extensions");
            String line = makeApiCall(getparams, null, "getSiteInfo");
            detectUncheckedErrors(line, null, null);

            // general site info
            String bits = line.substring(line.indexOf("<general "), line.indexOf("</general>"));
            wgCapitalLinks = parseAttribute(bits, "case", 0).equals("first-letter");
            timezone = ZoneId.of(parseAttribute(bits, "timezone", 0));
            mwVersion = parseAttribute(bits, "generator", 0);
            locale = new Locale(parseAttribute(bits, "lang", 0));
            dbname = parseAttribute(bits, "wikiid", 0);

            // parse extensions
            bits = line.substring(line.indexOf("<extensions>"), line.indexOf("</extensions>"));
            extensions = new ArrayList<>();
            String[] unparsed = bits.split("<ext ");
            for (int i = 1; i < unparsed.length; i++)
                extensions.add(parseAttribute(unparsed[i], "name", 0));

            // populate namespace cache
            namespaces = new LinkedHashMap<>(30);
            ns_subpages = new ArrayList<>(30);
            // xml form: <ns id="-2" canonical="Media" ... >Media</ns> or <ns id="0" ... />
            String[] items = line.split("<ns ");
            for (int i = 1; i < items.length; i++)
            {
                int ns = Integer.parseInt(parseAttribute(items[i], "id", 0));

                // parse localized namespace name
                // must be before parsing canonical namespace so that
                // namespaceIdentifier always returns the localized name
                int b = items[i].indexOf('>') + 1;
                int c = items[i].indexOf("</ns>");
                if (c < 0)
                    namespaces.put("", ns);
                else
                    namespaces.put(normalize(decode(items[i].substring(b, c))), ns);

                String canonicalnamespace = parseAttribute(items[i], "canonical", 0);
                if (canonicalnamespace != null)
                    namespaces.put(canonicalnamespace, ns);

                // does this namespace support subpages?
                if (items[i].contains("subpages=\"\""))
                    ns_subpages.add(ns);
            }
            siteinfofetched = true;
            log(Level.INFO, "getSiteInfo", "Successfully retrieved site info for " + getDomain());
        }
        siteinfo.put("usingcapitallinks", wgCapitalLinks);
        siteinfo.put("scriptpath", scriptPath);
        siteinfo.put("timezone", timezone);
        siteinfo.put("version", mwVersion);
        siteinfo.put("locale", locale);
        siteinfo.put("extensions", extensions);
        siteinfo.put("dbname", dbname);
        return siteinfo;
    }

    /**
     *  Gets the version of MediaWiki this wiki runs e.g. 1.20wmf5 (54b4fcb).
     *  See [[Special:Version]] on your wiki.
     *  @return (see above)
     *  @throws UncheckedIOException if the site info cache has not been
     *  populated and a network error occurred when populating it
     *  @since 0.14
     *  @see <a href="https://gerrit.wikimedia.org/">MediaWiki Git</a>
     */
    public String version()
    {
        ensureNamespaceCache();
        return mwVersion;
    }

    /**
     *  Detects whether a wiki forces upper case for the first character in a
     *  title. Example: en.wikipedia = true, en.wiktionary = false.
     *  @return (see above)
     *  @throws UncheckedIOException if the site info cache has not been
     *  populated and a network error occurred when populating it
     *  @see <a href="https://mediawiki.org/wiki/Manual:$wgCapitalLinks">MediaWiki
     *  documentation</a>
     *  @since 0.30
     */
    public boolean usesCapitalLinks()
    {
        ensureNamespaceCache();
        return wgCapitalLinks;
    }

    /**
     *  Returns the list of extensions installed on this wiki.
     *  @return (see above)
     *  @throws UncheckedIOException if the site info cache has not been
     *  populated and a network error occurred when populating it
     *  @see <a href="https://www.mediawiki.org/wiki/Manual:Extensions">MediaWiki
     *  documentation</a>
     *  @since 0.35
     */
    public List<String> installedExtensions()
    {
        ensureNamespaceCache();
        return new ArrayList<>(extensions);
    }

    /**
     *  Gets the timezone of this wiki
     *  @return (see above)
     *  @throws UncheckedIOException if the site info cache has not been
     *  populated and a network error occurred when populating it
     *  @since 0.35
     */
    public ZoneId timezone()
    {
        ensureNamespaceCache();
        return timezone;
    }

    /**
     *  Gets the locale of this wiki.
     *  @return (see above)
     *  @throws UncheckedIOException if the site info cache has not been
     *  populated and a network error occurred when populating it
     *  @since 0.35
     */
    public Locale locale()
    {
        ensureNamespaceCache();
        return locale;
    }

    /**
     *  Sets the user agent HTTP header to be used for requests. Default is
     *  <samp>"Wiki.java " + version</samp>.
     *  @param useragent the new user agent
     *  @since 0.22
     */
    public void setUserAgent(String useragent)
    {
        this.useragent = useragent;
    }

    /**
     *  Gets the user agent HTTP header to be used for requests. Default is
     *  <samp>"Wiki.java " + version</samp>.
     *  @return useragent the user agent
     *  @since 0.22
     */
    public String getUserAgent()
    {
        return useragent;
    }

    /**
     *  Enables/disables GZip compression for GET requests. Default: true.
     *  @param zipped whether we use GZip compression
     *  @since 0.23
     *  @deprecated this is now handled transparently; just delete calls to this method.
     */
    @Deprecated(forRemoval=true)
    public void setUsingCompressedRequests(boolean zipped)
    {
        this.zipped = zipped;
    }

    /**
     *  Checks whether we are using GZip compression for GET requests.
     *  Default: true.
     *  @return (see above)
     *  @since 0.23
     *  @deprecated this is now handled transparently; just delete calls to this method.
     */
    @Deprecated(forRemoval=true)
    public boolean isUsingCompressedRequests()
    {
        return zipped;
    }

    /**
     *  Checks whether API action=query dependencies automatically resolve
     *  redirects (default = false).
     *  @return (see above)
     *  @since 0.27
     */
    public boolean isResolvingRedirects()
    {
        return resolveredirect;
    }

    /**
     *  Sets whether API action=query dependencies automatically resolve
     *  redirects (default = false).
     *  @param b (see above)
     *  @since 0.27
     */
    public void setResolveRedirects(boolean b)
    {
        resolveredirect = b;
        if (b)
            defaultApiParams.put("redirects", "1");
        else
            defaultApiParams.remove("redirects");
    }

    /**
     *  Sets whether edits are marked as bot by default (may be overridden).
     *  Default = false. Works only if one has the required permissions.
     *  @param markbot (see above)
     *  @since 0.26
     */
    public void setMarkBot(boolean markbot)
    {
        this.markbot = markbot;
    }

    /**
     *  Are edits are marked as bot by default?
     *  @return whether edits are marked as bot by default
     *  @since 0.26
     */
    public boolean isMarkBot()
    {
        return markbot;
    }

    /**
     *  Sets whether edits are marked as minor by default (may be overridden).
     *  Default = false.
     *  @param minor (see above)
     *  @since 0.26
     */
    public void setMarkMinor(boolean minor)
    {
        this.markminor = minor;
    }

    /**
     *  Are edits are marked as minor by default?
     *  @return whether edits are marked as minor by default
     *  @since 0.26
     */
    public boolean isMarkMinor()
    {
        return markminor;
    }

    /**
     *  Returns the maximum number of results returned when querying the API.
     *  Default = Integer.MAX_VALUE
     *  @return see above
     *  @since 0.34
     */
    public int getQueryLimit()
    {
        return querylimit;
    }

    /**
     *  Sets the maximum number of results returned when querying the API.
     *  Useful for operating in constrained environments (e.g. web servers)
     *  or queries for which results are sorted by relevance (e.g. search).
     *
     *  @param limit the desired maximum number of results to retrieve
     *  @throws IllegalArgumentException if <var>limit</var> is not a positive
     *  integer
     *  @since 0.34
     */
    public void setQueryLimit(int limit)
    {
        if (limit < 1)
            throw new IllegalArgumentException("Query limit must be a positive integer.");
        querylimit = limit;
    }

    /**
     *   Returns a string representation of this Wiki.
     *   @return a string representation of this Wiki.
     *   @since 0.10
     */
    @Override
    public String toString()
    {
        // domain
        StringBuilder buffer = new StringBuilder("Wiki[url=");
        buffer.append(protocol);
        buffer.append(domain);
        buffer.append(scriptPath);

        // user
        buffer.append(",user=");
        buffer.append(Objects.toString(user));
        buffer.append(",");

        // throttle mechanisms
        buffer.append("throttle=");
        buffer.append(throttle);
        buffer.append(",maxlag=");
        buffer.append(maxlag);
        buffer.append(",assertionMode=");
        buffer.append(assertion);
        buffer.append(",statusCheckInterval=");
        buffer.append(statusinterval);
        buffer.append(",cookies=");
        buffer.append(cookies);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     *  Gets the maxlag parameter.
     *  @return the current maxlag, in seconds
     *  @see #setMaxLag
     *  @see #getCurrentDatabaseLag
     *  @see <a href="https://mediawiki.org/wiki/Manual:Maxlag_parameter">
     *  MediaWiki documentation</a>
     *  @since 0.11
     */
    public int getMaxLag()
    {
        return maxlag;
    }

    /**
     *  Sets the maxlag parameter. A value of less than 0s disables this
     *  mechanism. Default is 5s.
     *  @param lag the desired maxlag in seconds
     *  @see #getMaxLag
     *  @see #getCurrentDatabaseLag
     *  @see <a href="https://mediawiki.org/wiki/Manual:Maxlag_parameter">
     *  MediaWiki documentation</a>
     *  @since 0.11
     */
    public void setMaxLag(int lag)
    {
        maxlag = lag;
        log(Level.CONFIG, "setMaxLag", "Setting maximum allowable database lag to " + lag);
        if (maxlag >= 0)
            defaultApiParams.put("maxlag", String.valueOf(maxlag));
        else
            defaultApiParams.remove("maxlag");
    }

    /**
     *  Gets the assertion mode. Assertion modes are bitmasks.
     *  @return the current assertion mode
     *  @see #setAssertionMode
     *  @since 0.11
     */
    public int getAssertionMode()
    {
        return assertion;
    }

    /**
     *  Sets the assertion mode. Do this AFTER logging in, otherwise the login
     *  will fail. Assertion modes are bitmasks. Default is {@link #ASSERT_NONE}.
     *  @param mode an assertion mode
     *  @see #getAssertionMode
     *  @since 0.11
     */
    public void setAssertionMode(int mode)
    {
        assertion = mode;
        log(Level.CONFIG, "setAssertionMode", "Set assertion mode to " + mode);

        if ((assertion & ASSERT_BOT) == ASSERT_BOT)
            defaultApiParams.put("assert", "bot");
        else if ((assertion & ASSERT_USER) == ASSERT_USER)
            defaultApiParams.put("assert", "user");
        else
            defaultApiParams.remove("assert");
    }

    /**
     *  Gets the number of actions (edit, move, block, delete, etc) between
     *  status checks. A status check is where we update user rights, block
     *  status and check for new messages (if the appropriate assertion mode
     *  is set).
     *
     *  @return the number of edits between status checks
     *  @see #setStatusCheckInterval
     *  @since 0.18
     */
    public int getStatusCheckInterval()
    {
        return statusinterval;
    }

    /**
     *  Sets the number of actions (edit, move, block, delete, etc) between
     *  status checks. A status check is where we update user rights, block
     *  status and check for new messages (if the appropriate assertion mode
     *  is set). Default is 100.
     *
     *  @param interval the number of edits between status checks
     *  @see #getStatusCheckInterval
     *  @since 0.18
     */
    public void setStatusCheckInterval(int interval)
    {
        statusinterval = interval;
        log(Level.CONFIG, "setStatusCheckInterval", "Status check interval set to " + interval);
    }

    /**
     *  Set the logging level used by the internal logger.
     *  @param loglevel one of the levels specified in java.util.logging.LEVEL
     *  @since 0.31
     */
    public void setLogLevel(Level loglevel)
    {
        this.loglevel = loglevel;
        logger.setLevel(loglevel);
    }

    // META STUFF

    /**
     *  Logs in to the wiki. This method is thread-safe.
     *
     *  @param username a username
     *  @param password a password, as a {@code char[]} for security
     *  reasons. Overwritten once the password is used.
     *  @throws IOException if a network error occurs
     *  @throws FailedLoginException if the login failed due to an incorrect
     *  username or password, the requirement for an interactive login (not
     *  supported, use [[Special:BotPasswords]]) or some other reason
     *  @see #logout
     *  @see <a href="https://mediawiki.org/wiki/API:Login">MediaWiki
     *  documentation</a>
     */
    public synchronized void login(String username, char[] password) throws IOException, FailedLoginException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "login");
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("lgname", username);
        postparams.put("lgpassword", new String(password));
        postparams.put("lgtoken", getToken("login"));
        String line = makeApiCall(getparams, postparams, "login");
        detectUncheckedErrors(line, null, null);
        Arrays.fill(password, '0');

        // check for success
        if (line.contains("result=\"Success\""))
        {
            String returned_username = parseAttribute(line, "lgusername", 0);
            user = getUsers(List.of(returned_username)).get(0);
            boolean apihighlimit = user.isAllowedTo("apihighlimits");
            if (apihighlimit)
            {
                max = 5000;
                slowmax = 500;
            }
            log(Level.INFO, "login", "Successfully logged in as " + username + ", highLimit = " + apihighlimit);
        }
        else if (line.contains("result=\"Failed\""))
            throw new FailedLoginException("Login failed: " + parseAttribute(line, "reason", 0));
        // interactive login or bot password required
        else if (line.contains("result=\"Aborted\""))
            throw new FailedLoginException("Login failed: you need to use a bot password, see [[Special:Botpasswords]].");
        else
            throw new AssertionError("Unreachable!");
    }

    /**
     *  Logs in to the wiki. This method is thread-safe.
     *
     *  @param username a username
     *  @param password a string with the password
     *  @throws IOException if a network error occurs
     *  @throws FailedLoginException if the login failed due to an incorrect
     *  username or password, the requirement for an interactive login (not
     *  supported, use [[Special:Botpasswords]]) or some other reason
     *  @see #logout
     */
    public synchronized void login(String username, String password) throws IOException, FailedLoginException
    {
        login(username, password.toCharArray());
    }

    /**
     *  Logs out of the wiki. This method is thread safe (so that we don't log
     *  out during an edit). All operations are conducted offline, so you can
     *  serialize this Wiki first.
     *  @see #login
     *  @see #logoutServerSide
     */
    public synchronized void logout()
    {
        cookies.getCookieStore().removeAll();
        user = null;
        max = 500;
        slowmax = 50;
        log(Level.INFO, "logout", "Logged out");
    }

    /**
     *  Logs out of the wiki and destroys the session on the server. You will
     *  need to log in again instead of just reading in a serialized wiki.
     *  Equivalent to [[Special:Userlogout]]. This method is thread safe
     *  (so that we don't log out during an edit). WARNING: kills all
     *  concurrent sessions - if you are logged in with a browser this will log
     *  you out there as well.
     *
     *  @throws IOException if a network error occurs
     *  @since 0.14
     *  @see #login
     *  @see #logout
     *  @see <a href="https://mediawiki.org/wiki/API:Logout">MediaWiki
     *  documentation</a>
     */
    public synchronized void logoutServerSide() throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "logout");
        String response = makeApiCall(getparams, null, "logoutServerSide");
        detectUncheckedErrors(response, null, null);
        logout(); // destroy local cookies
    }

    /**
     *  Determines whether the current user has new messages. (A human would
     *  notice a yellow bar at the top of the page).
     *  @return whether the user has new messages
     *  @throws IOException if a network error occurs
     *  @since 0.11
     */
    public boolean hasNewMessages() throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("meta", "userinfo");
        getparams.put("uiprop", "hasmsg");
        String response = makeApiCall(getparams, null, "hasNewMessages");
        detectUncheckedErrors(response, null, null);
        return response.contains("messages=\"\"");
    }

    /**
     *  Determines the current database replication lag. This method does not
     *  wait if the maxlag setting is exceeded. This method is thread safe.
     *  @return the current database replication lag
     *  @throws IOException if a network error occurs
     *  @see #setMaxLag
     *  @see #getMaxLag
     *  @see <a href="https://mediawiki.org/wiki/Manual:Maxlag_parameter">
     *  MediaWiki documentation</a>
     *  @since 0.10
     */
    public double getCurrentDatabaseLag() throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("meta", "siteinfo");
        getparams.put("siprop", "dbrepllag");

        synchronized (this)
        {
            // bypass lag check for this request
            int temp = getMaxLag();
            setMaxLag(-1);
            String line = makeApiCall(getparams, null, "getCurrentDatabaseLag");
            detectUncheckedErrors(line, null, null);
            setMaxLag(temp);
    
            String lag = parseAttribute(line, "lag", 0);
            log(Level.INFO, "getCurrentDatabaseLag", "Current database replication lag is " + lag + " seconds");
            return Double.parseDouble(lag);
        }
    }

    /**
     *  Fetches some site statistics, namely the number of articles, pages,
     *  files, edits, users and admins. Equivalent to [[Special:Statistics]].
     *
     *  @return a map containing the stats. Use "articles", "pages", "files"
     *  "edits", "users", "activeusers", "admins" or "jobs" to retrieve the
     *  respective value
     *  @throws IOException if a network error occurs
     *  @since 0.14
     */
    public Map<String, Integer> getSiteStatistics() throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("meta", "siteinfo");
        getparams.put("siprop", "statistics");
        String text = makeApiCall(getparams, null, "getSiteStatistics");
        detectUncheckedErrors(text, null, null);
        Map<String, Integer> ret = new HashMap<>(20);
        ret.put("pages", Integer.parseInt(parseAttribute(text, "pages", 0)));
        ret.put("articles", Integer.parseInt(parseAttribute(text, "articles", 0)));
        ret.put("files", Integer.parseInt(parseAttribute(text, "images", 0)));
        ret.put("users", Integer.parseInt(parseAttribute(text, "users", 0)));
        ret.put("activeusers", Integer.parseInt(parseAttribute(text, "activeusers", 0)));
        ret.put("admins", Integer.parseInt(parseAttribute(text, "admins", 0)));
        ret.put("jobs", Integer.parseInt(parseAttribute(text, "jobs", 0))); // job queue length
        return ret;
    }
    
    /**
     *  Require the given extension be installed on this wiki, or throw an 
     *  UnsupportedOperationException if it isn't.
     *  @param extension the name of the extension to check
     *  @throws UnsupportedOperationException if that extension is not
     *  installed on this wiki
     *  @throws UncheckedIOException if the site info cache is not populated
     *  and a network error occurs when populating it
     *  @since 0.37
     */
    public void requiresExtension(String extension)
    {
        if (!installedExtensions().contains(extension))
            throw new UnsupportedOperationException("Extension \"" + extension
                + "\" is not installed on " + getDomain() + ". "
                + "Please check the extension name and [[Special:Version]].");
    }

    /**
     *  Renders the specified wiki markup as HTML by passing it to the MediaWiki
     *  parser through the API.
     *
     *  @param markup the markup to parse
     *  @return the parsed markup as HTML
     *  @throws IOException if a network error occurs
     *  @since 0.13
     */
    public String parse(String markup) throws IOException
    {
        Map<String, Object> content = new HashMap<>();
        content.put("text", markup);
        return parse(content, -1, false);
    }

    /**
     *  Parses wikitext, revisions or pages. Deleted pages and revisions to
     *  deleted pages are not allowed if you don't have the rights to view them.
     *
     *  <p>
     *  The returned HTML does not include "edit" links. Hyperlinks are
     *  rewritten from useless relative links to other wiki pages to full URLs.
     *  References to resources using protocol relative URLs are rewritten to
     *  use {@linkplain #getProtocol() this wiki's protocol}.
     *
     *  <p>
     *  <b>Warnings</b>:
     *  <ul>
     *  <li>The parameters to this method will be changed when the time comes
     *      for JDK11 refactoring to accept Map.Entry instead. I also haven't
     *      decided how many more boolean parameters to add, and what format
     *      they will take.
     *  </ul>
     *
     *  @param content a Map following the same scheme as specified by {@link 
     *  #diff(Map, Map)}
     *  @param section parse only this section (optional, use -1 to skip)
     *  @param nolimitreport do not include the HTML comment detailing limits
     *  @return the parsed wikitext
     *  @throws NoSuchElementException or IllegalArgumentException if no content
     *  was supplied for parsing
     *  @throws SecurityException if you pass a RevisionDeleted revision and
     *  lack the necessary privileges
     *  @throws IOException if a network error occurs
     *  @see #parse(String)
     *  @see #getRenderedText(String)
     *  @see Wiki.Revision#getRenderedText()
     *  @see <a href="https://mediawiki.org/wiki/API:Parsing_wikitext">MediaWiki
     *  documentation</a>
     *  @since 0.35
     */
    public String parse(Map<String, Object> content, int section, boolean nolimitreport) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "parse");
        getparams.put("prop", "text");
        if (nolimitreport)
            getparams.put("disablelimitreport", "1");
        getparams.put("disableeditsection", "1");
        Map<String, Object> postparams = new HashMap<>();

        Map.Entry<String, Object> entry = content.entrySet().iterator().next();
        Object value = entry.getValue();
        switch (entry.getKey())
        {
            case "title":
                getparams.put("page", normalize((String)value));
                break;
            case "revid":
                getparams.put("oldid", value.toString());
                break;
            case "revision":
                getparams.put("oldid", String.valueOf(((Revision)value).getID()));
                break;
            case "text":
                postparams.put("text", value);
                break;
            default:
                throw new IllegalArgumentException("No content was specified to parse!");
        }
        if (section >= 0)
            getparams.put("section", String.valueOf(section));

        String response = makeApiCall(getparams, postparams, "parse");
        Consumer<String> noop = desc -> {};
        Map<String, Consumer<String>> warnings = Map.of(
            "missingtitle", noop,
            "missingcontent", noop,
            "nosuchsection", noop,
            "nosuchrevid", noop);
        if (detectUncheckedErrors(response, null, warnings))
        {
            int y = response.indexOf('>', response.indexOf("<text")) + 1;
            int z = response.indexOf("</text>");

            // Rewrite URLs to replace useless relative links and make images work on
            // locally saved copies of wiki pages.
            String html = decode(response.substring(y, z));
            html = html.replace("href=\"/wiki", "href=\"" + protocol + domain + "/wiki");
            html = html.replace(" src=\"//", " src=\"" + protocol); // a little fragile for my liking, but will do
            return html;
        }
        return null;
    }

    /**
     *  Fetches a random page in the specified namespaces. Equivalent to
     *  [[Special:Random]].
     *
     *  @param ns the namespaces to fetch random pages from. If not present,
     *  fetch pages from {@link #MAIN_NAMESPACE}. Allows {@link #ALL_NAMESPACES}
     *  for obvious effect. Invalid namespaces, {@link #SPECIAL_NAMESPACE} and
     *  {@link #MEDIA_NAMESPACE} are ignored; if only these namespaces are
     *  provided, this is equivalent to {@link #ALL_NAMESPACES}.
     *  @return the title of the page
     *  @throws IOException if a network error occurs
     *  @since 0.13
     *  @see <a href="https://mediawiki.org/wiki/API:Random">MediaWiki
     *  documentation</a>
     */
    public String random(int... ns) throws IOException
    {
        if (ns.length == 0)
            ns = new int[] { MAIN_NAMESPACE };

        // no bulk queries here because they are deterministic
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("list", "random");
        if (ns[0] != ALL_NAMESPACES)
            getparams.put("rnnamespace", constructNamespaceString(ns));
        String line = makeApiCall(getparams, null, "random");
        detectUncheckedErrors(line, null, null);
        return parseAttribute(line, "title", 0);
    }

    /**
     *  Fetches edit and other types of tokens.
     *  @param type one of "csrf", "patrol", "rollback", "userrights", "watch"
     *  or "login"
     *  @return the token
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    public String getToken(String type) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("meta", "tokens");
        getparams.put("type", type);
        String content = makeApiCall(getparams, null, "getToken");
        detectUncheckedErrors(content, null, null);
        return parseAttribute(content, type + "token", 0);
    }

    // PAGE METHODS

    /**
     *  Returns the corresponding talk page to this page. Inverse of 
     *  {@link Wiki#getContentPage}.
     *
     *  @param title the page title
     *  @return the name of the talk page corresponding to <var>title</var>
     *  or "" if we cannot recognise it
     *  @throws IllegalArgumentException if given title is in a talk namespace
     *  or we try to retrieve the talk page of a Special: or Media: page.
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @see #getContentPage(String)
     *  @since 0.10
     */
    public String getTalkPage(String title)
    {
        // It is convention that talk namespaces are the original namespace + 1
        // and are odd integers.
        int namespace = namespace(title);
        if (namespace % 2 == 1)
            throw new IllegalArgumentException("Cannot fetch talk page of a talk page!");
        if (namespace < 0)
            throw new IllegalArgumentException("Special: and Media: pages do not have talk pages!");
        if (namespace != MAIN_NAMESPACE) // remove the namespace
            title = title.substring(title.indexOf(':') + 1);
        return namespaceIdentifier(namespace + 1) + ":" + title;
    }
    
    /**
     *  Returns the corresponding content page associated with this talk page.
     *  Inverse of {@link Wiki#getTalkPage}.
     *
     *  @param title the page title
     *  @return the name of the corresponding page corresponding to <var>title</var>
     *  @throws IllegalArgumentException if given title is in a content namespace
     *  or we try to retrieve the content page of a Special: or Media: page.
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @see #getTalkPage(String) 
     *  @since 0.37
     */
    public String getContentPage(String title)
    {
        // It is convention that talk namespaces are the original namespace + 1
        // and are odd integers.
        int namespace = namespace(title);
        if (namespace % 2 == 0)
            throw new IllegalArgumentException("Cannot fetch content page of a content page!");
        if (namespace < 0)
            throw new IllegalArgumentException("Special: and Media: pages do not have talk pages!");
        title = title.substring(title.indexOf(':') + 1);
        if (namespace == TALK_NAMESPACE)
            return title;
        return namespaceIdentifier(namespace - 1) + ":" + title;
    }

    /**
     *  If a namespace supports subpages, return the top-most page --
     *  {@code getRootPage("Talk:Aaa/Bbb/Ccc")} returns "Talk:Aaa" if the talk
     *  namespace supports subpages, "Talk:Aaa/Bbb/Ccc" if it doesn't. See
     *  also the <a href="https://mediawiki.org/wiki/Help:Magic_words">magic word</a>
     *  <kbd>{{ROOTPAGENAME}}</kbd>, though that removes the namespace prefix.
     *
     *  @param page a page
     *  @return (see above)
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @since 0.33
     */
    public String getRootPage(String page)
    {
        if (supportsSubpages(namespace(page)) && page.contains("/"))
            return page.substring(0, page.indexOf('/'));
        else
            return page;
    }

    /**
     *  If a namespace supports subpages, return the top-most page --
     *  {@code getParentPage("Talk:Aaa/Bbb/Ccc")} returns "Talk:Aaa/Bbb" if
     *  the talk namespace supports subpages, "Talk:Aaa/Bbb/Ccc" if it doesn't.
     *  See also the <a href="https://mediawiki.org/wiki/Help:Magic_words">magic
     *  word</a> <kbd>{{BASEPAGENAME}}</kbd>, though that removes the namespace
     *  prefix.
     *
     *  @param page a page
     *  @return (see above)
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @since 0.33
     */
    public String getParentPage(String page)
    {
        if (supportsSubpages(namespace(page)) && page.contains("/"))
            return page.substring(0, page.lastIndexOf('/'));
        else
            return page;
    }

    /**
     *  Returns a URL to the human readable version of <var>page</var>. Example:
     *  https://en.wikipedia.org/wiki/Create_a_page
     *  @param page a title
     *  @return (see above)
     *  @since 0.35
     */
    public String getPageUrl(String page)
    {
        page = normalize(page).replace(' ', '_');
        return articleUrl + URLEncoder.encode(page, StandardCharsets.UTF_8);
    }

    /**
     *  Removes the namespace identifier from a page title. Equivalent to the
     *  <a href="https://mediawiki.org/wiki/Help:Magic_words">magic word</a>
     *  <kbd>{{PAGENAME}}</kbd>.
     *
     *  @param page a page
     *  @param ns limit removal to only these namespaces. If empty, remove any
     *  namespace.
     *  @return (see above)
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @see #namespace(String)
     *  @see #namespaceIdentifier(int)
     *  @since 0.35
     */
    public String removeNamespace(String page, int... ns)
    {
        int namespace = namespace(page);
        if (namespace == 0)
            return page;
        if (ns.length == 0)
            return page.substring(page.indexOf(':') + 1);
        for (int nstest : ns)
            if (nstest == namespace)
                return page.substring(page.indexOf(':') + 1);
        return page;
    }

    /**
     *  Gets miscellaneous page info. Returns:
     *  <ul>
     *  <li><b>inputpagename</b>: (String) the page name supplied to this method
     *  <li><b>pagename</b>: (String) the normalized page name
     *  <li><b>displaytitle</b>: (String) the title of the page that is actually
     *    displayed. Example: "iPod"
     *  <li><b>protection</b>: (Map) the {@link #protect(String, Map, String)
     *    protection state} of the page. Does not cover implied protection
     *    levels (e.g. MediaWiki namespace).
     *  <li><b>exists</b>: (Boolean) whether the page exists
     *  <li><b>redirect</b>: (Boolean) whether the page is a redirection
     *  <li><b>lastpurged</b>: (OffsetDateTime) when the page was last purged or
     *    <code>null</code> if the page does not exist
     *  <li><b>lastrevid</b>: (Long) the revid of the top revision or -1L if the
     *    page does not exist
     *  <li><b>size</b>: (Integer) the size of the page or -1 if the page does
     *    not exist
     *  <li><b>pageid</b>: (Long) the id of the page or -1 if the page does not
     *    exist
     *  <li><b>timestamp</b>: (OffsetDateTime) when this method was called
     *  <li><b>watchers</b>: (Integer) number of watchers, may be restricted
     *  </ul>
     *
     *  <p>Note: <code>intestactions=X</code> is deliberately not implemented
     *  because it lowers the number of pages per network request by N, where N
     *  is the number of actions tested. 
     *
     *  @param pages the pages to get info for.
     *  @return (see above), or {@code null} for Special and Media pages.
     *  The Maps will come out in the same order as the processed array.
     *  @throws IOException if a network error occurs
     *  @since 0.23
     */
    public List<Map<String, Object>> getPageInfo(List<String> pages) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "info");
        getparams.put("inprop", "protection|displaytitle|watchers");
        Map<String, Object> postparams = new HashMap<>();
        Map<String, Map<String, Object>> metamap = new HashMap<>();
        // copy because normalization and redirect resolvers overwrite
        List<String> pages2 = new ArrayList<>(pages);
        for (String temp : constructTitleString(pages))
        {
            postparams.put("titles", temp);
            String line = makeApiCall(getparams, postparams, "getPageInfo");
            detectUncheckedErrors(line, null, null);
            resolveNormalizedParser(pages2, line);
            if (resolveredirect)
                resolveRedirectParser(pages2, line);

            // form: <page pageid="239098" ns="0" title="BitTorrent" ... >
            // <protection />
            // </page>
            for (int j = line.indexOf("<page "); j > 0; j = line.indexOf("<page ", ++j))
            {
                int x = Math.max(line.indexOf("</page>", j), line.indexOf(" />", j));
                String item = line.substring(j, x);
                Map<String, Object> tempmap = new HashMap<>(15);

                // either Special: or Media:, skip this page
                if (item.contains("special=\"\""))
                	continue;

                // does the page exist?
                String parsedtitle = parseAttribute(item, "title", 0);
                tempmap.put("pagename", parsedtitle);
                boolean exists = !item.contains("missing=\"\"");
                tempmap.put("exists", exists);
                if (exists)
                {
                    tempmap.put("lastpurged", OffsetDateTime.parse(parseAttribute(item, "touched", 0)));
                    tempmap.put("lastrevid", Long.parseLong(parseAttribute(item, "lastrevid", 0)));
                    tempmap.put("size", Integer.parseInt(parseAttribute(item, "length", 0)));
                    tempmap.put("pageid", Long.parseLong(parseAttribute(item, "pageid", 0)));
                }
                else
                {
                    tempmap.put("lastedited", null);
                    tempmap.put("lastrevid", -1L);
                    tempmap.put("size", -1);
                    tempmap.put("pageid", -1);
                }

                // parse protection level
                // expected form: <pr type="edit" level="sysop" expiry="infinity" cascade="" />
                Map<String, Object> protectionstate = new HashMap<>();
                for (int z = item.indexOf("<pr "); z > 0; z = item.indexOf("<pr ", ++z))
                {
                    String type = parseAttribute(item, "type", z);
                    String level = parseAttribute(item, "level", z);
                    protectionstate.put(type, level);
                    //if (level != NO_PROTECTION)
                    String expiry = parseAttribute(item, "expiry", z);
                    if (expiry.equals("infinity"))
                        protectionstate.put(type + "expiry", null);
                    else
                        protectionstate.put(type + "expiry", OffsetDateTime.parse(expiry));
                    // protected via cascade
                    if (item.contains("source=\""))
                        protectionstate.put("cascadesource", parseAttribute(item, "source", z));
                }
                // MediaWiki namespace
                if (namespace(parsedtitle) == MEDIAWIKI_NAMESPACE)
                {
                    protectionstate.put("edit", FULL_PROTECTION);
                    protectionstate.put("move", FULL_PROTECTION);
                    if (!exists)
                        protectionstate.put("create", FULL_PROTECTION);
                }

                protectionstate.put("cascade", item.contains("cascade=\"\""));
                tempmap.put("protection", protectionstate);

                tempmap.put("displaytitle", parseAttribute(item, "displaytitle", 0));
                tempmap.put("timestamp", OffsetDateTime.now(timezone));
                tempmap.put("redirect", item.contains("redirect=\"\""));

                // number of watchers
                if (item.contains("watchers=\""))
                    tempmap.put("watchers", Integer.parseInt(parseAttribute(item, "watchers", 0)));

                metamap.put(parsedtitle, tempmap);
            }
        }

        int size = pages.size();
        Map<String, Object>[] info = new HashMap[size];
        // Reorder. Make a new HashMap so that inputpagename remains unique.
        for (int i = 0; i < pages2.size(); i++)
        {
            Map<String, Object> tempmap = metamap.get(pages2.get(i));
            if (tempmap != null)
            {
                info[i] = new HashMap<>(tempmap);
                info[i].put("inputpagename", pages.get(i));
            }
        }
        log(Level.INFO, "getPageInfo", "Successfully retrieved page info for " + size + " pages.");
        return Arrays.asList(info);
    }

    /**
     *  Fills namespace cache.
     *  @throws UncheckedIOException if a network error occurs (unchecked for
     *  lambda friendliness, very rare since this should only be called once
     *  per session)
     *  @since 0.32
     */
    private void ensureNamespaceCache()
    {
        try
        {
            if (namespaces == null)
                getSiteInfo();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     *  Returns the namespace a page is in. There is no need to override this to
     *  add custom namespaces, though you may want to define static fields e.g.
     *  {@code public static final int PORTAL_NAMESPACE = 100;} for the Portal
     *  namespace on the English Wikipedia.
     *
     *  @param title any valid page name
     *  @return an integer representing the namespace of <var>title</var>
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @see #namespaceIdentifier(int)
     *  @since 0.03
     */
    public int namespace(String title)
    {
        ensureNamespaceCache();

        // perform a limited normalization
        if (title.startsWith(":"))
            title = title.substring(1);
        if (!title.contains(":"))
            return MAIN_NAMESPACE;
        title = title.replace("_", " ");
        String namespace = title.substring(0, title.indexOf(':'));
        Map<String, Integer> tempmap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tempmap.putAll(namespaces);
        return tempmap.getOrDefault(namespace, MAIN_NAMESPACE);
    }

    /**
     *  For a given namespace denoted as an integer, fetch the corresponding
     *  identification string e.g. {@code namespaceIdentifier(1)} should
     *  return "Talk" on en.wp. (This does the exact opposite to {@link
     *  #namespace(String)}). Strings returned are always localized.
     *
     *  @param namespace an integer corresponding to a namespace. If it does not
     *  correspond to a namespace, we assume you mean the main namespace (i.e.
     *  return "").
     *  @return the identifier of the namespace
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @see #namespace(String)
     *  @since 0.25
     */
    public String namespaceIdentifier(int namespace)
    {
        ensureNamespaceCache();

        // anything we cannot identify is assumed to be in the main namespace
        if (!namespaces.containsValue(namespace))
            return "";
        for (Map.Entry<String, Integer> entry : namespaces.entrySet())
            if (entry.getValue().equals(namespace))
                return entry.getKey();
        throw new AssertionError("Unreachable.");
    }

    /**
     *  Gets the namespaces used by this wiki.
     *  @return a map containing e.g. {"Media" : -2, "Special" : -1, ...}.
     *  Changes in this map do not propagate back to this Wiki object.
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @since 0.28
     */
    public LinkedHashMap<String, Integer> getNamespaces()
    {
        ensureNamespaceCache();
        return new LinkedHashMap<>(namespaces);
    }

    /**
     *  Returns true if the given namespace allows subpages.
     *  @param ns a namespace number
     *  @return (see above)
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @throws IllegalArgumentException if the give namespace does not exist
     *  @since 0.33
     */
    public boolean supportsSubpages(int ns)
    {
        ensureNamespaceCache();
        if (namespaces.containsValue(ns))
            return ns_subpages.contains(ns);
        throw new IllegalArgumentException("Invalid namespace " + ns);
    }

    /**
     *  Determines whether a series of pages exist.
     *  @param titles the titles to check.
     *  @return whether the pages exist, in the same order as the processed array
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.10
     */
    public boolean[] exists(List<String> titles) throws IOException
    {
        boolean[] ret = new boolean[titles.size()];
        List<Map<String, Object>> info = getPageInfo(titles);
        for (int i = 0; i < titles.size(); i++)
            ret[i] = (Boolean)info.get(i).get("exists");
        return ret;
    }

    /**
     *  Gets the raw wikicode for a set of pages. WARNING: does not support
     *  special pages. Check [[User talk:MER-C/Wiki.java#Special page equivalents]]
     *  for fetching the contents of special pages. Use {@link #getImage(String,
     *  File)} to fetch an image. If a page doesn't exist, the corresponding
     *  return value is {@code null}.
     *
     *  @param titles a list of titles
     *  @return the raw wikicode of those titles, in the same order as the input
     *  array
     *  @throws UnsupportedOperationException if you try to retrieve the text of
     *  a Special: or Media: page
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.32
     *  @see #edit
     */
    public List<String> getPageText(List<String> titles) throws IOException
    {
        return getText(titles, null, -1);
    }

    /**
     *  Gets the wikitext of a list of titles or revisions. If a page or
     *  revision doesn't exist or is deleted, return {@code null}.
     *  RevisionDeleted revisions are not allowed.
     *
     *  @param titles a list of titles (use null to skip, overrides revids)
     *  @param revids a list of revids (use null to skip)
     *  @param section a section number. This section number must exist in all
     *  titles or revids otherwise you will get vast swathes of your results
     *  being erroneously null. Optional, use -1 to skip.
     *  @return the raw wikicode of those titles, in the same order as the input
     *  array
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.35
     */
    public List<String> getText(List<String> titles, long[] revids, int section) throws IOException
    {
        // determine what type of request we have. Cannot mix the two.
        // FIXME: XML bleeding to return results for lists of pages
        boolean isrevisions;
        int count = 0;
        List<String> titles2 = null;
        if (titles != null)
        {
            // validate titles
            for (String title : titles)
                if (namespace(title) < 0)
                    throw new UnsupportedOperationException("Cannot retrieve \"" + title + "\": namespace < 0.");
            isrevisions = false;
            count = titles.size();
            // copy because normalization and redirect resolvers overwrite
            titles2 = new ArrayList<>(titles);
        }
        else if (revids != null)
        {
            isrevisions = true;
            count = revids.length;
        }
        else
            throw new IllegalArgumentException("Either titles or revids must be specified!");
        if (count == 0)
            return Collections.emptyList();

        Map<String, String> pageTexts = new HashMap<>(2 * count);
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "revisions");
        getparams.put("rvprop", "ids|content");
        if (section >= 0)
            getparams.put("rvsection", String.valueOf(section));
        Map<String, Object> postparams = new HashMap<>();

        List<String> chunks = isrevisions ? constructRevisionString(revids) : constructTitleString(titles);
        for (String chunk : chunks)
        {
            postparams.put(isrevisions ? "revids" : "titles", chunk);
            String temp = makeApiCall(getparams, postparams, "getText");
            detectUncheckedErrors(temp, null, Map.of("nosuchsection", desc -> {}));
            String[] results = temp.split(isrevisions ? "<rev " : "<page ");
            if (!isrevisions)
            {
                resolveNormalizedParser(titles2, results[0]);
                if (resolveredirect)
                    resolveRedirectParser(titles2, results[0]);
            }

            // skip first element to remove front crud
            for (int i = 1; i < results.length; i++)
            {
                // determine existance, then locate and extract content
                String key = parseAttribute(results[i], isrevisions ? "revid" : "title", 0);
                if (!results[i].contains("missing=\"\"") && !results[i].contains("texthidden=\"\""))
                {
                    int x = results[i].indexOf("<rev ", i);
                    int y = results[i].indexOf('>', x) + 1;
                    // this </rev> tag is not present for empty pages/revisions
                    int z = results[i].indexOf("</rev>", y);
                    // store result for later
                    String text = (z < 0) ? "" : decode(results[i].substring(y, z));
                    pageTexts.put(key, text);
                }
            }
        }

        // returned array is in the same order as input array
        String[] ret = new String[count];
        for (int i = 0; i < count; i++)
        {
            String key = isrevisions ? String.valueOf(revids[i]) : titles2.get(i);
            ret[i] = pageTexts.get(key);
        }
        log(Level.INFO, "getText", "Successfully retrieved text of " + count + (isrevisions ? " revisions." : " pages."));
        return Arrays.asList(ret);
    }

    /**
     *  Gets the text of a specific section. Useful for section editing.
     *  @param title the title of the relevant page
     *  @param section the section number of the section to retrieve text for
     *  @return the text of the given section, or {@code null} if the page doesn't
     *  have that many sections
     *  @throws IOException if a network error occurs
     *  @throws IllegalArgumentException if {@code section < 0}
     *  @since 0.24
     */
    public String getSectionText(String title, int section) throws IOException
    {
        if (section < 0)
            throw new IllegalArgumentException("Section numbers must be positive!");
        return getText(List.of(title), null, section).get(0);
    }

    /**
     *  Gets the contents of a page, rendered in HTML (as opposed to
     *  wikitext). WARNING: only supports special pages in certain
     *  circumstances, for example {@code getRenderedText("Special:Recentchanges")}
     *  returns the 50 most recent change to the wiki in pretty-print HTML. You
     *  should test any use of this method on-wiki through the text
     *  <kbd>{{Special:Specialpage}}</kbd>. Use {@link #getImage(String, File)}
     *  to fetch an image. Be aware of any transclusion limits, as outlined at
     *  [[Wikipedia:Template limits]].
     *
     *  @param title the title of the page
     *  @return the rendered contents of that page
     *  @throws IOException if a network error occurs
     *  @since 0.10
     */
    public String getRenderedText(String title) throws IOException
    {
        Map<String, Object> content = new HashMap<>();
        if (namespace(title) == SPECIAL_NAMESPACE)
            // not guaranteed to succeed...
            content.put("text", "{{:" + title + "}}");
        else
            content.put("title", title);
        return parse(content, -1, false);
    }

    /**
     *  Edits a page by setting its text to the supplied value. This method is
     *  {@linkplain #setThrottle(int) throttled}. The edit will be marked bot if
     *  {@link #isMarkBot()} is {@code true} and minor if {@link #isMarkMinor()}
     *  is {@code true}.
     *
     *  @param text the text of the page
     *  @param title the title of the page
     *  @param summary the edit summary. See [[Help:Edit summary]]. Summaries
     *  longer than 200 characters are truncated server-side.
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws UnsupportedOperationException if you try to edit a Special: or a
     *  Media: page
     *  @throws ConcurrentModificationException if an edit conflict occurs
     *  @see #getPageText
     */
    public void edit(String title, String text, String summary) throws IOException, LoginException
    {
        edit(title, text, summary, markminor, markbot, -2, null);
    }

    /**
     *  Edits a page by setting its <var>text</var> to the supplied value. This
     *  method is {@linkplain #setThrottle(int) throttled}. The edit will be
     *  marked bot if {@link #isMarkBot()} is {@code true} and minor if {@link
     *  #isMarkMinor()} is {@code true}.
     *
     *  @param text the text of the page
     *  @param title the title of the page
     *  @param summary the edit summary. See [[Help:Edit summary]]. Summaries
     *  longer than 200 characters are truncated server-side.
     *  @param basetime the timestamp of the revision on which <var>text</var> is
     *  based, used to check for edit conflicts. {@code null} disables this.
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws UnsupportedOperationException if you try to edit a Special: or a
     *  Media: page
     *  @throws ConcurrentModificationException if an edit conflict occurs
     *  @see #getPageText
     */
    public void edit(String title, String text, String summary, OffsetDateTime basetime) throws IOException, LoginException
    {
        edit(title, text, summary, markminor, markbot, -2, basetime);
    }

    /**
     *  Edits a section by setting its <var>text</var> to the supplied value.
     *  This method is {@linkplain #setThrottle(int) throttled}. The edit will
     *  be marked bot if {@link #isMarkBot()} is {@code true} and minor if
     *  {@link #isMarkMinor()} is {@code true}.
     *
     *  @param text the text of the page
     *  @param title the title of the page
     *  @param summary the edit summary. See [[Help:Edit summary]]. Summaries
     *  longer than 200 characters are truncated server-side.
     *  @param section the section to edit. Use -1 to specify a new section and
     *  -2 to disable section editing.
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws UnsupportedOperationException if you try to edit a Special: or a
     *  Media: page
     *  @throws ConcurrentModificationException if an edit conflict occurs
     *  @see #getPageText
     *  @since 0.25
     */
    public void edit(String title, String text, String summary, int section) throws IOException, LoginException
    {
        edit(title, text, summary, markminor, markbot, section, null);
    }

    /**
     *  Edits a page by setting its <var>text</var> to the supplied value. This
     *  method is {@linkplain #setThrottle(int) throttled}. The edit will be
     *  marked bot if {@link #isMarkBot()} is {@code true} and minor if {@link
     *  #isMarkMinor()} is {@code true}.
     *
     *  @param text the text of the page
     *  @param title the title of the page
     *  @param summary the edit summary. See [[Help:Edit summary]]. Summaries
     *  longer than 200 characters are truncated server-side.
     *  @param section the section to edit. Use -1 to specify a new section and
     *  -2 to disable section editing.
     *  @param basetime the timestamp of the revision on which <var>text</var> is
     *  based, used to check for edit conflicts. {@code null} disables this.
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws UnsupportedOperationException if you try to edit a Special: or a
     *  Media: page
     *  @throws ConcurrentModificationException if an edit conflict occurs
     *  @see #getPageText
     *  @since 0.25
     */
    public void edit(String title, String text, String summary, int section, OffsetDateTime basetime)
        throws IOException, LoginException
    {
        edit(title, text, summary, markminor, markbot, section, basetime);
    }

    /**
     *  Edits a page by setting its text to the supplied value. This method is
     *  {@linkplain #setThrottle(int) throttled}.
     *
     *  @param text the text of the page
     *  @param title the title of the page
     *  @param summary the edit summary or the title of the new section. See
     *  [[Help:Edit summary]]. Summaries longer than 255 characters are
     *  truncated server-side.
     *  @param minor whether the edit should be marked as minor. See
     *  [[Help:Minor edit]]. Overrides {@link #isMarkMinor()}.
     *  @param bot whether to mark the edit as a bot edit. Ignored if one does
     *  not have the necessary permissions. Overrides {@link #isMarkBot()}.
     *  @param section the section to edit. Use -1 to specify a new section and
     *  -2 to disable section editing.
     *  @param basetime the timestamp of the revision on which <var>text</var> is
     *  based, used to check for edit conflicts. {@code null} disables this.
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws UnsupportedOperationException if you try to edit a Special: or
     *  Media: page
     *  @throws ConcurrentModificationException if an edit conflict occurs
     *  @see #getPageText
     *  @since 0.17
     */
    public synchronized void edit(String title, String text, String summary, boolean minor, boolean bot,
        int section, OffsetDateTime basetime) throws IOException, LoginException
    {
        throttle();

        // protection
        Map<String, Object> info = getPageInfo(List.of(title)).get(0);
        if (!checkRights(info, "edit") || (Boolean)info.get("exists") && !checkRights(info, "create"))
        {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            log(Level.WARNING, "edit", "Cannot edit - permission denied. " + ex);
            throw ex;
        }

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "edit");
        getparams.put("title", normalize(title));

        // post data
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("text", text);
        // edit summary is created automatically if making a new section
        if (section != -1)
            postparams.put("summary", summary);
        postparams.put("token", getToken("csrf"));
        if (basetime != null)
        {
            postparams.put("starttimestamp", info.get("timestamp"));
            // I wonder if the time getPageText() was called suffices here
            postparams.put("basetimestamp", basetime);
        }
        if (minor)
            postparams.put("minor", "1");
        if (bot && user != null && user.isAllowedTo("bot"))
            postparams.put("bot", "1");
        if (section == -1)
        {
            postparams.put("section", "new");
            postparams.put("sectiontitle", summary);
        }
        else if (section != -2)
            postparams.put("section", section);
        
        String response = makeApiCall(getparams, postparams, "edit");
        checkErrorsAndUpdateStatus(response, "edit", Map.of(
            "editconflict", desc -> new ConcurrentModificationException(desc + "- [[" + title + "]]")), null);
        log(Level.INFO, "edit", "Successfully edited " + title);
    }

    /**
     *  Creates a new section on the specified page. Leave <var>subject</var> as
     *  the empty string if you just want to append. This method is
     *  {@linkplain #setThrottle(int) throttled}.
     *
     *  @param title the title of the page to edit
     *  @param subject the subject of the new section
     *  @param text the text of the new section
     *  @param minor whether the edit should be marked as minor. See
     *  [[Help:Minor edit]]. Overrides {@link #isMarkMinor()}.
     *  @param bot whether to mark the edit as a bot edit. Ignored if one does
     *  not have the necessary permissions. Overrides {@link #isMarkBot()}.
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws UnsupportedOperationException if you try to edit a Special: or
     *  Media: page
     *  @since 0.17
     */
    public void newSection(String title, String subject, String text, boolean minor, boolean bot) throws IOException, LoginException
    {
        edit(title, text, subject, minor, bot, -1, null);
    }

    /**
     *  Prepends something to the given page. A convenience method for
     *  adding maintenance templates, rather than getting and setting the
     *  page yourself. {@linkplain #setThrottle(int) throttled}.
     *
     *  @param title the title of the page
     *  @param stuff what to prepend to the page
     *  @param summary the edit summary. See [[Help:Edit summary]]. Summaries
     *  longer than 200 characters are truncated server-side.
     *  @param minor whether the edit is minor. Overrides {@link #isMarkMinor()}.
     *  @param bot whether to mark the edit as a bot edit. Ignored if one does
     *  not have the necessary permissions. Overrides {@link #isMarkBot()}.
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws UnsupportedOperationException if you try to retrieve the text
     *  of a Special: page or a Media: page
     *  @throws IOException if a network error occurs
     */
    public void prepend(String title, String stuff, String summary, boolean minor, boolean bot) throws IOException, LoginException
    {
        StringBuilder text = new StringBuilder(100000);
        text.append(stuff);
        // section 0 to save bandwidth
        text.append(getSectionText(title, 0));
        edit(title, text.toString(), summary, minor, bot, 0, null);
    }

    /**
     *  Deletes a page. Does not delete any page with more than 5000 revisions.
     *  {@linkplain #setThrottle(int) throttled}.
     *  @param title the page to delete
     *  @param reason the reason for deletion
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws SecurityException if the user lacks the privileges to delete
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if user is blocked
     *  @throws UnsupportedOperationException if <var>title</var> is a Special
     *  or Media page
     *  @since 0.24
     */
    public synchronized void delete(String title, String reason) throws IOException, LoginException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Cannot delete Special and Media pages!");
        checkPermissions("delete", "delete");
        throttle();

        // edit token
        Map<String, Object> info = getPageInfo(List.of(title)).get(0);
        if (Boolean.FALSE.equals(info.get("exists")))
        {
            log(Level.INFO, "delete", "Page \"" + title + "\" does not exist.");
            return;
        }

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "delete");
        getparams.put("title", normalize(title));
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        postparams.put("token", getToken("csrf"));
        
        String response = makeApiCall(getparams, postparams, "delete");
        checkErrorsAndUpdateStatus(response, "delete", null, null);
        log(Level.INFO, "delete", "Successfully deleted " + title);
    }

    /**
     *  Undeletes a page. Equivalent to [[Special:Undelete]]. Restores ALL deleted
     *  revisions and files by default. This method is {@linkplain
     *  #setThrottle(int) throttled}.
     *
     *  @param title a page to undelete
     *  @param reason the reason for undeletion
     *  @param revisions a list of revisions for selective undeletion
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws SecurityException if the user lacks the privileges to undelete
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if user is blocked
     *  @throws UnsupportedOperationException if <var>title</var> is a Special
     *  or Media page
     *  @since 0.30
     */
    public synchronized void undelete(String title, String reason, Revision... revisions) throws IOException, LoginException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Cannot delete Special and Media pages!");
        checkPermissions("undelete", "undelete");
        throttle();

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "undelete");
        getparams.put("title", normalize(title));

        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        postparams.put("token", getToken("csrf"));
        if (revisions.length != 0)
        {
            StringJoiner sj = new StringJoiner("|");
            // https://phabricator.wikimedia.org/T16449
            for (Wiki.Revision revision : revisions)
                sj.add(revision.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            postparams.put("timestamps", sj.toString());
        }
        
        String response = makeApiCall(getparams, postparams, "undelete");
        Map<String, Consumer<String>> info = Map.of(
            "cantundelete", desc -> log(Level.WARNING, "undelete", desc + " Page = [[" + title + "]]"));
        if (checkErrorsAndUpdateStatus(response, "undelete", null, info))
        {
            log(Level.INFO, "undelete", "Successfully undeleted " + title);
            for (Revision rev : revisions)
                rev.pageDeleted = false;
        }
    }

    /**
     *  Purges the server-side cache for various pages.
     *  @param titles the titles of the page to purge
     *  @param links update the links tables
     *  @throws IOException if a network error occurs
     *  @since 0.17
     */
    public void purge(boolean links, String... titles) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "purge");
        if (links)
            getparams.put("forcelinkupdate", "");
        Map<String, Object> postparams = new HashMap<>();
        for (String x : constructTitleString(List.of(titles)))
        {
            postparams.put("title", x);
            String response = makeApiCall(getparams, postparams, "purge");
            detectUncheckedErrors(response, null, null);
        }
        log(Level.INFO, "purge", "Successfully purged " + titles.length + " pages.");
    }

    /**
     *  Gets the list of images used on the given pages. If there are redirected
     *  images, both the source and target page are included. Return order is
     *  the same as the input order.
     *
     *  @param titles a list of pages
     *  @return the list of images used in those pages.
     *  @throws IOException if a network error occurs
     *  @since 0.36
     */
    public List<List<String>> getImagesOnPage(List<String> titles) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "images");

        List<List<String>> ret = makeVectorizedQuery("im", getparams, titles,
            "getImagesOnPage", -1, (data, result) ->
        {
            // xml form: <im ns="6" title="File:Example.jpg" />
            for (int a = data.indexOf("<im "); a > 0; a = data.indexOf("<im ", ++a))
                result.add(parseAttribute(data, "title", a));
        });
        log(Level.INFO, "getImagesOnPage", "Successfully retrieved images used on " + titles.size() + " pages.");
        return ret;
    }

    /**
     *  Gets the list of categories that the given list of pages belongs to.
     *  Includes the sortkey of a category if <var>sortkey</var> is true. The
     *  sortkey would then be appended to the element of the returned strings
     *  (separated by "|"). Accepted parameters from <var>helper</var> are:
     *  are:
     *
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#filterBy(Map) filter by}: "hidden"
     *  (hidden categories)
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param titles a list of pages
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @param sortkey return a sortkey as well (default = false)
     *  @return the list of categories that the page is in
     *  @throws IOException if a network error occurs
     *  @since 0.30
     */
    public List<List<String>> getCategories(List<String> titles, Wiki.RequestHelper helper, boolean sortkey) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "categories");
        int limit = -1;
        if (helper != null)
        {
            helper.setRequestType("cl");
            getparams.putAll(helper.addShowParameter());
            limit = helper.limit();
        }

        List<List<String>> ret = makeVectorizedQuery("cl", getparams, titles,
            "getCategories", limit, (data, result) ->
        {
            // xml form: <cl ns="14" title="Category:1879 births" sortkey=(long string) sortkeyprefix="" />
            // or      : <cl ns="14" title="Category:Images for cleanup" sortkey=(long string) sortkeyprefix="Borders" hidden="" />
            for (int a = data.indexOf("<cl "); a > 0; a = data.indexOf("<cl ", ++a))
            {
                String category = parseAttribute(data, "title", a);
                if (sortkey)
                    category += ("|" + parseAttribute(data, "sortkeyprefix", a));
                result.add(category);
            }
        });
        log(Level.INFO, "getCategories", "Successfully retrieved categories used on " + titles.size() + " pages.");
        return ret;
    }

    /**
     *  Gets the list of templates used on the given pages that are in a
     *  particular namespace(s). The order of elements in the return array is
     *  the same as the order of the list of titles.
     *
     *  @param titles a list of pages
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return the list of templates used by those pages page in that namespace
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    public List<List<String>> getTemplates(List<String> titles, int... ns) throws IOException
    {
        return getTemplates(titles, null, ns);
    }

    /**
     *  Determine whether a list of pages contains the given template. The order
     *  of elements in the return array is the same as the order of the list of
     *  titles.
     *
     *  @param pages a list of pages
     *  @param template the template to check for
     *  @return whether the given pages contain said template
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    public boolean[] pageHasTemplate(List<String> pages, String template) throws IOException
    {
        boolean[] ret = new boolean[pages.size()];
        List<List<String>> result = getTemplates(pages, template);
        for (int i = 0; i < result.size(); i++)
            ret[i] = !(result.get(i).isEmpty());
        return ret;
    }

    /**
     *  Gets the list of templates used on the given pages that are in a
     *  particular namespace(s). The order of elements in the return array is
     *  the same as the order of the list of titles.
     *
     *  @param titles a list of pages
     *  @param template restrict results to the supplied page. Useful for checking
     *  whether a list of pages contains a given template.
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return the list of templates used by those pages page in that namespace
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    protected List<List<String>> getTemplates(List<String> titles, String template, int... ns) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "templates");
        if (ns.length > 0)
            getparams.put("tlnamespace", constructNamespaceString(ns));
        if (template != null)
            getparams.put("tltemplates", normalize(template));

        List<List<String>> ret = makeVectorizedQuery("tl", getparams, titles,
            "getTemplates", -1, (data, result) ->
        {
            // xml form: <tl ns="10" title="Template:POTD" />
            for (int a = data.indexOf("<tl "); a > 0; a = data.indexOf("<tl ", ++a))
                result.add(parseAttribute(data, "title", a));
        });
        log(Level.INFO, "getTemplates", "Successfully retrieved templates used on " + titles.size() + " pages.");
        return ret;
    }

    /**
     *  Gets the list of interwiki links a particular page has. The returned
     *  map has the format { language code : the page on the external wiki
     *  linked to }.
     *
     *  @param titles a list of pages
     *  @return a map of interwiki links that page has (empty if there are no
     *  links)
     *  @throws IOException if a network error occurs
     *  @since 0.36
     */
    public List<Map<String, String>> getInterWikiLinks(List<String> titles) throws IOException
    {
        List<String> titles2 = new ArrayList<>(titles);
        Map<String, String> getparams = new HashMap<>();
        Map<String, Object> postparams = new HashMap<>();
        getparams.put("prop", "langlinks");
        
        BiConsumer<String, List<String[]>> parser = (line, results) ->
        {
            // xml form: <ll lang="en" />Main Page</ll> or <ll lang="en" /> for [[Main Page]]
            for (int a = line.indexOf("<ll "); a > 0; a = line.indexOf("<ll ", ++a))
            {
                String language = parseAttribute(line, "lang", a);
                int b = line.indexOf('>', a) + 1;
                int c = line.indexOf('<', b);
                String page = decode(line.substring(b, c));
                results.add(new String[] { language, page });
            }
        };
        
        List<Map<String, Map<String, String>>> temp = new ArrayList<>();
        for (String chunk : constructTitleString(titles2))
        {
            postparams.put("titles", chunk);
            temp.addAll(makeListQuery("ll", getparams, postparams, "getInterWikiLinks", -1, (line, results) ->
            {
                // Split the result into individual listings for each article.
                String[] x = line.split("<page ");
                resolveNormalizedParser(titles2, x[0]);
                if (resolveredirect)
                    resolveRedirectParser(titles2, x[0]);

                // Skip first element to remove front crud.
                for (int i = 1; i < x.length; i++)
                {
                    String parsedtitle = parseAttribute(x[i], "title", 0);
                    List<String[]> list = new ArrayList<>();
                    parser.accept(x[i], list);
                    
                    Map<String, String> interwikis = new HashMap<>(750);
                    list.forEach(result -> interwikis.put(result[0], result[1]));

                    Map<String, Map<String, String>> intermediate = new HashMap<>();
                    intermediate.put(parsedtitle, interwikis);
                    results.add(intermediate);
                }
            }));
        }

        // sort out the order
        Map<String, Map<String, String>> titletointerwiki = new HashMap<>();
        List<Map<String, String>> ret = new ArrayList<>();
        for (var entry : temp)
            titletointerwiki.putAll(entry);
        for (String title : titles2)
            ret.add(titletointerwiki.get(title));
        
        log(Level.INFO, "getInterWikiLinks", "Successfully retrieved interwiki links for " + titles.size() + " pages.");
        return ret;
    }

    /**
     *  Gets the list of wikilinks used on a particular page. Patch somewhat by
     *  wim.jongman
     *
     *  @param title a page
     *  @return the list of links used in the page
     *  @throws IOException if a network error occurs
     *  @since 0.24
     */
    public List<String> getLinksOnPage(String title) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "links");
        getparams.put("titles", normalize(title));

        List<String> links = makeListQuery("pl", getparams, null, "getLinksOnPage", -1, (line, results) ->
        {
            // xml form: <pl ns="6" title="page name" />
            for (int a = line.indexOf("<pl "); a > 0; a = line.indexOf("<pl ", ++a))
                results.add(parseAttribute(line, "title", a));
        });

        int size = links.size();
        log(Level.INFO, "getLinksOnPage", "Successfully retrieved links used on " + title + " (" + size + " links)");
        return links;
    }

    /**
     *  Gets the list of external links used on a list of pages. The return list
     *  contains results that correspond to the list of input titles, element wise.
     *
     *  @param titles a list of pages
     *  @return the lists of external links used on those pages
     *  @throws IOException if a network error occurs
     *  @since 0.35
     *  @see <a href="https://www.mediawiki.org/wiki/API:Extlinks">MediaWiki
     *  documentation</a>
     *  @see <a href="https://mediawiki.org/wiki/Manual:Externallinks_table">Externallinks
     *  table</a>
     */
    public List<List<String>> getExternalLinksOnPage(List<String> titles) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "extlinks");

        List<List<String>> ret = makeVectorizedQuery("el", getparams, titles,
            "getExternalLinksOnPage", -1, (data, result) ->
        {
            // xml form: <el stuff>http://example.com</el>
            for (int a = data.indexOf("<el "); a > 0; a = data.indexOf("<el ", ++a))
            {
                int start = data.indexOf('>', a) + 1;
                int end = data.indexOf("</el>", start);
                result.add(decode(data.substring(start, end)));
            }
        });
        log(Level.INFO, "getExternalLinksOnPage", "Successfully retrieved external links used on " + titles.size() + " pages.");
        return ret;
    }

    /**
     *  Gets the list of sections on the specified <var>page</var>. The returned
     *  map pairs the section numbering as in the table of contents with the
     *  section title, as in the following example:
     *
     *  <pre><samp>
     *  1 &#8594; How to nominate
     *  1.1 &#8594; Step 1 - Evaluate
     *  1.2 &#8594; Step 2 - Create subpage
     *  1.2.1 &#8594; Step 2.5 - Transclude and link
     *  1.3 &#8594; Step 3 - Update image
     *  </samp></pre>
     *
     *  @param page the page to get sections for
     *  @return the section map for that page
     *  @throws IOException if a network error occurs
     *  @since 0.18
     */
    public LinkedHashMap<String, String> getSectionMap(String page) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "parse");
        getparams.put("prop", "sections");
        getparams.put("text", "{{:" + normalize(page) + "}}__TOC__");
        String line = makeApiCall(getparams, null, "getSectionMap");
        detectUncheckedErrors(line, null, null);

        // xml form: <s toclevel="1" level="2" line="How to nominate" number="1" />
        LinkedHashMap<String, String> map = new LinkedHashMap<>(30);
        for (int a = line.indexOf("<s "); a > 0; a = line.indexOf("<s ", ++a))
        {
            String title = parseAttribute(line, "line", a);
            String number = parseAttribute(line, "number", a);
            map.put(number, title);
        }
        log(Level.INFO, "getSectionMap", "Successfully retrieved section map for " + page);
        return map;
    }

    /**
     *  Gets the most recent revision of a page, or {@code null} if the page
     *  does not exist.
     *  @param title a page
     *  @return the most recent revision of that page
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws UnsupportedOperationException if <var>title</var> is a Special
     *  or Media page
     *  @since 0.24
     */
    public Revision getTopRevision(String title) throws IOException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Special and Media pages do not have histories!");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "revisions");
        getparams.put("rvlimit", "1");
        getparams.put("titles", normalize(title));
        getparams.put("rvprop", "timestamp|user|ids|flags|size|comment|parsedcomment|sha1|tags");
        String line = makeApiCall(getparams, null, "getTopRevision");
        detectUncheckedErrors(line, null, null);
        int a = line.indexOf("<rev "); // important space
        int b = line.indexOf("</rev>", a);
        if (a < 0) // page does not exist
            return null;
        return parseRevision(line.substring(a, b), title);
    }

    /**
     *  Gets the first revision of a page, or {@code null} if the page does not
     *  exist.
     *  @param title a page
     *  @return the oldest revision of that page
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws UnsupportedOperationException if <var>title</var> is a Special
     *  or Media page
     *  @since 0.24
     */
    public Revision getFirstRevision(String title) throws IOException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Special and Media pages do not have histories!");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "revisions");
        getparams.put("rvlimit", "1");
        getparams.put("rvdir", "newer");
        getparams.put("titles", normalize(title));
        getparams.put("rvprop", "timestamp|user|ids|flags|size|comment|parsedcomment|sha1|tags");
        String line = makeApiCall(getparams, null, "getFirstRevision");
        detectUncheckedErrors(line, null, null);
        int a = line.indexOf("<rev "); // important space!
        int b = line.indexOf("</rev>", a);
        if (a < 0) // page does not exist
            return null;
        return parseRevision(line.substring(a, b), title);
    }

    /**
     *  Gets the newest page name or the name of a page where the asked pages
     *  redirect.
     *  @param titles a list of titles. These are normalized in the process.
     *  @return for each title, the page redirected to or the original page
     *  title if not a redirect
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.29
     *  @author Nirvanchik/MER-C
     */
    public List<String> resolveRedirects(List<String> titles) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        if (!resolveredirect)
            getparams.put("redirects", "");
        Map<String, Object> postparams = new HashMap<>();
        List<String> ret = new ArrayList<>(titles);
        for (String blah : constructTitleString(titles))
        {
            postparams.put("titles", blah);
            String line = makeApiCall(getparams, postparams, "resolveRedirects");
            detectUncheckedErrors(line, null, null);
            resolveNormalizedParser(ret, line);
            resolveRedirectParser(ret, line);
        }
        return ret;
    }

    /**
     *  Parses the output of queries that resolve redirects (extracted to
     *  separate method as requirement for all vectorized queries when
     *  {@link #isResolvingRedirects()} is {@code true}).
     *
     *  @param inputpages the array of pages to resolve redirects for. Entries
     *  will be overwritten.
     *  @param xml the xml to parse
     *  @since 0.34
     */
    protected void resolveRedirectParser(List<String> inputpages, String xml)
    {
        // expected form: <redirects><r from="Main page" to="Main Page"/>
        // <r from="Home Page" to="Home page"/>...</redirects>
        for (int j = xml.indexOf("<r "); j > 0; j = xml.indexOf("<r ", ++j))
        {
            String parsedtitle = parseAttribute(xml, "from", j);
            for (int i = 0; i < inputpages.size(); i++)
                if (inputpages.get(i).equals(parsedtitle))
                    inputpages.set(i, parseAttribute(xml, "to", j));
        }
    }

    /**
     *  Parses the output of queries that normalize page titles.
     *
     *  @param inputpages the array of pages to normalize titles for. Entries
     *  will be overwritten.
     *  @param xml the xml to parse
     *  @since 0.36
     */
    protected void resolveNormalizedParser(List<String> inputpages, String xml)
    {
    	// expected form: <normalized><n from="User:Male dewiki user" to="Benutzer:Male dewiki user"/>
        // <r from="User:Female dewiki user" to="Benutzerin:Female dewiki user"/>...</normalized>
        for (int j = xml.indexOf("<n "); j > 0; j = xml.indexOf("<n ", ++j))
        {
            String parsedtitle = parseAttribute(xml, "from", j);
            for (int i = 0; i < inputpages.size(); i++)
                if (inputpages.get(i).equals(parsedtitle))
                    inputpages.set(i, parseAttribute(xml, "to", j));
        }
    }

    /**
     *  Gets the revision history of a page. Accepted parameters from
     *  <var>helper</var> are:
     *
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#byUser(String) user}
     *  <li>{@link Wiki.RequestHelper#notByUser(String) not by user}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#taggedWith(String) tag}
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param title a page
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return the revisions of that page in that time span
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws UnsupportedOperationException if <var>title</var> is a Special
     *  or Media page
     *  @since 0.19
     *  @see <a href="https://mediawiki.org/wiki/API:Revisions">MediaWiki
     *  documentation</a>
     */
    public List<Revision> getPageHistory(String title, Wiki.RequestHelper helper) throws IOException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Special and Media pages do not have histories!");

        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "revisions");
        getparams.put("titles", normalize(title));
        getparams.put("rvprop", "timestamp|user|ids|flags|size|comment|parsedcomment|sha1|tags");
        if (helper != null)
        {
            helper.setRequestType("rv");
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addUserParameter());
            getparams.putAll(helper.addExcludeUserParameter());
            getparams.putAll(helper.addTagParameter());
            limit = helper.limit();
        }

        List<Revision> revisions = makeListQuery("rv", getparams, null, "getPageHistory", limit, (line, results) ->
        {
            for (int a = line.indexOf("<rev "); a > 0; a = line.indexOf("<rev ", ++a))
            {
                int b = line.indexOf("</rev>", a);
                results.add(parseRevision(line.substring(a, b), title));
            }
        });

        log(Level.INFO, "getPageHistory", "Successfully retrieved page history of "
            + title + " (" + revisions.size() + " revisions)");
        return revisions;
    }

    /**
     *  Gets the deleted history of a page. Accepted parameters from
     *  <var>helper</var> are:
     *
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#byUser(String) user}
     *  <li>{@link Wiki.RequestHelper#notByUser(String) not by user}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#taggedWith(String) tag}
     *  </ul>
     *
     *  @param title a page (mandatory)
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return the deleted revisions of that page subject to the optional
     *  constraints in helper
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws SecurityException if we cannot obtain deleted revisions
     *  @throws UnsupportedOperationException if <var>title</var> is a Special
     *  or Media page
     *  @since 0.30
     *  @see <a href="https://mediawiki.org/wiki/API:Deletedrevisions">MediaWiki
     *  documentation</a>
     */
    public List<Revision> getDeletedHistory(String title, Wiki.RequestHelper helper) throws IOException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Special and Media pages do not have histories!");
        checkPermissions("fetch deleted history", "deletedhistory");

        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "deletedrevisions");
        getparams.put("drvprop", "timestamp|ids|user|flags|size|comment|parsedcomment|sha1|tags");
        if (helper != null)
        {
            helper.setRequestType("drv");
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addUserParameter());
            getparams.putAll(helper.addExcludeUserParameter());
            getparams.putAll(helper.addTagParameter());
            limit = helper.limit();
        }
        getparams.put("titles", normalize(title));

        List<Revision> delrevs = makeListQuery("drv", getparams, null, "getDeletedHistory", limit, (response, results) ->
        {
            int x = response.indexOf("<deletedrevisions>");
            if (x < 0) // no deleted history
                return;
            for (x = response.indexOf("<page "); x > 0; x = response.indexOf("<page ", ++x))
            {
                String deltitle = parseAttribute(response, "title", x);
                int y = response.indexOf("</page>", x);
                for (int z = response.indexOf("<rev ", x); z < y && z >= 0; z = response.indexOf("<rev ", ++z))
                {
                    int aa = response.indexOf("</rev>", z);
                    Revision temp = parseRevision(response.substring(z, aa), deltitle);
                    temp.pageDeleted = true;
                    results.add(temp);
                }
            }
        });

        log(Level.INFO, "Successfully fetched " + delrevs.size() + " deleted revisions.", "deletedRevs");
        return delrevs;
    }

    /**
     *  Lists deleted files. <a href="https://phabricator.wikimedia.org/T60993">This 
     *  is deliberately NOT a privileged action</a>, although if you do not have
     *  {@code deletedhistory} rights then you will not get an edit summary. 
     *  Accepted parameters from <var>helper</var> are:
     *
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime, OffsetDateTime) date range}
     *      (upload date)
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param sha1 the SHA-1 of the deleted file (optional)
     *  @param prefix a filename prefix (excludes File: prefix, optional)
     *  @param helper a vehicle for optional parameters detailed above
     *  @return a list of deleted files that match the criteria
     *  @throws IOException if a network error occurs
     *  @since 0.37
     */
    public List<LogEntry> listDeletedFiles(String sha1, String prefix, Wiki.RequestHelper helper) throws IOException
    {
        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "filearchive");
        if (user != null && user.isAllowedTo("deletedhistory", "deletedtext"))
            getparams.put("faprop", "timestamp|ids|user|description|parseddescription");
        else
            getparams.put("faprop", "timestamp|ids|user");
        if (prefix != null)
            getparams.put("faprefix", normalize(prefix));
        if (sha1 != null)
            getparams.put("fasha1", sha1);
        if (helper != null)
        {
            helper.setRequestType("fa");
            getparams.putAll(helper.addDateRangeParameters());
            limit = helper.limit();
        }

        List<LogEntry> archive = makeListQuery("fa", getparams, null, "listDeletedFiles", limit, (response, results) ->
        {
            for (int x = response.indexOf("<fa "); x > 0; x = response.indexOf("<fa ", ++x))
            {
                int y = response.indexOf(" />", x + 1);
                String entry = response.substring(x, y);
                results.add(parseLogEntry(entry, null, Wiki.UPLOAD_LOG, "upload", null));
            }
        });

        log(Level.INFO, "Successfully fetched " + archive.size() + " deleted file metadata.", "deletedRevs");
        return archive;
    }

    /**
     *  Gets the deleted contributions of a user in the given namespace.
     *  Equivalent to [[Special:Deletedcontributions]]. Accepted parameters from
     *  <var>helper</var> are:
     *
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime, OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#inNamespaces(int...) namespaces}
     *  <li>{@link Wiki.RequestHelper#taggedWith(String) tag}
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param username a user (mandatory)
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return the deleted contributions of that user
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we cannot obtain deleted revisions
     *  @since 0.30
     *  @see <a href="https://mediawiki.org/wiki/API:Alldeletedrevisions">MediaWiki
     *  documentation</a>
     */
    public List<Revision> deletedContribs(String username, Wiki.RequestHelper helper) throws IOException
    {
        checkPermissions("fetch deleted history", "deletedhistory");

        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "alldeletedrevisions");
        getparams.put("adrprop", "ids|user|flags|size|comment|parsedcomment|timestamp|sha1|tags");
        getparams.put("adruser", username);
        if (helper != null)
        {
            helper.setRequestType("adr");
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addNamespaceParameter());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addTagParameter());
            limit = helper.limit();
        }

        List<Revision> delrevs = makeListQuery("adr", getparams, null, "deletedContribs", limit, (response, results) ->
        {
            int x = response.indexOf("<alldeletedrevisions>");
            if (x < 0) // no deleted history
                return;
            for (x = response.indexOf("<page ", x); x > 0; x = response.indexOf("<page ", ++x))
            {
                String deltitle = parseAttribute(response, "title", x);
                int y = response.indexOf("</page>", x);
                for (int z = response.indexOf("<rev ", x); z < y && z >= 0; z = response.indexOf("<rev ", ++z))
                {
                    int aa = response.indexOf("</rev>", z);
                    Revision temp = parseRevision(response.substring(z, aa), deltitle);
                    temp.pageDeleted = true;
                    results.add(temp);
                }
            }
        });

        log(Level.INFO, "Successfully fetched " + delrevs.size() + " deleted revisions.", "deletedRevs");
        return delrevs;
    }

    /**
     *  Returns all deleted pages that begin with the given prefix. WARNING:
     *  this does not behave like [[Special:Prefixindex]]. See [[Special:Undelete]]
     *  with no arguments.
     *
     *  @param prefix a prefix without a namespace specifier, empty string
     *  lists all deleted pages in the namespace.
     *  @param namespace one (and only one) namespace -- not ALL_NAMESPACES
     *  @return (see above)
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we cannot view deleted pages
     *  @throws IllegalArgumentException if namespace == ALL_NAMESPACES
     *  @since 0.31
     */
    public List<String> deletedPrefixIndex(String prefix, int namespace) throws IOException
    {
        checkPermissions("fetch deleted text", "deletedhistory", "deletedtext");
        // disallow ALL_NAMESPACES, this query is extremely slow and likely to error out.
        if (namespace == ALL_NAMESPACES)
            throw new IllegalArgumentException("deletedPrefixIndex: you must choose a namespace.");

        // use the generator here to get a list of pages, not revisions
        Map<String, String> getparams = new HashMap<>();
        getparams.put("generator", "alldeletedrevisions");
        getparams.put("gadrdir", "newer");
        getparams.put("gadrgeneratetitles", "1");
        getparams.put("gadrprefix", prefix);
        getparams.put("gadrnamespace", String.valueOf(namespace));

        List<String> pages = makeListQuery("gadr", getparams, null, "deletedPrefixIndex", -1, (text, results) ->
        {
            for (int x = text.indexOf("<page ", 0); x > 0; x = text.indexOf("<page ", ++x))
                results.add(parseAttribute(text, "title", x));
        });

        int size = pages.size();
        log(Level.INFO, "deletedPrefixIndex", "Successfully retrieved deleted page list (" + size + " items).");
        return pages;
    }

    /**
     *  Gets the text of a deleted page (it's like getPageText, but for deleted
     *  pages).
     *  @param page a page
     *  @return the deleted text, or null if there is no deleted text to retrieve
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we cannot obtain deleted revisions
     *  @since 0.30
     */
    public String getDeletedText(String page) throws IOException
    {
        checkPermissions("fetch deleted text", "deletedhistory", "deletedtext");

        // TODO: this can be multiquery(?)
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "deletedrevisions");
        getparams.put("drvlimit", "1");
        getparams.put("drvprop", "content");
        getparams.put("titles", normalize(page));

        // expected form: <rev timestamp="2009-04-05T22:40:35Z" xml:space="preserve">TEXT OF PAGE</rev>
        String line = makeApiCall(getparams, null, "getDeletedText");
        detectUncheckedErrors(line, null, null);
        int a = line.indexOf("<rev ");
        if (a < 0)
            return null;
        a = line.indexOf('>', a) + 1;
        int b = line.indexOf("</rev>", a); // tag not in empty pages
        log(Level.INFO, "getDeletedText", "Successfully retrieved deleted text of page " + page);
        return (b < 0) ? "" : line.substring(a, b);
    }

    /**
     *  Moves a page. Moves the associated talk page and leaves redirects, if
     *  applicable. Equivalent to [[Special:MovePage]]. This method is
     *  {@linkplain #setThrottle(int) throttled}. Does not recategorize pages
     *  in moved categories.
     *
     *  @param title the title of the page to move
     *  @param newTitle the new title of the page
     *  @param reason a reason for the move
     *  @throws UnsupportedOperationException if the original page is in the
     *  Special or Media namespaces. MediaWiki does not support moving of
     *  these pages.
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws CredentialException if page is protected and we can't move it
     *  @since 0.16
     */
    public void move(String title, String newTitle, String reason) throws IOException, LoginException
    {
        move(title, newTitle, reason, false, true, false);
    }

    /**
     *  Moves a page. Equivalent to [[Special:MovePage]]. This method is
     *  {@linkplain #setThrottle(int) throttled}. Does not recategorize pages
     *  in moved categories.
     *
     *  @param title the title of the page to move
     *  @param newTitle the new title of the page
     *  @param reason a reason for the move
     *  @param noredirect don't leave a redirect behind. You need to be a
     *  admin to do this, otherwise this option is ignored.
     *  @param movesubpages move the subpages of this page as well. You need to
     *  be an admin to do this, otherwise this will be ignored.
     *  @param movetalk move the talk page as well (if applicable)
     *  @throws UnsupportedOperationException if the original page is in the
     *  Special or Media namespaces. MediaWiki does not support moving of these
     *  pages.
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws CredentialException if page is protected and we can't move it
     *  @since 0.16
     */
    public synchronized void move(String title, String newTitle, String reason, boolean noredirect, boolean movetalk,
        boolean movesubpages) throws IOException, LoginException
    {
        if (namespace(title) < 0)
            throw new UnsupportedOperationException("Tried to move a Special or Media page.");
        checkPermissions("move", "move");
        throttle();

        // protection and token
        Map<String, Object> info = getPageInfo(List.of(title)).get(0);
        // determine whether the page exists
        if (Boolean.FALSE.equals(info.get("exists")))
            throw new IllegalArgumentException("Tried to move a non-existant page!");
        if (!checkRights(info, "move"))
        {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            log(Level.WARNING, "move", "Cannot move - permission denied. " + ex);
            throw ex;
        }

        // post data
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "move");
        getparams.put("from", normalize(title));
        getparams.put("to", normalize(newTitle));
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        postparams.put("token", getToken("csrf"));
        if (movetalk)
            postparams.put("movetalk", "1");
        if (noredirect && user.isAllowedTo("suppressredirect"))
            postparams.put("noredirect", "1");
        if (movesubpages && user.isAllowedTo("move-subpages"))
            postparams.put("movesubpages", "1");
        
        String response = makeApiCall(getparams, postparams, "move");
        checkErrorsAndUpdateStatus(response, "move", null, null);
        log(Level.INFO, "move", "Successfully moved " + title + " to " + newTitle);
    }

    /**
     *  Protects a page. This method is {@linkplain #setThrottle(int) throttled}.
     *  Structure of <var>protectionstate</var> (everything is optional, if a
     *  value is not present, then the corresponding values will be left
     *  untouched):
     *
     *  <pre><samp>
     *  {
     *     edit: one of { NO_PROTECTION, SEMI_PROTECTION, FULL_PROTECTION }, // restricts editing
     *     editexpiry: OffsetDateTime, // expiry time for edit protection, null = indefinite
     *     move, moveexpiry, // as above, prevents page moving
     *     create, createexpiry, // as above, prevents page creation (no effect on existing pages)
     *     upload, uploadexpiry, // as above, prevents uploading of files (FILE_NAMESPACE only)
     *     cascade: Boolean, // Enables cascading protection (requires edit=FULL_PROTECTION). Default: false.
     *     cascadesource: String // souce of cascading protection (here ignored)
     *  };
     *  </samp></pre>
     *
     *  @param page the page
     *  @param protectionstate (see above)
     *  @param reason the reason for (un)protection
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws SecurityException if we cannot protect
     *  @since 0.30
     */
    public synchronized void protect(String page, Map<String, Object> protectionstate, String reason) throws IOException, LoginException
    {
        checkPermissions("protect", "protect");
        throttle();

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "protect");
        getparams.put("title", normalize(page));
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        postparams.put("token", getToken("csrf"));
        // cascade protection
        if (protectionstate.containsKey("cascade"))
            postparams.put("cascade", "1");
        // protection levels
        StringBuilder pro = new StringBuilder();
        StringBuilder exp = new StringBuilder();
        protectionstate.forEach((key, value) ->
        {
            if (!key.contains("expiry") && !key.equals("cascade"))
            {
                pro.append(key);
                pro.append('=');
                pro.append(value);
                pro.append('|');

                // https://www.mediawiki.org/wiki/Timestamp
                // https://github.com/MER-C/wiki-java/issues/170
                OffsetDateTime expiry = (OffsetDateTime)protectionstate.get(key + "expiry");
                exp.append(expiry == null ? "never" : expiry.withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                exp.append('|');
            }
        });
        pro.delete(pro.length() - 1, pro.length());
        exp.delete(exp.length() - 1, exp.length());
        postparams.put("protections", pro);
        postparams.put("expiry", exp);
        
        String response = makeApiCall(getparams, postparams, "protect");
        checkErrorsAndUpdateStatus(response, "protect", null, null);
        log(Level.INFO, "edit", "Successfully protected " + page);
    }

    /**
     *  Completely unprotects a page. This method is {@linkplain #setThrottle(int)
     *  throttled}.
     *  @param page the page to unprotect
     *  @param reason the reason for unprotection
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws SecurityException if we cannot protect
     *  @since 0.30
     */
    public void unprotect(String page, String reason) throws IOException, LoginException
    {
        Map<String, Object> state = new HashMap<>();
        state.put("edit", NO_PROTECTION);
        state.put("move", NO_PROTECTION);
        if (namespace(page) == FILE_NAMESPACE)
            state.put("upload", NO_PROTECTION);
        state.put("create", NO_PROTECTION);
        protect(page, state, reason);
    }

    /**
     *  Exports the current revision of this page. Equivalent to
     *  [[Special:Export]].
     *  @param title the title of the page to export
     *  @return the exported text
     *  @throws IOException if a network error occurs
     *  @since 0.20
     */
    public String export(String title) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("export", "");
        getparams.put("exportnowrap", "");
        getparams.put("titles", normalize(title));
        String response = makeApiCall(getparams, null, "export");
        detectUncheckedErrors(response, null, null);
        return response;
    }

    // REVISION METHODS

    /**
     *  Gets a revision based on a given oldid. Automatically fills out all
     *  attributes of that revision except <var>rcid</var>.
     *
     *  @param oldid an oldid
     *  @return the revision corresponding to <var>oldid</var>, or {@code null}
     *  if it has been deleted or the ID is bad.
     *  @throws IOException if a network error occurs
     *  @since 0.17
     */
    public Revision getRevision(long oldid) throws IOException
    {
        return getRevisions(new long[] { oldid }).get(0);
    }

    /**
     *  Gets revisions based on given oldids. Automatically fills out all
     *  attributes of those revisions except <var>rcid</var>.
     *
     *  @param oldids a list of oldids
     *  @return the revisions corresponding to <var>oldids</var>, in the order
     *  of the input array. If a particular revision has been deleted or the ID
     *  is bad, the corresponding index is {@code null}.
     *  @throws IOException if a network error occurs
     *  @since 0.29
     */
    public List<Revision> getRevisions(long[] oldids) throws IOException
    {
        // build url and connect
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "revisions");
        getparams.put("rvprop", "ids|timestamp|user|comment|parsedcomment|flags|size|sha1|tags");
        Map<String, Object> postparams = new HashMap<>();
        HashMap<Long, Revision> revs = new HashMap<>(2 * oldids.length);

        // fetch and parse
        for (String chunk : constructRevisionString(oldids))
        {
            postparams.put("revids", chunk);
            String line = makeApiCall(getparams, postparams, "getRevision");
            detectUncheckedErrors(line, null, null);

            for (int i = line.indexOf("<page "); i > 0; i = line.indexOf("<page ", ++i))
            {
                int z = line.indexOf("</page>", i);
                String title = parseAttribute(line, "title", i);
                for (int j = line.indexOf("<rev ", i); j > 0 && j < z; j = line.indexOf("<rev ", ++j))
                {
                    int y = line.indexOf("</rev>", j);
                    String blah = line.substring(j, y);
                    Revision rev = parseRevision(blah, title);
                    revs.put(rev.getID(), rev);
                }
            }
        }

        // reorder
        Revision[] revisions = new Revision[oldids.length];
        for (int i = 0; i < oldids.length; i++)
            revisions[i] = revs.get(oldids[i]);
        log(Level.INFO, "getRevisions", "Successfully retrieved " + oldids.length + " revisions.");
        return Arrays.asList(revisions);
    }

    /**
     *  Reverts a series of edits on the same page by the same user quickly
     *  provided that they are the most recent revisions on that page. If this
     *  is not the case, then this method does nothing. The edit and reverted
     *  edits will be marked as bot if {@link #isMarkBot()} is {@code true}.
     *
     *
     *  @param revision the revision to revert. All subsequent revisions to
     *  the corresponding page must be made by this revision's user in order for
     *  the rollback to succeed.
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we do not have the privileges to rollback
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if the user is blocked
     *  @since 0.19
     *  @see <a href="https://mediawiki.org/wiki/Manual:Parameters_to_index.php#rollback">
     *  MediaWiki documentation</a>
     */
    public void rollback(Revision revision) throws IOException, LoginException
    {
        rollback(revision, markbot, "");
    }

    /**
     *  Reverts a series of edits on the same page by the same user quickly
     *  provided that they are the most recent revisions on that page. If this
     *  is not the case, then this method does nothing.
     *
     *  @param revision the revision to revert. All subsequent revisions to
     *  the corresponding page must be made by this revision's user in order for
     *  the rollback to succeed.
     *  @param bot whether to mark this edit and the reverted revisions as
     *  bot edits (ignored if we cannot do this, overrides {@link #isMarkBot()}).
     *  @param reason (optional) a reason for the rollback. Use "" for the
     *  default ([[MediaWiki:Revertpage]]).
     *  @throws IOException if a network error occurs
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws SecurityException if we do not have the privileges to rollback
     *  @throws AccountLockedException if the user is blocked
     *  @see <a href="https://mediawiki.org/wiki/Manual:Parameters_to_index.php#rollback">
     *  MediaWiki documentation</a>
     *  @since 0.19
     */
    public synchronized void rollback(Revision revision, boolean bot, String reason) throws IOException, LoginException
    {
        checkPermissions("rollback", "rollback");
        // This method is intentionally NOT throttled.

        // Perform the rollback.
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "rollback");
        getparams.put("title", normalize(revision.getTitle()));
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("title", normalize(revision.getTitle()));
        postparams.put("user", revision.getUser());
        postparams.put("token", getToken("rollback"));
        if (bot && user.isAllowedTo("markbotedits"))
            postparams.put("markbot", "1");
        if (!reason.isEmpty())
            postparams.put("summary", reason);

        String response = makeApiCall(getparams, postparams, "rollback");
        Map<String, Consumer<String>> info = Map.of(
            "alreadyrolled", desc -> log(Level.INFO, "rollback", 
                "Edit has already been rolled back or cannot be rolled back due to intervening edits."),
            "onlyauthor", desc -> log(Level.INFO, "rollback", desc));
        if (checkErrorsAndUpdateStatus(response, "rollback", null, info))
            log(Level.INFO, "rollback", "Successfully reverted edits by " + user + " on " + revision.getTitle());
    }

    /**
     *  Deletes and undeletes revisions or log entries. This method is
     *  {@linkplain #setThrottle(int) throttled}.
     *
     *  @param hidecontent hide the content of the revision (true/false
     *  = hide/unhide, {@code null} = status quo)
     *  @param hidereason hide the edit summary or the reason for an action
     *  @param hideuser hide who made the revision/action
     *  @param reason the reason why the (un)deletion was performed
     *  @param suppress [[Wikipedia:Oversight]] the information in question
     *  (ignored if we cannot {@code suppressrevision}, {@code null} = status quo).
     *  @param events the list of Events to (un)delete. All Events must be of
     *  the same type (no mixing Revisions or LogEntries). Pseudo-LogEntries and
     *  revisions to deleted pages are currently not allowed.
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we do not have the privileges to delete
     *  Revisions or LogEntries
     *  @throws AccountLockedException if the user is blocked
     *  @see <a href="https://mediawiki.org/wiki/Help:RevisionDelete">MediaWiki
     *  help page</a>
     *  @see <a href="https://mediawiki.org/wiki/API:Revisiondelete">MediaWiki
     *  documentation</a>
     *  @since 0.30
     */
    public synchronized void revisionDelete(Boolean hidecontent, Boolean hideuser, Boolean hidereason, String reason, Boolean suppress,
        List<? extends Event> events) throws IOException, LoginException
    {
        long[] ids = new long[events.size()];
        Event first = events.get(0);
        Class<? extends Event> clazz = first.getClass();
        for (int i = 0; i < events.size(); i++)
        {
            // all events submitted must be of the same type
            Event temp = events.get(i);
            if (!temp.getClass().equals(clazz))
                throw new IllegalArgumentException("All Events to be RevisionDeleted must be of the same type.");
            // TODO: Apparently you can RevisionDelete old files (i.e.
            // pseudo-LogEntries from getImageHistory and the file archive, but
            // I have no idea how to get the necessary ID parameter.
            // You can also RevisionDelete deleted revisions, but I need to
            // test this first.
            if (temp.getID() < 0)
                throw new UnsupportedOperationException("RevisionDeletion of pseudo-LogEntries is not supported.");
            ids[i] = temp.getID();
        }
        checkPermissions("revisionDelete", "deleterevision", "deletelogentry");
        throttle();

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "revisiondelete");
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        if (user.isAllowedTo("suppressrevision") && suppress != null)
            postparams.put("suppress", suppress ? "yes" : "no");
        // So, what do we have here?
        if (first instanceof Revision)
            postparams.put("type", "revision");
        else if (first instanceof LogEntry)
            postparams.put("type", "logging");
        else // unreachable... for now.
            throw new UnsupportedOperationException("Unsupported Event type in RevisionDelete.");
        // this is really stupid... I'm open to suggestions
        StringBuilder hide = new StringBuilder();
        StringBuilder show = new StringBuilder();
        if (Boolean.TRUE.equals(hidecontent))
            hide.append("content|");
        else if (Boolean.FALSE.equals(hidecontent))
            show.append("content|");
        if (Boolean.TRUE.equals(hideuser))
            hide.append("user|");
        else if (Boolean.FALSE.equals(hideuser))
            show.append("user|");
        if (Boolean.TRUE.equals(hidereason))
            hide.append("comment");
        else if (Boolean.FALSE.equals(hidereason))
            show.append("comment");
        if (hide.lastIndexOf("|") == hide.length() - 2)
            hide.delete(hide.length() - 2, hide.length());
        if (show.lastIndexOf("|") == show.length() - 2)
            show.delete(show.length() - 2, show.length());
        postparams.put("hide", hide);
        postparams.put("show", show);

        // send/read response
        for (String revstring : constructRevisionString(ids))
        {
            postparams.put("token", getToken("csrf"));
            postparams.put("ids", revstring);
            String response = makeApiCall(getparams, postparams, "revisionDelete");
            checkErrorsAndUpdateStatus(response, "revisionDelete", null, null);
            for (Event event : events)
            {
                if (hideuser != null)
                    event.setUserDeleted(hideuser);
                if (hidereason != null)
                    event.setCommentDeleted(hidereason);
                if (hidecontent != null)
                    event.setContentDeleted(hidecontent);
            }
        }
        log(Level.INFO, "revisionDelete", "Successfully (un)deleted " + events.size() + " events.");
    }

    /**
     *  Undoes revisions, equivalent to the "undo" button in the GUI page
     *  history. A quick explanation on how this might work - suppose the edit
     *  history was as follows:
     *
     *  <ul>
     *  <li> (revid=541) 2009-01-13 00:01 92.45.43.227
     *  <li> (revid=325) 2008-12-10 11:34 Example user
     *  <li> (revid=314) 2008-12-10 10:15 127.0.0.1
     *  <li> (revid=236) 2008-08-08 08:00 Anonymous
     *  <li> (revid=200) 2008-07-31 16:46 EvilCabalMember
     *  </ul>
     *  Then:
     *  <code>
     *  wiki.undo(wiki.getRevision(314L), null, reason, false); // undo revision 314 only
     *  wiki.undo(wiki.getRevision(236L), wiki.getRevision(325L), reason, false); // undo revisions 236-325
     *  </code>
     *
     *  This will only work if revision 541 or any subsequent edits do not
     *  clash with the change resulting from the undo. This method is
     *  {@linkplain #setThrottle(int) throttled}.
     *
     *  @param rev a revision to undo
     *  @param to the most recent in a range of revisions to undo. Set to null
     *  to undo only one revision.
     *  @param reason an edit summary (optional). Use "" to get the default
     *  [[MediaWiki:Undo-summary]].
     *  @param minor whether this is a minor edit
     *  @param bot whether this is a bot edit
     *  @throws IOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws CredentialException if page is protected and we can't edit it
     *  @throws IllegalArgumentException if the revisions are not on the same
     *  page.
     *  @throws ConcurrentModificationException if an edit conflict occurs
     *  @since 0.20
     */
    public synchronized void undo(Revision rev, Revision to, String reason, boolean minor,
        boolean bot) throws IOException, LoginException
    {
        throttle();

        // check here to see whether the titles correspond
        if (to != null && !rev.getTitle().equals(to.getTitle()))
            throw new IllegalArgumentException("Cannot undo - the revisions supplied are not on the same page!");

        // protection
        Map<String, Object> info = getPageInfo(List.of(rev.getTitle())).get(0);
        if (!checkRights(info, "edit"))
        {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            log(Level.WARNING, "undo", "Cannot edit - permission denied." + ex);
            throw ex;
        }

        // send data
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "edit");
        getparams.put("title", rev.getTitle());
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("title", rev.getTitle());
        if (!reason.isEmpty())
            postparams.put("summary", reason);
        postparams.put("undo", rev.getID());
        if (to != null)
            postparams.put("undoafter", to.getID());
        if (minor)
            postparams.put("minor", "1");
        if (bot)
            postparams.put("bot", "1");
        postparams.put("token", getToken("csrf"));
        
        String response = makeApiCall(getparams, postparams, "undo");
        checkErrorsAndUpdateStatus(response, "edit", Map.of(
            "editconflict", desc -> new ConcurrentModificationException(desc + "- [[" + rev.getTitle() + "]]")), null);
        String log = "Successfully undid revision(s) " + rev.getID();
        if (to != null)
            log += (" - " + to.getID());
        log(Level.INFO, "undo", log);
    }

    /**
     *  Fetches a HTML rendered diff table; see the table at the <a
     *  href="https://en.wikipedia.org/w/index.php?diff=343490272">example</a>.
     *  Returns the empty string for moves, protections and similar dummy edits
     *  (<a href="https://en.wikipedia.org/w/index.php?oldid=738178354&diff=prev">example</a>)
     *  and pairs of revisions where there is no difference. Returns
     *  {@code null} for bad section numbers and revision IDs. Deleted pages and
     *  revisions to deleted pages are not allowed if you don't have the
     *  necessary privileges.
     *
     *  <p>
     *  <var>from</var> refers to content on the left side of the diff table
     *  while <var>to</var> refers to content on the right side. Only one of the
     *  following keys representing content sources must be specified for each:
     *
     *  <ul>
     *  <li><b>title</b> (String) -- a page title
     *  <li><b>revid</b> (long) -- a {@linkplain Revision#getID() unique ID for
     *  a revision}. {@link Wiki#NEXT_REVISION}, {@link Wiki#PREVIOUS_REVISION}
     *  and {@link Wiki#CURRENT_REVISION} can be used in <var>to</var> for
     *  obvious effect.
     *  <li><b>revision</b> ({@link Wiki.Revision}) -- a Revision
     *  <li><b>text</b> (String) -- some wikitext
     *  </ul>
     *
     *  <p>
     *  <b>WARNING</b>: the parameters to this method will be changed when the
     *  time comes for Multi-Content revisions and JDK11 refactoring.
     *
     *  @param from the content on the left hand side of the diff
     *  @param to the content on the right hand side of the diff
     *  @return a HTML difference table between the two texts, "" for dummy
     *  edits or null as described above
     *  @throws NoSuchElementException or IllegalArgumentException if no from or
     *  to content is specified
     *  @throws SecurityException if you pass a RevisionDeleted revision and
     *  don't have the necessary privileges
     *  @throws IOException if a network error occurs
     *  @see <a href="https://mediawiki.org/wiki/API:Compare">MediaWiki documentation</a>
     *  @since 0.35
     */
    public String diff(Map<String, Object> from, Map<String, Object> to) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "compare");
        HashMap<String, Object> postparams = new HashMap<>();

        Map.Entry<String, Object> entry = from.entrySet().iterator().next();
        Object value = entry.getValue();
        switch (entry.getKey())
        {
            case "title":
                getparams.put("fromtitle", normalize((String)value));
                break;
            case "revid":
                getparams.put("fromrev", value.toString());
                break;
            case "revision":
                getparams.put("fromrev", String.valueOf(((Revision)value).getID()));
                break;
            case "text":
                getparams.put("fromslots", "main");
                getparams.put("fromcontentmodel-main", "wikitext");
                postparams.put("fromtext-main", value);
            default:
                throw new IllegalArgumentException("From content not specified!");
        }

        entry = to.entrySet().iterator().next();
        value = entry.getValue();
        switch (entry.getKey())
        {
            case "title":
                getparams.put("totitle", normalize((String)value));
                break;
            case "revid":
                if (value.equals(PREVIOUS_REVISION))
                    getparams.put("torelative", "prev");
                else if (value.equals(CURRENT_REVISION))
                    getparams.put("torelative", "cur");
                else if (value.equals(NEXT_REVISION))
                    getparams.put("torelative", "next");
                else
                    getparams.put("torev", value.toString());
                break;
            case "revision":
                getparams.put("torev", String.valueOf(((Revision)value).getID()));
                break;
            case "text":
                getparams.put("toslots", "main");
                getparams.put("tocontentmodel-main", "wikitext");
                postparams.put("totext-main", value);
                break;
            default:
                throw new IllegalArgumentException("To content not specified!");
        }

        String line = makeApiCall(getparams, postparams, "diff");
        Consumer<String> noop = desc -> {};
        Map<String, Consumer<String>> warnings = Map.of(
            "missingtitle", noop,
            "missingcontent", noop,
            "nosuchsection", noop,
            "nosuchrevid", noop);
        if (detectUncheckedErrors(line, null, warnings))
        {
            // strip extraneous information
            if (line.contains("</compare>"))
            {
                // a warning may occur here in certain circumstances e.g.
                // https://en.wikipedia.org/w/api.php?&torelative=prev&maxlag=5&format=xml&action=compare&fromrev=255072509
                int a = line.lastIndexOf("<compare");
                a = line.indexOf('>', a) + 1;
                int b = line.indexOf("</compare>", a);
                return decode(line.substring(a, b));
            }
            else if (line.contains("<compare "))
                // <compare> tag has no content if there is no diff or the two
                // revisions are identical. In particular, the API does not
                // distinguish between:
                // https://en.wikipedia.org/w/index.php?title=Source_Filmmaker&diff=804972897&oldid=803731343 (no difference)
                // https://en.wikipedia.org/w/index.php?title=Dayo_Israel&oldid=738178354&diff=prev (dummy edit)
                return "";
            else
                throw new AssertionError("Unreachable.");
        }
        return null;
    }

    /**
     *  Parses stuff of the form <samp>title="L. Sprague de Camp"
     *  timestamp="2006-08-28T23:48:08Z" minor="" comment="robot  Modifying:
     *  [[bg:Blah]]"</samp> into {@link Wiki.Revision} objects. Used by
     *  <code>contribs()</code>, <code>watchlist()</code>, <code>getPageHistory()</code>
     *  and <code>recentChanges()</code>. NOTE: if RevisionDelete was used on a
     *  revision, the relevant values will be null.
     *
     *  @param xml the XML to parse
     *  @param title an optional title parameter if we already know what it is
     *  (use "" if we don't)
     *  @return the Revision encoded in the XML
     *  @since 0.17
     */
    protected Revision parseRevision(String xml, String title)
    {
        long oldid = Long.parseLong(parseAttribute(xml, " revid", 0));
        OffsetDateTime timestamp = OffsetDateTime.parse(parseAttribute(xml, "timestamp", 0));

        // title
        if (title.isEmpty())
            title = parseAttribute(xml, "title", 0);

        // summary
        String summary = null, parsedsummary = null;
        if (xml.contains("comment=\""))
        {
            summary = parseAttribute(xml, "comment", 0);
            parsedsummary = parseAttribute(xml, "parsedcomment", 0);
        }

        // user
        String user2 = null;
        if (xml.contains("user=\""))
            user2 = parseAttribute(xml, "user", 0);

        // flags: minor, bot, new
        boolean minor = xml.contains("minor=\"\"");
        boolean bot = xml.contains("bot=\"\"");
        boolean rvnew = xml.contains("new=\"\"");

        // size
        int size = 0;
        if (xml.contains("newlen=")) // recentchanges
            size = Integer.parseInt(parseAttribute(xml, "newlen", 0));
        else if (xml.contains("size=\""))
            size = Integer.parseInt(parseAttribute(xml, "size", 0));
        else if (xml.contains("len=\"")) // deletedrevs
            size = Integer.parseInt(parseAttribute(xml, "len", 0));

        // sha1
        String sha1 = null;
        if (xml.contains("sha1=\""))
            sha1 = parseAttribute(xml, "sha1", 0);

        Revision revision = new Revision(oldid, timestamp, user2, summary, parsedsummary, title, sha1, minor, bot, rvnew, size);
        // set rcid
        if (xml.contains("rcid=\""))
            revision.setRcid(Long.parseLong(parseAttribute(xml, "rcid", 0)));

        // previous revision
        if (xml.contains("parentid")) // page history/getRevision
            revision.previous = Long.parseLong(parseAttribute(xml, "parentid", 0));
        else if (xml.contains("old_revid")) // watchlist
            revision.previous = Long.parseLong(parseAttribute(xml, "old_revid", 0));

        // sizediff
        if (xml.contains("oldlen=\"")) // recentchanges
            revision.sizediff = revision.size - Integer.parseInt(parseAttribute(xml, "oldlen", 0));
        else if (xml.contains("sizediff=\""))
            revision.sizediff = Integer.parseInt(parseAttribute(xml, "sizediff", 0));
        
        // tags
        List<String> tags = new ArrayList<>();
        if (xml.contains("<tag>"))
        {
            for (int idx = xml.indexOf("<tag>"); idx >= 0; idx = xml.indexOf("<tag>", ++idx))
                tags.add(xml.substring(idx + 5, xml.indexOf("</tag>", idx)));
        }
        revision.setTags(tags);

        // revisiondelete
        revision.setCommentDeleted(xml.contains("commenthidden=\""));
        revision.setUserDeleted(xml.contains("userhidden=\""));
        // Silly workaround: prop=revisions, prop=deletedrevisions,
        // list=recentchanges and list=alldeletedrevisions all don't tell you
        // whether content has been revision deleted until you fetch the content.
        // Instead, fetch the SHA-1 of the content to minimize data transfer.
        revision.setContentDeleted(xml.contains("sha1hidden=\""));
        // list=usercontribs does tell you
        if (xml.contains("texthidden=\""))
            revision.setContentDeleted(true);
        return revision;
    }

    // IMAGE METHODS

    /**
     *  Fetches an image and saves it in the given file. Warning: the specified
     *  file is overwritten! Works for files originating from external
     *  repositories (e.g. Wikimedia Commons).
     *
     *  @param title the title of the image (may contain "File")
     *  @param file the file to save the image to.
     *  @return true or false if the image doesn't exist
     *  @throws FileNotFoundException if the file is a directory, cannot be
     *  created or opened
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.30
     */
    public boolean getImage(String title, File file) throws FileNotFoundException, IOException
    {
        return getImage(title, -1, -1, file);
    }

    /**
     *  Fetches a thumbnail of an image file and saves the image data into the
     *  given file. Warning: the specified file is overwritten! Works for files
     *  originating from external repositories (e.g. Wikimedia Commons).
     *
     *  @param title the title of the image (may contain "File")
     *  @param width the width of the thumbnail (use -1 for actual width)
     *  @param height the height of the thumbnail (use -1 for actual height)
     *  @param file a write-able file to save the data to.
     *  @return true or false if the image doesn't exist
     *  @throws FileNotFoundException if the file is a directory, cannot be
     *  created or opened
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.30
     */
    public boolean getImage(String title, int width, int height, File file) throws FileNotFoundException, IOException
    {
        // this is a two step process - first we fetch the image url
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "imageinfo");
        getparams.put("iiprop", "url");
        getparams.put("titles", "File:" + removeNamespace(normalize(title), FILE_NAMESPACE));
        getparams.put("iiurlwidth", String.valueOf(width));
        getparams.put("iiurlheight", String.valueOf(height));
        String line = makeApiCall(getparams, null, "getImage");
        detectUncheckedErrors(line, null, null);
        if (!line.contains("<imageinfo>"))
            return false;
        String url2 = parseAttribute(line, "url", 0);

        // then we read the image
        logurl(url2, "getImage");
        HttpRequest request = makeConnection(url2).GET().build();
        try
        {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            boolean zip = "gzip".equals(response.headers().firstValue("Content-Encoding").orElse(""));
            InputStream input = zip ? new GZIPInputStream(response.body()) : response.body();
            Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log(Level.INFO, "getImage", "Successfully retrieved image \"" + title + "\"");
            return true;
        }
        catch (InterruptedException ex)
        {
            return false; // hmmmm
        }
    }

    /**
     *  Gets the file metadata for a file. The keys are:
     *
     *  * size (file size in bytes, Long)
     *  * width (Integer)
     *  * height (Integer)
     *  * sha1 (String)
     *  * mime (MIME type, String)
     *  * plus EXIF metadata (Strings)
     *
     *  @param file the image to get metadata for (may contain "File")
     *  @return the metadata for the image or null if it doesn't exist
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.20
     */
    public Map<String, Object> getFileMetadata(String file) throws IOException
    {
        return getFileMetadata(List.of(file)).get(0);
    }
    
    /**
     *  Gets the file metadata for a list of files. Returns a result regardless
     *  of whether a file is hosted locally or a shared repository (e.g. 
     *  Wikimedia Commons). The keys are:
     *
     *  * size (file size in bytes, Long)
     *  * width (Integer)
     *  * height (Integer)
     *  * sha1 (String)
     *  * mime (MIME type, String)
     *  * plus EXIF metadata (Strings)
     *
     *  @param files the files to get metadata for (may contain "File")
     *  @return the metadata for each file in order, or null if the corresponding
     *  image doesn't exist
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.37
     */
    public List<Map<String, Object>> getFileMetadata(List<String> files) throws IOException
    {
        // Support for videos is blocked on https://phabricator.wikimedia.org/T89971
        Map<String, String> getparams = new HashMap<>();
        Map<String, Object> postparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "imageinfo");
        getparams.put("iiprop", "size|sha1|mime|metadata");
        // copy because normalization and redirect resolvers overwrite
        List<String> files2 = new ArrayList<>(files);

        Map<String, Map<String, Object>> intermediate = new HashMap<>();
        for (String chunk : constructTitleString(files))
        {
            postparams.put("titles", chunk);            
            String line = makeApiCall(getparams, postparams, "getFileMetadata");
            detectUncheckedErrors(line, null, null);
            resolveNormalizedParser(files2, line);
            if (resolveredirect)
                resolveRedirectParser(files2, line);
            String[] results = line.split("<page ");
            for (int i = 1; i < results.length; i++) // 1 = skipping front crud
            {
                String result = results[i];
                // missing image - missing attribute means no local file page.
                // Query returns results from repositories (e.g. Wikimedia Commons)
                if (!result.contains("<ii ")) 
                    continue;
                String parsedtitle = parseAttribute(result, "title", 0);
                Map<String, Object> metadata = new HashMap<>(30);

                // size, width, height, sha, mime type
                metadata.put("size", Long.valueOf(parseAttribute(result, "size", 0)));
                metadata.put("width", Integer.valueOf(parseAttribute(result, "width", 0)));
                metadata.put("height", Integer.valueOf(parseAttribute(result, "height", 0)));
                metadata.put("sha1", parseAttribute(result, "sha1", 0));
                metadata.put("mime", parseAttribute(result, "mime", 0));

                // exif
                for (int j = result.indexOf("<metadata "); j > 0; j = result.indexOf("<metadata ", ++j))
                {
                    // FIXME: discards nesting in metadata
                    String name = parseAttribute(result, "name", j);
                    // for SVGs, metadata contains "width" and "height". Ignore these.
                    if (name.equals("width") || name.equals("height"))
                        continue;
                    String value = parseAttribute(result, "value", j);
                    metadata.put(name, value);
                }
                intermediate.put(parsedtitle, metadata);
            }
        }
        
        // reorder results
        List<Map<String, Object>> ret = new ArrayList<>();
        for (String normalizedtitle : files2)
            ret.add(intermediate.get(normalizedtitle));
        return ret;
    }

    /**
     *  Gets duplicates of this file. Equivalent to [[Special:FileDuplicateSearch]].
     *  Works for, and returns files from external repositories (e.g. Wikimedia
     *  Commons).
     *
     *  @param file the file for checking duplicates (may contain "File")
     *  @return the duplicates of that file
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.18
     */
    public List<String> getDuplicates(String file) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "duplicatefiles");
        getparams.put("titles", "File:" + removeNamespace(normalize(file), FILE_NAMESPACE));

        List<String> duplicates = makeListQuery("df", getparams, null, "getDuplicates", -1, (line, results) ->
        {
            if (line.contains("missing=\"\""))
                return;

            // xml form: <df name="Star-spangled_banner_002.ogg" other stuff >
            for (int a = line.indexOf("<df "); a > 0; a = line.indexOf("<df ", ++a))
                results.add("File:" + parseAttribute(line, "name", a));
        });

        int size = duplicates.size();
        log(Level.INFO, "getDuplicates", "Successfully retrieved duplicates of " + file + " (" + size + " files)");
        return duplicates;
    }

    /**
     *  Returns the upload history of an image. This is not the same as
     *  {@linkplain #getLogEntries(String, String, Wiki.RequestHelper) fetching
     *  a page's upload log}, as the image may have been deleted. This returns
     *  only the live history of an image.
     *
     *  @param title the title of the image (may contain File)
     *  @return the image history of the image
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.20
     */
    public List<LogEntry> getImageHistory(String title) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "imageinfo");
        getparams.put("iiprop", "timestamp|user|comment|parsedcomment");
        getparams.put("titles", "File:" + removeNamespace(normalize(title), FILE_NAMESPACE));

        String prefixtitle = namespaceIdentifier(FILE_NAMESPACE) + ":" + title;
        List<LogEntry> history = makeListQuery("ii", getparams, null, "getImageHistory", -1, (line, results) ->
        {
            if (line.contains("missing=\"\""))
                return;

            // xml form: <ii timestamp="2010-05-23T05:48:43Z" user="Prodego" comment="Match to new version" />
            for (int a = line.indexOf("<ii "); a > 0; a = line.indexOf("<ii ", ++a))
            {
                int b = line.indexOf('>', a);
                String temp = line.substring(a, b);
                LogEntry le = parseLogEntry(temp, null, UPLOAD_LOG, "overwrite", prefixtitle);
                results.add(le);
            }
        });

        // crude hack: action adjusting for first image (in the history, not our list)
        int size = history.size();
        if (size == 0)
            return Collections.emptyList();
        LogEntry last = history.get(size - 1);
        last.action = "upload";
        history.set(size - 1, last);
        return history;
    }

    /**
     *  Gets an old image revision and writes the image data in a file.
     *  Warning: This does overwrite any file content!
     *  You will have to do the thumbnailing yourself.
     *  @param entry the upload log entry that corresponds to the image being
     *  uploaded
     *  @param file the file to write the image to
     *  @return true if the file exists in the local repository (i.e. not on
     *  Commons or deleted)
     *  @throws FileNotFoundException if the file is a directory, cannot be created or opened
     *  @throws IOException if a network error occurs
     *  @throws IllegalArgumentException if the entry is not in the upload log
     *  @since 0.30
     */
    public boolean getOldImage(LogEntry entry, File file) throws IOException
    {
        // check for type
        if (!entry.getType().equals(UPLOAD_LOG))
            throw new IllegalArgumentException("You must provide an upload log entry!");
        // no thumbnails for image history, sorry.
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "imageinfo");
        getparams.put("iilimit", "max");
        getparams.put("iiprop", "timestamp|url|archivename");
        getparams.put("titles", normalize(entry.getTitle()));
        String line = makeApiCall(getparams, null, "getOldImage");
        detectUncheckedErrors(line, null, null);
        
        // find the correct log entry by comparing timestamps
        // xml form: <ii timestamp="2010-05-23T05:48:43Z" user="Prodego" comment="Match to new version" />
        for (int a = line.indexOf("<ii "); a > 0; a = line.indexOf("<ii ", ++a))
        {
            OffsetDateTime timestamp = OffsetDateTime.parse(parseAttribute(line, "timestamp", a));
            if (timestamp.equals(entry.getTimestamp()))
            {
                // this is it
                String url = parseAttribute(line, "url", a);
                logurl(url, "getOldImage");
                HttpRequest request = makeConnection(url).GET().build();
                try
                {
                    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    boolean zip = "gzip".equals(response.headers().firstValue("Content-Encoding").orElse(""));
                    InputStream input = zip ? new GZIPInputStream(response.body()) : response.body();
                    Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // scrape archive name for logging purposes
                    String archive = Objects.requireNonNullElse(parseAttribute(line, "archivename", 0), 
                        entry.getTitle());
                    log(Level.INFO, "getOldImage", "Successfully retrieved old image \"" + archive + "\"");
                    return true;
                }
                catch (InterruptedException ex)
                {
                    // hmmm
                }
            }
        }
        return false;
    }

    /**
     *  Gets the (non-deleted) uploads of a user between the specified times.
     *  Results are sorted in chronological order.
     *
     *  <p>
     *  Accepted parameters from <var>helper</var> are:
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param user the user to get uploads for
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return a list of all live images the user has uploaded
     *  @throws IOException if a network error occurs
     *  @since 0.28
     */
    public List<LogEntry> getUploads(String user, Wiki.RequestHelper helper) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "allimages");
        getparams.put("aisort", "timestamp");
        getparams.put("aiprop", "timestamp|comment|parsedcomment");
        getparams.put("aiuser", user);
        int limit = -1;
        if (helper != null)
        {
            helper.setRequestType("ai");
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addReverseParameter());
            limit = helper.limit();
        }

        List<LogEntry> uploads = makeListQuery("ai", getparams, null, "getUploads", limit, (line, results) ->
        {
            for (int i = line.indexOf("<img "); i > 0; i = line.indexOf("<img ", ++i))
            {
                int b = line.indexOf("/>", i);
                String temp = line.substring(i, b);
                LogEntry le = parseLogEntry(temp, user, UPLOAD_LOG, "upload", null);
                results.add(le);
            }
        });

        log(Level.INFO, "getUploads", "Successfully retrieved uploads of " + user + " (" + uploads.size() + " uploads)");
        return uploads;
    }

    /**
     *  Uploads an image. Equivalent to [[Special:Upload]]. Supported
     *  extensions are (case-insensitive) "png", "jpg", "gif" and "svg". You
     *  need to be logged on to do this. Automatically breaks uploads into
     *  chunks of size 2^x, where x is the value of the <code>loguploadsize</code>
     *  property. This method is {@linkplain #setThrottle(int) throttled}.
     *
     *  @param file the image file
     *  @param filename the target file name (may contain File)
     *  @param contents the contents of the image description page, set to ""
     *  if overwriting an existing file
     *  @param reason an upload summary (defaults to <var>contents</var>, use ""
     *  to not specify one)
     *  @throws SecurityException if not logged in
     *  @throws CredentialException if (page is protected OR file is on a central
     *  repository) and we can't upload
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws IOException or UncheckedIOException if a network or local
     *  filesystem error occurs
     *  @throws AccountLockedException if user is blocked
     *  @since 0.21
     */
    public synchronized void upload(File file, String filename, String contents, String reason) throws IOException, LoginException
    {
        filename = removeNamespace(filename, FILE_NAMESPACE);
        checkPermissions("upload", "upload");
        throttle();

        // protection
        Map<String, Object> info = getPageInfo(List.of("File:" + filename)).get(0);
        if (!checkRights(info, "upload"))
        {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            log(Level.WARNING, "upload", "Cannot upload - permission denied." + ex);
            throw ex;
        }

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "upload");
        // chunked upload setup
        long filesize = file.length();
        long chunks = (filesize >> log2_upload_size) + 1;
        String filekey = "";
        try (FileInputStream fi = new FileInputStream(file))
        {
            // upload the image
            for (int i = 0; i < chunks; i++)
            {
                Map<String, Object> postparams = new HashMap<>(50);
                postparams.put("filename", normalize(filename));
                postparams.put("token", getToken("csrf"));
                postparams.put("ignorewarnings", "true");
                if (chunks == 1)
                {
                    // Chunks disabled due to a small filesize.
                    // This is just a normal upload.
                    postparams.put("text", contents);
                    if (!reason.isEmpty())
                        postparams.put("comment", reason);
                    byte[] by = Files.readAllBytes(file.toPath());
                    // Why this is necessary?
                    postparams.put("file\"; filename=\"" + file.getName(), by);
                }
                else
                {
                    long offset = i << log2_upload_size;
                    postparams.put("stash", "1");
                    postparams.put("offset", offset);
                    postparams.put("filesize", filesize);
                    if (i != 0)
                        postparams.put("filekey", filekey);

                    // write the actual file
                    long buffersize = Math.min(1 << log2_upload_size, filesize - offset);
                    byte[] by = new byte[(int)buffersize]; // 32 bit problem. Why must array indices be ints?
                    fi.read(by);
                    postparams.put("chunk\"; filename=\"" + file.getName(), by);
                }

                String response = makeApiCall(getparams, postparams, "upload");
                checkErrorsAndUpdateStatus(response, "upload", null, null);
                // look for filekey
                if (chunks > 1)
                {
                    if (response.contains("filekey=\""))
                        filekey = parseAttribute(response, "filekey", 0);
                    else
                        throw new IOException("No filekey present! Server response was " + response);
                }
            }
        }

        // unstash upload if chunked
        if (chunks > 1)
        {
            Map<String, Object> postparams = new HashMap<>(50);
            postparams.put("filename", normalize(filename));
            postparams.put("token", getToken("csrf"));
            postparams.put("text", contents);
            if (!reason.isEmpty())
                postparams.put("comment", reason);
            postparams.put("ignorewarnings", "true");
            postparams.put("filekey", filekey);
            String response = makeApiCall(getparams, postparams, "upload");
            checkErrorsAndUpdateStatus(response, "upload", null, null);
        }
        log(Level.INFO, "upload", "Successfully uploaded to File:" + filename + ".");
    }

    /**
     *  Uploads an image by copying it from the given URL. Equivalent to
     *  [[Special:Upload]]. Supported extensions are (case-insensitive) "png",
     *  "jpg", "gif" and "svg". You need to be logged on to do this. This method
     *  is {@linkplain #setThrottle(int) throttled}.
     *
     *  @param url the URL of the image to fetch
     *  @param filename the target file name (may contain File)
     *  @param contents the contents of the image description page, set to ""
     *  if overwriting an existing file
     *  @param reason an upload summary (defaults to <var>contents</var>, use ""
     *  to not specify one)
     *  @throws SecurityException if not logged in
     *  @throws CredentialException if (page is protected OR file is on a central
     *  repository) and we can't upload
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws AccountLockedException if user is blocked
     *  @throws AccessDeniedException if the wiki does not permit us to upload
     *  from this URL
     *  @since 0.32
     */
    public synchronized void upload(URL url, String filename, String contents, String reason) throws IOException, LoginException
    {
        filename = removeNamespace(filename, FILE_NAMESPACE);
        checkPermissions("upload", "upload_by_url");
        throttle();

        // protection
        Map<String, Object> info = getPageInfo(List.of("File:" + filename)).get(0);
        if (!checkRights(info, "upload"))
        {
            CredentialException ex = new CredentialException("Permission denied: page is protected.");
            log(Level.WARNING, "upload", "Cannot upload - permission denied." + ex);
            throw ex;
        }

        // send and build request
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "upload");
        getparams.put("filename", normalize(filename));
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("ignorewarnings", "1");
        postparams.put("token", getToken("csrf"));
        postparams.put("text", contents);
        postparams.put("comment", reason);
        postparams.put("url", url.toExternalForm());
        
        String response = makeApiCall(getparams, postparams, "upload");
        checkErrorsAndUpdateStatus(response, "upload", null, null);
        log(Level.INFO, "upload", "Successfully uploaded to File:" + filename + ".");
    }

    // USER METHODS

    /**
     *  Determines whether a specific user exists. Should evaluate to false
     *  for anons.
     *
     *  @param username a username
     *  @return whether the user exists
     *  @throws IOException if a network error occurs
     *  @since 0.05
     */
    public boolean userExists(String username) throws IOException
    {
        return getUsers(List.of(username)).get(0) != null;
    }

    /**
     *  Determines whether the specified users exist. Should evaluate to false
     *  for anons. Output array is in the same order as the input usernames.
     *
     *  @param usernames an array of usernames
     *  @return whether these users exist
     *  @throws IOException if a network error occurs
     *  @since 0.33
     */
    public boolean[] userExists(List<String> usernames) throws IOException
    {
        boolean[] ret = new boolean[usernames.size()];
        List<User> info = getUsers(usernames);
        for (int i = 0; i < usernames.size(); i++)
            ret[i] = (info.get(i) != null);
        return ret;
    }

    /**
     *  Gets the specified number of users (as a String) starting at the
     *  given string, in alphabetical order. Equivalent to [[Special:Listusers]].
     *
     *  @param start the string to start enumeration
     *  @param number the number of users to return
     *  @return a String[] containing the usernames
     *  @throws IOException if a network error occurs
     *  @since 0.05
     */
    public List<String> allUsers(String start, int number) throws IOException
    {
        return allUsers(start, number, "", "", "", "", false, false);
    }

    /**
     *  Returns all usernames of users in given group(s)
     *  @param group a group name. Use pipe-char "|" to separate if several
     *  @return (see above)
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    public List<String> allUsersInGroup(String group) throws IOException
    {
        return allUsers("", -1, "", group, "", "", false, false);
    }

    /**
     *  Returns all usernames of users who are not in given group(s)
     *  @param excludegroup a group name. Use pipe-char "|" to separate if several
     *  @return (see above)
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    public List<String> allUsersNotInGroup(String excludegroup) throws IOException
    {
        return allUsers("", -1, "", "", excludegroup, "", false, false);
    }

    /**
     *  Returns all usernames of users who have given right(s)
     *  @param rights a right name. Use pipe-char "|" to separate if several
     *  @return (see above)
     *  @throws IOException if a network error occurs
     *  @since 0.32
     */
    public List<String> allUsersWithRight(String rights) throws IOException
    {
        return allUsers("", -1, "", "", "", rights, false, false);
    }

    /**
     *  Returns all usernames with the given prefix.
     *  @param prefix a username prefix (without User:)
     *  @return (see above)
     *  @throws IOException if a network error occurs
     *  @since 0.28
     */
    public List<String> allUsersWithPrefix(String prefix) throws IOException
    {
        return allUsers("", -1, prefix, "", "", "", false, false);
    }

    /**
     *  Gets the specified number of users (as a String) starting at the
     *  given string, in alphabetical order. Equivalent to [[Special:Listusers]].
     *
     *  @param start the string to start enumeration
     *  @param number the number of users to return
     *  @param prefix list all users with this prefix (overrides start and
     *  amount), use "" to not not specify one
     *  @param group list all users in this group(s). Use pipe-char "|" to
     *  separate group names.
     *  @param excludegroup list all users who are not in this group(s). Use
     *  pipe-char "|" to separate group names.
     *  @param rights list all users with this right(s). Use pipe-char "|" to
     *  separate right names.
     *  @param activeonly return only users who have edited in the last 30 days
     *  @param skipzero return only users with edits
     *  @return a String[] containing the usernames
     *  @throws IOException if a network error occurs
     *  @since 0.28
     */
    public List<String> allUsers(String start, int number, String prefix, String group,
        String excludegroup, String rights, boolean activeonly, boolean skipzero) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("list", "allusers");
        String next = "";
        if (prefix.isEmpty())
        {
            getparams.put("aulimit", String.valueOf(( number > slowmax || number == -1 ) ? slowmax : number));
            next = start;
        }
        else
        {
            getparams.put("aulimit", String.valueOf(slowmax));
            getparams.put("auprefix", normalize(prefix));
        }
        if (!group.isEmpty())
            getparams.put("augroup", group);
        if (!excludegroup.isEmpty())
            getparams.put("auexcludegroup", excludegroup);
        if (!rights.isEmpty())
            getparams.put("aurights", rights);
        if (activeonly)
            getparams.put("auactiveusers", "1");
        if (skipzero)
            getparams.put("auwitheditsonly", "1");
        List<String> members = new ArrayList<>(6667); // enough for most requests
        do
        {
            if (!next.isEmpty())
                getparams.put("aufrom", normalize(next));
            String line = makeApiCall(getparams, null, "allUsers");
            detectUncheckedErrors(line, null, null);
            
            // bail if nonsense groups/rights
            if (line.contains("Unrecognized values for parameter"))
                return Collections.emptyList();

            // parse
            next = parseAttribute(line, "aufrom", 0);
            for (int w = line.indexOf("<u "); w > 0; w = line.indexOf("<u ", ++w))
            {
                members.add(parseAttribute(line, "name", w));
                if (members.size() == number)
                {
                    next = null;
                    break;
                }
            }
        }
        while (next != null);
        int size = members.size();
        log(Level.INFO, "allUsers", "Successfully retrieved user list (" + size + " users)");
        return members;
    }

    /**
     *  Gets the users with the given usernames and fills all {@linkplain Wiki.User 
     *  available metadata and properties}. If a user doesn't exist, the result
     *  is {@code null}. Output array is in the same order as the input array.
     *  @param usernames a list of usernames
     *  @return the users with those usernames
     *  @since 0.33
     *  @throws IOException if a network error occurs
     */
    public List<User> getUsers(List<String> usernames) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("list", "users");
        getparams.put("usprop", "editcount|groups|rights|emailable|blockinfo|gender|registration");
        Map<String, Object> postparams = new HashMap<>();
        Map<String, User> metamap = new HashMap<>();
        for (String fragment : constructTitleString(usernames))
        {
            postparams.put("ususers", fragment);
            String line = makeApiCall(getparams, postparams, "getUserInfo");
            detectUncheckedErrors(line, null, null);
            String[] results = line.split("<user ");
            for (int i = 1; i < results.length; i++)
            {
                // skip non-existent and IP addresses
                String result = results[i];
                if (result.contains("missing=\"\"") || result.contains("invalid=\"\""))
                    continue;

                String parsedname = parseAttribute(result, "name", 0);

                String registrationdate = parseAttribute(result, "registration", 0);
                OffsetDateTime registration = null;
                // TODO remove check when https://phabricator.wikimedia.org/T24097 is resolved
                if (registrationdate != null && !registrationdate.isEmpty())
                    registration = OffsetDateTime.parse(registrationdate);

                List<String> rights = new ArrayList<>();
                for (int x = result.indexOf("<r>"); x > 0; x = result.indexOf("<r>", ++x))
                {
                    int y = result.indexOf("</r>", x);
                    rights.add(result.substring(x + 3, y));
                }

                List<String> groups = new ArrayList<>();
                for (int x = result.indexOf("<g>"); x > 0; x = result.indexOf("<g>", ++x))
                {
                    int y = result.indexOf("</g>", x);
                    groups.add(result.substring(x + 3, y));
                }

                int editcount = Integer.parseInt(parseAttribute(result, "editcount", 0));
                boolean emailable = result.contains("emailable=\"");
                Gender gender = Gender.valueOf(parseAttribute(result, "gender", 0));
                boolean blocked = result.contains("blockedby=\"");

                User user = new User(parsedname, registration, rights, groups, gender, emailable, blocked, editcount);
                metamap.put(parsedname, user);
            }
        }

        // reorder
        List<User> ret = new ArrayList<>();
        for (String username : usernames)
            ret.add(metamap.get(normalize(username)));
        
        log(Level.INFO, "getUsers", "Successfully retrieved user info for " + usernames.size() + " users.");
        return ret;
    }

    /**
     *  Gets the user we are currently logged in as. If not logged in, returns
     *  null.
     *  @return the current logged in user
     *  @since 0.05
     */
    public User getCurrentUser()
    {
        return user;
    }

    /**
     *  Gets the contributions by a range of IP addresses. Supported ranges are
     *  a whole number of bytes (/8, /16, /24, /32, /40, etc.), anything not a
     *  whole number of bytes is rounded down. WARNING: calls for large IP
     *  ranges may not return for a VERY long time. This may also return edits
     *  by users pretending to be IP addresses e.g. 127.0.0.l.
     *
     *  @param range the CIDR range of IP addresses to get contributions for
     *  @return the contributions of that range
     *  @throws IOException if a network error occurs
     *  @throws UnknownHostException if the IP address is not valid
     *  @throws NumberFormatException if the subnet mask is not valid
     *  @since 0.17
     *  @deprecated As of MediaWiki 1.31, you can call {@code contribs(range)}.
     */
    @Deprecated
    public List<Revision> rangeContribs(String range) throws IOException
    {
        String[] parts = range.split("/");
        String ip = parts[0];
        InetAddress ipaddr = InetAddress.getByName(ip);
        byte[] bytes = ipaddr.getAddress();
        StringBuilder contribuser = new StringBuilder();
        if (ipaddr instanceof Inet6Address)
        {
            // MediaWiki represents IPv6 addresses as
            // 0:0:0:0:0:0:0:1 or 1234:123:4567:567:AAAA:BBBB:CCCC:DDEF
            // hex letters must be upper case
            int numbytes = (parts.length < 2) ? 16 : (Integer.parseInt(parts[1]) >> 3);
            bytes = Arrays.copyOf(Arrays.copyOf(bytes, numbytes), 16);
            short[] shorts = new short[numbytes >> 1];
            ByteBuffer.wrap(bytes, 0, numbytes).order(ByteOrder.BIG_ENDIAN)
                .asShortBuffer().get(shorts);
            for (int i = 0; i < shorts.length; i++)
            {
                contribuser.append(String.format("%X", shorts[i]));
                if (i != 8)
                    contribuser.append(":");
            }
            if (numbytes % 2 == 1)
                contribuser.append(String.format("%X", bytes[numbytes - 1]));
        }
        else // IPv4
        {
            int numbytes = (parts.length < 2) ? 4 : (Integer.parseInt(parts[1]) >> 3);
            bytes = Arrays.copyOf(bytes, numbytes);
            for (int i = 0; i < bytes.length; i++)
            {
                contribuser.append(bytes[i]);
                if (i != 3)
                    contribuser.append(".");
            }
        }
        return prefixContribs(contribuser.toString(), null);
    }

    /**
     *  Gets contributions for all users starting with <var>prefix</var>. See
     *  {@link #contribs(List, String, RequestHelper)} for full documentation.
     *
     *  @param prefix a prefix of usernames.
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any optional parameters)
     *  @return contributions of users with this prefix
     *  @throws IOException if a network error occurs
     */
    public List<Revision> prefixContribs(String prefix, Wiki.RequestHelper helper) throws IOException
    {
        return contribs(Collections.emptyList(), prefix, helper).get(0);
    }

    /**
     *  Gets the contributions for a user, an IP address or a range of IP
     *  addresses. See {@link #contribs(List, String, RequestHelper)} for full
     *  documentation.
     *
     *  @param user the user, IP address or IP range to get contributions for
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any optional parameters)
     *  @return contributions of this user, or an empty list if the user does
     *  not exist
     *  @throws IOException if a network error occurs
     *  @since 0.17
     */
    public List<Revision> contribs(String user, Wiki.RequestHelper helper) throws IOException
    {
        return contribs(List.of(user), null, helper).get(0);
    }

    /**
     *  Gets the contributions for a list of users, IP addresses or ranges of IP
     *  addresses. Equivalent to [[Special:Contributions]]. Be careful when
     *  using <var>prefix</var> and <var>users</var> on large wikis because more
     *  than 100000 edits may be returned for certain values of <var>users</var>.
     *
     *  <p>
     *  Accepted parameters from <var>helper</var> are:
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#inNamespaces(int...) namespaces}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#taggedWith(String) tag}
     *  <li>{@link Wiki.RequestHelper#filterBy(Map) filter by}: "minor", "top",
     *  "new", "patrolled"
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param users a list of users, IP addresses or IP ranges to get
     *  contributions for
     *  @param prefix a prefix of usernames. Overrides <var>users</var>. Use null
     *  to not specify one.
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return contributions of <var>users</var> in the same order as
     *  <var>users</var>, or an empty list where the user does not exist
     *  @throws IOException if a network error occurs
     *  @since 0.34
     */
    public List<List<Revision>> contribs(List<String> users, String prefix, Wiki.RequestHelper helper) throws IOException
    {
        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "usercontribs");
        getparams.put("ucprop", "title|timestamp|flags|comment|parsedcomment|ids|size|sizediff|tags");
        if (helper != null)
        {
            helper.setRequestType("uc");
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addNamespaceParameter());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addTagParameter());
            getparams.putAll(helper.addShowParameter());
            limit = helper.limit();
        }

        BiConsumer<String, List<Revision>> parser = (line, results) ->
        {
            // xml form: <item user="Wizardman" ... size="59460" />
            for (int a = line.indexOf("<item "); a > 0; a = line.indexOf("<item ", ++a))
            {
                int b = line.indexOf("</item>", a);
                results.add(parseRevision(line.substring(a, b), ""));
            }
        };

        if (prefix == null || prefix.isEmpty())
        {
            Map<String, Object> postparams = new HashMap<>();
            List<Revision> revisions = new ArrayList<>();
            for (String userstring : constructTitleString(users))
            {
                postparams.put("ucuser", userstring);
                revisions.addAll(makeListQuery("uc", getparams, postparams, "contribs", limit, parser));
            }
            // group and reorder
            // implementation note: the API does not distinguish between users/IPs
            // with zero edits and users that do not exist
            List<List<Revision>> ret = new ArrayList<>();
            List<String> normusers = new ArrayList<>();
            for (String localuser : users)
            {
                normusers.add(normalize(localuser));
                ret.add(new ArrayList<>());
            }
            for (Wiki.Revision revision : revisions)
                for (int i = 0; i < users.size(); i++)
                    if (normusers.get(i).equals(revision.getUser()))
                        ret.get(i).add(revision);

            log(Level.INFO, "contribs", "Successfully retrived contributions for " + users.size() + " users.");
            return ret;
        }
        else
        {
            getparams.put("ucuserprefix", prefix);
            List<Revision> revisions = makeListQuery("uc", getparams, null, "contribs", limit, parser);
            log(Level.INFO, "prefixContribs", "Successfully retrived prefix contributions (" + revisions.size() + " edits)");
            return Arrays.asList(revisions);
        }
    }

    /**
     *  Sends an email message to a user in a similar manner to [[Special:Emailuser]].
     *  You and the target user must have a confirmed email address and the
     *  target user must have email contact enabled. Messages are sent in plain
     *  text (no wiki markup or HTML). This method is {@linkplain
     *  #setThrottle(int) throttled}.
     *
     *  @param usertomail a wiki user with email enabled
     *  @param subject the subject of the message
     *  @param message the plain text message
     *  @param emailme whether to send a copy of the message to your email address
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we cannot send email
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if you have been blocked from sending email
     *  @throws UnsupportedOperationException if email is disabled or if you do
     *  not have a verified email address
     *  @see <a href="https://mediawiki.org/wiki/API:Emailuser">MediaWiki documentation</a>
     *  @see Wiki.User#canBeEmailed()
     *  @since 0.24
     */
    public synchronized void emailUser(User usertomail, String message, String subject, boolean emailme) throws IOException, LoginException
    {
        if (!usertomail.canBeEmailed())
        {
            // should throw an exception here
            log(Level.WARNING, "emailUser", "User " + user.getUsername() + " is not emailable");
            return;
        }
        checkPermissions("email", "sendemail");
        throttle();

        // post email
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "emailuser");
        getparams.put("target", usertomail.getUsername());
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("token", getToken("csrf"));
        if (emailme)
            postparams.put("ccme", "1");
        postparams.put("text", message);
        postparams.put("subject", subject);
        
        String response = makeApiCall(getparams, postparams, "emailUser");
        checkErrorsAndUpdateStatus(response, "email",
            Map.of("cantsend", desc -> new UnsupportedOperationException(desc)), null);
        log(Level.INFO, "emailUser", "Successfully emailed " + user.getUsername() + ".");
    }

    /**
     *  Blocks a user, IP address or IP address range. Overwrites existing
     *  blocks. This method is {@linkplain #setThrottle(int) throttled}. Allowed
     *  keys for <var>blockoptions</var> include:
     *
     *  <ul>
     *  <li><b>nocreate</b>: prevent account creation from the relevant IP
     *    addresses (includes IP addresses underlying user accounts)
     *  <li><b>noemail</b>: prevent use of [[Special:Emailuser]]
     *  <li><b>allowusertalk</b>: allow use of [[User talk:<var>usertoblock</var>]]
     *    by the blocked user
     *  <li><b>autoblock</b>: block IP addresses used by this user, includes a
     *    a cookie (see <a href="https://mediawiki.org/wiki/Autoblock">MediaWiki
     *    documentation</a>)
     *  <li><b>anononly</b>: for IP addresses and ranges, allow the creation of
     *    new accounts on that range
     *  </ul>
     *
     *  @param usertoblock the user to block
     *  @param reason the reason for blocking
     *  @param expiry when the block expires (use {@code null} for indefinite)
     *  @param blockoptions (see above)
     *  @throws IllegalArgumentException if <var>expiry</var> is in the past
     *  @throws SecurityException if the user lacks the privileges to block
     *  @throws IOException if a network error occurs
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if you have been blocked
     *  @see #unblock(String, String)
     *  @see <a href="https://mediawiki.org/wiki/API:Block">MediaWiki documentation</a>
     *  @since 0.35
     */
    public synchronized void block(String usertoblock, String reason, OffsetDateTime expiry, Map<String, Boolean> blockoptions) throws IOException, LoginException
    {
        // Note: blockoptions is implemented as a Map because more might be added
        // in the future.
        if (expiry != null && expiry.isBefore(OffsetDateTime.now()))
            throw new IllegalArgumentException("Cannot set a block with a past expiry time!");
        checkPermissions("block", "block");
        throttle();

        // send request
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "block");
        getparams.put("user", usertoblock);
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("token", getToken("csrf"));
        postparams.put("reason", reason);
        postparams.put("expiry", Objects.requireNonNullElse(expiry, "indefinite"));
        postparams.put("reblock", "1");
        if (blockoptions != null)
        {
            blockoptions.forEach((key, value) ->
            {
                if (Boolean.TRUE.equals(value))
                    postparams.put(key, "1");
            });
        }
        
        String response = makeApiCall(getparams, postparams, "block");
        checkErrorsAndUpdateStatus(response, "block", null, null);
        log(Level.INFO, "block", "Successfully blocked " + user);
    }

    /**
     *  Unblocks a user. This method is {@linkplain #setThrottle(int) throttled}.
     *  @param blockeduser the user to unblock
     *  @param reason the reason for unblocking
     *  @throws SecurityException if the user lacks the privileges to unblock
     *  @throws IOException if a network error occurs
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if you have been blocked
     *  @see #block(String, String, OffsetDateTime, Map)
     *  @see <a href="https://mediawiki.org/wiki/API:Block">MediaWiki documentation</a>
     *  @since 0.31
     */
    public synchronized void unblock(String blockeduser, String reason) throws IOException, LoginException
    {
        checkPermissions("unblock", "unblock");
        throttle();

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "unblock");
        getparams.put("user", blockeduser);
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        postparams.put("token", getToken("csrf"));
        
        String response = makeApiCall(getparams, postparams, "unblock");
        Map<String, Consumer<String>> info = Map.of(
            "cantunblock", desc -> log(Level.INFO, "unblock", desc),
            "blockedasrange", desc -> log(Level.SEVERE, "unblock", desc)); // throw exception?
        if (checkErrorsAndUpdateStatus(response, "unblock", null, info))
            log(Level.INFO, "unblock", "Successfully unblocked " + blockeduser);
    }

    /**
     *  Changes the privilege level of the given user. Permissions that you
     *  don't have the ability to add or remove are silently ignored. {@code
     *  expiry.length} must be either of 0 (add all permissions indefinitely),
     *  1 (add all permissions until this time) or {@code addedgroups.length}
     *  (add each permission for the time given by the index in <var>expiry</var>).
     *  See [[Special:Listgrouprights]] for the permissions you can add or
     *  remove and what they do. Equivalent to [[Special:UserRights]]. This
     *  method is {@linkplain #setThrottle(int) throttled}.
     *
     *  @param u the user to change permissions for
     *  @param granted a list of groups to add
     *  @param expiry the expiry time of these rights
     *  @param revoked a list of
     *  @param reason the reason for granting and/or removal
     *  @throws IllegalArgumentException if expiry.length != 0, 1 or
     *  addedgroups.length or if any expiry time is in the past
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if you are not logged in
     *  @throws CredentialExpiredException if cookies have expired
     *  @throws AccountLockedException if you have been blocked
     *  @since 0.35
     *  @see <a href="https://www.mediawiki.org/wiki/API:User_group_membership">MediaWiki
     *  documentation</a>
     */
    public synchronized void changeUserPrivileges(User u, List<String> granted, List<OffsetDateTime> expiry,
        List<String> revoked, String reason) throws IOException, LoginException
    {
        // validate parameters
        int numexpirydates = expiry.size();
        if (numexpirydates > 1 && numexpirydates != granted.size())
            throw new IllegalArgumentException("Expiry array length must be 0, 1 or addedgroups.length");
        final OffsetDateTime now = OffsetDateTime.now();
        if (expiry.stream().anyMatch(date -> date.isBefore(now)))
            throw new IllegalArgumentException("Supplied dates must be in the future!");
        if (user == null)
            throw new SecurityException("You need to be logged in to change user privileges.");

        // warn for ignored groups
        List<String> groups = u.getGroups();
        List<String> alreadyhas = new ArrayList<>(groups);
        alreadyhas.retainAll(granted);
        if (!alreadyhas.isEmpty())
            log(Level.WARNING, "changeUserprivileges", "User " + u + " is already in groups you attempted to add: "
                + Arrays.toString(alreadyhas.toArray()));
        List<String> notingroups = new ArrayList<>(revoked);
        notingroups.removeAll(groups);
        if (!notingroups.isEmpty())
            log(Level.WARNING, "changeUserPrivileges", "User " + u + " is not in groups you attempted to remove: "
                + Arrays.toString(notingroups.toArray()));
        throttle();

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "userrights");
        getparams.put("user", u.getUsername());
        Map<String, Object> postparams = new HashMap<>();
        postparams.put("reason", reason);
        postparams.put("token", getToken("userrights"));
        postparams.put("add", granted);
        postparams.put("expiry", numexpirydates == 0 ? "indefinite" : expiry);
        postparams.put("remove", revoked);
        
        String response = makeApiCall(getparams, postparams, "changeUserPrivileges");
        checkErrorsAndUpdateStatus(response, "changeUserPrivileges", null, null);
        log(Level.INFO, "changeUserPrivileges", "Successfully changed privileges of user " + u.getUsername());
    }

    // WATCHLIST METHODS

    /**
     *  Adds a page to the watchlist. You need to be logged in to use this.
     *  @param titles the pages to add to the watchlist
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @see #unwatch
     *  @since 0.18
     */
    public void watch(List<String> titles) throws IOException
    {
        watchInternal(titles, false);
        watchlist.addAll(titles);
    }

    /**
     *  Removes pages from the watchlist. You need to be logged in to use
     *  this. (Does not do anything if the page is not watched).
     *
     *  @param titles the pages to remove from the watchlist.
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @see #watch
     *  @since 0.18
     */
    public void unwatch(List<String> titles) throws IOException
    {
        watchInternal(titles, true);
        watchlist.removeAll(titles);
    }

    /**
     *  Internal method for interfacing with the watchlist, since the API URLs
     *  for (un)watching are very similar.
     *
     *  @param titles the titles to (un)watch
     *  @param unwatch whether we should unwatch these pages
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @see #watch
     *  @see #unwatch
     *  @since 0.18
     */
    protected void watchInternal(List<String> titles, boolean unwatch) throws IOException
    {
        // create the watchlist cache
        if (watchlist == null)
            getRawWatchlist();
        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "watch");
        Map<String, Object> postparams = new HashMap<>();
        if (unwatch)
            postparams.put("unwatch", "1");
        String state = unwatch ? "unwatch" : "watch";
        for (String titlestring : constructTitleString(titles))
        {
            postparams.put("titles", titlestring);
            postparams.put("token", getToken("watch"));
            String response = makeApiCall(getparams, postparams, state);
            detectUncheckedErrors(response, null, null);
        }
        log(Level.INFO, state, "Successfully " + state + "ed " + titles.size() + " pages.");
    }

    /**
     *  Fetches the list of titles on the currently logged in user's watchlist.
     *  Equivalent to [[Special:Watchlist/raw]].
     *  @return the contents of the watchlist
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @since 0.18
     */
    public List<String> getRawWatchlist() throws IOException
    {
        return getRawWatchlist(true);
    }

    /**
     *  Fetches the list of titles on the currently logged in user's watchlist.
     *  Equivalent to [[Special:Watchlist/raw]].
     *  @param cache whether we should use the watchlist cache
     *  (no online activity, if the cache exists)
     *  @return the contents of the watchlist
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @since 0.18
     */
    public List<String> getRawWatchlist(boolean cache) throws IOException
    {
        // cache
        checkPermissions("access the watchlist", "viewmywatchlist");
        if (watchlist != null && cache)
            return new ArrayList<>(watchlist);

        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "watchlistraw");

        watchlist = makeListQuery("wr", getparams, null, "getRawWatchlist", -1, (line, results) ->
        {
            // xml form: <wr ns="14" title="Categorie:Even more things"/>
            for (int a = line.indexOf("<wr "); a > 0; a = line.indexOf("<wr ", ++a))
            {
                String title = parseAttribute(line, "title", a);
                // is this supposed to not retrieve talk pages?
                if (namespace(title) % 2 == 0)
                    results.add(title);
            }
        });

        // log
        int size = watchlist.size();
        log(Level.INFO, "getRawWatchlist", "Successfully retrieved raw watchlist (" + size + " items)");
        return new ArrayList<>(watchlist);
    }

    /**
     *  Determines whether a page is watched. (Uses a cache).
     *  @param title the title to be checked
     *  @return whether that page is watched
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @since 0.18
     */
    public boolean isWatched(String title) throws IOException
    {
        // populate the watchlist cache
        if (watchlist == null)
            getRawWatchlist();
        return watchlist.contains(title);
    }

    /**
     *  Fetches recent changes to pages on your watchlist. Data is
     *  retrieved from the <a href="https://mediawiki.org/wiki/Manual:Recentchanges_table">
     *  recentchanges table</a> and hence cannot be retrieved after <a
     *  href="https://mediawiki.org/wiki/Manual:$wgRCMaxAge">a certain amount
     *  of time</a>.
     *
     *  <p>
     *  Accepted parameters from <var>helper</var> are:
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#inNamespaces(int...) namespaces}
     *  <li>{@link Wiki.RequestHelper#byUser(String) user}
     *  <li>{@link Wiki.RequestHelper#notByUser(String) not by user}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#filterBy(Map) filter by}: "minor", "bot",
     *  "anon", "patrolled", "top", "unread"
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return list of changes to watched pages and their talk pages
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if not logged in
     *  @see <a href="https://www.mediawiki.org/wiki/API:Watchlist">MediaWiki
     *  documentation</a>
     *  @since 0.27
     */
    public List<Revision> watchlist(Wiki.RequestHelper helper) throws IOException
    {
        checkPermissions("access the watchlist", "viewmywatchlist");
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "watchlist");
        getparams.put("wlprop", "ids|title|timestamp|user|comment|parsedcomment|sizes|tags");
        int limit = -1;
        if (helper != null)
        {
            helper.setRequestType("wl");
            helper.addDateRangeParameters();
            helper.addNamespaceParameter();
            helper.addUserParameter();
            helper.addExcludeUserParameter();
            helper.addReverseParameter();
            helper.addShowParameter();
            limit = helper.limit();
        }

        List<Revision> wl = makeListQuery("wl", getparams, null, "watchlist", limit, (line, results) ->
        {
            // xml form: <item pageid="16396" revid="176417" ns="0" title="API:Query - Lists" />
            for (int i = line.indexOf("<item "); i > 0; i = line.indexOf("<item ", ++i))
            {
                int j = line.indexOf("</item>", i);
                results.add(parseRevision(line.substring(i, j), ""));
            }
        });

        log(Level.INFO, "watchlist", "Successfully retrieved watchlist (" + wl.size() + " items)");
        return wl;
    }

    // LISTS

    /**
     *  Performs a full text search of the wiki. Equivalent to
     *  [[Special:Search]], or that little textbox in the sidebar. Returns an
     *  array of search results in decreasing order of relevance, where each
     *  result has the form:
     *
     *  <ul>
     *  <li><b>title</b>: (String) the page name
     *  <li><b>sectiontitle</b>: (String) the matching section title, may not be
     *    present
     *  <li><b>snippet</b>: (String) a snippet of the matching text
     *  <li><b>size</b>: (Integer) the size of the page in bytes
     *  <li><b>wordcount</b>: (Integer) the number of words in the matching page
     *  <li><b>lastedittime</b>: (OffsetDateTime) when the page was last edited
     *  </ul>
     *
     *  @param search a search string
     *  @param namespaces the namespaces to search. If not present, search
     *  {@link #MAIN_NAMESPACE} only.
     *  @return the search results as detailed above
     *  @throws IOException if a network error occurs
     *  @since 0.14
     *  @see <a href="https://mediawiki.org/wiki/API:Search">MediaWiki
     *  documentation</a>
     */
    public List<Map<String, Object>> search(String search, int... namespaces) throws IOException
    {
        // default to main namespace
        if (namespaces.length == 0)
            namespaces = new int[] { MAIN_NAMESPACE };
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "search");
        getparams.put("srwhat", "text");
        getparams.put("srprop", "snippet|sectionsnippet|wordcount|size|timestamp");
        getparams.put("srsearch", search);
        getparams.put("srnamespace", constructNamespaceString(namespaces));

        List<Map<String, Object>> results = makeListQuery("sr", getparams, null, "search", -1, (line, list) ->
        {
            // xml form: <p ns="0" title="Main Page" snippet="Blah blah blah" sectiontitle="Section"/>
            for (int x = line.indexOf("<p "); x > 0; x = line.indexOf("<p ", ++x))
            {
                Map<String, Object> result = new HashMap<>();
                result.put("title", parseAttribute(line, "title", x));
                result.put("snippet", parseAttribute(line, "snippet", x));
                result.put("wordcount", Integer.parseInt(parseAttribute(line, "wordcount", x)));
                result.put("size", Integer.parseInt(parseAttribute(line, "size", x)));
                result.put("lastedittime", OffsetDateTime.parse(parseAttribute(line, "timestamp", x)));

                // section title (if available). Stupid API documentation is misleading.
                if (line.contains("sectionsnippet=\""))
                    result.put("sectiontitle", parseAttribute(line, "sectionsnippet", x));

                list.add(result);
            }
        });

        int size = results.size();
        log(Level.INFO, "search", "Successfully searched for string \"" + search + "\" (" + size + " items found)");
        return results;
    }

    /**
     *  Returns a list of pages in the specified namespaces which use the
     *  specified image.
     *  @param image the image (may contain File:)
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return the list of pages that use this image
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.10
     */
    public List<String> imageUsage(String image, int... ns) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "imageusage");
        getparams.put("iutitle", "File:" + removeNamespace(normalize(image), FILE_NAMESPACE));
        if (ns.length > 0)
            getparams.put("iunamespace", constructNamespaceString(ns));

        List<String> pages = makeListQuery("iu", getparams, null, "imageUsage", -1, (line, results) ->
        {
            // xml form: <iu pageid="196465" ns="7" title="File talk:Wiki.png" />
            for (int x = line.indexOf("<iu "); x > 0; x = line.indexOf("<iu ", ++x))
                results.add(parseAttribute(line, "title", x));
        });

        int size = pages.size();
        log(Level.INFO, "imageUsage", "Successfully retrieved usages of " + image + " (" + size + " items)");
        return pages;
    }

    /**
     *  Returns a list of all pages linking to this page. Equivalent to
     *  [[Special:Whatlinkshere]].
     *
     *  @param title the title of the page
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return the list of pages linking to the specified page
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.10
     */
    public String[] whatLinksHere(String title, int... ns) throws IOException
    {
        return whatLinksHere(List.of(title), false, false, ns).get(0).toArray(String[]::new);
    }

    /**
     *  Returns lists of all pages linking to the given pages within the specified
     *  namespaces. Output order is the same as input order. Alternatively, we
     *  can retrieve a list of what redirects to a page by setting
     *  <var>redirects</var> to true. Equivalent to [[Special:Whatlinkshere]].
     * 
     *  <p>
     *  If <var>addredirects</var> is true, pages that link to a redirect that
     *  targets all given pages are added to the results. If namespaces are
     *  specified, both redirect and linking page must be in those namespaces. 
     *  WARNING: this is significantly slower!
     *
     *  @param titles a list of titles
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @param redirects whether we should limit to redirects only
     *  @param addredirects should we fetch all links to redirects to each page 
     *  as well (slow)?
     *  @return the list of pages linking to the specified page
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.10
     */
    public List<List<String>> whatLinksHere(List<String> titles, boolean redirects, boolean addredirects, int... ns) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        if (addredirects)
        {
            // only the non-vectorized API query permits this i.e. one page = at
            // least one network request (the limit is also halved)
            List<List<String>> ret = new ArrayList<>();
            getparams.put("list", "backlinks");
            getparams.put("blredirect", "1");
            if (redirects)
                getparams.put("blfilterredir", "redirects");
            if (ns.length > 0)
                getparams.put("blnamespace", constructNamespaceString(ns));
            for (String title : titles)
            {
                getparams.put("bltitle", title);
                List<String> backlinks = makeListQuery("bl", getparams, null, "whatLinksHere", -1, (result, results) ->
                {
                    // xml form: <bl pageid="323710" ns="0" title="MediaWiki" />
                    for (int a = result.indexOf("<bl "); a > 0; a = result.indexOf("<bl ", ++a))
                        results.add(parseAttribute(result, "title", a));
                });
                ret.add(backlinks);
            }
            log(Level.INFO, "whatLinksHere", "Successfully retrieved " + (redirects ? "redirects to " : "links to ") + ret.size() + " pages.");
            return ret;
        }
        getparams.put("prop", "linkshere");
        if (ns.length > 0)
            getparams.put("lhnamespace", constructNamespaceString(ns));
        if (redirects)
            getparams.put("lhshow", "redirect");

        List<List<String>> ret = makeVectorizedQuery("lh", getparams, titles,
            "whatLinksHere", -1, (data, result) ->
        {
            // xml form: <lh pageid="1463" ns="1" title="Talk:Apollo program" />
            for (int a = data.indexOf("<lh "); a > 0; a = data.indexOf("<lh ", ++a))
                result.add(parseAttribute(data, "title", a));
        });
        log(Level.INFO, "whatLinksHere", "Successfully retrieved " + (redirects ? "redirects to " : "links to ") + ret.size() + " pages.");
        return ret;
    }

    /**
     *  Returns lists of all pages transcluding to a page within the specified
     *  namespaces. Output order is the same as the input order.
     *
     *  @param titles a list of titles
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return the lists of pages transcluding the specified pages
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.36
     */
    public List<List<String>> whatTranscludesHere(List<String> titles, int... ns) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("prop", "transcludedin");
        if (ns.length > 0)
            getparams.put("tinamespace", constructNamespaceString(ns));

        List<List<String>> ret = makeVectorizedQuery("ti", getparams, titles,
            "whatTranscludesHere", -1, (data, result) ->
        {
            // xml form: <ti pageid="15199344" ns="2" title="User:Example" />
            for (int a = data.indexOf("<ti "); a > 0; a = data.indexOf("<ti ", ++a))
                result.add(parseAttribute(data, "title", a));
        });
        log(Level.INFO, "whatTranscludesHere", "Successfully retrieved transclusions for " + ret.size() + " pages.");
        return ret;
    }

    /**
     *  Gets the number of pages in the list of given catgories without
     *  {@linkplain #getCategoryMembers(String, int...) performing a full lookup
     *  of category members}. This is equivalent to the <kbd>{{PAGESINCATEGORY}}</kbd>
     *  parser function. The input order is the same as the return order.
     *
     *  @param categories a list of categories (with or without Category: prefix)
     *  @return an array for each category, where the first entry = total number
     *  of pages, second = number of normal pages, third = number of files and
     *  fourth = number of subcategories
     *  @throws IOException if a network error occurs
     *  @since 0.36
     *  @see <a href="https://mediawiki.org/wiki/API:Categoryinfo">MediaWiki
     *  documentation</a>
     */
    public List<int[]> getCategoryMemberCounts(List<String> categories) throws IOException
    {
        // force all titles to have namespace(title) == CATEGORY_NAMESPACE because
        // the API requires it
        List<String> norm_cats = new ArrayList<>();
        String catns = namespaceIdentifier(CATEGORY_NAMESPACE);
        for (String category : categories)
        {
            if (namespace(category) != CATEGORY_NAMESPACE)
                norm_cats.add(catns + ":" + category);
            else
                norm_cats.add(category);
        }

        Map<String, String> getparams = new HashMap<>();
        getparams.put("action", "query");
        getparams.put("prop", "categoryinfo");
        Map<String, Object> postparams = new HashMap<>();
        Map<String, int[]> metamap = new HashMap<>();
        for (String titlestring : constructTitleString(norm_cats))
        {
            postparams.put("titles", titlestring);
            String result = makeApiCall(getparams, postparams, "getCategoryMemberCount");
            detectUncheckedErrors(result, null, null);
            resolveNormalizedParser(norm_cats, result);
            if (resolveredirect)
                resolveRedirectParser(norm_cats, result);

            // form: <page _idx="2504643" pageid="2504643" ns="14" title="Category:Albert Einstein">
            // <categoryinfo size="95" pages="87" files="0" subcats="8" />
            // </page>
            for (int j = result.indexOf("<page "); j > 0; j = result.indexOf("<page ", ++j))
            {
                int x = result.indexOf("</page>", j);
                String item = result.substring(j, x);
                boolean exists = !item.contains("missing=\"\"");
                int[] values = new int[4];
                if (exists)
                {
                    values[0] = Integer.parseInt(parseAttribute(item, "size", 0));
                    values[1] = Integer.parseInt(parseAttribute(item, "pages", 0));
                    values[2] = Integer.parseInt(parseAttribute(item, "files", 0));
                    values[3] = Integer.parseInt(parseAttribute(item, "subcats", 0));
                }
                String parsedtitle = parseAttribute(item, "title", 0);
                metamap.put(parsedtitle, values);
            }
        }

        // reorder
        List<int[]> ret = new ArrayList<>();
        for (String category : norm_cats)
            ret.add(metamap.get(category));
        log(Level.INFO, "getCategoryMemberCounts", "Successfully retrieved category member counts for " + categories.size() + " categories.");
        return ret;
    }

    /**
     *  Gets the members of a category, sorted as in the UI.
     *
     *  @param name the name of the category (with or without namespace attached)
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return a String[] containing page titles of members of the category
     *  @throws IOException if a network error occurs
     *  @since 0.03
     */
    public List<String> getCategoryMembers(String name, int... ns) throws IOException
    {
        return getCategoryMembers(name, 0, new ArrayList<>(), false, ns);
    }

    /**
     *  Gets the members of a category, sorted as in the UI.
     *
     *  @param name the name of the category
     *  @param subcat do you want to return members of sub-categories also? (default: false)
     *  Recursion is limited to a depth of one.
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return a String[] containing page titles of members of the category
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.03
     */
    public List<String> getCategoryMembers(String name, boolean subcat, int... ns) throws IOException
    {
        return getCategoryMembers(name, (subcat ? 1 : 0), new ArrayList<>(), false, ns);
    }

    /**
     *  Gets the members of a category with maxdepth recursion.
     *
     *  @param name the name of the category
     *  @param maxdepth depth of recursion for subcategories
     *  @param sorttimestamp whether to sort the returned array by date/time
     *  added to category (earliest first)
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return a String[] containing page titles of members of the category
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.31
     */
    public List<String> getCategoryMembers(String name, int maxdepth, boolean sorttimestamp, int... ns) throws IOException
    {
        return getCategoryMembers(name, maxdepth, new ArrayList<>(), sorttimestamp, ns);
    }

    /**
     *  Gets the members of a category.
     *
     *  @param name the name of the category
     *  @param maxdepth depth of recursion for subcategories
     *  @param visitedcategories list of already visited categories
     *  @param sorttimestamp whether to sort the returned array by date/time
     *  added to category (earliest first)
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @return a String[] containing page titles of members of the category
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.03
     */
    protected List<String> getCategoryMembers(String name, int maxdepth, List<String> visitedcategories,
        boolean sorttimestamp, int... ns) throws IOException
    {
        name = removeNamespace(normalize(name), CATEGORY_NAMESPACE);
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "categorymembers");
        getparams.put("cmprop", "title");
        getparams.put("cmtitle", "Category:" + name);
        if (sorttimestamp)
            getparams.put("cmsort", "timestamp");
        boolean nocat = ns.length != 0;
        if (maxdepth > 0 && nocat)
        {
            for (int i = 0; nocat && i < ns.length; i++)
                nocat = (ns[i] != CATEGORY_NAMESPACE);
            if (nocat)
            {
                int[] temp = Arrays.copyOf(ns, ns.length + 1);
                temp[ns.length] = CATEGORY_NAMESPACE;
                getparams.put("cmnamespace", constructNamespaceString(temp));
            }
            else if (ns.length > 0)
                getparams.put("cmnamespace", constructNamespaceString(ns));
        }
        else
            getparams.put("cmnamespace", constructNamespaceString(ns));
        final boolean nocat2 = nocat;

        List<String> members = makeListQuery("cm", getparams, null, "getCategoryMembers", -1, (line, results) ->
        {
            try
            {
                // xml form: <cm pageid="24958584" ns="3" title="User talk:86.29.138.185" />
                for (int x = line.indexOf("<cm "); x > 0; x = line.indexOf("<cm ", ++x))
                {
                    String member = parseAttribute(line, "title", x);

                    // fetch subcategories
                    boolean iscat = namespace(member) == CATEGORY_NAMESPACE;
                    if (maxdepth > 0 && iscat && !visitedcategories.contains(member))
                    {
                        visitedcategories.add(member);
                        List<String> categoryMembers = getCategoryMembers(member, maxdepth - 1, visitedcategories, sorttimestamp, ns);
                        results.addAll(categoryMembers);
                    }

                    // ignore this item if we requested subcat but not CATEGORY_NAMESPACE
                    if (!(maxdepth > 0) || !nocat2 || !iscat)
                        results.add(member);
                }
            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }
        });

        int size = members.size();
        log(Level.INFO, "getCategoryMembers", "Successfully retrieved contents of Category:" + name + " (" + size + " items)");
        return members;
    }

    /**
     *  Searches the wiki for external links. Equivalent to [[Special:Linksearch]].
     *  Returns a list of pairs, where the first item is a page and the second
     *  the relevant url. Wildcards (*) are only permitted at the start of the
     *  search string.
     *
     *  @param pattern the pattern (String) to search for (e.g. example.com,
     *  *.example.com)
     *  @throws IOException if a network error occurs
     *  @return a list of results where each entry is { page, URL }
     *  @since 0.06
     */
    public List<String[]> linksearch(String pattern) throws IOException
    {
        return linksearch(pattern, "http");
    }

    /**
     *  Searches the wiki for external links. Equivalent to [[Special:Linksearch]].
     *  Returns a list of pairs, where the first item is a page and the second
     *  the relevant url. Wildcards (*) are only permitted at the start of the
     *  search string.
     *
     *  <p><b>Warnings:</b>
     *  <ul>
     *  <li>Searching by namespace with a query limit won't return that many
     *      results if <a href="https://mediawiki.org/wiki/Manual:$wgMiserMode">$wgMiserMode
     *      is enabled</a>. This is the case for most large wikis.
     *  </ul>
     *
     *  @param pattern the pattern (String) to search for (e.g. example.com,
     *  *.example.com)
     *  @param ns a list of namespaces to filter by, empty = all namespaces.
     *  @param protocol one of the protocols listed in the API documentation or
     *  "" (equivalent to http)
     *  @throws IOException if a network error occurs
     *  @return a list of results where each entry is { page, URL }
     *  @since 0.24
     *  @see <a href="https://mediawiki.org/wiki/API:Exturlusage">MediaWiki API
     *  documentation</a>
     *  @see <a href="https://mediawiki.org/wiki/Help:Linksearch">[[Special:Linksearch]]
     *  documentation</a>
     *  @see <a href="https://mediawiki.org/wiki/Manual:Externallinks_table">Externallinks
     *  table</a>
     */
    public List<String[]> linksearch(String pattern, String protocol, int... ns) throws IOException
    {
        // I'm still not happy with the return type, but I think this is as good
        // as it gets in vanilla JDK.
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "exturlusage");
        getparams.put("euprop", "title|url");
        getparams.put("euquery", pattern);
        getparams.put("euprotocol", protocol);
        if (ns.length > 0)
            getparams.put("eunamespace", constructNamespaceString(ns));

        List<String[]> links = makeListQuery("eu", getparams, null, "linksearch", -1, (line, results) ->
        {
            // xml form: <eu ns="0" title="Main Page" url="http://example.com" />
            for (int x = line.indexOf("<eu"); x > 0; x = line.indexOf("<eu ", ++x))
            {
                String link = parseAttribute(line, "url", x);
                String pagename = parseAttribute(line, "title", x);
                if (link.charAt(0) == '/') // protocol relative url
                    results.add(new String[] { pagename, protocol + ":" + link });
                else
                    results.add(new String[] { pagename, link });
            }
        });

        log(Level.INFO, "linksearch", "Successfully returned instances of external link " + pattern + " (" + links.size() + " links)");
        return links;
    }

    /**
     *  Fetches part of the list of currently operational blocks. Equivalent to
     *  [[Special:BlockList]]. WARNING: cannot tell whether a particular IP
     *  is autoblocked as this is non-public data (see [[wmf:Privacy policy]]).
     *  Accepted parameters from <var>helper</var> are:
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#filterBy(Map) filter by}: "range" (range blocks),
     *      ip (IP blocks), "temp" (temporary blocks), "account" (account blocks)
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param users a list of users that might have been blocked. Use null to
     *  not specify one. May be an IP (e.g. "127.0.0.1") or a CIDR range (e.g.
     *  "127.0.0.0/16") but not an autoblock (e.g. "#123456").
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @return a list of the blocks
     *  @throws IOException or UncheckedIOException if a network error occurs
     *  @since 0.12
     */
    public List<LogEntry> getBlockList(List<String> users, Wiki.RequestHelper helper) throws IOException
    {
        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "blocks");
        getparams.put("bkprop", "id|user|by|timestamp|expiry|reason|flags|restrictions");
        if (helper != null)
        {
            helper.setRequestType("bk");
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addShowParameter());
            limit = helper.limit();
        }
        BiConsumer<String, List<Wiki.LogEntry>> parser = (line, results) ->
        {
            // XML form: <block id="7844197" user="223.205.208.198" by="ProcseeBot"
            // timestamp="2017-09-24T07:17:08Z" expiry="2017-11-23T07:17:08Z"
            // reason="{{blocked proxy}} <!-- 8080 -->" nocreate="" allowusertalk=""/>
            for (int a = line.indexOf("<block "); a > 0; a = line.indexOf("<block ", ++a))
            {
                // find entry
                int b = Math.max(line.indexOf("/>", a), line.indexOf("</block>", a));
                String temp = line.substring(a, b);

                String blocker = parseAttribute(temp, "by", 0);
                String blockeduser = parseAttribute(temp, "user", 0);
                String target;
                if (blockeduser == null) // autoblock
                    target = "#" + parseAttribute(temp, "id", 0);
                else
                    target = namespaceIdentifier(USER_NAMESPACE) + ":" + blockeduser;

                LogEntry le = parseLogEntry(temp, blocker, BLOCK_LOG, "block", target);
                results.add(le);
            }
        };
        List<LogEntry> entries = new ArrayList<>();
        if (users == null)
            entries.addAll(makeListQuery("bk", getparams, null, "getBlockList", limit, parser));
        else
        {
            for (String bkusers : constructTitleString(users))
            {
                getparams.put("bkusers", bkusers);
                entries.addAll(makeListQuery("bk", getparams, null, "getBlockList", limit, parser));
            }
        }
        log(Level.INFO, "getBlockList", "Successfully fetched block list " + entries.size() + " entries)");
        return entries;
    }

    /**
     *  Gets the specified amount of log entries between the given times by
     *  the given user on the given target. Equivalent to [[Special:Log]].
     *  Accepted parameters from <var>helper</var> are:
     *
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#byUser(String) user}
     *  <li>{@link Wiki.RequestHelper#byTitle(String) title}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#inNamespaces(int...) namespaces} (one
     *      namespace only, must not be used if a title is specified)
     *  <li>{@link Wiki.RequestHelper#taggedWith(String) tag}
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  @param logtype what log to get (e.g. {@link #DELETION_LOG})
     *  @param action what action to get (e.g. delete, undelete, etc.), use
     *  {@code null} to not specify one
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters noted above)
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if the user lacks the credentials needed to
     *  access a privileged log
     *  @return the specified log entries
     *  @since 0.08
     */
    public List<LogEntry> getLogEntries(String logtype, String action, Wiki.RequestHelper helper) throws IOException
    {
        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "logevents");
        getparams.put("leprop", "ids|title|type|user|timestamp|comment|parsedcomment|details|tags");
        if (!logtype.equals(ALL_LOGS))
        {
            if (action == null)
                getparams.put("letype", logtype);
            else
                getparams.put("leaction", logtype + "/" + action);
        }
        if (helper != null)
        {
            helper.setRequestType("le");
            getparams.putAll(helper.addTitleParameter());
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addUserParameter());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addNamespaceParameter());
            getparams.putAll(helper.addTagParameter());
            limit = helper.limit();
        }

        List<LogEntry> entries = makeListQuery("le", getparams, null, "getLogEntries", limit, (line, results) ->
        {
            String[] items = line.split("<item ");
            for (int i = 1; i < items.length; i++)
            {
                LogEntry le = parseLogEntry(items[i], null, null, null, null);
                results.add(le);
            }
        });

        // log the success
        StringBuilder console = new StringBuilder("Successfully retrieved log (type=");
        console.append(logtype);
        console.append(", ");
        console.append(entries.size());
        console.append(" entries)");
        log(Level.INFO, "getLogEntries", console.toString());
        return entries;
    }

    /**
     *  Parses xml generated by <code>getLogEntries()</code>,  
     *  <code>getImageHistory()</code> and <code>getBlockList()</code> into {@link Wiki.LogEntry}
     *  objects. Override this if you want custom log types. NOTE: if
     *  RevisionDelete was used on a log entry, the relevant values will be
     *  null.
     *
     *  @param xml the xml to parse
     *  @param user null, or use this value for the performer of the log entry
     *  @param type null, or use this value for the type of the log entry
     *  @param action null, or use this value for the action of the log entry
     *  @param target null, or user this value for the target of the log entry
     *  @return the parsed log entry
     *  @since 0.18
     */
    protected LogEntry parseLogEntry(String xml, String user, String type, String action, String target)
    {
        // ID (getLogEntries only)
        long id = -1;
        if (xml.contains("logid=\""))
            id = Long.parseLong(parseAttribute(xml, "logid", 0));

        boolean actionhidden = xml.contains("actionhidden=\"");
        if (type == null && xml.contains("type=\"")) // only getLogEntries
        {
            type = parseAttribute(xml, "type", 0);
            action = parseAttribute(xml, "action", 0);
        }

        // reason
        String reason, parsedreason;
        boolean reasonhidden = xml.contains("commenthidden=\"");
        if (USER_CREATION_LOG.equals(type))
        {
            // there is no reason for creating a user
            reason = "";
            parsedreason = "";
        }
        else if (xml.contains("reason=\""))
        {
            reason = parseAttribute(xml, "reason", 0);
            parsedreason = null; // not available in list=blocks / getBlockList!
        }
        else
        {
            reason = parseAttribute(xml, "comment", 0);
            parsedreason = parseAttribute(xml, "parsedcomment", 0);
        }

        // generic performer name
        boolean userhidden = xml.contains("userhidden=\"\"");
        if (user == null && xml.contains("user=\""))
            user = parseAttribute(xml, "user", 0);

        // generic target name
        // space is important -- commons.getImageHistory("File:Chief1.gif");
        if (target == null && xml.contains(" title=\""))
            target = parseAttribute(xml, "title", 0);

        OffsetDateTime timestamp = OffsetDateTime.parse(parseAttribute(xml, "timestamp", 0));

        // details
        Map<String, String> details = new HashMap<>();
        if (xml.contains("commenthidden")) // oversighted
            details = null;
        else if (type.equals(MOVE_LOG))
            details.put("target_title", parseAttribute(xml, "target_title", 0));
        else if (type.equals(BLOCK_LOG) || xml.contains("<block"))
        {
            int a = xml.indexOf("<block") + 7;
            String s = xml.substring(a);
            int c = xml.contains("expiry=\"") ? s.indexOf("expiry=") + 8 : s.indexOf("duration=") + 10;
            if (c > 10) // not an unblock
            {
                int d = s.indexOf('\"', c);
                if (s.contains("anononly")) // anon-only
                    details.put("anononly", "true");
                if (s.contains("nocreate")) // account creation blocked
                    details.put("nocreate", "true");
                if (s.contains("noautoblock")) // autoblock disabled
                    details.put("noautoblock", "true");
                if (s.contains("noemail")) // email disabled
                    details.put("noemail", "true");
                if (s.contains("nousertalk")) // cannot edit talk page
                    details.put("nousertalk", "true");
                details.put("expiry", s.substring(c, d));
                
                // partial block parameters
                if (s.contains("<restrictions>"))
                {
                    details.put("partial", "true");
                    if (s.contains("<namespaces>"))
                    {
                        details.put("type", "namespaces");
                        // TODO: add the actual namespaces
                    }
                    else if (s.contains("<pages>"))
                    {
                        details.put("type", "pages");
                        // TODO: add the actual pages
                    }
                }
            }
        }
        else if (type.equals(PROTECTION_LOG))
        {
            if (action.equals("protect"))
            {
                // FIXME: return a protectionstate here?
                details.put("protection string", parseAttribute(xml, "description", 0));
            }
        }
        else if (type.equals(USER_RENAME_LOG))
        {
            int a = xml.indexOf("<param>") + 7;
            int b = xml.indexOf("</param>", a);
            details.put("new username", decode(xml.substring(a, b)));
        }
        else if (type.equals(USER_RIGHTS_LOG))
        {
            int a = xml.indexOf("<oldgroups>");
            if (a != -1) {
	            int b = xml.indexOf("</oldgroups>", a);
	            var oldgroups = xml.substring(a + 11, b);
	            var old_list = new ArrayList<String>();
	            for (int end = 0, start = oldgroups.indexOf("<g>"); start != -1; start = oldgroups.indexOf("<g>", end)) {
	                end = oldgroups.indexOf("</g>", start);
	                old_list.add(oldgroups.substring(start + 3, end));
	            }
	            details.put("oldgroups", String.join(",", old_list));
            } else // self-closing empty "<oldgroups />" tag
                details.put("oldgroups", "");
            int c = xml.indexOf("<newgroups>");
            if (c != -1) {
	            int d = xml.indexOf("</newgroups>", c);
	            var newgroups = xml.substring(c + 11, d);
	            var new_list = new ArrayList<String>();
	            for (int end = 0, start = newgroups.indexOf("<g>"); start != -1; start = newgroups.indexOf("<g>", end)) {
	                end = newgroups.indexOf("</g>", start);
	                new_list.add(newgroups.substring(start + 3, end));
	            }
	            details.put("newgroups", String.join(",", new_list));
            } else // self-closing empty "<newgroups />" tag
                details.put("newgroups", "");
        }

        // tags
        List<String> tags = new ArrayList<>();
        if (xml.contains("<tag>"))
        {
            for (int idx = xml.indexOf("<tag>"); idx >= 0; idx = xml.indexOf("<tag>", ++idx))
                tags.add(xml.substring(idx + 5, xml.indexOf("</tag>", idx)));
        }
        
        LogEntry le = new LogEntry(id, timestamp, user, reason, parsedreason, type, action, target, details);
        le.setUserDeleted(userhidden);
        le.setCommentDeleted(reasonhidden);
        le.setContentDeleted(actionhidden);
        le.setTags(tags);
        return le;
    }

    /**
     *  Lists pages that start with a given prefix. Equivalent to
     *  [[Special:Prefixindex]].
     *
     *  @param prefix the prefix
     *  @return the list of pages with that prefix
     *  @throws IOException if a network error occurs
     *  @since 0.15
     */
    public List<String> prefixIndex(String prefix) throws IOException
    {
        return listPages(prefix, null, ALL_NAMESPACES, -1, -1, null);
    }

    /**
     *  List pages below a certain size in the main namespace. Equivalent to
     *  [[Special:Shortpages]].
     *  @param cutoff the maximum size in bytes these short pages can be
     *  @return pages below that size
     *  @throws IOException if a network error occurs
     *  @since 0.15
     */
    public List<String> shortPages(int cutoff) throws IOException
    {
        return listPages("", null, MAIN_NAMESPACE, -1, cutoff, null);
    }

    /**
     *  List pages below a certain size in any namespace. Equivalent to
     *  [[Special:Shortpages]].
     *  @param cutoff the maximum size in bytes these short pages can be
     *  @param namespace a namespace
     *  @throws IOException if a network error occurs
     *  @return pages below that size in that namespace
     *  @since 0.15
     */
    public List<String> shortPages(int cutoff, int namespace) throws IOException
    {
        return listPages("", null, namespace, -1, cutoff, null);
    }

    /**
     *  List pages above a certain size in the main namespace. Equivalent to
     *  [[Special:Longpages]].
     *  @param cutoff the minimum size in bytes these long pages can be
     *  @return pages above that size
     *  @throws IOException if a network error occurs
     *  @since 0.15
     */
    public List<String> longPages(int cutoff) throws IOException
    {
        return listPages("", null, MAIN_NAMESPACE, cutoff, -1, null);
    }

    /**
     *  List pages above a certain size in any namespace. Equivalent to
     *  [[Special:Longpages]].
     *  @param cutoff the minimum size in nbytes these long pages can be
     *  @param namespace a namespace
     *  @return pages above that size
     *  @throws IOException if a network error occurs
     *  @since 0.15
     */
    public List<String> longPages(int cutoff, int namespace) throws IOException
    {
        return listPages("", null, namespace, cutoff, -1, null);
    }

    /**
     *  Lists pages with titles containing a certain prefix with a certain
     *  protection state and in a certain namespace. Equivalent to
     *  [[Special:Allpages]], [[Special:Prefixindex]], [[Special:Protectedpages]]
     *  and [[Special:Allmessages]] (if namespace == MEDIAWIKI_NAMESPACE).
     *  WARNING: Limited to 500 values (5000 for bots), unless a prefix or
     *  protection level is specified.
     *
     *  @param prefix the prefix of the title. Use "" to not specify one.
     *  @param protectionstate a {@link #protect protection state}, use null
     *  to not specify one
     *  @param namespace a namespace. ALL_NAMESPACES is not suppported, an
     *  UnsupportedOperationException will be thrown.
     *  @return the specified list of pages
     *  @since 0.09
     *  @throws IOException if a network error occurs
     */
    public List<String> listPages(String prefix, Map<String, Object> protectionstate, int namespace) throws IOException
    {
        return listPages(prefix, protectionstate, namespace, -1, -1, null);
    }

    /**
     *  Lists pages with titles containing a certain prefix with a certain
     *  protection state and in a certain namespace. Equivalent to
     *  [[Special:Allpages]], [[Special:Prefixindex]], [[Special:Protectedpages]]
     *  [[Special:Allmessages]] (if namespace == MEDIAWIKI_NAMESPACE),
     *  [[Special:Shortpages]] and [[Special:Longpages]]. WARNING: Limited to
     *  500 values (5000 for bots), unless a prefix, (max|min)imum size or
     *  protection level is specified.
     *
     *  @param prefix the prefix of the title. Use "" to not specify one.
     *  @param protectionstate a {@link #protect protection state}, use null
     *  to not specify one
     *  @param namespace a namespace. ALL_NAMESPACES is not suppported, an
     *  UnsupportedOperationException will be thrown unless a prefix is specified.
     *  @param minimum the minimum size in bytes these pages can be. Use -1 to
     *  not specify one.
     *  @param maximum the maximum size in bytes these pages can be. Use -1 to
     *  not specify one.
     *  @param redirects Boolean.TRUE = list redirects only, Boolean.FALSE = list
     *  non-redirects only, null = list both
     *  @return the specified list of pages
     *  @since 0.09
     *  @throws IOException if a network error occurs
     */
    public List<String> listPages(String prefix, Map<String, Object> protectionstate, int namespace, int minimum,
        int maximum, Boolean redirects) throws IOException
    {
        // No varargs namespace here because MW API only supports one namespace
        // for this module.
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "allpages");
        if (!prefix.isEmpty())
        {
            if (namespace == ALL_NAMESPACES)
            {
                namespace = namespace(prefix);
                prefix = removeNamespace(prefix);
            }
            getparams.put("apprefix", normalize(prefix));
        }
        else if (namespace == ALL_NAMESPACES) // check for namespace
            throw new UnsupportedOperationException("ALL_NAMESPACES not supported in MediaWiki API.");
        getparams.put("apnamespace", String.valueOf(namespace));
        if (protectionstate != null)
        {
            StringBuilder apprtype = new StringBuilder();
            StringBuilder apprlevel = new StringBuilder();
            protectionstate.forEach((key, value) ->
            {
                if (key.equals("cascade"))
                    getparams.put("apprfiltercascade", (Boolean)value ? "cascading" : "noncascading");
                else if (!key.contains("expiry"))
                {
                    apprtype.append(key);
                    apprtype.append("|");
                    apprlevel.append(value);
                    apprlevel.append("|");
                }
            });
            getparams.put("apprtype", apprtype.substring(0, apprtype.length() - 1));
            getparams.put("apprlevel", apprlevel.substring(0, apprlevel.length() - 1));
        }
        // max and min
        if (minimum >= 0)
            getparams.put("apminsize", String.valueOf(minimum));
        if (maximum >= 0)
            getparams.put("apmaxsize", String.valueOf(maximum));
        if (redirects != null)
            getparams.put("apfilterredir", redirects ? "redirects" : "nonredirects");

        // set query limit = 1 request if max, min, prefix or protection level
        // not specified
        int limit = -1;
        if (maximum < 0 && minimum < 0 && prefix.isEmpty() && protectionstate == null)
            limit = max;
        List<String> pages = makeListQuery("ap", getparams, null, "listPages", limit, (line, results) ->
        {
            // xml form: <p pageid="1756320" ns="0" title="Kre'fey" />
            for (int a = line.indexOf("<p "); a > 0; a = line.indexOf("<p ", ++a))
                results.add(parseAttribute(line, "title", a));
        });

        int size = pages.size();
        log(Level.INFO, "listPages", "Successfully retrieved page list (" + size + " pages)");
        return pages;
    }

    /**
     *  Fetches data from one of a set of miscellaneous special pages.
     *
     *  <p><b>Warnings:</b>
     *  <ul>
     *  <li>Many reports may be cached, limited and/or disabled if <a
     *      href="https://mediawiki.org/wiki/Manual:$wgMiserMode">$wgMiserMode
     *      is enabled</a>. This is the case for most large wikis.
     *  </ul>
     *
     *  @param page one of the qppage values specifed by the documentation below
     *  (case sensitive)
     *  @return the list of pages returned by that particular special page
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if the user lacks the privileges necessary to
     *  view a report (e.g. unwatchedpages)
     *  @since 0.28
     *  @see <a href="https://mediawiki.org/wiki/API:Querypage">MediaWiki
     *  documentation</a>
     */
    public List<String> queryPage(String page) throws IOException
    {
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "querypage");
        getparams.put("qppage", page);

        List<String> pages = makeListQuery("qp", getparams, null, "queryPage", -1, (line, results) ->
        {
            // xml form: <page value="0" ns="0" title="Anorthosis Famagusta FC in European football" />
            for (int x = line.indexOf("<page "); x > 0; x = line.indexOf("<page ", ++x))
                results.add(parseAttribute(line, "title", x));
        });

        int temp = pages.size();
        log(Level.INFO, "queryPage", "Successfully retrieved [[Special:" + page + "]] (" + temp + " pages)");
        return pages;
    }

    /**
     *  Fetches recently created pages. See {@link #recentChanges(Wiki.RequestHelper,
     *  String)} for full documentation. Equivalent to [[Special:Newpages]].
     *
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any optional parameters
     *  @return the revisions that created the pages satisfying the requirements
     *  above
     *  @throws IOException if a network error occurs
     *  @since 0.35
     */
    public List<Revision> newPages(Wiki.RequestHelper helper) throws IOException
    {
        return recentChanges(helper, "new");
    }

    /**
     *  Fetches recent edits to this wiki. See {@link
     *  #recentChanges(Wiki.RequestHelper, String)} for full documentation.
     *  Equivalent to [[Special:Recentchanges]].
     *
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any optional parameters
     *  @return the recent changes that satisfy these criteria
     *  @throws IOException if a network error occurs
     *  @since 0.23
     */
    public List<Revision> recentChanges(Wiki.RequestHelper helper) throws IOException
    {
        return recentChanges(helper, null);
    }

    /**
     *  Fetches recent changes to this wiki. WARNING: The <a
     *  href="https://mediawiki.org/wiki/Manual:Recentchanges_table">recentchanges
     *  table</a> stores edits for a <a href="https://mediawiki.org/wiki/Manual:$wgRCMaxAge">
     *  finite period of time</a>; it is not possible to retrieve pages created
     *  before then. Equivalent to [[Special:Recentchanges]].
     * 
     *  <p>
     *  Accepted parameters from <var>helper</var> are:
     *  <ul>
     *  <li>{@link Wiki.RequestHelper#withinDateRange(OffsetDateTime,
     *      OffsetDateTime) date range}
     *  <li>{@link Wiki.RequestHelper#byUser(String) user}
     *  <li>{@link Wiki.RequestHelper#notByUser(String) not by user}
     *  <li>{@link Wiki.RequestHelper#reverse(boolean) reverse}
     *  <li>{@link Wiki.RequestHelper#inNamespaces(int...) namespaces}
     *  <li>{@link Wiki.RequestHelper#taggedWith(String) tag}
     *  <li>{@link Wiki.RequestHelper#filterBy(Map) filter by}: "minor", "bot",
     *  "anon", "redirect", "patrolled"
     *  <li>{@link Wiki.RequestHelper#limitedTo(int) local query limit}
     *  </ul>
     *
     *  <p>
     *  If {@code rctype} is not {@code "edit"} or {@code "new"} then the results 
     *  consist of pseudo-revisions whose data does not correspond to an actual
     *  on-wiki state. For example:
     *
     *  <ul>
     *  <li>{@code rctype == "log"} yields {@code id == 0} and {@code title} is
     *      the log entry target
     *  <li>{@code rctype == "external"} yields {@code id} as the most recent
     *      edit to {@code title}, {@code previous_id == id}, {@code user} is
     *      the external user making the change, {@code sizediff == 0} and 
     *      {@code comment} describes the external change
     *  <li>{@code rctype =="categorize"} yields {@code title} as the category
     *      added or removed and {@code comment} specifies the page added or
     *      removed to that category
     *  </ul>
     *
     *  @param helper a {@link Wiki.RequestHelper} (optional, use null to not
     *  provide any of the optional parameters described above
     *  @param rctype null, "edit" (edits only) or "new" (new pages); your 
     *  mileage may vary for other types (log, external, categorize)
     *  @return the recent changes that satisfy these criteria
     *  @throws IOException if a network error occurs
     *  @since 0.35
     */
    protected List<Revision> recentChanges(Wiki.RequestHelper helper, String rctype) throws IOException
    {
        int limit = -1;
        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "recentchanges");
        getparams.put("rcprop", "title|ids|user|timestamp|flags|comment|parsedcomment|sizes|sha1|tags");
        if (helper != null)
        {
            helper.setRequestType("rc");
            getparams.putAll(helper.addNamespaceParameter());
            getparams.putAll(helper.addUserParameter());
            getparams.putAll(helper.addExcludeUserParameter());
            getparams.putAll(helper.addDateRangeParameters());
            getparams.putAll(helper.addTagParameter());
            getparams.putAll(helper.addReverseParameter());
            getparams.putAll(helper.addShowParameter());
            limit = helper.limit();
        }

        if (rctype != null)
            getparams.put("rctype", rctype);

        List<Revision> revisions = makeListQuery("rc", getparams, null, "recentChanges", limit,
            (line, results) ->
        {
            // xml form <rc type="edit" ns="0" title="Main Page" ... />
            for (int i = line.indexOf("<rc "); i > 0; i = line.indexOf("<rc ", ++i))
            {
                int j = line.indexOf("</rc>", i);
                results.add(parseRevision(line.substring(i, j), ""));
            }
        });

        log(Level.INFO, "recentChanges", "Successfully retrieved recent changes (" + revisions.size() + " revisions)");
        return revisions;
    }

    /**
     *  Fetches all pages that use interwiki links to the specified wiki and the
     *  page on that wiki that is linked to. For example, {@code
     *  getInterWikiBacklinks("testwiki")} may return:
     *  <samp>
     *  {
     *      { "Spam", "testwiki:Blah" },
     *      { "Test", "testwiki:Main_Page" }
     *  }
     *  </samp>
     *  <p>
     *  Here the page [[Spam]] contains the interwiki link [[testwiki:Blah]] and
     *  the page [[Test]] contains the interwiki link [[testwiki:Main_Page]].
     *  This does not resolve nested interwiki prefixes, e.g. [[wikt:fr:Test]].
     *
     *  <p>
     *  For WMF wikis, see <a href="https://meta.wikimedia.org/wiki/Interwiki_map">
     *  the interwiki map</a> for where some prefixes link to.
     *
     *  @param prefix the interwiki prefix that denotes a wiki
     *  @return all pages that contain interwiki links to said wiki
     *  @throws IOException if a network error occurs
     *  @since 0.23
     */
    public List<String[]> getInterWikiBacklinks(String prefix) throws IOException
    {
        return getInterWikiBacklinks(prefix, "|");
    }

    /**
     *  Fetches all pages that use interwiki links with a certain <var>prefix</var>
     *  and <var>title</var>. <var>prefix</var> refers to the wiki being linked to
     *  and <var>title</var> refers to the page on said wiki being linked to. In
     *  wiki syntax, this is [[prefix:title]]. This does not resolve nested
     *  prefixes, e.g. [[wikt:fr:Test]].
     *
     *  <p>
     *  Example: If [[Test]] and [[Spam]] both contain the interwiki link
     *  [[testwiki:Blah]] then {@code getInterWikiBacklinks("testwiki", "Blah");}
     *  will return (sorted by <var>title</var>)
     *  <samp>
     *  {
     *      { "Spam", "testwiki:Blah" },
     *      { "Test", "testwiki:Blah" }
     *  }
     *  </samp>
     *
     *  <p>
     *  For WMF wikis, see <a href="https://meta.wikimedia.org/wiki/Interwiki_map">
     *  the interwiki map</a>for where some prefixes link to.
     *
     *  @param prefix the interwiki prefix to search
     *  @param title the title of the page on the other wiki to search for
     *  (optional, use "|" to not specify one). Warning: "" is a valid interwiki
     *  target!
     *  @return a list of all pages that use interwiki links satisfying the
     *  parameters given
     *  @throws IOException if a network error occurs
     *  @throws IllegalArgumentException if a title is specified without a
     *  prefix (the MediaWiki API doesn't like this)
     *  @since 0.23
     */
    public List<String[]> getInterWikiBacklinks(String prefix, String title) throws IOException
    {
        // must specify a prefix
        if (title.equals("|") && prefix.isEmpty())
            throw new IllegalArgumentException("Interwiki backlinks: title specified without prefix!");

        Map<String, String> getparams = new HashMap<>();
        getparams.put("list", "iwbacklinks");
        getparams.put("iwblprefix", prefix);
        if (!title.equals("|"))
            getparams.put("iwbltitle", normalize(title));
        getparams.put("iwblprop", "iwtitle|iwprefix");

        List<String[]> links = makeListQuery("iwbl", getparams, null, "getInterWikiBacklinks", -1, (line, results) ->
        {
            // xml form: <iw pageid="24163544" ns="0" title="Elisabeth_of_Wroclaw" iwprefix="pl" iwtitle="Main_Page" />
            for (int x = line.indexOf("<iw "); x > 0; x = line.indexOf("<iw ", ++x))
            {
                results.add(new String[]
                {
                    parseAttribute(line, "title", x),
                    parseAttribute(line, "iwprefix", x) + ':' + parseAttribute(line, "iwtitle", x)
                });
            }
        });

        log(Level.INFO, "getInterWikiBacklinks", "Successfully retrieved interwiki backlinks (" + links.size() + " interwikis)");
        return links;
    }

    // INNER CLASSES
    
    /**
     *  Subclass for wiki users.
     *  @since 0.05
     */
    public class User implements Comparable<User>
    {
        private final String username;
        private final OffsetDateTime registration;
        // user privileges (volatile, changes rarely)
        private List<String> rights;
        private List<String> groups;
        private boolean blocked;
        // user preferences (volatile, changes rarely)
        private Gender gender;
        private boolean emailable;
        // volatile, changes often
        private int editcount;

        /**
         *  Creates a new user object. Does not create a new user on the
         *  wiki (we don't implement this for a very good reason). Shouldn't
         *  be called for anons.
         *
         *  @param username the username of the user
         *  @param registration when the user was registered
         *  @param rights the rights this user has
         *  @param groups the groups this user belongs to
         *  @param gender the self-declared {@link Wiki.Gender Gender} of this user.
         *  @param emailable whether the user can be emailed through [[Special:Emailuser]]
         *  @param blocked whether this user is blocked
         *  @param editcount the internal edit count of this user
         *  @since 0.05
         */
        protected User(String username, OffsetDateTime registration, List<String> rights, List<String> groups,
            Gender gender, boolean emailable, boolean blocked,int editcount)
        {
            this.username = Objects.requireNonNull(username);
            // can be null per https://phabricator.wikimedia.org/T24097
            this.registration = registration;
            this.rights = Objects.requireNonNull(rights);
            this.groups = Objects.requireNonNull(groups);
            this.gender = gender;
            this.emailable = emailable;
            this.blocked = blocked;
            this.editcount = editcount;
        }

        /**
         *  Gets this user's username.
         *  @return this user's username
         *  @since 0.08
         */
        public final String getUsername()
        {
            return username;
        }

        /**
         *  Gets the date/time at which this user account was created. May be
         *  {@code null} per <a href="https://phabricator.wikimedia.org/T24097">
         *  https://phabricator.wikimedia.org/T24097</a>.
         *  @return (see above)
         *  @since 0.35
         */
        public final OffsetDateTime getRegistrationDate()
        {
            return registration;
        }

        /**
         *  Returns {@code true} if the user is allowed to perform the specified
         *  action(s). Read [[Special:Listgrouprights]] before using this!
         *  @param right a specific action
         *  @param morerights additional actions to check
         *  @return whether the user is allowed to execute them
         *  @since 0.24
         */
        public boolean isAllowedTo(String right, String... morerights)
        {
            List<String> temp = new ArrayList<>();
            temp.add(right);
            temp.addAll(List.of(morerights));
            return rights.containsAll(temp);
        }

        /**
         *  Returns {@code true} if the user is a member of the specified group.
         *  @param group a specific group
         *  @return whether the user is in it
         *  @since 0.24
         */
        public boolean isA(String group)
        {
            return groups.contains(group);
        }

        /**
         *  Returns the groups the user is a member of. See [[Special:Listgrouprights]].
         *  Changes in this list do not propagate to this object or the wiki.
         *  @return (see above)
         *  @since 0.35
         */
        public List<String> getGroups()
        {
            return new ArrayList<>(groups);
        }

        /**
         *  Returns the specific permissions this user has. See [[Special:Listgrouprights]].
         *  Changes in this list do not propagate to the object or the wiki.
         *  @return (see above)
         *  @since 0.35
         */
        public List<String> getRights()
        {
            return new ArrayList<>(rights);
        }

        /**
         *  Returns whether this user can be emailed through [[Special:Emailuser]].
         *  @return (see above)
         *  @see #emailUser(Wiki.User, String, String, boolean)
         *  @since 0.35
         */
        public boolean canBeEmailed()
        {
            return emailable;
        }

        /**
         *  Returns the self-disclosed {@linkplain Wiki.Gender gender} of this
         *  user.
         *  @return (see above)
         *  @see Wiki.Gender
         *  @since 0.35
         */
        public Gender getGender()
        {
            return gender;
        }

        /**
         *  Determines whether this user is blocked at the time of construction.
         *  If you want a live check, look  up the user on the {@linkplain
         *  #getBlockList list of blocks}.
         *  @return whether this user is blocked
         *  @since 0.12
         */
        public boolean isBlocked()
        {
            return blocked;
        }

        /**
         *  Fetches the internal edit count for this user at the time of
         *  construction, which includes all live edits and deleted edits after
         *  (I think) January 2007. If you want to count live edits only,
         *  compute the size of {@link User#contribs(int...) User.contribs()}.
         *
         *  @return the user's edit count
         *  @since 0.16
         */
        public int countEdits()
        {
            return editcount;
        }

        /**
         *  Returns a log of the times when the user has been blocked.
         *  @return records of the occasions when this user has been blocked
         *  @throws IOException if something goes wrong
         *  @since 0.08
         */
        public List<LogEntry> blockLog() throws IOException
        {
            Wiki.RequestHelper rh = new RequestHelper().byTitle("User:" + username);
            return Wiki.this.getLogEntries(Wiki.BLOCK_LOG, null, rh);
        }

        /**
         *  Fetches the contributions for this user in a particular namespace(s).
         *  @param ns a list of namespaces to filter by, empty = all namespaces.
         *  @return a revision array of contributions
         *  @throws IOException if a network error occurs
         *  @since 0.17
         */
        public List<Revision> contribs(int... ns) throws IOException
        {
            Wiki.RequestHelper rh = new RequestHelper().inNamespaces(ns);
            return Wiki.this.contribs(username, rh);
        }

        /**
         *  Returns the list of logged actions performed by this user.
         *  @param logtype what log to get ({@link Wiki#DELETION_LOG} etc.)
         *  @param action what action to get (e.g. delete, undelete), use
         *  "" to not specify one
         *  @return (see above)
         *  @throws IOException if a network error occurs
         *  @since 0.33
         */
        public List<LogEntry> getLogEntries(String logtype, String action) throws IOException
        {
            Wiki.RequestHelper rh = new RequestHelper().byUser(username);
            return Wiki.this.getLogEntries(logtype, action, rh);
        }

        /**
         *  Tests whether this user is equal to another one.
         *  @param x another object
         *  @return whether the usernames of the users are equal
         */
        @Override
        public boolean equals(Object x)
        {
            if (!(x instanceof User))
                return false;
            User other = (User)x;
            return Objects.equals(username, other.username)
                && Objects.equals(registration, other.registration);
        }

        /**
         *  Returns a hashcode of this user based on the username and
         *  registration date.
         *  @return see above
         */
        @Override
        public int hashCode()
        {
            return username.hashCode() * 127 + registration.hashCode();
        }

        /**
         *  Enables sorting of users by their username.
         *  @param other some other user
         *  @return less than zero if this user is alphabetically before the
         *  other, 0 if they are the same and 1 if alphabetically after
         */
        @Override
        public int compareTo(User other)
        {
            return username.compareTo(other.username);
        }

        /**
         *  Returns a string representation of this user.
         *  @return see above
         */
        @Override
        public String toString()
        {
            return getClass().getName()
                + "[username=" + username
                + ",registration=" + (registration != null ? registration.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "unset")
                + ",groups=" + Arrays.toString(groups.toArray()) + "]";
        }
    }

    /**
     *  A data super class for an event happening on a wiki such as a {@link
     *  Wiki.Revision} or a {@link Wiki.LogEntry}.
     *  @since 0.35
     */
    public abstract class Event implements Comparable<Event>
    {
        private final long id;
        private final OffsetDateTime timestamp;
        private final String user;
        private final String title;
        private final String comment;
        private final String parsedcomment;
        private List<String> tags;
        private boolean commentDeleted = false, userDeleted = false,
            contentDeleted = false;

        /**
         *  Creates a new Event record.
         *  @param id the unique ID of the event
         *  @param timestamp the timestamp at which it occurred
         *  @param user the user or IP address performing the event
         *  @param title the title of the page affected
         *  @param comment the comment left by the user when performing the
         *  event (e.g. an edit summary)
         *  @param parsedcomment comment, but parsed into HTML
         */
        protected Event(long id, OffsetDateTime timestamp, String user, String title, String comment, String parsedcomment)
        {
            this.id = id;
            this.timestamp = Objects.requireNonNull(timestamp);
            this.user = user;
            this.title = title;
            this.comment = comment;
            // Rewrite parsedcomments to fix useless relative hyperlinks to
            // other wiki pages
            if (parsedcomment == null)
                this.parsedcomment = null;
            else
                this.parsedcomment = parsedcomment.replace("href=\"/wiki", "href=\"" + protocol + domain + "/wiki");
        }

        /**
         *  Gets the unique ID of this event. For a {@link Wiki.Revision}, this
         *  number is referred to as the "oldid" or "revid" and should not be
         *  confused with "rcid" (which is the ID in the recentchanges table).
         *  For a {@link Wiki.LogEntry}, this value only makes sense if the
         *  record was obtained through {@link Wiki#getLogEntries(String, String,
         *  RequestHelper)} and overloads (other methods return pseudo-LogEntries).
         *  @return the ID of this revision
         */
        public long getID()
        {
            return id;
        }

        /**
         *  Gets the timestamp of this event.
         *  @return the timestamp of this event
         */
        public OffsetDateTime getTimestamp()
        {
            return timestamp;
        }

        /**
         *  Returns the user or anon who performed this event. You should pass
         *  this (if not an IP) to {@link #getUser} to obtain a {@link
         *  Wiki.User} object. Returns {@code null} if the user was
         *  RevisionDeleted and you lack the necessary privileges.
         *  @return the user or anon
         */
        public String getUser()
        {
            return user;
        }

        /**
         *  Sets a boolean flag that the user triggering this event has been
         *  RevisionDeleted in on-wiki records.
         *  @param deleted (see above)
         *  @see #isUserDeleted()
         *  @see #getUser()
         */
        protected void setUserDeleted(boolean deleted)
        {
            userDeleted = deleted;
        }

        /**
         *  Returns {@code true} if the user triggering this event is
         *  RevisionDeleted.
         *  @return (see above)
         *  @see #getUser()
         */
        public boolean isUserDeleted()
        {
            return userDeleted;
        }

        /**
         *  Returns the page affected by this event. May be {@code null} for
         *  certain types of LogEntry and/or if the LogEntry is RevisionDeleted
         *  and you don't have the ability to access it.
         *  @return (see above)
         *  @see #isContentDeleted()
         */
        public String getTitle()
        {
            return title;
        }

        /**
         *  Gets the comment for this event in wikitext. If this is a {@link
         *  Wiki.Revision}, this is the edit summary. If this is a {@link
         *  Wiki.LogEntry}, this is the reason for the logged action. WARNING:
         *  returns {@code null} if the reason was RevisionDeleted and you lack
         *  the necessary privileges.
         *  @return the comment associated with the event
         *  @see #getParsedComment()
         */
        public String getComment()
        {
            return comment;
        }

        /**
         *  Gets the comment for this event, with limited parsing into HTML.
         *  Hyperlinks in the returned HTML are rewritten from useless relative
         *  URLs to full URLs that point to the wiki page in question. Returns
         *  {@code null} if {@linkplain #isCommentDeleted() the comment was
         *  RevisionDeleted} and you lack the necessary privileges.
         *
         *  <p><b>Warnings:</b>
         *  <ul>
         *  <li>Not available through {@link #getBlockList}.
         *  </ul>
         *
         *  @return the comment associated with the event, parsed into HTML
         *  @see #getComment()
         */
        public String getParsedComment()
        {
            return parsedcomment;
        }

        /**
         *  Sets a boolean flag that the comment associated with this event has
         *  been RevisionDeleted in on-wiki records.
         *  @param deleted (see above)
         *  @see #getComment
         *  @see #getParsedComment()
         *  @see #isCommentDeleted()
         */
        protected void setCommentDeleted(boolean deleted)
        {
            commentDeleted = deleted;
        }

        /**
         *  Returns {@code true} if the comment is RevisionDeleted.
         *  @return (see above)
         *  @see #getComment
         *  @see #getParsedComment()
         */
        public boolean isCommentDeleted()
        {
            return commentDeleted;
        }

        /**
         *  Sets a boolean flag that the content of this event has been
         *  RevisionDeleted.
         *  @param deleted (see above)
         *  @see #isContentDeleted()
         */
        protected void setContentDeleted(boolean deleted)
        {
            contentDeleted = deleted;
        }

        /**
         *  Returns {@code true} if the content of this event has been
         *  RevisionDeleted. For a {@link Wiki.LogEntry}, this refers to the
         *  page the logged action affects and the logged action performed (e.g.
         *  "unblock" or "delete").
         *  @return (see above)
         */
        public boolean isContentDeleted()
        {
            return contentDeleted;
        }
        
        /**
         *  Returns the list of tags attached to this event. Modifying the
         *  return value does not affect this Revision object or the wiki state.
         *  @return (see above)
         *  @see <a href="https://www.mediawiki.org/wiki/Help:Tags">MediaWiki
         *  documentation</a>
         *  @since 0.37
         */
        public List<String> getTags()
        {
            return new ArrayList<>(tags);
        }
        
        /**
         *  Sets the list of tags attached to this event. Modifying the supplied
         *  list does not affect this Revision object or change on-wiki state.
         *  @param tags a list of change tags
         *  @see <a href="https://www.mediawiki.org/wiki/Help:Tags">MediaWiki
         *  documentation</a>
         *  @since 0.37
         */
        protected void setTags(List<String> tags)
        {
            this.tags = new ArrayList<>(tags);
        }

        /**
         *  Returns a String representation of this Event. Subclasses only need
         *  to lop off the trailing "]" and add their own fields when overriding
         *  this method.
         *  @return (see above)
         */
        @Override
        public String toString()
        {
            return getClass().getName()
               + "[id=" + id
               + ",timestamp=" + timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
               + ",user=\"" + Objects.toString(user, "[DELETED]") + "\""
               + ",userDeleted=" + userDeleted
               + ",title=\"" + Objects.toString(title, "[null or deleted]") + "\""
               + ",comment=\"" + Objects.toString(comment, "[DELETED]") + "\""
               + ",commentDeleted=" + commentDeleted
               + ",contentDeleted=" + contentDeleted + ']';
        }

        /**
         *  Determines whether this Event is equal to some other object. This
         *  method checks the ID, timestamp, user, title and comment.
         *  @param other the other object to compare to
         *  @return whether this instance is equal to that object
         */
        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Event))
                return false;
            Event event = (Event)other;
            return id == event.id
                && Objects.equals(timestamp, event.timestamp)
                && Objects.equals(user, event.user)
                && Objects.equals(title, event.title)
                && Objects.equals(comment, event.comment);
        }

        /**
         *  Returns a hash code for this object based on the ID, timestamp,
         *  user, title and comment.
         *  @return (see above)
         */
        @Override
        public int hashCode()
        {
            int hc = Long.hashCode(id);
            hc = 127 * hc + timestamp.hashCode();
            hc = 127 * hc + Objects.hashCode(user);
            hc = 127 * hc + Objects.hashCode(title);
            hc = 127 * hc + Objects.hashCode(comment);
            return hc;
        }

        /**
         *  Compares this event to another one based on the recentness of their
         *  timestamps (more recent = positive return value), then
         *  alphabetically by user.
         *  @param other the event to compare to
         *  @return the comparator value, negative if less, positive if greater
         */
        @Override
        public int compareTo(Wiki.Event other)
        {
            int result = timestamp.compareTo(other.timestamp);
            if (result == 0 && user != null)
                result = user.compareTo(other.user);
            return result;
        }
    }

    /**
     *  A wrapper class for an entry in a wiki log, which represents an action
     *  performed on the wiki.
     *
     *  @see #getLogEntries
     *  @since 0.08
     */
    public class LogEntry extends Event
    {
        private final String type;
        private String action;
        private Map<String, String> details;

        /**
         *  Creates a new log entry. WARNING: does not perform the action
         *  implied. Use Wiki.class methods to achieve this.
         *
         *  @param id the unique of this log entry
         *  @param timestamp the local time when the action was performed.
         *  @param user the user who performed the action
         *  @param comment why the action was performed
         *  @param parsedcomment like comment, but parsed into HTML
         *  @param type the type of log entry, one of {@link #USER_CREATION_LOG},
         *  {@link #DELETION_LOG}, {@link #BLOCK_LOG}, etc.
         *  @param action the type of action that was performed e.g. "delete",
         *  "unblock", "overwrite", etc.
         *  @param target the target of the action
         *  @param details the details of the action (e.g. the new title of
         *  the page after a move was performed).
         *  @since 0.08
         */
        protected LogEntry(long id, OffsetDateTime timestamp, String user, String comment,
            String parsedcomment, String type, String action, String target, Map<String, String> details)
        {
            super(id, timestamp, user, target, comment, parsedcomment);
            this.type = Objects.requireNonNull(type);
            this.action = action;
            this.details = details;
        }

        /**
         *  Gets the type of log that this entry is in.
         *  @return one of {@link Wiki#DELETION_LOG}, {@link Wiki#BLOCK_LOG}, etc.
         *  @since 0.08
         */
        public String getType()
        {
            return type;
        }

        /**
         *  Gets a string description of the action performed, for example
         *  "delete", "protect", "overwrite", ... WARNING: returns null if the
         *  action was RevisionDeleted.
         *  @return the type of action performed
         *  @since 0.08
         */
        public String getAction()
        {
            return action;
        }

        /**
         *  Gets the details of this log entry. Return values are as follows:
         *
         *  <table>
         *  <caption>Log types and return values</caption>
         *  <tr><th>Log type <th>Return value
         *  <tr><td>MOVE_LOG
         *      <td>The new page title
         *  <tr><td>USER_RENAME_LOG
         *      <td>The new username
         *  <tr><td>BLOCK_LOG
         *      <td>new Object[] { boolean anononly, boolean nocreate, boolean
         *      noautoblock, boolean noemail, boolean nousertalk, String duration }
         *  <tr><td>USER_RIGHTS_LOG
         *      <td>The old ("oldgroups") and new ("newgroups") user rights,
         *          comma-separated
         *  <tr><td>PROTECTION_LOG
         *      <td>if action == "protect" or "modify" return the protection level
         *          (int, -2 if unrecognized) if action == "move_prot" return
         *          the old title, else null
         *  <tr><td>Others or RevisionDeleted
         *      <td>null
         *  </table>
         *
         *  Note that the duration of a block may be given as a period of time
         *  (e.g. "31 hours") or a timestamp (e.g. 20071216160302). To tell
         *  these apart, feed it into <code>Long.parseLong()</code> and catch
         *  any resulting exceptions.
         *
         *  @return the details of the log entry
         *  @since 0.08
         */
        public Map<String, String> getDetails()
        {
            return details;
        }

        /**
         *  Returns a string representation of this log entry.
         *  @return a string representation of this object
         *  @since 0.08
         */
        @Override
        public String toString()
        {
            StringBuilder s = new StringBuilder(super.toString());
            s.deleteCharAt(s.length() - 1);
            s.append(",type=");
            s.append(type);
            s.append(",action=");
            s.append(Objects.toString(action, "[DELETED]"));
            s.append(",details=");
            s.append(details);
            s.append("]");
            return s.toString();
        }

        /**
         *  Determines whether two LogEntries are equal based on the underlying
         *  {@linkplain Event#equals(Object) Event}, type and action.
         *  @param other some object to compare to
         *  @return (see above)
         *  @since 0.33
         */
        @Override
        public boolean equals(Object other)
        {
            if (!super.equals(other))
                return false;
            if (!(other instanceof LogEntry))
                return false;
            LogEntry le = (LogEntry)other;
            return Objects.equals(type, le.type)
                && Objects.equals(action, le.action);
        }

        /**
         *  Computes a hashcode for this LogEntry based on the underlying
         *  {@linkplain Event#hashCode() Event}, type and action.
         *  @return (see above)
         *  @since 0.35
         */
        @Override
        public int hashCode()
        {
            int hc = super.hashCode();
        hc = 127 * hc + type.hashCode();
            hc = 127 * hc + Objects.hashCode(action);
            return hc;
        }
    }

    /**
     *  Represents a contribution and/or a revision to a page.
     *  @since 0.17
     */
    public class Revision extends Event
    {
        private final boolean minor, bot, rvnew;
        private final String sha1;
        private long rcid = -1;
        private long previous = 0, next = 0;
        private int size = 0, sizediff = 0;
        private boolean pageDeleted = false;

        /**
         *  Constructs a new Revision object.
         *  @param revid the id of the revision (this is a long since
         *  {{NUMBEROFEDITS}} on en.wikipedia.org is now (January 2018) ~38%
         *  of {@code Integer.MAX_VALUE}
         *  @param timestamp when this revision was made
         *  @param user the user making this revision (may be anonymous)
         *  @param comment the edit summary
         *  @param parsedcomment the edit summary, parsed into HTML
         *  @param title the concerned article
         *  @param sha1 the SHA-1 hash of the revision
         *  @param minor whether this was a minor edit
         *  @param bot whether this was a bot edit
         *  @param rvnew whether this revision created a new page
         *  @param size the size of the revision
         *  @since 0.17
         */
        public Revision(long revid, OffsetDateTime timestamp, String user, String comment,
            String parsedcomment, String title, String sha1, boolean minor, boolean bot,
            boolean rvnew, int size)
        {
            super(revid, timestamp, user, Objects.requireNonNull(title), comment, parsedcomment);
            this.sha1 = sha1;
            this.minor = minor;
            this.bot = bot;
            this.rvnew = rvnew;
            this.size = size;
        }

        /**
         *  Fetches the contents of this revision.
         *  @return the contents of the appropriate article at <var>timestamp</var>
         *  @throws IOException if a network error occurs
         *  @throws IllegalArgumentException if page == Special:Log/xxx.
         *  @since 0.17
         */
        public String getText() throws IOException
        {
            // logs have no content
            if (getID() < 1L)
                throw new IllegalArgumentException("Log entries have no valid content!");

            // TODO: returning a 404 here when revision content has been deleted
            // is not a good idea.
            if (pageDeleted) // FIXME: broken if a page is live, but has deleted revisions
            {
                Map<String, String> getparams = new HashMap<>();
                getparams.put("action", "query");
                getparams.put("prop", "deletedrevisions");
                getparams.put("drvprop", "content");
                getparams.put("revids", String.valueOf(getID()));
                String temp = makeApiCall(getparams, null, "Revision.getText");
                detectUncheckedErrors(temp, null, null);
                int a = temp.indexOf("<rev ");
                a = temp.indexOf('>', a) + 1;
                int b = temp.indexOf("</rev>", a); // tag not present if revision has no content
                log(Level.INFO, "Revision.getText", "Successfully retrieved text of revision " + getID());
                return (b < 0) ? "" : temp.substring(a, b);
            }
            else
                return Wiki.this.getText(null, new long[] { getID() }, -1).get(0);
        }

        /**
         *  Gets the rendered text of this revision.
         *  @return the rendered contents of the appropriate article at
         *  <var>timestamp</var>
         *  @throws IOException if a network error occurs
         *  @throws IllegalArgumentException if page == Special:Log/xxx.
         *  @since 0.17
         */
        public String getRenderedText() throws IOException
        {
            if (getID() < 1L)
                throw new IllegalArgumentException("Log entries have no valid content!");
            Map<String, Object> content = new HashMap<>();
            content.put("revision", this);
            return Wiki.this.parse(content, -1, false);
        }

        /**
         *  Returns the SHA-1 hash (base 16, lower case) of the content of this
         *  revision, or {@code null} if the revision content is RevisionDeleted
         *  and we cannot access it.
         *
         *  <p><b>Warnings:</b>
         *  <ul>
         *  <li>Not available through {@link #watchlist(RequestHelper)} or {@link
         *      #contribs(List, String, RequestHelper)}.
         *  </ul>
         *
         *  @return (see above)
         *  @since 0.35
         */
        public String getSha1()
        {
            return sha1;
        }

        /**
         *  Returns a HTML rendered diff table of this revision to <var>other</var>.
         *  See {@link #diff(Map, Map)} for full documentation.
         *
         *  @param other another revision on the same page.
         *  @return the difference between this and the other revision
         *  @throws IOException if a network error occurs
         *  @throws SecurityException if this or the other revision is
         *  RevisionDeleted and the user lacks the necessary privileges
         *  @since 0.21
         */
        public String diff(Revision other) throws IOException
        {
            return Wiki.this.diff(Map.of("revision", this), Map.of("revision", other));
        }

        /**
         *  Returns a HTML rendered diff table from this revision to the given
         *  <var>text</var>. Useful for emulating the "show changes"
         *  functionality. See the table at the <a
         *  href="https://en.wikipedia.org/w/index.php?diff=343490272">example</a>.
         *
         *  @param text some wikitext
         *  @return the difference between this and the the text provided
         *  @throws IOException if a network error occurs
         *  @throws SecurityException if this or the other revision is
         *  RevisionDeleted and the user lacks the necessary privileges
         *  @since 0.21
         */
        public String diff(String text) throws IOException
        {
            return Wiki.this.diff(Map.of("revision", this), Map.of("text", text));
        }

        /**
         *  Returns a HTML rendered diff table from this revision to the given
         *  <var>oldid</var>. See {@link #diff(Map, Map)} for full documentation.
         *
         *  @param oldid the oldid of a revision on the same page. {@link
         *  Wiki#NEXT_REVISION}, {@link Wiki#PREVIOUS_REVISION} and {@link
         *  Wiki#CURRENT_REVISION} can be used here for obvious effect.
         *  @return the difference between this and the other revision
         *  @throws IOException if a network error occurs
         *  @throws SecurityException if this or the other revision is
         *  RevisionDeleted and the user lacks the necessary privileges
         *  @since 0.26
         */
        public String diff(long oldid) throws IOException
        {
            Map<String, Object> from = new HashMap<>();
            from.put("revision", this);
            Map<String, Object> to = new HashMap<>();
            to.put("revid", oldid);
            return Wiki.this.diff(from, to);
        }

        /**
         *  Determines whether this Revision is equal to another based on the
         *  underlying {@linkplain Event#equals(Object) Event}.
         *  @param o an object
         *  @return whether o is equal to this object
         *  @since 0.17
         */
        @Override
        public boolean equals(Object o)
        {
            // Note to self: don't use SHA-1 until all API calls provide it.
            if (!super.equals(o))
                return false;
            if (!(o instanceof Revision))
                return false;
            // Revision rev = (Revision)o;
            // return Objects.equals(sha1, rev.sha1);
            return true;
        }

        /**
         *  Returns a hash code of this revision based on the underlying
         *  {@linkplain Event#hashCode() Event}.
         *  @return a hash code
         *  @since 0.17
         */
        @Override
        public int hashCode()
        {
            // Note to self: don't use SHA-1 until all API calls provide it.
            int hc = super.hashCode();
            hc = 127 * hc;
            return hc;
        }

        /**
         *  Checks whether this edit was marked as minor. See
         *  [[Help:Minor edit]] for details.
         *
         *  @return whether this revision was marked as minor
         *  @since 0.17
         */
        public boolean isMinor()
        {
            return minor;
        }

        /**
         *  Determines whether this revision was made by a bot.
         *  @return (see above)
         *  @since 0.23
         */
        public boolean isBot()
        {
            return bot;
        }

        /**
         *  Determines whether this revision created a new page.
         *
         *  <p><b>Warnings:</b>
         *  <ul>
         *  <li>Returning {@code true} does not imply this is the bottommost
         *  revision on the page due to histmerges.
         *  <li>Not available through {@link #getPageHistory(String, Wiki.RequestHelper)}
         *  </ul>
         *
         *  @return (see above)
         *  @since 0.27
         */
        public boolean isNew()
        {
            return rvnew;
        }

        /**
         *  Returns {@code true} if this revision is deleted (not the same as
         *  RevisionDeleted).
         *  @return (see above)
         *  @since 0.31
         */
        public boolean isPageDeleted()
        {
            return pageDeleted;
        }

        /**
         *  Gets the size of this revision in bytes.
         *  @return see above
         *  @since 0.25
         */
        public int getSize()
        {
            return size;
        }

        /**
         *  Returns the change in page size caused by this revision. Not available
         *  through getPageHistory or getDeletedHistory.
         *  @return see above
         *  @since 0.28
         */
        public int getSizeDiff()
        {
            return sizediff;
        }

        /**
         *  Returns a string representation of this revision.
         *  @return see above
         *  @since 0.17
         */
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.deleteCharAt(sb.length() - 1);
            sb.append(",minor=");
            sb.append(minor);
            sb.append(",bot=");
            sb.append(bot);
            sb.append(",size=");
            sb.append(size);
            sb.append(",rcid=");
            sb.append(rcid == -1 ? "unset" : rcid);
            sb.append(",previous=");
            sb.append(previous);
            sb.append(",next=");
            sb.append(next);
            sb.append("]");
            return sb.toString();
        }

        /**
         *  Gets the previous revision.
         *  @return the previous revision, or null if this is the first revision
         *  or this object was spawned via contribs().
         *  @throws IOException if a network error occurs
         *  @since 0.28
         */
        public Revision getPrevious() throws IOException
        {
            return previous == 0 ? null : getRevision(previous);
        }

        /**
         *  Gets the next revision.
         *  @return the next revision, or null if this is the last revision
         *  or this object was spawned via contribs().
         *  @throws IOException if a network error occurs
         *  @since 0.28
         */
        public Revision getNext() throws IOException
        {
            return next == 0 ? null : getRevision(next);
        }

        /**
         *  Sets the <var>rcid</var> of this revision, used for patrolling.
         *  This parameter is optional. This is publicly editable for
         *  subclassing.
         *  @param rcid the rcid of this revision (long)
         *  @since 0.17
         */
        public void setRcid(long rcid)
        {
            this.rcid = rcid;
        }

        /**
         *  Gets the <var>rcid</var> of this revision for patrolling purposes.
         *  @return the rcid of this revision (long)
         *  @since 0.17
         */
        public long getRcid()
        {
            return rcid;
        }

        /**
         *  Gets a permanent URL to the human-readable version of this Revision.
         *  This URL uses index.php, not Special:Permanentlink for ease of adding
         *  other parameters.
         *  @return (see above)
         *  @since 0.35
         */
        public String permanentUrl()
        {
            return getIndexPhpUrl() + "?oldid=" + getID();
        }

        /**
         *  Reverts this revision using the rollback method.
         *
         *  @throws IOException if a network error occurs
         *  @throws SecurityException if the user lacks the privileges to rollback
         *  @throws CredentialExpiredException if cookies have expired
         *  @throws AccountLockedException if the user is blocked
         *  @see Wiki#rollback(org.wikipedia.Wiki.Revision)
         *  @since 0.19
         */
        public void rollback() throws IOException, LoginException
        {
            Wiki.this.rollback(this, false, "");
        }

        /**
         *  Reverts this revision using the rollback method.
         *
         *  @param bot mark this and the reverted revision(s) as bot edits
         *  @param reason (optional) a custom reason
         *  @throws IOException if a network error occurs
         *  @throws SecurityException if the user lacks the privileges to rollback
         *  @throws CredentialExpiredException if cookies have expired
         *  @throws AccountLockedException if the user is blocked
         *  @see Wiki#rollback(org.wikipedia.Wiki.Revision)
         *  @since 0.19
         */
        public void rollback(boolean bot, String reason) throws IOException, LoginException
        {
            Wiki.this.rollback(this, bot, reason);
        }
    }

    /**
     *  Vehicle for stuffing standard optional parameters into Wiki queries.
     *  {@code RequestHelper} objects are reusable. The following example
     *  fetches articles from the back of the new pages queue on the
     *  English Wikipedia.
     *
     *  {@code <pre>
     *  Wiki.RequestHelper rh = enWiki.new RequestHelper()
     *      .inNamespaces(Wiki.MAIN_NAMESPACE)
     *      .reverse();
     *  List<Wiki.Revision> newpages = enWiki.newPages(rh);
     *  </pre>}
     *
     *  @since 0.36
     */
    public class RequestHelper
    {
        private String title;
        private String byuser;
        private OffsetDateTime earliest, latest;
        private int[] localns = new int[0];
        private boolean reverse = false;
        private String notbyuser;
        private String tag;
        private Map<String, Boolean> options;
        private String requestType;
        private int limit = -1;

        /**
         *  Creates a new RequestHelper.
         */
        public RequestHelper()
        {
        }

        /**
         *  Limits query results to Events occuring on the given title. If a
         *  query mandates a title parameter (e.g. {@link #getPageHistory(String,
         *  RequestHelper)}, don't use this. Use the parameter in the query
         *  method instead.
         *  @param title a page title
         *  @return this RequestHelper
         */
        public RequestHelper byTitle(String title)
        {
            this.title = (title == null) ? null : normalize(title);
            return this;
        }

        /**
         *  Limits query results to Events triggered by the given user. If a query
         *  mandates a user parameter (e.g. {@link #contribs(List, String, RequestHelper)},
         *  don't use this. Use the parameter in the query method instead.
         *  @param byuser some username or IP address
         *  @return this RequestHelper
         */
        public RequestHelper byUser(String byuser)
        {
            this.byuser = (byuser == null) ? null : normalize(byuser);
            return this;
        }

        /**
         *  Limit results to be within this date range.
         *  @param earliest the lower (earliest) date bound, use {@code null} to
         *  not set one
         *  @param latest the higher (latest) date bound, use {@code null} to
         *  not set one
         *  @throws IllegalArgumentException if {@code earliest.isAfter(latest)}
         *  @return this RequestHelper
         */
        public RequestHelper withinDateRange(OffsetDateTime earliest, OffsetDateTime latest)
        {
            if (earliest != null && latest != null && earliest.isAfter(latest))
                throw new IllegalArgumentException("Earliest date must be before latest date!");
            this.earliest = earliest;
            this.latest = latest;
            return this;
        }

        /**
         *  Limits query results to the given namespaces.
         *  @param ns a list of namespaces
         *  @return this RequestHelper
         */
        public RequestHelper inNamespaces(int... ns)
        {
            localns = ns;
            return this;
        }

        /**
         *  Should we perform this query in reverse order (earliest first).
         *  @param reverse whether to reverse this query
         *  @return this RequestHelper
         */
        public RequestHelper reverse(boolean reverse)
        {
            this.reverse = reverse;
            return this;
        }

        /**
         *  Limits query results to {@link Event Events} that have been tagged
         *  with the given tag.
         *  @param tag a change tag
         *  @return this RequestHelper
         */
        public RequestHelper taggedWith(String tag)
        {
            this.tag = tag;
            return this;
        }

        /**
         *  Limits query results to Events NOT triggered by the given user.
         *  @param notbyuser some username or IP address to exclude
         *  @return this RequestHelper
         */
        public RequestHelper notByUser(String notbyuser)
        {
            this.notbyuser = (notbyuser == null) ? null : normalize(notbyuser);
            return this;
        }

        /**
         *  Return no more than the given number of results. Overrides {@linkplain
         *  #setQueryLimit(int) global limits}. Supply a negative integer to use
         *  global limits.
         *  @param limit a positive integer
         *  @return this RequestHelper
         */
        public RequestHelper limitedTo(int limit)
        {
            this.limit = limit;
            return this;
        }

        /**
         *  Filters a set of returned results using the given options. Please
         *  check calling method documentation for supported options.
         *
         *  <p>
         *  When filtering revisions, available keys may include "minor", "top",
         *  "new", "bot", "anon", "redirect", "patrolled" and "unread" for
         *  vanilla MediaWiki. Extensions may define their own. For instance,
         *  {@code rcoptions = { minor = true, anon = false,  patrolled = false}}
         *  returns all minor edits from logged in users that aren't patrolled.
         *  Setting "patrolled" limits results to no older than <a
         *  href="https://mediawiki.org/wiki/Manual:$wgRCMaxAge">retention</a> in
         *  the <a href="https://mediawiki.org/wiki/Manual:Recentchanges_table">recentchanges
         *  table</a>.
         *
         *  @param options the options to filter by
         *  @return this RequestHelper
         */
        public RequestHelper filterBy(Map<String, Boolean> options)
        {
            this.options = options;
            return this;
        }

        /**
         *  Sets the prefix of API request parameters (the XX in XXlimit, XXdir,
         *  XXnamespace and so forth). Internal use only.
         *  @param prefix the prefix to use (must not be null)
         */
        protected void setRequestType(String prefix)
        {
            requestType = Objects.requireNonNull(prefix);
        }

        /**
         *  Returns a HTTP request parameter containing the title to get
         *  events for, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addTitleParameter()
        {
            if (title != null)
                return Map.of(requestType + "title", title);
            return Collections.emptyMap();
        }

        /**
         *  Returns a HTTP request parameter containing the user to filter
         *  returned events by, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addUserParameter()
        {
            if (byuser != null)
                return Map.of(requestType + "user", byuser);
            return Collections.emptyMap();
        }

        /**
         *  Returns a HTTP request parameter containing the dates to start
         *  and end enumeration, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addDateRangeParameters()
        {
            // https://phabricator.wikimedia.org/T16449
            Map<String, String> temp = new HashMap<>();
            OffsetDateTime odt = reverse ? earliest : latest;
            if (odt != null)
                temp.put(requestType + "start",
                    // https://www.mediawiki.org/wiki/Timestamp
                    // https://github.com/MER-C/wiki-java/issues/170
                    odt.withOffsetSameInstant(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.MICROS)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            odt = reverse ? latest : earliest;
            if (odt != null)
                temp.put(requestType + "end",
                    // https://www.mediawiki.org/wiki/Timestamp
                    // https://github.com/MER-C/wiki-java/issues/170
                    odt.withOffsetSameInstant(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.MICROS)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            return temp;
        }

        /**
         *  Returns a HTTP request parameter containing the namespaces to limit
         *  this query to, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addNamespaceParameter()
        {
            if (localns.length != 0)
                return Map.of(requestType + "namespace", constructNamespaceString(localns));
            return Collections.emptyMap();
        }

        /**
         *  Returns a HTTP request parameter instructing the API to reverse the
         *  query, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addReverseParameter()
        {
            return Map.of(requestType + "dir", reverse ? "newer" : "older");
        }

        /**
         *  Returns a HTTP request parameter containing the tag to limit
         *  returned events to, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addTagParameter()
        {
            if (tag != null)
                return Map.of(requestType + "tag", tag);
            return Collections.emptyMap();
        }

        /**
         *  Returns a HTTP request parameter containing the user to exclude
         *  when returning events, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addExcludeUserParameter()
        {
            if (notbyuser != null)
                return Map.of(requestType + "excludeuser", notbyuser);
            return Collections.emptyMap();
        }

        /**
         *  Returns HTTP request parameter(s) containing flags to filter returned
         *  revisions by, or an empty map if not wanted.
         *  @return (see above)
         */
        protected Map<String, String> addShowParameter()
        {
            Map<String, String> temp = new HashMap<>();
            if (options != null && !options.isEmpty())
            {
                // deal with MW API annoyance for action=query&list=watchlist - see watchlist(rh)
                Boolean top = null;
                if (requestType.equals("wl"))
                {
                    top = options.remove("top");
                    if (Boolean.TRUE.equals(top))
                        temp.put("wlallrev", "1");
                }

                StringBuilder sb = new StringBuilder();
                options.forEach((key, value) ->
                {
                    if (Boolean.FALSE.equals(value))
                        sb.append('!');
                    sb.append(key);
                    sb.append("|");
                });
                temp.put(requestType + "show", sb.substring(0, sb.length() - 1));

                if (top != null) // put it back
                    options.put("top", top);
            }
            return temp;
        }

        /**
         *  Returns the number of results the query should be limited to. If not
         *  present, use {@linkplain #setQueryLimit(int) global limits}.
         *  @return (see above)
         */
        public int limit()
        {
            return limit;
        }
    }

    // INTERNALS

    /**
     *  Performs a vectorized <samp>action=query&amp;prop=X</samp> type API query 
     *  over titles.
     *  @param queryPrefix the request type prefix (e.g. "pl" for prop=links)
     *  @param getparams a bunch of parameters to send via HTTP GET
     *  @param titles a list of titles
     *  @param caller the name of the calling method
     *  @param limit fetch no more than this many results
     *  @param parser a BiConsumer that parses the XML returned by the MediaWiki
     *  API into things we want, dumping them into the given List
     *  @return a list of results, where each element corresponds to the element
     *  at the same index in the input title list
     *  @since 0.36
     *  @throws IOException if a network error occurs
     */
    protected List<List<String>> makeVectorizedQuery(String queryPrefix, Map<String, String> getparams,
        List<String> titles, String caller, int limit, BiConsumer<String, List<String>> parser) throws IOException
    {
        // copy because normalization and redirect resolvers overwrite
        List<String> titles2 = new ArrayList<>(titles);
        List<Map<String, List<String>>> stuff = new ArrayList<>();
        Map<String, Object> postparams = new HashMap<>();
        for (String temp : constructTitleString(titles2))
        {
            postparams.put("titles", temp);
            stuff.addAll(makeListQuery(queryPrefix, getparams, postparams, caller, -1, (line, results) ->
            {
                // Split the result into individual listings for each article.
                String[] x = line.split("<page ");
                resolveNormalizedParser(titles2, x[0]);
                if (resolveredirect)
                    resolveRedirectParser(titles2, x[0]);

                // Skip first element to remove front crud.
                for (int i = 1; i < x.length; i++)
                {
                    String parsedtitle = parseAttribute(x[i], "title", 0);
                    List<String> list = new ArrayList<>();
                    parser.accept(x[i], list);

                    Map<String, List<String>> intermediate = new HashMap<>();
                    intermediate.put(parsedtitle, list);
                    results.add(intermediate);
                }
            }));
        }

        // prepare the return list
        List<List<String>> ret = Stream.generate(() -> new ArrayList<String>())
            .limit(titles2.size())
            .collect(Collectors.toCollection(ArrayList::new));
        // then retrieve the results from the intermediate list of maps,
        // ensuring results correspond to inputs
        stuff.forEach(map ->
        {
            String parsedtitle = map.keySet().iterator().next();
            List<String> templates = map.get(parsedtitle);
            for (int i = 0; i < titles2.size(); i++)
                if (titles2.get(i).equals(parsedtitle))
                    ret.get(i).addAll(templates);
        });
        return ret;
    }

    /**
     *  Fetches list-type results from the MediaWiki API.
     *
     *  @param <T> a class describing the parsed API results (e.g. String,
     *  LogEntry, Revision)
     *  @param queryPrefix the request type prefix (e.g. "pl" for prop=links)
     *  @param getparams a bunch of parameters to send via HTTP GET
     *  @param postparams if not null, send these parameters via POST (see
     *  {@link #makeApiCall(Map, Map, String) }).
     *  @param caller the name of the calling method
     *  @param limit fetch no more than this many results
     *  @param parser a BiConsumer that parses the XML returned by the MediaWiki
     *  API into things we want, dumping them into the given List
     *  @return the query results
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we don't have the credentials to perform a
     *  privileged action (mostly avoidable)
     *  @since 0.34
     */
    protected <T> List<T> makeListQuery(String queryPrefix, Map<String, String> getparams,
        Map<String, Object> postparams, String caller, int limit, BiConsumer<String, List<T>> parser) throws IOException
    {
        if (limit < 0)
            limit = querylimit;
        getparams.put("action", "query");
        List<T> results = new ArrayList<>(1333);
        String limitstring = queryPrefix + "limit";
        do
        {
            getparams.put(limitstring, String.valueOf(Math.min(limit - results.size(), max)));
            String line = makeApiCall(getparams, postparams, caller);
            detectUncheckedErrors(line, null, null);
            getparams.keySet().removeIf(param -> param.endsWith("continue"));

            // Continuation parameter has form:
            // <continue rccontinue="20170924064528|986351741" continue="-||" />
            if (line.contains("<continue "))
            {
                int a = line.indexOf("<continue ") + 9;
                int b = line.indexOf(" />", a);
                String cont = line.substring(a, b);
                for (String contpair : cont.split("\" "))
                {
                    contpair = " " + contpair.trim();
                    String contattr = contpair.substring(0, contpair.indexOf("=\""));
                    getparams.put(contattr.trim(), parseAttribute(cont, contattr, 0));
                }
            }

            parser.accept(line, results);
        }
        while (getparams.containsKey("continue") && results.size() < limit);
        return results;
    }

    // miscellany
    
    /**
     *  Convenience method for checking user permissions.
     *  @param right a user rights
     *  @param morerights additional user rights
     *  @throws SecurityException if the permission check fails
     *  @since 0.37
     */
    protected void checkPermissions(String action, String right, String... morerights)
    {
        if (user == null)
            throw new SecurityException("Cannot " + action + ": not logged in.");
        if (!user.isAllowedTo(right, morerights))
            throw new SecurityException("Cannot " + action + ": permission denied.");
    }
            
    /**
     *  Constructs, sends and handles calls to the MediaWiki API. This is a
     *  low-level method for making your own, custom API calls.
     *
     *  <p>
     *  If <var>postparams</var> is not {@code null} or empty, the request is
     *  sent using HTTP GET, otherwise it is sent using HTTP POST. A
     *  {@code byte[]} value in <var>postparams</var> causes the request to be
     *  sent as a multipart POST. Anything else is converted to String via the
     *  following means:
     *
     *  <ul>
     *  <li>String[] -- {@code String.join("|", arr)}
     *  <li>StringBuilder -- {@code sb.toString()}
     *  <li>Number -- {@code num.toString()}
     *  <li>OffsetDateTime -- {@code date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}
     *  <li>{@code Collection<?>} -- {@code coll.stream()
     *      .map(item -> convertToString(item)) // using the above rules
     *      .collect(Collectors.joining("|"))}
     *  </ul>
     *
     *  <p>
     *  All supplied Strings and objects converted to String are automatically
     *  URLEncoded in UTF-8 if this is a normal POST request.
     *
     *  <p>
     *  Here we also check the database lag and wait if it exceeds
     *  <var>maxlag</var>, see <a href="https://mediawiki.org/wiki/Manual:Maxlag_parameter">
     *  here</a> for how this works.
     *
     *  @param getparams append these parameters to the urlbase
     *  @param postparams if null, send the request using POST otherwise use GET
     *  @param caller the caller of this method
     *  @return the server response
     *  @throws IOException if a network error occurs
     *  @throws SecurityException if we don't have the credentials to perform a
     *  privileged action (mostly avoidable)
     *  @throws AssertionError if assert=user|bot fails
     *  @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2">Multipart/form-data</a>
     *  @since 0.18
     */
    public String makeApiCall(Map<String, String> getparams, Map<String, Object> postparams, String caller) throws IOException
    {
        // build the URL
        StringBuilder urlbuilder = new StringBuilder(apiUrl + "?");
        getparams.putAll(defaultApiParams);
        for (Map.Entry<String, String> entry : getparams.entrySet())
        {
            urlbuilder.append('&');
            urlbuilder.append(entry.getKey());
            urlbuilder.append('=');
            urlbuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        String url = urlbuilder.toString();

        // POST stuff
        boolean isPOST = (postparams != null && !postparams.isEmpty());
        StringBuilder stringPostBody = new StringBuilder();
        boolean multipart = false;
        ByteArrayOutputStream multipartPostBody = new ByteArrayOutputStream();
        String boundary = "----------NEXT PART----------";        
        if (isPOST)
        {
            // determine whether this is a multipart post and convert any values
            // to String if necessary
            for (Map.Entry<String, Object> entry : postparams.entrySet())
            {
                Object value = entry.getValue();
                if (value instanceof byte[])
                    multipart = true;
                else
                    entry.setValue(convertToString(value));
            }

            // now we know how we're sending it, construct the post body
            if (multipart)
            {        
                String nextpart = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"";
                for (Map.Entry<String, ?> entry : postparams.entrySet())
                {
                    Object value = entry.getValue();
                    multipartPostBody.write((nextpart + entry.getKey() + "\"").getBytes(StandardCharsets.UTF_8));
                    if (value instanceof String)
                        multipartPostBody.write(("Content-Type: text/plain; charset=UTF-8\r\n\r\n" + (String)value + "\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    else if (value instanceof byte[])
                    {
                        multipartPostBody.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                        multipartPostBody.write((byte[])value);
                        multipartPostBody.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
                multipartPostBody.write((nextpart + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            else
            {
                // automatically encode Strings sent via normal POST
                for (Map.Entry<String, Object> entry : postparams.entrySet())
                {
                    stringPostBody.append('&');
                    stringPostBody.append(entry.getKey());
                    stringPostBody.append('=');
                    stringPostBody.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
                }
            }
        }

        // main fetch/retry loop
        String response = null;
        int tries = maxtries;
        do
        {
            logurl(url, caller);
            tries--;
            try
            {
                var connection = makeConnection(url);
                if (isPOST)
                {
                    if (multipart)
                        connection = connection.POST(HttpRequest.BodyPublishers.ofByteArray(multipartPostBody.toByteArray()))
                            .header("Content-Type", "multipart/form-data; boundary=" + boundary);
                    else
                        connection = connection.POST(HttpRequest.BodyPublishers.ofString(stringPostBody.toString()))
                            .header("Content-Type", "application/x-www-form-urlencoded");
                }

                HttpResponse<InputStream> hr = client.send(connection.build(), HttpResponse.BodyHandlers.ofInputStream());
                boolean zipped_ = hr.headers().firstValue("Content-Encoding").orElse("").equals("gzip");
                if (checkLag(hr))
                {
                    tries++;
                    throw new HttpRetryException("Database lagged.", 503);
                }

                try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    zipped_ ? new GZIPInputStream(hr.body()) : hr.body(), "UTF-8")))
                {
                    response = in.lines().collect(Collectors.joining("\n"));
                }

                // Check for rate limit (though might be a long one e.g. email)
                if (response.contains("error code=\"ratelimited\""))
                {
                    // the Retry-After header field is useless here
                    // see https://phabricator.wikimedia.org/T172293
                    log(Level.WARNING, caller, "Server-side throttle hit.");
                    Thread.sleep(10000);
                    throw new HttpRetryException("Action throttled.", 503);
                }
                // Check for database lock
                if (response.contains("error code=\"readonly\""))
                {
                    log(Level.WARNING, caller, "Database locked!");
                    Thread.sleep(10000);
                    throw new HttpRetryException("Database locked!", 503);
                }

                // No need to retry anymore, success or unrecoverable failure.
                tries = 0;
            }
            catch (IOException ex)
            {
                // Exception deliberately ignored until retries are depleted.
                if (tries == 0)
                    throw ex;
            }
            catch (InterruptedException ignored)
            {
            }
        }
        while (tries != 0);
    
        // empty response from server
        if (response.isEmpty())
            throw new UnknownError("Received empty response from server!");
        return response;
    }

    /**
     *  Converts HTTP POST parameters to Strings. See {@link #makeApiCall(Map,
     *  Map, String)} for the description.
     *  @param param the parameter to convert
     *  @return that parameter, as a String
     *  @throws UnsupportedOperationException if param is not a supported data type
     *  @since 0.35
     */
    private String convertToString(Object param)
    {
        // TODO: Replace with type switch in JDK 11/12
        if (param instanceof String)
            return (String)param;
        else if (param instanceof StringBuilder || param instanceof Number)
            return param.toString();
        else if (param instanceof String[])
            return String.join("|", (String[])param);
        else if (param instanceof OffsetDateTime)
        {
            OffsetDateTime date = (OffsetDateTime)param;
            // https://www.mediawiki.org/wiki/Timestamp
            // https://github.com/MER-C/wiki-java/issues/170
            return date.atZoneSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        else if (param instanceof Collection)
        {
            Collection<?> coll = (Collection)param;
            return coll.stream()
                .map(item -> convertToString(item))
                .collect(Collectors.joining("|"));
        }
        else
            throw new UnsupportedOperationException("Unrecognized data type");
    }

    /**
     *  Checks for database lag and sleeps if {@code lag < getMaxLag()}.
     *  @param response the HTTP response received
     *  @return true if there was sufficient database lag.
     *  @throws InterruptedException if any wait was interrupted
     *  @see #getMaxLag()
     *  @see <a href="https://mediawiki.org/wiki/Manual:Maxlag_parameter">
     *  MediaWiki documentation</a>
     *  @since 0.32
     */
    protected synchronized boolean checkLag(HttpResponse response) throws InterruptedException
    {
        HttpHeaders hdrs = response.headers();
        long lag = hdrs.firstValueAsLong("X-Database-Lag").orElse(-5);
        // X-Database-Lag is the current lag rounded down to the nearest integer.
        // Thus, we need to retry in case of equality.
        if (lag >= maxlag)
        {
            long time = hdrs.firstValueAsLong("Retry-After").orElse(10);
            logger.log(Level.WARNING, "Current database lag {0} s exceeds maxlag of {1} s, waiting {2} s.", new Object[] { lag, maxlag, time });
            Thread.sleep(time * 1000L);
            return true;
        }
        return false;
    }

    /**
     *  Sets the HTTPClient used by this instance. Use this to set a proxy, 
     *  SSL parameters and the connection timeout.
     *  @param builder a HTTP request builder
     *  @since 0.37
     */
    public void setHttpClient(HttpClient.Builder builder)
    {
        client = builder.cookieHandler(cookies).build();
    }

    /**
     *  Creates a new HTTP request. Override to change request properties.
     *  @param url a URL string
     *  @return a HTTP request builder for that URL
     *  @throws IOException if a network error occurs
     *  @since 0.31
     */
    protected HttpRequest.Builder makeConnection(String url) throws IOException
    {
        return HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMillis(read_timeout_msec))
            .header("User-Agent", useragent)
            .header("Accept-encoding", "gzip");
    }

    /**
     *  Checks for errors from standard read/write requests and throws the
     *  appropriate unchecked exception.
     *
     *  @param response the response from the server to analyze
     *  @param errors additional errors to check for where throwing an unchecked
     *  exception is the desired behavior (function is of MediaWiki error 
     *  message)
     *  @param warnings additional errors to check for where throwing an exception
     *  is not required (function is of MediaWiki error message)
     *  @throws RuntimeException or subclasses depending on the type of errors
     *  checked for
     *  @return whether the action was successful
     *  @since 0.37
     */
    protected boolean detectUncheckedErrors(String response, Map<String, Function<String, ? extends RuntimeException>> errors,
        Map<String, Consumer<String>> warnings)
    {
        if (response.contains("<error code="))
        {
            String error = parseAttribute(response, "code", 0);
            String description = parseAttribute(response, "info", 0);
            if (errors != null && errors.containsKey(error))
                throw errors.get(error).apply(description);
            if (warnings != null && warnings.containsKey(error))
            {
                warnings.get(error).accept(description);
                return false;
            }
            switch (error)
            {
                case "assertbotfailed":
                case "assertuserfailed":
                    throw new AssertionError(description);
                case "permissiondenied":
                    throw new SecurityException(description);
                // Something *really* bad happened. Most of these are self-explanatory
                // and are indicative of bugs (not necessarily in this framework) or
                // can be avoided entirely. Others are kicked to the caller to handle.
                default:
                    throw new UnknownError("MW API error. Server response was: " + response);
            }
        }
        return true;
    }
    
    /**
     *  Checks for errors from standard read/write requests and performs
     *  occasional status checks.
     *
     *  @param line the response from the server to analyze
     *  @param caller what we tried to do
     *  @param uncheckederrors additional errors to check for where throwing an
     *  unchecked exception is the desired behavior (function is of MediaWiki 
     *  error message)
     *  @param info additional errors to check for where throwing an exception
     *  is not required (function is of MediaWiki error message)
     *  @throws CredentialException if the page is protected
     *  @throws AccountLockedException if the user is blocked
     *  @throws AssertionError if assertions fail
     *  @throws RuntimeException if any 
     *  @throws IOException if a network error occurs
     *  @throws UnknownError in the case of a MediaWiki bug
     *  @return whether the action was successful
     *  @since 0.18
     */
    protected boolean checkErrorsAndUpdateStatus(String line, String caller, 
        Map<String, Function<String, ? extends RuntimeException>> uncheckederrors,
        Map<String, Consumer<String>> info) throws IOException, LoginException
    {
        // perform various status checks every 100 or so edits
        if (statuscounter > statusinterval)
        {
            // purge user rights in case of desysop or loss of other priviliges
            user = getUsers(List.of(user.getUsername())).get(0);
            if ((assertion & ASSERT_SYSOP) == ASSERT_SYSOP && !user.isA("sysop"))
                // assert user.isA("sysop") : "Sysop privileges missing or revoked, or session expired";
                throw new AssertionError("Sysop privileges missing or revoked, or session expired");
            // check for new messages
            if ((assertion & ASSERT_NO_MESSAGES) == ASSERT_NO_MESSAGES && hasNewMessages())
                // assert !hasNewMessages() : "User has new messages";
                throw new AssertionError("User has new messages");
            statuscounter = 0;
        }
        else
            statuscounter++;

        if (!line.contains("<error code=\""))
            return true;
        String error = parseAttribute(line, "code", 0);
        String description = parseAttribute(line, "info", 0);
        switch (error)
        {
            // protected pages
            case "protectedpage":
            case "protectedtitle":
            case "protectednamespace":
            case "protectednamespace-interface":
            case "immobilenamespace":
            case "customcssprotected":
            case "customjsprotected":
            case "customcssjsprotected":
            case "cascadeprotected":
            case "fileexists-shared-forbidden":
                throw new CredentialException(description);
            // banned accounts
            case "blocked":
            case "blockedfrommail":
            case "autoblocked":
                throw new AccountLockedException(description);
            // upload errors
            case "copyuploadbaddomain":
                throw new AccessDeniedException(description);
            case "http-bad-status":
                throw new IOException("Server-side network error when fetching image: " + line);
        }
        return detectUncheckedErrors(line, uncheckederrors, info);
    }

    /**
     *  Strips entity references like &quot; from the supplied string. This
     *  might be useful for subclasses.
     *  @param in the string to remove URL encoding from
     *  @return that string without URL encoding
     *  @since 0.11
     */
    protected String decode(String in)
    {
        // Remove entity references. Oddly enough, URLDecoder doesn't nuke these.
        in = in.replace("&lt;", "<").replace("&gt;", ">"); // html tags
        in = in.replace("&quot;", "\"");
        in = in.replace("&#039;", "'");
        in = in.replace("&amp;", "&");
        return in;
    }

    /**
     *  Parses the next XML attribute with the given name.
     *  @param xml the xml to search
     *  @param attribute the attribute to search
     *  @param index where to start looking
     *  @return the value of the given XML attribute, or null if the attribute
     *  is not present
     *  @since 0.28
     */
    protected String parseAttribute(String xml, String attribute, int index)
    {
        // let's hope the JVM always inlines this
        if (xml.contains(attribute + "=\""))
        {
            int a = xml.indexOf(attribute + "=\"", index) + attribute.length() + 2;
            int b = xml.indexOf('\"', a);
            return decode(xml.substring(a, b));
        }
        else
            return null;
    }

    /**
     *  Convenience method for converting a namespace list into String form.
     *  Negative namespace numbers are removed.
     *  @param ns the list of namespaces to append
     *  @return the namespace list in String form
     *  @since 0.27
     */
    protected String constructNamespaceString(int[] ns)
    {
        return Arrays.stream(ns)
            .distinct()
            .filter(namespace -> namespace >= 0)
            .sorted()
            .mapToObj(String::valueOf)
            .collect(Collectors.joining("|"));
    }

    /**
     *  Cuts up a list of revisions into batches for prop=X&amp;ids=Y type
     *  queries. IDs less than 0 are ignored.
     *  @param ids a list of revision IDs
     *  @return the revisions ready for insertion into a URL
     *  @since 0.32
     */
    protected List<String> constructRevisionString(long[] ids)
    {
        // sort and remove duplicates per https://mediawiki.org/wiki/API
        String[] sortedids = Arrays.stream(ids)
            .distinct()
            .filter(id -> id >= 0)
            .sorted()
            .mapToObj(String::valueOf)
            .toArray(String[]::new);

        StringBuilder buffer = new StringBuilder();
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < sortedids.length; i++)
        {
            buffer.append(sortedids[i]);
            if (i == ids.length - 1 || (i % slowmax) == slowmax - 1)
            {
                chunks.add(buffer.toString());
                buffer.setLength(0);
            }
            else
                buffer.append('|');
        }
        return chunks;
    }

    /**
     *  Cuts up a list of titles into batches for prop=X&amp;titles=Y type queries.
     *  @param titles a list of titles.
     *  @return the titles ready for insertion into a URL
     *  @since 0.29
     */
    protected List<String> constructTitleString(List<String> titles)
    {
        // sort and remove duplicates
        List<String> titles_unique = titles.stream().sorted().distinct().collect(Collectors.toList());
        
        // actually construct the string
        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < titles_unique.size() / slowmax + 1; i++)
        {
            ret.add(String.join("|", 
            		titles_unique.subList(i * slowmax, Math.min(titles_unique.size(), (i + 1) * slowmax))));     
        }
        return ret;
    }

    /**
     *  Convenience method for normalizing MediaWiki titles. (Converts all
     *  underscores to spaces, localizes namespace names, fixes case of first
     *  char and does some other unicode fixes). Beware that this will not
     *  produce the same results as server-side normalization in a few corner
     *  cases, most notably: HTML entities and gender distinction in the user
     *  namespace prefix.
     *  @param s the string to normalize
     *  @return the normalized string
     *  @throws IllegalArgumentException if the title is invalid
     *  @throws UncheckedIOException if the namespace cache has not been
     *  populated, and a network error occurs when populating it
     *  @since 0.27
     */
    public String normalize(String s)
    {
        // remove section names
        if (s.contains("#"))
            s = s.substring(0, s.indexOf("#"));
        // remove leading colon
        if (s.startsWith(":"))
            s = s.substring(1);
        s = s.replace('_', ' ').trim();
        if (s.isEmpty())
            throw new IllegalArgumentException("Empty or whitespace only title.");

        int ns = namespace(s);
        // localize namespace names
        if (ns != MAIN_NAMESPACE)
        {
            int colon = s.indexOf(':');
            s = namespaceIdentifier(ns) + s.substring(colon);
        }
        char[] temp = s.toCharArray();
        if (wgCapitalLinks)
        {
            // convert first character in the actual title to upper case
            if (ns == MAIN_NAMESPACE)
                temp[0] = Character.toUpperCase(temp[0]);
            else
            {
                int index = namespaceIdentifier(ns).length() + 1; // + 1 for colon
                temp[index] = Character.toUpperCase(temp[index]);
            }
        }

        for (int i = 0; i < temp.length; i++)
        {
            switch (temp[i])
            {
                // illegal characters
                case '{':
                case '}':
                case '<':
                case '>':
                case '[':
                case ']':
                case '|':
                    throw new IllegalArgumentException(s + " is an illegal title");
            }
        }
        // https://mediawiki.org/wiki/Unicode_normalization_considerations
        String temp2 = new String(temp).replaceAll("\\s+", " ");
        return Normalizer.normalize(temp2, Normalizer.Form.NFC);
    }

    /**
     *  Ensures no less than <var>throttle</var> milliseconds pass between edits
     *  and other write actions.
     *  @since 0.30
     */
    protected synchronized void throttle()
    {
        try
        {
            long time = throttle - System.currentTimeMillis() + lastThrottleActionTime;
            if (time > 0)
                Thread.sleep(time);
        }
        catch (InterruptedException ignored)
        {
        }
        this.lastThrottleActionTime = System.currentTimeMillis();
    }

    // user rights methods

    /**
     *  Checks whether the currently logged on user has sufficient rights to
     *  edit/move a protected page.
     *
     *  @param pageinfo the output from {@link #getPageInfo} for an article
     *  @param action what we are doing
     *  @return whether the user can perform the specified action
     *  @throws IOException if a network error occurs
     *  @since 0.10
     */
    protected boolean checkRights(Map<String, Object> pageinfo, String action) throws IOException
    {
        Map<String, Object> protectionstate = (Map<String, Object>)pageinfo.get("protection");
        if (protectionstate.containsKey(action))
        {
            String level = (String)protectionstate.get(action);
            if (level.equals(SEMI_PROTECTION))
                return user.isAllowedTo("autoconfirmed");
            if (level.equals(FULL_PROTECTION))
                return user.isAllowedTo("editprotected");
        }
        if (Boolean.TRUE.equals(protectionstate.get("cascade")))
            return user.isAllowedTo("editprotected");
        return true;
    }

    // logging methods

    /**
     *  Logs a successful result.
     *  @param text string the string to log
     *  @param method what we are currently doing
     *  @param level the level to log at
     *  @since 0.06
     */
    protected void log(Level level, String method, String text)
    {
        logger.logp(level, "Wiki", method, "[{0}] {1}", new Object[] { domain, text });
    }

    /**
     *  Logs a url fetch.
     *  @param url the url we are fetching
     *  @param method what we are currently doing
     *  @since 0.08
     */
    protected void logurl(String url, String method)
    {
        logger.logp(Level.INFO, "Wiki", method, "Fetching URL {0}", url);
    }
}
