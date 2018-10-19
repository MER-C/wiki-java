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
 *  Tools for Wikipedia's featured picture collection. TODO: check each featured
 *  picture for use in articles.
 * 
 *  @author MER-C
 *  @see <a href="https://en.wikipedia.org/wiki/Wikipedia:Featured_pictures">Featured Pictures</a>
 */
public class FeaturedPictureCuration
{
    private static final Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Set<String> fpcanonical = getFeaturedPicturesFromList();
        List<String> fpcat = Arrays.asList(enWiki.getCategoryMembers("Category:Featured pictures", Wiki.FILE_NAMESPACE));
        
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
     *  Fetches the canonical list of featured pictures on the English Wikipedia,
     *  available <a href="https://en.wikipedia.org/wiki/Wikipedia:Featured_pictures">here</a>.
     *  
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public static Set<String> getFeaturedPicturesFromList() throws IOException
    {
        String[] allfppages = enWiki.prefixIndex("Wikipedia:Featured pictures/");
        Set<String> fps = new HashSet<>();
        for (String fppage : allfppages)
            fps.addAll(Arrays.asList(enWiki.getImagesOnPage(fppage)));
        return fps;
    }
    
}
