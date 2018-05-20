package org.wikiutils;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import org.wikipedia.ArrayUtils;

/**
 * Contains some useful String related methods.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.WikiUtils
 */
public class StringUtils
{
	
	/**
	 * Hiding constructor from JavaDoc
	 */
	private StringUtils()
	{
	}
	
	/**
	 * Performs a case-insensitive java.lang.String.contains() operation.
	 * 
	 * @param text main body of text to check.
	 * @param s2 we will try to check if s2 is contained in text.
	 * 
	 * @return True if <tt>s2</tt> was found, case-insensitive, in <tt>text</tt>.
	 * 
	 */

	public static boolean containsIgnoreCase(String text, String s2)
	{
		return text.toUpperCase().contains(s2.toUpperCase());
	}
	
	/**
	 * Parses a String, assumed to be a list, into a String array. Each item in the list must be
	 * separated by a newline character. Space characters are ignored. The ignore list is case
	 * sensitive, and only checks if each String in the list starts with the specified text; this is
	 * useful for parsing out comments and/or unwanted crap.
	 * 
	 * @param s The String to use (must be delimited by new line chars)
	 * @param prefix Adds a prefix to the beginning of each element. Specify "" for no prefix.
	 * @param ignorelist If an item in the list contains a String in this list, it won't be included
	 *           in the final result. Whatever you do, don't put an empty string in here; you'll be
	 *           sorry :3
	 */

	public static String[] listify(String s, String prefix, String... ignorelist)
	{
		ArrayList<String> l = new ArrayList<String>();
		Scanner m = new Scanner(s);
		while (m.hasNextLine())
		{
			String b = m.nextLine().trim();
			if (b.length() > 0)
				l.add(prefix + b);
		}

		if (ignorelist.length > 0)
		{
			ArrayList<String> x = new ArrayList<String>();

			for (String a : l)
			{
				boolean good = true;

				for (String bad : ignorelist)
					if (a.contains(bad))
						good = false;

				if (good)
					x.add(a);
			}

			l = x;
		}

		return l.toArray(new String[0]);
	}
	
	
	/**
	 * Parses out the extension for a filename.
	 * 
	 * @param fn The filename to parse (e.g. Example.jpg)
	 * @return The extension of the file, without the "." Returns null if this file has no extension.
	 * @see #getFileExt(File)
	 */
	public static String getFileExt(String fn)
	{
		int i = fn.lastIndexOf('.');
		if (i == -1)
			return null;
		return fn.substring(i + 1).toLowerCase();
	}
	
	/**
	 * Parses out the extension of a file.
	 * 
	 * @param f The file to get an extension from
	 * @throws IllegalArgumentException If <tt>f</tt> is a directory.
	 * @return The extension of the file, without the "."
	 * 
	 * @see #getFileExt(String)
	 */
	public static String getFileExt(File f)
	{
		if(f.isDirectory())
			throw new IllegalArgumentException("Specified param CANNOT be a directory");
		return getFileExt(f.getName());
	}
}