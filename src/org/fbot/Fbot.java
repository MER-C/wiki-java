package org.fbot;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.security.auth.login.FailedLoginException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.wikipedia.Wiki;

/**
 * Contains static MediaWiki bot/API methods built off MER-C's Wiki.java. 
 * Visit our Google Code Project home <a href="http://code.google.com/p/wiki-java/">here!</a>
 * This code and project are licensed under the terms of the <a
 * href="http://www.gnu.org/copyleft/gpl.html">GNU GPL v3 license</a>
 * 
 * @see org.fbot.FbotUtil
 * @see org.wikipedia.Wiki
 * @see org.fbot.FbotParse
 * 
 * @author Fastily
 */

public class Fbot
{
	// Hiding constructor from Javadoc
	private Fbot()
	{
		// do nothing
	}

	/**
	 * Generic login method which turns off maxlag and throttle. Catches IOExceptions 8 times, and
	 * then exits program.
	 * 
	 * @param wiki Wiki object to perform changes on
	 * @param user User to login as, without "User:" prefix
	 * @param p User's password, in the form of a char array.
	 * 
	 */

	public static void loginAndSetPrefs(Wiki wiki, String user, char[] p)
	{
		wiki.setThrottle(1);
		wiki.setMaxLag(-1);

		short i = 0;
		boolean success = false;
		do
		{
			try
			{
				wiki.login(user, p);
				success = true;
			}
			catch (IOException e)
			{
				System.out.println("Encountered IOException.  This was try #" + i);
				if (++i > 8)
					System.exit(1);
			}
			catch (FailedLoginException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		} while (!success);
	}

	/**
	 * Login with loginAndSetPrefs(), but using a GUI. Throttle auto-set to 6 edits/min.
	 * 
	 * @param wiki Wiki object to perform changes on
	 * 
	 * @see #loginAndSetPrefs(Wiki, String, char[])
	 * 
	 */
	public static void guiLogin(Wiki wiki)
	{
		String[] b = showLoginScreen("Login");
		wiki.setMaxLag(-1);
		try
		{
		  wiki.login(b[0], b[1].toCharArray());
		}
		catch(Throwable e)
		{
			JOptionPane.showMessageDialog(null, "Username/Password did not match or we encountered a network issue.  Program will now exit.");
			System.exit(1);
		}
		wiki.setThrottle(5);
	}


	/**
	 * Creates and shows user a login screen, returning user login details in a String array.  Does not check to see if login details are 
	 * valid.
	 * 
	 * @param title The title of the login window
	 * @return An array of length 2, in the form {username, password}.
	 */
	public static String[] showLoginScreen(String title)
	{
		JTextField u = new JTextField(12);
		JPasswordField px = new JPasswordField(12);
		
		if (JOptionPane.showConfirmDialog(null, FbotUtil.buildForm(title, new JLabel("Username:", JLabel.TRAILING), u, new JLabel("Password:",
				JLabel.TRAILING), px), title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
			System.exit(0);
		
		return new String[] { u.getText().trim(), new String(px.getPassword()) };
	}
	
	/**
	 * Gets the target of the redirect page. </br><b>PRECONDITION</b>: <tt>redirect</tt> must be a
	 * Redirect.
	 * 
	 * @param redirect The title of the redirect to get the target for.
	 * @param wiki The wiki object to use.
	 * 
	 * @return String The title of the redirect's target.
	 * 
	 * @throws UnsupportedOperationException If the page was not a redirect page.
	 * @throws IOException If network error
	 */

	public static String getRedirectTarget(String redirect, Wiki wiki) throws IOException
	{
		String text = wiki.getPageText(redirect).trim();

		if (text.matches("(?si)^#(redirect)\\s*?\\[\\[.+?\\]\\].*?"))
			return text.substring(text.indexOf("[[") + 2, text.indexOf("]]"));

		throw new UnsupportedOperationException("Parameter passed in is not a redirect page!");
	}

	/**
	 * Checks to see if a given page exists on Wiki (page is not a 'red-link')
	 * 
	 * @param page The page to check for.
	 * @param wiki The wiki object to use.
	 * 
	 * @return True if the page exists.
	 * 
	 * @throws IOException If network error
	 */

	public static boolean exists(String page, Wiki wiki) throws IOException
	{
		return ((Boolean) wiki.getPageInfo(page).get("exists")).booleanValue();
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

	/**
	 * Downloads a file
	 * 
	 * @param title The title of the file to download <ins>on the Wiki</ins> <b>excluding</b> the "
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
	 * 
	 */
	public static void downloadFile(String title, String localpath, Wiki wiki) throws IOException, FileNotFoundException
	{
		FileOutputStream fos = new FileOutputStream(localpath);
		fos.write(wiki.getImage(title));
		fos.close();
	}
}
