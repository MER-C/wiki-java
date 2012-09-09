package org.fbot;

import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import org.wikipedia.Wiki;

/**
 * Miscellaneous utility methods for Fbot. Contains several non-wiki related functions whose uses
 * extend beyond Wikis.
 * 
 * @see org.fbot.Fbot
 * @see org.fbot.MBot
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
	 * Returns a randomized String [A-Z].
	 * 
	 * @param len The length of the String to return
	 * 
	 * @return The random string
	 */

	public static String generateRandomString(int len)
	{
		Random r = new Random();

		String s = "";
		for (int i = 0; i < len; i++)
			s += (char) (r.nextInt(25) + 65);
		return s;
	}

	/**
	 * Splits an array of Strings into an array of smaller arrays. Useful for multithreaded bots.
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
	 * Recursively fetches a list of Wiki-uploadable files in contained in the specified directory
	 * and its subfolders.
	 * 
	 * @param dir The top directory to start with
	 * @param fl The ArrayList to add the files we found to.
	 * 
	 * @throws UnsupportedOperationException If dir is not a directory.
	 */

	public static void listFilesR(File dir, ArrayList<File> fl)
	{
		if (!dir.exists() || !dir.isDirectory())
			throw new UnsupportedOperationException("Not a directory:  " + dir.getName());
		for (File f : dir.listFiles())
		{
			String fn = f.getName();
			if (f.isDirectory() && !fn.startsWith("."))
				listFilesR(f, fl);
			else if (isUploadable(fn))
				fl.add(f);
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
	 * entry. Example line: "KEY:VALUE". Useful for storing deletion/editing reasons.
	 * 
	 * @param path The path of the file to read from
	 * 
	 * @return The HashMap we created by parsing the file
	 * 
	 */

	public static HashMap<String, String> buildReasonCollection(String path) throws FileNotFoundException
	{
		HashMap<String, String> l = new HashMap<String, String>();

		for (String s : loadFromFile(path, ""))
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
	 * 
	 * @return The resulting array
	 * 
	 * @throws FileNotFoundException If the specified file was not found.
	 */

	public static String[] loadFromFile(String file, String prefix) throws FileNotFoundException
	{
		Scanner m = new Scanner(new File(file));
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
	 * Appends a byte to the end of a file.
	 * 
	 * @param f The file to write to.
	 * @param a An int representing the byte to be written
	 * 
	 * @throws FileNotFoundException If the source file could not be found
	 * @throws IOException If we encountered some sort of read/write error
	 */

	public static void writeByte(String f, int a) throws FileNotFoundException, IOException
	{
		FileOutputStream out = new FileOutputStream(new File(f), true);
		out.write(a);
		out.close();
	}

	/**
	 * Determines if a file is uploadable to a WMF Wiki. Uploadable files currently are
	 * (png|gif|jpg|jpeg|xcf|mid|ogg|ogv|svg|djvu|tiff|tif|oga|pdf).
	 * 
	 * @param f The filename to check
	 * 
	 * @return True if the file is uploadable to a WMF wiki.
	 */

	public static boolean isUploadable(String f)
	{
		return f.matches("(?i).*?\\.(png|gif|jpg|jpeg|xcf|mid|ogg|ogv|svg|djvu|tiff|tif|oga|pdf)");
	}

	/**
	 * Reverses an array of Objects.
	 * 
	 * @param array The array to create a reversed shallow copy of.
	 * 
	 * @return The reversed array (shallow copy of 'array').
	 */

	public static Object[] reverseArray(Object[] array)
	{
		Object[] l = new Object[array.length];
		for (int i = 0; i < array.length; i++)
			l[i] = array[array.length - 1 - i];

		return l;
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
	 * 
	 * @return The contents of the file as a String.
	 * 
	 * @throws FileNotFoundException If the file specified does not exist.
	 */
	public static String fileToString(File f) throws FileNotFoundException
	{
		Scanner m = new Scanner(f);
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
	 * Creates a JPanel form with a SpringLayout.  Fields are dynamically resized when the window size is
	 * modified by the user.  
	 * @param cl The list of containers to work with.  Elements should be in the order, e.g. JLabel1, JTextField1,
	 * JLabel 2, JTextField2, etc.
	 * 
	 * @return A JPanel with a SpringLayout in a form.
	 * @throws UnsupportedOperationException If cl.length == 0 || cl.length % 2 == 1
	 */
	public static JPanel buildForm(Container...cl)
	{
		SpringLayout l = new SpringLayout();
		JPanel pl = new JPanel(l);
		
		//Sanity check.  There must be at least two elements in cl
		if(cl.length == 0 || cl.length % 2 == 1)
			throw new UnsupportedOperationException("Either cl is empty or has an odd number of elements!");
	   
		for(Container c : cl)
			pl.add(c);
		
		l.putConstraint(SpringLayout.WEST, cl[0], 5, SpringLayout.WEST, pl);
		l.putConstraint(SpringLayout.NORTH, cl[0], 5, SpringLayout.NORTH, pl);
		l.putConstraint(SpringLayout.WEST, cl[1], 5, SpringLayout.EAST, cl[0]);
		l.putConstraint(SpringLayout.NORTH, cl[1], 5, SpringLayout.NORTH, pl);
		l.putConstraint(SpringLayout.EAST, pl, 5, SpringLayout.EAST, cl[1]);
		
	   for(int i = 2; i < cl.length; i+=2)
	   {
	   	
			l.putConstraint(SpringLayout.WEST, cl[i], 5, SpringLayout.WEST, pl);
			l.putConstraint(SpringLayout.NORTH, cl[i], 10, SpringLayout.SOUTH, cl[i-2]);
			l.putConstraint(SpringLayout.WEST, cl[i+1], 5, SpringLayout.EAST, cl[i]);
			l.putConstraint(SpringLayout.NORTH, cl[i+1], 5, SpringLayout.SOUTH, cl[i-1]);
			l.putConstraint(SpringLayout.EAST, pl, 5, SpringLayout.EAST, cl[i+1]);
	   }
	   
	   pl.setPreferredSize(new Dimension(500, 250));
	   
	   return pl;
	}
	
	
	/**
	 * Class containing some general central tendency calculators.
	 * 
	 * @author Fastily
	 * 
	 */
	public class FStats
	{
		/**
		 * Represents the dataset we'll be using.
		 */
		private double[] ds;
		
		/**
		 * Constructs an FStats object.  You must provide an array/list of items with >0 items.
		 * 
		 * @param ds The array of doubles representing the data set to use.
		 * @throws IllegalArgumentException If you did not provide an array of length > 0.
		 */
		public FStats(double... ds)
		{
			if (ds.length == 0)
				throw new IllegalArgumentException("'ds' must have a minimum of length 1!");
			this.ds = ds;
		}

		/**
		 * Sets the dataset of doubles.
		 * 
		 * @param ds The array to set the internal dataset to.
		 */
		public void setDataset(double...ds)
		{
			this.ds = ds;
		}
		
		/**
		 * Fetches the internal double[] representing the dataset.
		 * 
		 * @return The internal double[] representing the dataset
		 */
		public double[] getDataset()
		{
			return ds;
		}
		
		/**
		 * Calculates the mean of a set of data.
		 * 
		 * @return The population mean of the data.
		 */
		public double mean()
		{
			double m = 0;
			for (double d : ds)
				m += d;
			return m / ds.length;
		}

		/**
		 * Calculates the median of a set of data. 
		 * 
		 * @return The median of the data.
		 */
		public double median()
		{
			double[] bb = Arrays.copyOf(ds, ds.length);
			Arrays.sort(bb);

			return bb.length % 2 == 1 ? bb[bb.length / 2] : (bb[bb.length / 2] + bb[bb.length / 2 - 1]) / 2;
		}

		/**
		 * Calculates variance of a dataset.
		 * 
		 * @return The variance of the dataset.
		 */
		public double variance()
		{
			double mean = this.mean();
			double var = 0;

			for (int i = 0; i < ds.length; i++)
				var += Math.pow(ds[i] - mean, 2);

			return var/ds.length;
		}

		/**
		 * Performs standard deviation on a set of data
		 * 
		 * @return The standard deviation (i.e. mean distance of each item in the data set from the
		 *         mean of the data)
		 */
		public double standardDeviation()
		{
			return Math.sqrt(this.variance());
		}

	}
	
	
	
}
