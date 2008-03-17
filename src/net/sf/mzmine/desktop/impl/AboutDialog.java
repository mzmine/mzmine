/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.desktop.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    public static final String ABOUT_TEXT_FILE = "AboutText.html";

    /**
     * Creates new form AboutDialog
     */
    public AboutDialog() {

        super(MZmineCore.getDesktop().getMainFrame(), "About MZmine 2", true);

        // Read the contents of the about dialog
        InputStream aboutTextStream = AboutDialog.class.getResourceAsStream(ABOUT_TEXT_FILE);
        InputStreamReader isr = new InputStreamReader(aboutTextStream);
        BufferedReader reader = new BufferedReader(isr);
        StringBuffer contentString = new StringBuffer();
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                contentString.append(line);
            }
        } catch (IOException e) {
            // ignore
        }

        JPanel buttonPanel = new JPanel();
        GUIUtils.addButtonInPanel(buttonPanel, "OK", this);
        add(buttonPanel, BorderLayout.SOUTH);

        JEditorPane textPane = new JEditorPane();
        textPane.setBorder(new MatteBorder(new Insets(0, 10, 10, 10),
                Color.white));
        textPane.setContentType("text/html");
        textPane.setText(contentString.toString());
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
