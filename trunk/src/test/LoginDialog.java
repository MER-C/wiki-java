/**
 *  @(#)LoginDialog.java 0.01 29/08/2011
 *  Copyright (C) 2007 - 2011 MER-C and contributors
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version. Additionally
 *  this file is subject to the "Classpath" exception.
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
 *  Simple login dialog to aid offline testing.
 */
public class LoginDialog extends JDialog
{
    public LoginDialog(final Wiki wiki)
    {
        super((JFrame)null, "Log in");

        // login panel
        JPanel login = new JPanel();
        add(login, BorderLayout.CENTER);
        login.setLayout(new GridLayout(2, 2));
        login.add(new JLabel("Username"));
        final JTextField username = new JTextField(10);
        login.add(username);
        login.add(new JLabel("Password"));
        final JPasswordField password = new JPasswordField(10);
        login.add(password);

        // buttons
        JPanel buttons = new JPanel();
        add(buttons, BorderLayout.SOUTH);
        JButton ok = new JButton("OK");
        buttons.add(ok);
        ok.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    setVisible(false);
                    wiki.login(username.getText(), password.getPassword());
                    System.exit(0);
                }
                // todo (if I can be bothered):
                // more specific exception handling
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
        pack();
        setVisible(true);
    }
}
