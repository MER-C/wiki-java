/**
 *  @(#)ArrayUtils.java 0.01 31/10/2017
 *  Copyright (C) 2007 - 2018 MER-C and contributors
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

import java.util.*;
import java.util.stream.Collectors;

/**
 *  Convenience methods for dealing with lists of pages and revisions.
 *  @author MER-C
 *  @version 0.01
 */
public class ArrayUtils
{
    /**
     *  Determines the intersection of the list of pages <var>a</var> and any
     *  number of lists of articles <var>b</var>. Such lists might be generated
     *  from the various list methods below. Examples from the English Wikipedia:
     *
     *  <pre>{@code 
     *  // find all orphaned and unwikified articles
     *  String[] articles = ArrayUtils.intersection(wikipedia.getCategoryMembers("All orphaned articles", Wiki.MAIN_NAMESPACE),
     *      wikipedia.getCategoryMembers("All pages needing to be wikified", Wiki.MAIN_NAMESPACE));
     *
     *  // find all (notable) living people who are related to Barack Obama
     *  String[] people = ArrayUtils.intersection(wikipedia.getCategoryMembers("Living people", Wiki.MAIN_NAMESPACE),
     *      wikipedia.whatLinksHere("Barack Obama", Wiki.MAIN_NAMESPACE));
     *  }</pre>
     *
     *  @param a a list of pages
     *  @param b at least one other list of pages 
     *  @return the intersection of <var>a</var> and each of <var>b</var>
     *  @since Wiki.java 0.04
     */
    public static String[] intersection(String[] a, String[]... b)
    {
        List<String> intersec = new ArrayList<>(5000);
        intersec.addAll(Arrays.asList(a));
        for (String[] subarray : b)
            intersec.retainAll(Arrays.asList(subarray));
        return intersec.toArray(new String[intersec.size()]);
    }

    /**
     *  Determines the list of articles that are in <var>a</var> but not any of 
     *  <var>b</var>. This operation does not commute. Such lists might be 
     *  generated from the various lists below. Some examples from the English 
     *  Wikipedia:
     *
     *  <pre>{@code
     *  // find all Martian crater articles that do not have an infobox
     *  String[] articles = ArrayUtils.relativeComplement(wikipedia.getCategoryMembers("Craters on Mars"),
     *      wikipedia.whatTranscludesHere("Template:MarsGeo-Crater", Wiki.MAIN_NAMESPACE));
     *
     *  // find all images without a description that haven't been tagged "no license"
     *  String[] images = ArrayUtils.relativeComplement(wikipedia.getCategoryMembers("Images lacking a description"),
     *      wikipedia.getCategoryMembers("All images with unknown copyright status"));
     *  }</pre>
     *
     *  @param a a list of pages
     *  @param b another list of pages
     *  @return the array of pages in <var>a</var> that are not in any of
     *  <var>b</var>
     *  @since Wiki.java 0.14
     */
    public static String[] relativeComplement(String[] a, String[]... b)
    {
        List<String> compl = new ArrayList<>(5000);
        compl.addAll(Arrays.asList(a));
        for (String[] subarray : b)
            compl.removeAll(Arrays.asList(subarray));
        return compl.toArray(new String[compl.size()]);
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
                if (sha1 == null)
                    continue;
                if (hashes.contains(sha1))
                    iter.remove();
                hashes.add(sha1);
            }
            ret.addAll(listofrevisions);
        });
        return new ArrayList<>(ret);
    }
}
