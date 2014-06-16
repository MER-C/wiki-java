package org.wikiutils;

import java.util.ArrayList;
import java.util.Arrays;
import org.wikipedia.Wiki;

/**
 * Contains a few generic data structure related methods.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.DateUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 * @see org.wikiutils.WikiUtils
 */
public class CollectionUtils
{
	/**
	 * Hiding constructor from JavaDoc
	 */
	private CollectionUtils()
	{
	}

	/**
	 * Splits an array of objects into an array of smaller arrays. Useful for multithreaded bots.
	 * CAVEAT: if splits > z.length, splits will be set to z.length.
	 * 
	 * @param z The array we'll be splitting
	 * @param splits The number of sub-arrays you want.
	 * 
	 */

	public static Object[][] arraySplitter(Object[] z, int splits)
	{

		if (splits > z.length)
			splits = z.length;

		Object[][] xf;

		if (splits == 0)
		{
			xf = new Object[][] { z };
			return xf;
		}
		else
		{
			xf = new Object[splits][];
			for (int i = 0; i < splits; i++)
			{
				Object[] temp;
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
	 * Uses a list of truth values to extract corresponding objects from an another array. Useful
	 * assistant method for Wiki.exists(). If ol.length != bl.length, the method returns null.
	 * Caveat: You will have to cast each item individally if you wish to treat it as anything other
	 * than an Object.
	 * 
	 * @param ol The object list to use
	 * @param bl The corresponding boolean list
	 * @param searchvalue The values to look for in bl. Any value in bl matching searchvalue at a
	 *           given index will cause the corresponding object in ol to be extracted and returned
	 *           in the new array.
	 * 
	 * @return An Object array containing the corresponding values from bl.
	 */
	public static Object[] fetchCorresponding(Object[] ol, boolean[] bl, boolean searchvalue)
	{
		if (ol.length != bl.length)
			return null;

		ArrayList<Object> l = new ArrayList<Object>();
		for (int i = 0; i < ol.length; i++)
			if (bl[i] == searchvalue)
				l.add(ol[i]);

		return l.toArray(new Object[0]);
	}

	/**
	 * Determines if two String arrays share any elements.
	 * 
	 * @param a List 1
	 * @param b List 2
	 * @return True if the two lists share any elements.
	 */
	public static boolean arraysShareElement(String[] a, String[] b)
	{
		return Wiki.intersection(a, b).length > 0;
	}
}