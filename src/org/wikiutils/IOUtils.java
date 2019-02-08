package org.wikiutils;

import java.io.*;
import java.util.*;
import java.nio.file.*;

import org.wikipedia.Wiki;

/**
 * Contains several read-write operations.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 * @see org.wikiutils.WikiUtils
 */
public class IOUtils
{
	/**
	 * Hiding constructor from JavaDoc
	 */
	private IOUtils(){}

	
	/**
	 *  Creates a HashMap from a file. Key and Value must be separated by 
         *  colons. One newline per entry. Example line: "KEY:VALUE". Useful for
         *  storing deletion/editing reasons. The text file to read from must be
         *  in UTF-8 encoding.
	 * 
	 *  @param path The path of the file to read from
	 *  @throws IOException if a filesystem error occurs
	 *  @return The HashMap we created by parsing the file
	 */
	public static HashMap<String, String> buildReasonCollection(String path) throws IOException
	{
            HashMap<String, String> l = new HashMap<>();
            Files.readAllLines(Paths.get(path)).forEach(line ->
            {
                line = line.trim();
                int i = line.indexOf(":");
                l.put(line.substring(0, i), line.substring(i + 1));
            });
            return l;
	}
	
	/**
	 * Recursively searches a directory for any files (not directories).  
	 * 
	 * @param f The root directory to start searching in.
	 * @param fl Any files we find will be added to this list.
	 * @param ff Optional FileFilter.  Specify null to not use one.
	 * 
	 */
	public static void listFilesR(File f, ArrayList<File> fl, FileFilter ff)
	{
		if(!f.isDirectory() || !f.exists())
			throw new IllegalArgumentException("File passed in is not a directory or is non-existent!");
		
		File[] cfl;
		if(ff == null)
			cfl = f.listFiles();
		else
			cfl = f.listFiles(ff);
		
		for(File z : cfl)
		{
			if(z.isDirectory())
				listFilesR(z, fl, ff);
			else
				fl.add(z);
		}
	}
}