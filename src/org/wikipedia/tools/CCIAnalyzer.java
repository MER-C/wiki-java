/**
 *  @(#)CCIAnalyzer.java 0.01 16/12/2013
 *  Copyright (C) 2013 - 2014 MER-C
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
package org.wikipedia.tools;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import org.wikipedia.*;

/**
 *  Identifies trivial diffs in a contributor copyright investigation.
 *  @author MER-C
 *  @version 0.01
 */
public class CCIAnalyzer
{
    private static Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  args[0] = wiki page to read (optional)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        enWiki.setLogLevel(Level.WARNING);
        StringBuilder cci = new StringBuilder(1000000);
        if (args.length < 1)
        {
            // read in from file
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                System.exit(0);      
            List<String> lines = Files.readAllLines(fc.getSelectedFile().toPath());
            for (String line : lines)
            {
                cci.append(line);
                cci.append("\n");
            }
        }
        else
            // or read in from supplied wiki page
            cci.append(enWiki.getPageText(args[0]));
        
        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev
        String diffaddedbegin = "<td class=\"diff-addedline\">";
        String diffaddedend = "</td>";
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        
        // count number of diffs
        int count = 0;
        for (int i = cci.indexOf("[[Special:Diff/"); i >= 0; i = cci.indexOf("[[Special:Diff/", ++i))
            count++;
        
        // parse the list of diffs
        int parsed = 0;
        long start = System.currentTimeMillis();
        ArrayList<String> minoredits = new ArrayList<>(500);
        boolean exception = false;
        for (int i = cci.indexOf("[[Special:Diff/"); i >= 0; i = cci.indexOf("[[Special:Diff/", ++i))
        {
            int xx = cci.indexOf("/", i);
            int yy = cci.indexOf("|", xx);
            int zz = cci.indexOf("]]", xx) + 2;
            String edit = cci.substring(i, zz);
            long oldid = Long.parseLong(cci.substring(xx + 1, yy));

            // Fetch diff. No plain text diffs for performance reasons, see
            // https://phabricator.wikimedia.org/T15209
            String diff = "";
            try 
            {
                Map<String, Object> from = new HashMap<>();
                from.put("revid", oldid);
                Map<String, Object> to = new HashMap<>();
                to.put("revid", Wiki.PREVIOUS_REVISION);
                diff = enWiki.diff(from, to);
                exception = false;
            }
            catch (IOException ex)
            {
                System.err.println();
                System.out.flush();
                ex.printStackTrace();
                
                // bail if two IOExceptions are thrown consecutively
                if (exception)
                {
                    System.err.printf("\033[31;1mBailing due to consecutive IOExceptions!\033[0m\n");
                    break;
                }
                System.err.printf("\033[31;1mSkipping oldid %s\033[0m\n", oldid);
                exception = true;
                continue;
            }
            catch (UnknownError err)
            {
                // HACK: RevisionDeleted revision or deleted article.
                continue;
            }
            if (diff == null) // RevisionDeleted revision
                continue;
            // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
            diff = diff.toLowerCase(enWiki.locale());
            diff = diff.replace(deltaend + " " + deltabegin, " ");
            // If the diff is empty (see https://en.wikipedia.org/w/index.php?diff=343490272)
            // it will not contain diffaddedbegin -> default major to true.
            boolean major = true;
            // It is easy to strip the HTML.
            for (int j = diff.indexOf(diffaddedbegin); j >= 0; j = diff.indexOf(diffaddedbegin, j))
            {
                int y2 = diff.indexOf(diffaddedend, j);
                String addedline = diff.substring(j + diffaddedbegin.length(), y2);
                addedline = addedline.replaceFirst("^<div>", "");
                addedline = addedline.replace("</div>;", "");
                if (addedline.contains(deltabegin))
                {
                    for (int k = addedline.indexOf(deltabegin); k >= 0; k = addedline.indexOf(deltabegin, k))
                    {
                        int y3 = addedline.indexOf(deltaend, k);
                        String delta = addedline.substring(k + deltabegin.length(), y3);
                        major = analyzeDelta(delta);
                        if (major)
                            break;
                        k = y3;
                    }
                }
                else
                    major = analyzeDelta(addedline);
                if (major)
                    break;
                j = y2;
            }
            if (!major)
                minoredits.add(edit);
            long now = System.currentTimeMillis();
            double percent = 100.0 * ++parsed / count;
            int elapsed = (int)((now - start) / 1000.0);
            int projected = elapsed * count / parsed;
            int eta = projected - elapsed;
            System.err.printf("\r\033[K%d of %d diffs parsed (%2.2f%%, %d:%02d remaining)", 
                parsed, count, percent, eta / 60, eta % 60);
        }
        System.err.println();
        
        // remove all minor edits from the CCI
        for (String minoredit : minoredits)
        {
            int x = cci.indexOf(minoredit);
            cci.delete(x, x + minoredit.length());
            
            // we don't care about minor edits that add less than 500 chars
            int yy = minoredit.indexOf('|') + 2;
            int zz = minoredit.indexOf(")]]");
            int size = Integer.parseInt(minoredit.substring(yy, zz));
            if (size > 499)
                System.out.println(minoredit);
        }
        System.out.println("----------------------");
        
        // clean up output CCI listing
        String[] articles = cci.toString().split("\\n");
        StringBuilder cleaned = new StringBuilder();
        int removedarticles = 0;
        Pattern pattern = Pattern.compile(".*edits?\\):\\s+");
        for (String article : articles)
        {
            // remove articles where all diffs are trivial
            if (pattern.matcher(article).matches())
            {
                removedarticles++;
                continue;
            }
            cleaned.append(article);
            cleaned.append("\n");
        }
        System.out.println(cleaned);
        System.err.println("Diffs removed: " + minoredits.size() + " of " + count 
            + ", articles removed: " + removedarticles);
    }
    
    /**
     *  Determines whether a given delta is a major edit. A "major edit" is
     *  defined as something that adds more than 9 words.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     */
    public static boolean analyzeDelta(String delta)
    {
        // remove some common strings
        // {{subst:afd}}
        if (delta.contains("please do not remove or change this afd message until the issue is settled"))
            return false;
        if (delta.contains("end of afd message, feel free to edit beyond this point"))
            return false;
        if (delta.contains("{{afdm|"))
            return false;
        // {{subst:prod}}
        if (delta.contains("{{proposed deletion/dated|"))
            return false;
        // {{subst:prod blp}}
        if (delta.contains("{{prod blp/dated|"))
            return false;
        if (delta.contains("{{infobox "))
            return false;
        
        // replace wikilinks, categories and files with their descriptions
        StringBuilder temp = new StringBuilder(delta);
        for (int i = temp.indexOf("[["); i >= 0; i = temp.indexOf("[["))
        {
            int j = temp.indexOf("]]", i);
            if (j < 0) // unbalanced brackets
                break;
            List<String> parsedlink = WikitextUtils.parseWikilink(temp.substring(i, j + 2));
            if (parsedlink.get(0).length() > 100)
                // something has gone wrong here
                break;
            else if (enWiki.namespace(parsedlink.get(0)) == Wiki.CATEGORY_NAMESPACE)
                // I'm not interested in the category sortkey
                temp.delete(i, j + 2);
            else
                temp.replace(i, j + 2, parsedlink.get(1));
        }
        
        // decode() the delta
        String delta2 = temp.toString().replace("&lt;", "<");
        delta2 = delta2.replace("&gt;", ">");
        
        // From what I see, all articles still have 9 words between other markup.
        StringTokenizer tk = new StringTokenizer(delta2, "<>{}|=");
        while (tk.hasMoreTokens())
        {
            String token =  tk.nextToken();
            if (token.split("\\s").length > 9)
                return true;
        }
        return false;
    }
}
