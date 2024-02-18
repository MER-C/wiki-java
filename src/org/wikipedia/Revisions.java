/**
 *  @(#)Revisions.java 0.01 07/08/2018
 *  Copyright (C) 2018-20XX MER-C and contributors
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
import java.util.stream.*;

/**
 *  Utility classes for dealing with (lists of) wiki revisions.
 *  @author MER-C
 *  @version 0.01
 *  @see org.wikipedia.Wiki.Revision
 */
public class Revisions
{
    private final Wiki wiki;
    
    private Revisions(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki or access wiki state).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Revisions of(Wiki wiki)
    {
        return new Revisions(wiki);
    }
    
    /**
     *  Removes reverts from a list of revisions. A revert is defined as any 
     *  revision on a page that has the same SHA-1 as any previous (as in time) 
     *  revision on that page. As a side effect, the returned list is sorted
     *  by timestamp with the earliest revision first and with duplicates 
     *  removed.
     * 
     *  @param revisions the revisions to remove reverts from
     *  @return a copy of the list of revisions with reverts removed
     */
    public static List<Wiki.Revision> removeReverts(List<Wiki.Revision> revisions)
    {
        // Group revisions by page, then sort so that the oldest edits are first.
        Map<String, Set<Wiki.Revision>> stuff = revisions.stream()
            .collect(Collectors.groupingBy(Wiki.Revision::getTitle, Collectors.toCollection(TreeSet::new)));
        Set<Wiki.Revision> ret = new LinkedHashSet<>();
        stuff.forEach((page, listofrevisions) ->
        {
            // Therefore, if a sha1 matches any previous revisions it is a revert.
            Set<String> hashes = new HashSet<>();
            Iterator<Wiki.Revision> iter = listofrevisions.iterator();
            while (iter.hasNext())
            {
                String sha1 = iter.next().getSha1();
                if (sha1 == null || sha1.equals(Wiki.Event.CONTENT_DELETED))
                    continue;
                if (hashes.contains(sha1))
                    iter.remove();
                hashes.add(sha1);
            }
            ret.addAll(listofrevisions);
        });
        return new ArrayList<>(ret);
    }
    
    /**
     *  Turns a list of revisions into human-readable wikitext. Be careful, as
     *  slowness may result when copying large amounts of wikitext produced by
     *  this method, or by the wiki trying to parse it. Takes the form of:
     *
     *  <p>*(diff link) 2009-01-01 00:00 User (talk | contribs) (edit summary)
     *  @param revisions a list of revisions
     *  @return those revisions as wikitext
     *  @since Wiki.java 0.20
     */
    public static String toWikitext(Iterable<Wiki.Revision> revisions)
    {
        StringBuilder buffer = new StringBuilder(100000);
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
            buffer.append("|prev]]) ");
            
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
            if (user2 == null || user2.equals(Wiki.Event.USER_DELETED))
                buffer.append(Events.DELETED_EVENT_HTML);
            else
                buffer.append(Users.generateWikitextSummaryLinksShort(user2));
            
            // size
            buffer.append(" .. (");
            buffer.append(rev.getSize());
            buffer.append(" bytes) (");
            buffer.append(rev.getSizeDiff());
            
            // edit summary
            buffer.append(") .. (");
            String summary = rev.getComment();
            if (summary == null || summary.equals(Wiki.Event.COMMENT_DELETED))
                buffer.append(Events.DELETED_EVENT_HTML);
            else
            {
                // kill wikimarkup
                buffer.append("<nowiki>");
                buffer.append(summary);
                buffer.append("</nowiki>");
            }
            buffer.append(")\n");
        }
        buffer.append("</div>");
        return buffer.toString();
    }
    
    /**
     *  Generates HTML from Wiki.Revisions. Output is a wikitable to parsed 
     *  wikitext returned by {@link #toWikitext(Iterable)}.
     *  @param revisions the revisions to convert
     *  @return (see above)
     *  @see #toWikitext(Iterable)
     */
    public String toHTML(Iterable<Wiki.Revision> revisions)
    {
        StringBuilder buffer = new StringBuilder(100000);
        Users userutils = Users.of(wiki);
        Pages pageutils = Pages.of(wiki);
        buffer.append("<table class=\"wikitable revisions\">\n");
        for (Wiki.Revision rev : revisions)
        {
            String revurl = rev.permanentUrl();
            String page = rev.getTitle();
            String user = rev.getUser();
            String comment = rev.getParsedComment();
            int sizediff = rev.getSizeDiff();
            String userhtml = user == null || user.equals(Wiki.Event.USER_DELETED)
                ? Events.DELETED_EVENT_HTML : userutils.generateHTMLSummaryLinksShort(user);
            String commenthtml = comment == null || comment.equals(Wiki.Event.COMMENT_DELETED)
                ? Events.DELETED_EVENT_HTML : comment;
            
            buffer.append("""
                <tr class="revision">
                <td class="difflink"><a href="%s&diff=prev">prev</a>
                <td class="date"><a href="%s">%s</a>
                <td class="flag">%s
                <td class="flag">%s
                <td class="flag">%s
                <td class="title">%s
                <td class="user">%s
                <td class="revsize">%d bytes
                <td class="revsizediff"><span class="%s">%d</span>
                <td>%s
                """.formatted(revurl, revurl, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp()),
                rev.isNew() ? "<b>N</b>" : ".", rev.isMinor() ? "<b>m</b>" : ".", rev.isBot() ? "<b>b</b>" : ".",
                pageutils.generatePageLink(page, true), userhtml, rev.getSize(), 
                sizediff > 0 ? "sizeincreased" : "sizedecreased", sizediff, commenthtml));
        }
        buffer.append("</table>\n");
        return buffer.toString();
    }
}
