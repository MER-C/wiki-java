/**
 *  @(#)ParserUtils.java 0.02 23/12/2016
 *  Copyright (C) 2012-2018 MER-C
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
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *  Various parsing methods that turn Wiki.java objects into wikitext and HTML
 *  and vice versa.
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
            buffer.append(rev.getID());
            buffer.append("|");
            buffer.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp()));
            buffer.append("]] ");
            
            // diff link
            buffer.append("([[Special:Diff/");
            buffer.append(rev.getID());
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
            buffer.append(rev.getTitle());
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
            String summary = rev.getComment();
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
            String page = recode(rev.getTitle());
            String revurl = "<a href=\"" + rev.permanentUrl();
            buffer.append(revurl);
            buffer.append("&diff=prev\">prev</a>) ");
            
            // date
            buffer.append(revurl);
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
            
            // page name
            buffer.append("<a href=\"");
            buffer.append(wiki.getPageUrl(page));
            buffer.append("\" class=\"pagename\">");
            buffer.append(page);
            buffer.append("</a> .. ");
            
            // user links
            String temp = rev.getUser();
            if (temp != null)
            {
                temp = recode(temp);
                buffer.append("<a href=\"");
                buffer.append(wiki.getPageUrl("User:" + temp));
                buffer.append("\">");
                buffer.append(temp);
                buffer.append("</a> (<a href=\"");
                buffer.append(wiki.getPageUrl("User talk:" + temp));
                buffer.append("\">talk</a> | <a href=\"");
                buffer.append(wiki.getPageUrl("Special:Contributions/" + temp));
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
            if (rev.getParsedComment() != null)
                buffer.append(rev.getParsedComment());
            else
                buffer.append(DELETED);
            buffer.append(")\n");
        }
        buffer.append("</ul>\n");
        return buffer.toString();
    }
    
    /**
     *  Creates user links in HTML of the form <samp>User (talk | contribs | 
     *  deletedcontribs | block | block log)</samp>
     *  @param username the username
     *  @param wiki the wiki to build links for
     *  @return the generated HTML
     *  @see #generateUserLinksAsWikitext(java.lang.String) 
     */
    public static String generateUserLinks(Wiki wiki, String username)
    {
        try
        {
            String indexPHPURL = wiki.getIndexPhpUrl();
            String userenc = URLEncoder.encode(username, "UTF-8");
            return "<a href=\"" + wiki.getPageUrl("User:" + username) + "\">" + username + "</a> ("
                +  "<a href=\"" + wiki.getPageUrl("User talk:" + username) + "\">talk</a> | "
                +  "<a href=\"" + wiki.getPageUrl("Special:Contributions/" + username) + "\">contribs</a> | "
                +  "<a href=\"" + wiki.getPageUrl("Special:DeletedContributions/" + username) + "\">deleted contribs</a> | "
                +  "<a href=\"" + indexPHPURL + "?title=Special:Log&user=" + userenc + "\">logs</a> | "
                +  "<a href=\"" + wiki.getPageUrl("Special:Block/" + username) + "\">block</a> | "
                +  "<a href=\"" + indexPHPURL + "?title=Special:Log&type=block&page=User:" + userenc + "\">block log</a>)";
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex); // seriously?
        }
    }
    
    /**
     *  Creates user links in wikitext of the form <samp>User (talk | contribs | 
     *  deletedcontribs | block | block log)</samp>
     *  @param username the username
     *  @return the generated wikitext
     *  @see #generateUserLinks(org.wikipedia.Wiki, java.lang.String) 
     */
    public static String generateUserLinksAsWikitext(String username)
    {
        try
        {
            String userenc = URLEncoder.encode(username, "UTF-8");
            return "* [[User:" + username + "|" + username + "]] (" 
                +  "[[User talk:" + username + "|talk]] | "
                +  "[[Special:Contributions/" + username + "|contribs]] | "
                +  "[[Special:DeletedContributions/" + username + "|deleted contribs]] | "
                +  "[{{fullurl:Special:Log|user=" + userenc + "}} logs] | "
                +  "[[Special:Block/" + username + "|block]] | "
                +  "[{{fullurl:Special:Log|type=block&page=User:" + userenc + "}} block log])\n";
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex); // seriously?
        }
    }
    
    /**
     *  Parses a wikilink into its target and linked text. Example: <kbd>[[Link]]</kbd>
     *  returns {@code { "Link", "Link" }} and <kbd>[[Link|Test]]</kbd> returns
     *  {@code { "Link", "Test" }}. Can also be used to get sortkeys from 
     *  categorizations. Use with caution on file uses because they can
     *  contain their own wikilinks.
     *  @param wikitext the wikitext to parse
     *  @return first element = the target of the link, the second being the
     *  description
     *  @throws IllegalArgumentException if wikitext is not a valid wikilink
     */
    public static List<String> parseWikilink(String wikitext)
    {
        int wikilinkstart = wikitext.indexOf("[[");
        int wikilinkend = wikitext.indexOf("]]", wikilinkstart);
        if (wikilinkstart < 0 || wikilinkend < 0)
            throw new IllegalArgumentException("\"" + wikitext + "\" is not a valid wikilink.");
        // strip escaping of categories and files
        String linktext = wikitext.substring(wikilinkstart + 2, wikilinkend).trim();
        if (linktext.startsWith(":"))
            linktext = linktext.substring(1);
        // check for description, if not there then set it to the target
        int pipe = linktext.indexOf('|');
        if (pipe >= 0)
            return Arrays.asList(linktext.substring(0, pipe).trim(), linktext.substring(pipe + 1).trim());
        else
        {
            String temp = linktext.trim();
            return Arrays.asList(temp, temp);
        }        
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
