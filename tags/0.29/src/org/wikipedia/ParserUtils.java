/**
 *  @(#)ParserUtils.java 0.01 16/10/2012
 *  Copyright (C) 2012
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

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  Various parsing methods that e.g. turning Wiki.java objects into wikitext 
 *  and HTML and vice versa.
 *  @author MER-C
 *  @version 0.01
 */
public class ParserUtils
{
    /**
     *  Standard markup for RevDeleted stuff.
     */
    private static final String DELETED = "<span style=\"color: #aaaaaa, text-decoration: strike\">deleted</span>";
    
    /**
     *   Parses a list of links into its individual elements. Such a list
     *   should be in the form:
     *
     *  <pre>
     *  * [[Main Page]]
     *  * [[Wikipedia:Featured picture candidates]]
     *  * [[:File:Example.png]]
     *  </pre>
     *
     *  in which case <tt>{ "Main Page", "Wikipedia:Featured picture
     *  candidates", "File:Example.png" }</tt> is the return value.
     *
     *  @param list a list of pages
     *  @see #formatList
     *  @return an array of the page titles
     *  @since Wiki.java 0.11
     */
    public static String[] parseList(String list)
    {
        StringTokenizer tokenizer = new StringTokenizer(list, "[]");
        ArrayList<String> titles = new ArrayList<String>(667);
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
        for (int i = 0; i < pages.length; i++)
        {
            buffer.append("*[[:");
            buffer.append(pages[i]);
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
        buffer.append("<div style=\"font-family: monospace\">\n");
        for (Wiki.Revision rev : revisions)
        {
            // base oldid link
            StringBuilder base2 = new StringBuilder(50);
            base2.append("<span class=\"plainlinks\">[");
            base2.append(wiki.base);
            base2.append(rev.getPage().replace(' ', '_'));
            base2.append("&oldid=");
            base2.append(rev.getRevid());
            
            // timestamp, link to oldid
            buffer.append("*");
            Calendar timestamp = rev.getTimestamp();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            buffer.append(base2);
            buffer.append(" ");
            buffer.append(format.format(timestamp.getTime()));
            buffer.append("]</span> ");
            
            // diff link
            buffer.append("(");
            buffer.append(base2);
            buffer.append("&diff=prev diff]</span>) ");
            
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
            
            // edit summary
            buffer.append(" (");
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
        buffer.append("<ul style=\"font-family: monospace\">\n");
        for (Wiki.Revision rev : revisions)
        {
            // diff link
            String page = rev.getPage();
            StringBuilder temp2 = new StringBuilder("<a href=\"");
            temp2.append(wiki.base);
            temp2.append(page.replace(' ', '_'));
            temp2.append("&oldid=");
            temp2.append(rev.getRevid());
            buffer.append("<li>(");
            buffer.append(temp2);
            buffer.append("&diff=prev\">prev</a>) ");
            // date
            buffer.append(temp2);
            buffer.append("\">");
            Calendar timestamp = rev.getTimestamp();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            buffer.append(format.format(timestamp.getTime()));
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
            buffer.append("<a href=\"http://");
            buffer.append(wiki.getDomain());
            buffer.append("/wiki/");
            buffer.append(page.replace(' ', '_'));
            buffer.append("\" style=\"color: #0066aa\">");
            buffer.append(page);
            buffer.append("</a> .. ");
            
            // usernames never contain XSS characters
            String temp = rev.getUser();
            if (temp != null)
            {
                buffer.append("<a href=\"http://");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/User:");
                buffer.append(temp);
                buffer.append("\">");
                buffer.append(temp);
                buffer.append("</a> (<a href=\"http://");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/User talk:");
                buffer.append(temp);
                buffer.append("\">talk</a> | <a href=\"http://");
                buffer.append(wiki.getDomain());
                buffer.append("/wiki/Special:Contributions/");
                buffer.append(temp);
                buffer.append("\">contribs</a>)");
            }
            else
            {
                buffer.append(DELETED);
            }
            
            // edit summary
            buffer.append(" .. (");
            if (rev.getSummary() != null)
                buffer.append(rev.getSummary());
            else
                buffer.append(DELETED);
            buffer.append(")\n");
        }
        buffer.append("</ul>\n");
        return buffer.toString();
    }
}
