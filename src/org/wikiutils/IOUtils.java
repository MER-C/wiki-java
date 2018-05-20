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
	 * Loads list of pages contained in a file into an array. One newline per item.
	 * 
	 * @param file The abstract filename of the file to load from.
	 * @param prefix Append prefix/namespace to items? If not, specify <tt>""</tt>.
	 * @param encoding The text encoding of the file to load from.
	 * 
	 * @return The resulting array
	 * 
	 * @throws FileNotFoundException If the specified file was not found.
         * @deprecated replace with {@code Files.lines(path, charset).map(line -> prefix + line.trim()).collect(Collectors.joining("\n"));}
	 */
        @Deprecated
	public static String[] loadFromFile(String file, String prefix, String encoding) throws FileNotFoundException
	{
		Scanner m = new Scanner(new File(file), encoding);
		ArrayList<String> l = new ArrayList<String>();
		while (m.hasNextLine())
			l.add(prefix + m.nextLine().trim());
		return l.toArray(new String[0]);
	}
	
	/**
	 * Reads the contents of a file into a String. Use with a <tt>.txt</tt> files for best results.
	 * There seems to be an issue with reading in non-standard ascii characters at the moment.
	 * 
	 * @param f The file to read from
	 * @param encoding The endcoding standard to use. (e.g. UTF-8, UTF-16)
	 * 
	 * @return The contents of the file as a String.
	 * 
	 * @throws FileNotFoundException If the file specified does not exist.
         * @deprecated replace with {@code Files.lines(path, charset).map(String::trim).collect(Collectors.joining("\n"));}
         * in the JDK.
	 */
        @Deprecated
	public static String fileToString(File f, String encoding) throws FileNotFoundException
	{
		Scanner m = new Scanner(f, encoding);
		String s = "";
		while (m.hasNextLine())
			s += m.nextLine().trim() + "\n";

		m.close();
		return s.trim();

	}
	
	/**
	 * Writes a string to a file.
	 * 
	 * @param text The text to write to file
	 * @param file The file to use, abstract or absolute pathname
	 * 
	 * @throws IOException If we encountered a read/write error
         * @deprecated replace with {@code Files.write(Paths.get(file), Arrays.asList(text));}
	 */
        @Deprecated
	public static void writeToFile(String text, String file) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(file)));
		out.write(text);
		out.close();
	}
	
	/**
	 * Downloads a file
	 * 
	 * @param title The title of the file to download <u>on the Wiki</u> <b>excluding</b> the "
	 *           <tt>File:</tt>" prefix.
	 * @param localpath The pathname to save this file to (e.g. "<tt>/Users/Fastily/Example.jpg</tt>
	 *           "). Note that if a file with that name already exists at that pathname, it <span
	 *           style="color:Red;font-weight:bold">will</span> be overwritten!
	 * @param wiki The wiki object to use.
	 * 
	 * @throws IOException If we had a network error
	 * @throws FileNotFoundException If <tt>localpath</tt> cannot be resolved to a pathname on the
	 *            local system.
	 * 
	 * @deprecated replace with Wiki.getImage(title, new File(localpath))
	 */
        @Deprecated
	public static void downloadFile(String title, String localpath, Wiki wiki) throws IOException, FileNotFoundException
	{
		wiki.getImage(title, new File(localpath));
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