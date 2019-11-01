/**
 *  @(#)FeaturedPictureCuration.java 0.01 19/10/2018
 *  Copyright (C) 2018 - 20xx MER-C
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

import java.io.IOException;
import java.util.*;
import org.wikipedia.*;

/**
 *  Tools for Wikipedia's featured picture collection. 
 * 
 *  @author MER-C
 *  @version 0.01
 *  @see <a href="https://en.wikipedia.org/wiki/Wikipedia:Featured_pictures">English 
 *  Wikipedia Featured Pictures</a>
 *  @see <a href="https://commons.wikimedia.org/wiki/Commons:Featured_pictures">Wikimedia
 *  Commons Featured Pictures</a>
 */
public class FeaturedPictureCuration
{
    private static final Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    private static final Wiki commons = Wiki.newSession("commons.wikimedia.org");
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {   
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.FeaturedPictureCuration", "[options]")
            .description("A tool for curating featured pictures.")
            .addHelp()
            .addVersion("FeaturedPictureCuration v0.01\n" + CommandLineParser.GPL_VERSION_STRING)
            .addBooleanFlag("--checktags", "Check whether FPs are tagged correctly (en.wp ONLY).")
            .addBooleanFlag("--checkusage", "Checks whether FPs are used in articles (en.wp or commons ONLY).")
            .addSingleArgumentFlag("--checknoms", "April 2019", "Checks whether all FPCs for a given month have been transcluded (en.wp ONLY)")
            .addSingleArgumentFlag("--wiki", "example.org", "Fetch FPs from this wiki (see --checkusage), default en.wikipedia.org.")
            .parse(args);
        
        if (parsedargs.containsKey("--checktags"))
            checkFPTags();
        if (parsedargs.containsKey("--checkusage"))
        {
            if (!parsedargs.containsKey("--wiki"))
                System.err.println("WARNING: No wiki specified, defaulting to en.wikipedia.org.");
            Wiki wiki = Wiki.newSession(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
            Set<String> fpcanonical = getFeaturedPicturesFromList(wiki);
            
            // NOTE 1: API imageusage does not take into account file redirects
            // therefore usage is always <= the real usage. File redirects should 
            // be bypassed by editing the wiki.
            // NOTE 2: There is a small amount of contamination of non-featured 
            // pictures on Commons.
            List<String> fps = new ArrayList<>(fpcanonical);
            List<Map<String, Object>> metadata = enWiki.getFileMetadata(fps);
            enWiki.setQueryLimit(100);
            for (int i = 0; i < fps.size(); i++)
            {
                String image = fps.get(i);
                Map<String, Object> data = metadata.get(i);
                List<String> usage = enWiki.imageUsage(image, Wiki.MAIN_NAMESPACE);
                System.out.println(String.join(",", List.of(image, String.valueOf(data.get("mime")),
                    String.valueOf(data.get("width")), String.valueOf(data.get("height")), 
                    String.valueOf(usage.size()), "\"" + String.join(",", usage) + "\"")));
            }
        }
        if (parsedargs.containsKey("--checknoms"))
        {
            List<String> noms = checkNominationsAreTranscluded(parsedargs.get("--checknoms"));
            if (noms.isEmpty())
                System.out.println("All nominations transcluded.");
            for (String nom : noms)
                System.out.println(nom);
        }
        
        /*
         * Gets FPs with descriptions from the FP galleries to look for duplicates. 
         * Needs some normalisation to remove wikilinks and some better string parsing.
         *
         
        String[] fppages = enWiki.getCategoryMembers("Category:Wikipedia featured pictures categories");
        String[] texts = enWiki.getPageText(fppages);
        List<String> csv = new ArrayList<>();
        for (String text : texts)
        {
            if (text.contains("<gallery"))
            {
                int a = text.indexOf("<gallery");
                int b = text.indexOf("</gallery>");
                String[] lines = text.substring(a, b).split("\\n");
                for (String line : lines)
                {
                    if (line.contains("File:") || line.contains("Image:"))
                    {
                        String temp = "\"" + line.replaceFirst("\\|", "\",\""); // escape file name and caption
                        temp = temp.replaceAll("'''", "");
                        temp = temp.replaceFirst(", by", "\",");
                        csv.add(temp);
                    }
                        
                }
            }
        }
        for (String line : csv)
            System.out.println(line);
        */
    }
    
    /**
     *  Fetches the canonical list of featured pictures on the supplied wiki, 
     *  being the list of pictures used on subpages of [[Project:Featured pictures]].
     *  
     *  @param wiki the wiki to fetch FPs for
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public static Set<String> getFeaturedPicturesFromList(Wiki wiki) throws IOException
    {
        List<String> allfppages = new ArrayList<>();
        String domain = wiki.getDomain();
        if (domain.equals("en.wikipedia.org"))
            allfppages.addAll(wiki.getCategoryMembers("Category:Wikipedia featured pictures categories"));
        else if (domain.equals("commons.wikimedia.org"))
            allfppages.addAll(wiki.getCategoryMembers("Category:Featured picture galleries"));
        else
            return Collections.emptySet();

        Set<String> fps = new HashSet<>();
        boolean temp = wiki.isResolvingRedirects();
        wiki.setResolveRedirects(true);
        for (List<String> fpimages : wiki.getImagesOnPage(allfppages))
            fps.addAll(fpimages);
        wiki.setResolveRedirects(temp);
        return fps;
    }
    
    /**
     *  Checks whether all pictures listed at [[Wikipedia:Featured pictures]]
     *  are actually tagged, and vice versa.
     *  @throws IOException if a network error occurs
     */
    public static void checkFPTags() throws IOException
    {
        Set<String> fpcanonical = getFeaturedPicturesFromList(enWiki);
        List<String> fpcat = enWiki.getCategoryMembers("Category:Featured pictures", Wiki.FILE_NAMESPACE);
        
        // check for FPs that are no longer tagged as such
        List<String> missingfps = new ArrayList(fpcanonical);
        missingfps.removeAll(fpcat);
        System.out.println("Images that should be tagged FP, but aren't:");
        System.out.println(Pages.toWikitextList(missingfps, Pages.LIST_OF_LINKS, false));
        
        // check for images tagged as FP, but aren't listed at [[WP:FP]]
        missingfps.clear();
        missingfps.addAll(fpcat);
        missingfps.removeAll(fpcanonical);
        System.out.println("Images that are tagged as FP, but aren't listed at [[WP:FP]]:");
        System.out.println(Pages.toWikitextList(missingfps, Pages.LIST_OF_LINKS, false));   
    }
    
    /**
     *  Checks whether all FPC nominations have been transcluded for a given
     *  month. Needless to say, this should be run early in the next month.
     *  @param month a string MMMM-YYYY (e.g. "April 2019")
     *  @return the list of nominations not transcluded for that month
     *  @throws IOException if a network error occurs
     */
    public static List<String> checkNominationsAreTranscluded(String month) throws IOException
    {
        List<String> nominations = enWiki.getCategoryMembers("Category:Featured picture nominations/" + month);
        boolean[] closed = enWiki.pageHasTemplate(nominations, "Template:FPCresult");
        List<String> currentnoms = enWiki.getTemplates(List.of("Wikipedia:Featured picture candidates"), 
            Wiki.PROJECT_NAMESPACE).get(0);
        
        List<String> results = new ArrayList();
        for (int i = 0; i < nominations.size(); i++)
            if (!closed[i])
                results.add(nominations.get(i));
        results.removeAll(currentnoms);
        return results;
    }
    
    public static List<Map<String, Object>> fpSearch(String query) throws IOException
    {
        // maybe useful for helping people search for existing FPs before nominating...?
        List<Map<String, Object>> results = new ArrayList<>();
        results.addAll(enWiki.search(query + " prefix:Wikipedia:Featured_pictures/"));
        results.addAll(enWiki.search(query + " prefix:Wikipedia:Featured_picture_candidates/"));
        results.addAll(commons.search(query + " prefix:Commons:Featured_pictures/"));
        results.addAll(commons.search(query + " prefix:Commons:Featured_picture_candidates"));
        return results;
    }
            
}
