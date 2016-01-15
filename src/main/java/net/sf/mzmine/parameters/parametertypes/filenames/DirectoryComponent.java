/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */
package net.sf.mzmine.parameters.parametertypes.filenames;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 */
public class DirectoryComponent extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Text field width.
    private static final int TEXT_FIELD_COLUMNS = 15;

    // Text field font.
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN,
            10);

    // Chooser title.
    private static final String TITLE = "Select Directory";

    // Text field.
    private final JTextField txtDirectory;

    /**
     * Create the component.
     */
    public DirectoryComponent() {

        super(new BorderLayout());

        // Create text field.
        txtDirectory = new JTextField();
        txtDirectory.setColumns(TEXT_FIELD_COLUMNS);
        txtDirectory.setFont(SMALL_FONT);

        // Chooser button.
        final JButton btnFileBrowser = new JButton("...");
        btnFileBrowser.addActionListener(this);

        add(txtDirectory, BorderLayout.CENTER);
        add(btnFileBrowser, BorderLayout.EAST);
    }

    public File getValue() {

        return new File(txtDirectory.getText());
    }

    public void setValue(final File value) {

        txtDirectory.setText(value.getPath());
    }

    @Override
    public void setToolTipText(final String text) {

        txtDirectory.setToolTipText(text);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        // Create chooser.
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogTitle(TITLE);

        // Set current directory.
        final String currentPath = txtDirectory.getText();
        if (currentPath.length() > 0) {

            final File currentFile = new File(currentPath);
            final File currentDir = currentFile.getParentFile();
            if (currentDir != null && currentDir.exists()) {

                chooser.setCurrentDirectory(currentDir);
            }
        }

        // Open chooser.
        if (chooser.showDialog(null, TITLE) == JFileChooser.APPROVE_OPTION) {
            txtDirectory.setText(chooser.getSelectedFile().getPath());
        }
    }
}
