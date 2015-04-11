/**
 *  @(#)OtherTests.java
 *  Copyright (C) 2011 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
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

import java.util.Arrays;
import org.wikipedia.*;

/**
 *  Miscellaneous tests.
 *  @author MER-C
 */
public class OtherTests
{
    private static WMFWiki enWiki = new WMFWiki("en.wikipedia.org");
    
    public static void main(String[] args) throws java.io.IOException
    {
        // WMFWiki.getSiteMatrix()
        // for (Wiki x : WMFWiki.getSiteMatrix())
        //     System.out.println(x.getDomain());
        
        // WMFWiki.getGlobalUsage()
        for (String[] x : enWiki.getGlobalUsage("File:Deinocheirusbcn.JPG"))
            System.out.println(Arrays.toString(x));
    }
}
