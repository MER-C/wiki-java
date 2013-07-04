package org.wikiutils;

import java.io.IOException;
import javax.security.auth.login.FailedLoginException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.wikipedia.Wiki;

/**
 * Contains a few useful login methods
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.DateUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 * @see org.wikiutils.WikiUtils
 */
public class LoginUtils
{
	
	/**
	 * Hiding constructor from JavaDoc
	 */
	private LoginUtils()
	{
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
		JTextField u = new JTextField(20);
		JPasswordField px = new JPasswordField(20);
		
		if (JOptionPane.showConfirmDialog(null, GUIUtils.buildForm(title, new JLabel("Username:", JLabel.TRAILING), u, new JLabel("Password:",
				JLabel.TRAILING), px), title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
			System.exit(0);
		
		return new String[] { u.getText().trim(), new String(px.getPassword()) };
	}
}