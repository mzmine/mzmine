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
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class FileNamesComponent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JTextArea txtFilename;

    private FileFilter[] filters;

    public FileNamesComponent(FileFilter[] filters) {

        this.filters = filters;

        setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

        txtFilename = new JTextArea();
        txtFilename.setColumns(40);
        txtFilename.setRows(6);
        txtFilename.setFont(smallFont);
        add(new JScrollPane(txtFilename));

        JButton btnFileBrowser = new JButton("...");
        btnFileBrowser.addActionListener(this);
        add(btnFileBrowser);

    }

    public File[] getValue() {
        String fileNameStrings[] = txtFilename.getText().split("\n");
        List<File> files = new ArrayList<>();
        for (String fileName : fileNameStrings) {
            if (fileName.trim().equals(""))
                continue;
            files.add(new File(fileName.trim()));
        }
        return files.toArray(new File[0]);
    }

    public void setValue(File[] value) {
        if (value == null)
            return;
        StringBuilder b = new StringBuilder();
        for (File file : value) {
            b.append(file.getPath());
            b.append("\n");
        }
        txtFilename.setText(b.toString());
    }

    public void actionPerformed(ActionEvent e) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        if ((filters != null) && (filters.length > 0)) {
            for (FileFilter f : filters)
                fileChooser.addChoosableFileFilter(f);
            fileChooser.setFileFilter(filters[0]);

        }
        String currentPaths[] = txtFilename.getText().split("\n");
        if (currentPaths.length > 0) {
            File currentFile = new File(currentPaths[0].trim());
            File currentDir = currentFile.getParentFile();
            if (currentDir != null && currentDir.exists())
                fileChooser.setCurrentDirectory(currentDir);
        }

        int returnVal = fileChooser.showDialog(null, "Select files");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFiles[] = fileChooser.getSelectedFiles();
            setValue(selectedFiles);
        }
    }

    @Override
    public void setToolTipText(String toolTip) {
        txtFilename.setToolTipText(toolTip);
    }

}
