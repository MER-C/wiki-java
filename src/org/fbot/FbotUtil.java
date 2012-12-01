package org.fbot;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.wikipedia.Wiki;

/**
 * Miscellaneous utility methods for Fbot. Contains several non-wiki related functions whose uses
 * can potentially extend beyond Wikis.
 * 
 * @see org.fbot.Fbot
 * @see org.fbot.FbotParse
 * 
 * @author Fastily
 */

public class FbotUtil
{
	/**
	 * Hiding constructor from Javadoc
	 */
	private FbotUtil()
	{
		// does nothing
	}

	/**
	 * Splits an array of objects into an array of smaller arrays. Useful for multithreaded bots.
	 * CAVEAT: if splits > z.length, splits will be set to z.length.
	 * 
	 * @param z The array we'll be splitting
	 * @param splits The number of sub-arrays you want.
	 * 
	 */

	public static String[][] arraySplitter(String[] z, int splits)
	{

		if (splits > z.length)
			splits = z.length;

		String[][] xf;

		if (splits == 0)
		{
			xf = new String[][] { z };
			return xf;
		}
		else
		{
			xf = new String[splits][];
			for (int i = 0; i < splits; i++)
			{
				String[] temp;
				if (i == 0)
					temp = Arrays.copyOfRange(z, 0, z.length / splits);
				else if (i == splits - 1)
					temp = Arrays.copyOfRange(z, z.length / splits * (splits - 1), z.length);
				else
					temp = Arrays.copyOfRange(z, z.length / splits * (i), z.length / splits * (i + 1));

				xf[i] = temp;
			}
			return xf;
		}
	}

	/**
	 * Checks to see if an array of objects contains a given element
	 * 
	 * @param array The array to check
	 * @param el The element to look for in this array
	 * 
	 * @return True if this array contains the element, else false.
	 * 
	 */

	public static boolean arrayContains(Object[] array, Object el)
	{
		return Arrays.asList(array).contains(el);
	}

	/**
	 * Creates a HashMap from a file. Key and Value must be separated by colons. One newline per
	 * entry. Example line: "KEY:VALUE". Useful for storing deletion/editing reasons.  The text file to read
	 * from must be in UTF-8 encoding.
	 * 
	 * @param path The path of the file to read from
	 * 
	 * @return The HashMap we created by parsing the file
	 * 
	 */

	public static HashMap<String, String> buildReasonCollection(String path) throws FileNotFoundException
	{
		HashMap<String, String> l = new HashMap<String, String>();

		for (String s : loadFromFile(path, "", "UTF-8"))
		{
			int i = s.indexOf(":");
			l.put(s.substring(0, i), s.substring(i + 1));
		}

		return l;
	}

	/**
	 * Determines if a substring in an array of strings is present in at least one string of that
	 * array. String substring is the substring to search for.
	 * 
	 * @param list The list of elements to use
	 * @param substring The substring to search for in the list
	 * @param caseinsensitive If true, ignore upper/lowercase distinctions.
	 * 
	 * @return boolean True if we found a matching substring in the specified list.
	 * 
	 */

	public static boolean listElementContains(String[] list, String substring, boolean caseinsensitive)
	{
		if (caseinsensitive)
		{
			substring = substring.toLowerCase();
			for (String s : list)
				if (s.toLowerCase().contains(substring))
					return true;
			return false;
		}
		else
		{
			for (String s : list)
				if (s.contains(substring))
					return true;
			return false;
		}
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
	 * Loads list of pages contained in a file into an array. One newline per item.
	 * 
	 * @param file The abstract filename of the file to load from.
	 * @param prefix Append prefix/namespace to items? If not, specify <tt>""</tt>.
	 * @param encoding The text encoding of the file to load from.
	 * 
	 * @return The resulting array
	 * 
	 * @throws FileNotFoundException If the specified file was not found.
	 */

	public static String[] loadFromFile(String file, String prefix, String encoding) throws FileNotFoundException
	{
		Scanner m = new Scanner(new File(file), encoding);
		ArrayList<String> l = new ArrayList<String>();
		while (m.hasNextLine())
			l.add(prefix + m.nextLine().trim());
		return l.toArray(new String[0]);
	}

	/**
	 * Copies a file.
	 * 
	 * @param src The path of the source file
	 * @param dest The location to copy the file to.
	 * 
	 * @throws FileNotFoundException If the source file could not be found
	 * @throws IOException If we encountered some sort of read/write error
	 */

	public static void copyFile(String src, String dest) throws FileNotFoundException, IOException
	{
		FileInputStream in = new FileInputStream(new File(src));
		FileOutputStream out = new FileOutputStream(new File(dest));

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
			out.write(buf, 0, len);

		in.close();
		out.close();
	}

	/**
	 * Determines if two arrays of Strings share at least one element.
	 * 
	 * @param a1 The first array
	 * @param a2 The second array
	 * 
	 * @return True if the arrays share at least one element
	 * 
	 */

	public static boolean arraysShareElement(String[] a1, String[] a2)
	{
		return Wiki.intersection(a1, a2).length > 0;
	}

	/**
	 * Returns a Gregorian Calendar offset by a given number of days from the current system clock.
	 * Use positive int to offset by future days and negative numbers to offset to days before.
	 * Automatically set to UTC.
	 * 
	 * @param days The number of days to offset by -/+. Use 0 for no offset.
	 * 
	 * @return The newly modified calendar.
	 * 
	 */

	public static GregorianCalendar offsetTime(int days)
	{
		GregorianCalendar utc = (GregorianCalendar) new Wiki().makeCalendar();
		utc.setTimeInMillis(utc.getTime().getTime() + 86400000L * days);

		return utc;
	}

	/**
	 * Outputs the date/time in UTC. Based on the format and offset in days.
	 * 
	 * @param format Must be specified in accordance with java.text.DateFormat.
	 * @param offset The offset from the current time, in days. Accepts positive values (for future),
	 *           negative values (for past), and 0 for no offset.
	 * 
	 * @return The formatted date string.
	 * 
	 */

	public static String fetchDateUTC(String format, int offset)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(offsetTime(offset).getTime());
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
	 */
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
	 */
	public static String getFileExt(String fn)
	{
		int i = fn.lastIndexOf('.');
		if (i == -1)
			return null;
		return fn.substring(i + 1);
	}

	/**
	 * Shows a JOptionPane with the stacktrace/backtrace for the Throwable passed in. Useful in GUI
	 * applications.
	 * 
	 * @param e
	 */
	public static void showStackTrace(Throwable e)
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		JTextArea t = new JTextArea(sw.toString());
		t.setEditable(false);
		JOptionPane.showMessageDialog(null, t, "Critical Error!", JOptionPane.PLAIN_MESSAGE);
	}

	
	/**
	 * Creates a form in the form of a JPanel.  Fields are dynamically resized when the window size is
	 * modified by the user.  
	 * 
	 * @param title Title to use in the border.  Specify null if you don't want one. Specify empty string if you want just border.
	 * @param cl The list of containers to work with.  Elements should be in the order, e.g. JLabel1, JTextField1,
	 * JLabel 2, JTextField2, etc.
	 * 
	 * @return A JPanel with a SpringLayout in a form.
	 * @throws UnsupportedOperationException If cl.length == 0 || cl.length % 2 == 1
	 */
	public static JPanel buildForm(String title, JComponent...cl)
	{
		JPanel pl = new JPanel(new GridBagLayout());
		
		//Sanity check.  There must be at least two elements in cl
		if(cl.length == 0 || cl.length % 2 == 1)
			throw new UnsupportedOperationException("Either cl is empty or has an odd number of elements!");
	   
		if(title != null)
		   pl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(5,5,5,5)));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		
		for(int i = 0; i < cl.length; i+=2)
		{	
			c.gridx = 0;
			c.gridy = i;
			c.anchor = GridBagConstraints.EAST; //should anchor East
			pl.add(cl[i], c);
			
			c.anchor = GridBagConstraints.CENTER; //reset anchor to default
			
			c.weightx = 0.5; //Fill weights
			c.gridx = 1;
			c.gridy = i;
			c.ipady = 5; //sometimes components render funky when there is no extra vertical buffer
 			pl.add(cl[i+1], c);
 			
 			//reset default values for next iteration
 			c.weightx = 0;
 			c.ipady = 0;
		}
	   
	   return pl;
	}
	
	/**
	 * Writes a string to a file.
	 * 
	 * @param text The text to write to file
	 * @param file The file to use, abstract or absolute pathname
	 * 
	 * @throws IOException If we encountered a read/write error
	 */
	public static void writeToFile(String text, String file) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(file)));
		out.write(text);
		out.close();
	}
	
}
