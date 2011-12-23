/**
 *  @(#)LoggedInTests.java
 *  Copyright (C) 2011 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package test;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.wikipedia.Wiki;

/**
 *  Tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInTests
{
    private static Wiki wiki = new Wiki("en.wikipedia.org");

    public static void main(String[] args)
    {
        // need a login dialog to input password
        // don't want to store plaintext passwords on our hard disk, do we?
        final JDialog dialog = new JDialog((JFrame)null, "Log in");

        // login panel
        JPanel login = new JPanel();
        dialog.add(login, BorderLayout.CENTER);
        login.setLayout(new GridLayout(2, 2));
        login.add(new JLabel("Username"));
        final JTextField username = new JTextField(10);
        login.add(username);
        login.add(new JLabel("Password"));
        final JPasswordField password = new JPasswordField(10);
        login.add(password);

        // buttons
        JPanel buttons = new JPanel();
        dialog.add(buttons, BorderLayout.SOUTH);
        JButton ok = new JButton("OK");
        buttons.add(ok);
        ok.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    dialog.setVisible(false);
                    test(username.getText(), password.getPassword());
                    System.exit(0);
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(null, "Exception: " + ex);
                    ex.printStackTrace();
                }
            }
        });
        JButton cancel = new JButton("Cancel");
        buttons.add(cancel);
        cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     *  Testing occurs here.
     *  @param username the username to log in
     *  @param password the password to use
     */
    public static void test(String username, char[] password) throws Exception
    {
        // login
        wiki.login(username, password);

        // watchlist
        for (String page : wiki.getRawWatchlist())
            System.out.println(page);

        // email
        // wiki.emailUser(wiki.getCurrentUser(), "Testing", "Blah", false);

        // BOT TESTS
        // org.wikipedia.bots.CPBot.main(new String[0]);

        // edit
        wiki.edit("User:MER-C/BotSandbox", "Testing " + Math.random(), "test", false, false);
        // wiki.edit("User:MER-C/BotSandbox", "Testing " + Math.random(), "test", false, false);
    }
}
