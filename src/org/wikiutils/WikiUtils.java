package org.wikiutils;

import java.io.IOException;
import java.util.ArrayList;
import org.wikipedia.Wiki;

/**
 * Contains several editing or log action related methods.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.DateUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 */
public class WikiUtils
{
	
	/**
	 * Hiding constructor from JavaDoc
	 */
	private WikiUtils()
	{
	}
	
	/**
	 * Returns all the items in an array that are within the specified namespace.
	 * 
	 * @param list The list of items to use
	 * @param namespace The namespace of items to return
	 * @param wiki The wiki object to use.
	 * 
	 * @return The list of items in the list that were in the specified namespace.
	 * 
	 * @throws IOException If we had an issue populating namespace cache
	 * 
	 */

	public static String[] listNamespaceSort(String[] list, int namespace, Wiki wiki) throws IOException
	{
		ArrayList<String> l = new ArrayList<String>();
		for (String s : list)
		{
			if (wiki.namespace(s) == namespace)
				l.add(s);
		}

		return l.toArray(new String[0]);
	}
	
	/**
	 * Deletes all the elements in an array and their associated talk pages
	 * 
	 * @param list The array to use
	 * @param reason The reason to use while deleting
	 * @param talkReason The reason to use when deleting talk pages of the pages we're deleting.
	 *           Specify "null" if talk pages are not to be deleted
	 * @param wiki The wiki object to use.
	 * 
	 * @return An array containing the elements we were unable to delete.
	 */

	public static String[] arrayNuke(String[] list, String reason, String talkReason, Wiki wiki)
	{
		ArrayList<String> f = new ArrayList<String>();
		for (String s : list)
		{
			try
			{
				wiki.delete(s, reason);
			}
			catch (Throwable e)
			{
				f.add(s);
				continue;
			}

			if (talkReason != null)
			{
				try
				{
					wiki.delete(wiki.getTalkPage(s), talkReason);
				}
				catch (Throwable e)
				{
					// We'll probably only be here if we tried to delete a talk page of a talk page :P
				}
			}

		}
		return f.toArray(new String[0]);
	}
	
	
	/**
	 * Attempts to add specified text to the end of each page in a list.
	 * 
	 * @param pages The list of pages to use
	 * @param text The text to append
	 * @param summary Edit summary to use.
	 * @param wiki The wiki object to use.
	 * 
	 */
	public static void addTextList(String[] pages, String text, String summary, Wiki wiki)
	{
		for (String page : pages)
		{
			try
			{
				wiki.edit(page, wiki.getPageText(text) + text, summary);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Replaces a file with another for a given list of pages.
	 * 
	 * @param list The list of pages to perform replacement on
	 * @param file The file to be replaced, without the "File:" prefix
	 * @param replacement The file to replace the first file with, without the "File:" prefix
	 * @param summary The edit summary to use
	 * @param wiki The wiki object to use.
	 * 
	 */

	public static void fileReplace(String[] list, String file, String replacement, String summary, Wiki wiki)
	{
		file = file.replace("_", " "); // need to guarantee that we aren't using underscores
		String regex = "(?i)(" + file + "|" + file.replace(" ", "_") + ")";

		for (String page : list)
		{
			try
			{
				wiki.edit(page, wiki.getPageText(page).replaceAll(regex, replacement), summary);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
}