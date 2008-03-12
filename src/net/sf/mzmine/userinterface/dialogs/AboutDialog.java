/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class represents the About dialog box.
 * 
 */
public class AboutDialog extends JDialog implements ActionListener {

    // Contents of the dialog, this string is available (in editable format) in
    // file AboutText.html
    private String contentString = "<html>  <center> 	<h1>MZmine 2</h1> 	<h3>under construction</h3>  	<p> 	Copyright (c) 2005-2008 MZmine Development Team 	</p> </center>  <p> Please use mailing list mzmine-devel@lists.sourceforge.net to contact the MZmine Development Team. </p>  <br> <h3>License information for MZmine</h3>  <p> This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version. </p>  <p> This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. </p>  <p> You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. </p>  <br> <h3>Information of included third-party libraries</h3>  <p>TODO: update this section.</p>   </body> </html>";

    /**
     * Creates new form AboutDialog
     */
    public AboutDialog() {

        super(MZmineCore.getDesktop().getMainFrame(), "About MZmine", true);

        JPanel buttonPanel = new JPanel();
        GUIUtils.addButtonInPanel(buttonPanel, "OK", this);
        add(buttonPanel, BorderLayout.SOUTH);

        JEditorPane textPane = new JEditorPane();
        textPane.setBorder(new MatteBorder(new Insets(0, 10, 10, 10),
                Color.white));
        textPane.setContentType("text/html");
        textPane.setText(contentString);
        textPane.setEditable(false);
        textPane.setCaretPosition(0);

        JScrollPane textScroll = new JScrollPane();
        textScroll.setViewportView(textPane);
        add(textScroll, BorderLayout.CENTER);

        pack();
        setBounds(0, 0, 600, 300);
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        dispose();
    }

}
