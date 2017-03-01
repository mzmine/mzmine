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

package net.sf.mzmine.parameters.parametertypes.filenames;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 */
public class FileNameComponent extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JTextField txtFilename;

    public FileNameComponent(int textfieldcolumns) {

        setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

        txtFilename = new JTextField();
        txtFilename.setColumns(textfieldcolumns);
        txtFilename.setFont(smallFont);
        add(txtFilename);

        JButton btnFileBrowser = new JButton("...");
        btnFileBrowser.addActionListener(this);
        add(btnFileBrowser);

    }

    public File getValue() {
        String fileName = txtFilename.getText();
        File file = new File(fileName);
        return file;
    }

    public void setValue(File value) {
        txtFilename.setText(value.getPath());
    }

    public void actionPerformed(ActionEvent e) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);

        String currentPath = txtFilename.getText();
        if (currentPath.length() > 0) {
            File currentFile = new File(currentPath);
            File currentDir = currentFile.getParentFile();
            if (currentDir != null && currentDir.exists())
                fileChooser.setCurrentDirectory(currentDir);
        }

        int returnVal = fileChooser.showDialog(null, "Select file");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getPath();
            txtFilename.setText(selectedPath);
        }
    }

    @Override
    public void setToolTipText(String toolTip) {
        txtFilename.setToolTipText(toolTip);
    }

}
