package org.wikiutils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Contains several GUI methods that could be useful for editing applications
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.DateUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 * @see org.wikiutils.WikiUtils
 */
public class GUIUtils
{
	/**
	 * Hiding constructor from JavaDoc
	 */
	private GUIUtils()
	{
	}

	/**
	 * Shows a JOptionPane with the stacktrace/backtrace for the Throwable passed in. Useful in GUI
	 * applications.
	 * 
	 * @param e The Throwable object to print a stack trace for
	 */
	public static void showStackTrace(Throwable e)
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		JTextArea t = new JTextArea(sw.toString());
		t.setEditable(false);
		JOptionPane.showMessageDialog(null, t, "Fatal Error!", JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Creates a form in the form of a JPanel. Fields are dynamically resized when the window size is
	 * modified by the user.
	 * 
	 * @param title Title to use in the border. Specify null if you don't want one. Specify empty
	 *           string if you want just border.
	 * @param cl The list of containers to work with. Elements should be in the order, e.g. JLabel1,
	 *           JTextField1, JLabel 2, JTextField2, etc.
	 * 
	 * @return A JPanel with a SpringLayout in a form.
	 * @throws UnsupportedOperationException If cl.length == 0 || cl.length % 2 == 1
	 */
	public static JPanel buildForm(String title, JComponent... cl)
	{
		JPanel pl = new JPanel(new GridBagLayout());

		// Sanity check. There must be at least two elements in cl
		if (cl.length == 0 || cl.length % 2 == 1)
			throw new UnsupportedOperationException("Either cl is empty or has an odd number of elements!");

		if (title != null)
			pl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),
					BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		for (int i = 0; i < cl.length; i += 2)
		{
			c.gridx = 0;
			c.gridy = i;
			c.anchor = GridBagConstraints.EAST; // should anchor East
			pl.add(cl[i], c);

			c.anchor = GridBagConstraints.CENTER; // reset anchor to default

			c.weightx = 0.5; // Fill weights
			c.gridx = 1;
			c.gridy = i;
			c.ipady = 5; // sometimes components render funky when there is no extra vertical buffer
			pl.add(cl[i + 1], c);

			// reset default values for next iteration
			c.weightx = 0;
			c.ipady = 0;
		}

		return pl;
	}

	/**
	 * Creates a JFileChooser that allows user to select multiple directories. Program is exited if
	 * user cancels/closes the dialog box.
	 * 
	 * @param starting Starting directory. If you want to use the user's home directory, I suggest
	 *           using <tt>System.getProperty("user.home")</tt>
	 * @return A list of Files representing directories the user selected.
	 * @throws IllegalArgumentException If the solitary param is non-existent or not a directory.
	 */
	public static File[] directoryFetch(File starting)
	{
		if (!starting.isDirectory()) // idiot proofing
			throw new IllegalArgumentException("Specified directory does not exist or is not a directory");

		JFileChooser fc = new JFileChooser(starting);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle("select directories");
		fc.setMultiSelectionEnabled(true);

		if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			System.exit(0);

		return fc.getSelectedFiles();
	}
	
	/**
	 * Returns a trailing JLabel with specified String.
	 * 
	 * @param s The string to make into a label.
	 * @return The JLabel.
	 */
	public static JLabel makeLabel(String s)
	{
		return new JLabel(s, JLabel.TRAILING);
	}
}