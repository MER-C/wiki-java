/**
 *  @(#)ParserUtils.java 0.02 23/12/2016
 *  Copyright (C) 2012-2017 MER-C
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

import java.time.format.DateTimeFormatter;
import java.util.*;
import org.wikipedia.servlets.ServletUtils;

/**
 *  Various parsing methods that e.g. turning Wiki.java objects into wikitext 
 *  and HTML and vice versa.
 *  @author MER-C
 *  @version 0.02
 */
public class ParserUtils
{
    /**
     *  Standard markup for RevDeleted stuff. (The class name, <tt>history-deleted</tt>
     *  comes from MediaWiki.)
     */
    private static final String DELETED = "<span class=\"history-deleted\">deleted</span>";
    
    /**
     *   Parses a list of links into its individual elements. Such a list
     *   should be in the form:
     *
     *  <pre>
     *  * [[Main Page]]
     *  * [[Wikipedia:Featured picture candidates]]
     *  * [[:File:Example.png]]
     *  * [[Cape Town#Economy]]
     *  </pre>
     *
     *  in which case <tt>{ "Main Page", "Wikipedia:Featured picture
     *  candidates", "File:Example.png", "Cape Town#Economy" }</tt> is the 
     *  return value. Numbered lists are allowed. Nested lists are flattened. 
     *
     *  @param list a list of pages
     *  @see #formatList
     *  @return an array of the page titles
     *  @since Wiki.java 0.11
     */
    public static String[] parseList(String list)
    {
        StringTokenizer tokenizer = new StringTokenizer(list, "[]");
        ArrayList<String> titles = new ArrayList<>(667);
        tokenizer.nextToken(); // skip the first token
        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();
            // skip any containing new lines or double letters
            if (token.contains("\n") || token.isEmpty())
                continue;
            
            // trim the starting colon, if present
            if (token.startsWith(":"))
                token = token.substring(1);
            titles.add(token);
        }
        return titles.toArray(new String[titles.size()]);
    }

    /**
     *  Formats a list of pages, say, generated from one of the query methods
     *  into something that would be editor-friendly. Does the exact opposite
     *  of <tt>parseList()</tt>, i.e. { "Main Page", "Wikipedia:Featured
     *  picture candidates", "File:Example.png" } becomes the string:
     *
     *  <pre>
     *  *[[:Main Page]]
     *  *[[:Wikipedia:Featured picture candidates]]
     *  *[[:File:Example.png]]
     *  </pre>
     *
     *  @param pages an array of page titles
     *  @return see above
     *  @see #parseList
     *  @since Wiki.java 0.14
     */
    public static String formatList(String[] pages)
    {
        StringBuilder buffer = new StringBuilder(10000);
        for (String page : pages)
        {
            buffer.append("*[[:");
            buffer.append(page);
            buffer.append("]]\n");
        }
        return buffer.toString();
    }

    /**
     *  Turns a list of revisions into human-readable wikitext. Be careful, as
     *  slowness may result when copying large amounts of wikitext produced by
     *  this method, or by the wiki trying to parse it. Takes the form of:
     *
     *  <p>*(diff link) 2009-01-01 00:00 User (talk | contribs) (edit summary)
     *  @param wiki the parent wiki
     *  @param revisions a list of revisions
     *  @return those revisions as wikitext
     *  @since Wiki.java 0.20
     */
    public static String revisionsToWikitext(Wiki wiki, Wiki.Revision[] revisions)
    {
        StringBuilder buffer = new StringBuilder(revisions.length * 100);
        buffer.append("<div style=\"font-family: monospace; font-size: 120%\">\n");
        for (Wiki.Revision rev : revisions)
        {
            // timestamp, link to oldid
            buffer.append("*");
            buffer.append("[[Special:Permanentlink/");
            buffer.append(rev.getRevid());
            buffer.append("|");
            buffer.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp()));
            buffer.append("]] ");
            
            // diff link
            buffer.append("([[Special:Diff/");
            buffer.append(rev.getRevid());
            buffer.append("|diff]]) ");
            
            if (rev.isNew())
                buffer.append("'''N''' ");
            else
                buffer.append(". ");
            if (rev.isMinor())
                buffer.append("'''m''' ");
            else
                buffer.append(". ");
            if (rev.isBot())
                buffer.append("'''b''' ");
            else
                buffer.append(". ");
            
            buffer.append("[[");
            buffer.append(rev.getPage());
            buffer.append("]] .. ");
            
            // user
            String user2 = rev.getUser();
            if (user2 != null)
            {
                buffer.append("[[User:");
                buffer.append(user2);
                buffer.append("|");
                buffer.append(user2);
                buffer.append("]] ([[User talk:");
                buffer.append(user2);
                buffer.append("|talk]] | [[Special:Contributions/");
                buffer.append(user2);
                buffer.append("|contribs]])");
            }
            else
                buffer.append(DELETED);
            
            // size
            buffer.append(" .. (");
            buffer.append(rev.getSize());
            buffer.append(" bytes) (");
            buffer.append(rev.getSizeDiff());
            
            // edit summary
            buffer.append(") .. (");
            String summary = rev.getSummary();
            if (summary == null)
                buffer.append(DELETED);
            // kill wikimarkup
            buffer.append("<nowiki>");
            buffer.append(summary);
            buffer.append("</nowiki>)\n");
        }
        buffer.append("</div>");
        return buffer.toString();
    }
    
    /**
     *  Generates HTML from Wiki.Revisions. Output is similar to parsed wikitext
     *  returned by <tt>revisionsToWikitext()</tt>.
     *  @param wiki the parent wiki of the revisions
     *  @param revisions the revisions to convert
     *  @return (see above)
     *  @see #revisionsToWikitext(Wiki wiki, Wiki.Revision[] revisions)
     */
    public static String revisionsToHTML(Wiki wiki, Wiki.Revision[] revisions)
    {
        StringBuilder buffer = new StringBuilder(100000);
        buffer.append("<ul class=\"htmlrevisions\">\n");
        boolean colored = true;
        for (Wiki.Revision rev : revisions)
        {         
            // alternate background color for readability
            if (colored)
                buffer.append("<li class=\"shaded\">(");
            else
                buffer.append("<li>(");
            colored = !colored;
            
            // diff link
            String page = recode(rev.getPage());
            StringBuilder temp2 = new StringBuilder("<a href=\"");
            temp2.append(wiki.base);
            temp2.append(page.replace(' ', '_'));
            temp2.append("&oldid=");
            temp2.append(rev.getRevid());
            buffer.append(temp2);
            buffer.append("&diff=prev\">prev</a>) ");
            
            // date
            buffer.append(temp2);
            buffer.append("\">");
            buffer.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp()));
            buffer.append("</a> ");
            
            if (rev.isNew())
                buffer.append("<b>N</b>");
            else
                buffer.append(".");
            if (rev.isMinor())
                buffer.append("<b>m</b>");
            else
                buffer.append(".");
            if (rev.isBot())
                buffer.append("<b>b</b> ");
            else
                buffer.append(". ");
            
            // pages never contain XSS characters
            buffer.append("<a href=\"//");
            buffer.append(wiki.getDomain());
            buffer.append("/wiki/");
            buffer.append(page.replace(' ', '_'));
            buffer.append("\" class=\"pagename\">");
            buffer.append(page);
            buffer.append("</a> .. ");
            
            // usernames never contain XSS characters
            String temp = recode(rev.getUser());
            if (temp != null)
            {
                buffer.append("<a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/User:");
                buffer.append(temp);
                buffer.append("\">");
                buffer.append(temp);
                buffer.append("</a> (<a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/User talk:");
                buffer.append(temp);
                buffer.append("\">talk</a> | <a href=\"//");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/Special:Contributions/");
                buffer.append(temp);
                buffer.append("\">contribs</a>)");
            }
            else
                buffer.append(DELETED);
            
            // size
            buffer.append(" .. (");
            buffer.append(rev.getSize());
            buffer.append(" bytes) (");
            int sizediff = rev.getSizeDiff();
            if (sizediff > 0)
                buffer.append("<span class=\"sizeincreased\">");
            else
                buffer.append("<span class=\"sizedecreased\">");
            buffer.append(rev.getSizeDiff());
            buffer.append("</span>");
            
            // edit summary
            buffer.append(") .. (");
            if (rev.getSummary() != null)
                buffer.append(recode(rev.getSummary()));
            else
                buffer.append(DELETED);
            buffer.append(")\n");
        }
        buffer.append("</ul>\n");
        return buffer.toString();
    }
    
    /**
     *  Renders output of {@link Wiki#linksearch} in wikitext.
     *  @param results the results to render
     *  @param domain the domain that was searched
     *  @return the rendered wikitext
     *  @since 0.02
     */
    public static String linksearchResultsToWikitext(List<String[]> results, String domain)
    {
        StringBuilder builder = new StringBuilder(100);
        int linknumber = results.size();
        for (String[] result : results)
        {
            builder.append("# [[");
            builder.append(result[0]);
            builder.append("]] uses link [");
            builder.append(result[1]);
            builder.append("]\n");
        }
        builder.append(linknumber);
        builder.append(" links found. ([[Special:Linksearch/*.");
        builder.append(domain);
        builder.append("|Linksearch]])");
        return builder.toString();
    }
    
    /**
     *  Renders output of {@link Wiki#linksearch} in HTML.
     *  @param results the results to render
     *  @param domain the domain that was searched (should already be sanitized
     *  for XSS)
     *  @param wiki the wiki that was searched
     *  @return the rendered HTML
     *  @since 0.02
     */
    public static String linksearchResultsToHTML(List<String[]> results, Wiki wiki, String domain)
    {
        StringBuilder buffer = new StringBuilder(1000);
        buffer.append("<p>\n<ol>\n");
        for (String[] result : results)
        {
            buffer.append("\t<li><a href=\"//");
            buffer.append(wiki.getDomain());
            buffer.append("/wiki/");
            buffer.append(result[0]);
            buffer.append("\">");
            buffer.append(result[0]);
            buffer.append("</a> uses link <a href=\"");
            buffer.append(result[1]);
            buffer.append("\">");
            buffer.append(result[1]);
            buffer.append("</a>\n");
        }
        buffer.append("</ol>\n<p>");
        buffer.append(results.size());
        buffer.append(" links found. (<a href=\"//");
        buffer.append(wiki.getDomain());
        buffer.append("/wiki/Special:Linksearch/*.");
        buffer.append(domain);
        buffer.append("\">Linksearch</a>)\n");
        return buffer.toString();
    }
    
    /**
     *  Creates user links in HTML of the form <samp>User (talk | contribs | 
     *  deletedcontribs | block | block log)</samp>
     *  @param username the username, NOT sanitized for XSS
     *  @param wiki the wiki to build links for
     *  @return the generated HTML
     */
    public static String generateUserLinks(Wiki wiki, String username)
    {
        String domain = wiki.getDomain();
        String userenc = ServletUtils.sanitizeForURL(username.replace(' ', '_'));
        return "<a href=\"//" + domain + "/wiki/User:" + userenc + "\">" + username + "</a> ("
            +  "<a href=\"//" + domain + "/wiki/User_talk:" + userenc + "\">talk</a> | "
            +  "<a href=\"//" + domain + "/wiki/Special:Contributions/" + userenc + "\">contribs</a> | "
            +  "<a href=\"//" + domain + "/wiki/Special:DeletedContributions/" + userenc + "\">deleted contribs</a> | "
            +  "<a href=\"//" + domain + "/wiki/Special:Block/" + userenc + "\">block</a> | "
            +  "<a href=\"//" + domain + "/w/index.php?title=Special:Log&type=block&page=User:" + userenc + "\">block log</a>)";
    }
    
    /**
     *  Reverse of Wiki.decode()
     *  @param in input string
     *  @return recoded input string
     */
    public static String recode(String in)
    {
        in = in.replace("&", "&amp;");
        in = in.replace("<", "&lt;").replace(">", "&gt;"); // html tags
        in = in.replace("\"", "&quot;");
        in = in.replace("'", "&#039;");
        return in;
    }
}
