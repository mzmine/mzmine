/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.filenames;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.concurrent.FutureTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javafx.application.Platform;
import javafx.stage.FileChooser;

/**
 */
public class FileNameComponent extends JPanel
        implements ActionListener, LastFilesComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JTextField txtFilename;
    private JLastFilesButton btnLastFiles;

    public FileNameComponent(int textfieldcolumns, List<File> lastFiles) {
        setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

        txtFilename = new JTextField();
        txtFilename.setColumns(textfieldcolumns);
        txtFilename.setFont(smallFont);
        add(txtFilename);

        // last used files chooser button
        // on click - set file name to textField
        btnLastFiles = new JLastFilesButton("last",
                file -> txtFilename.setText(file.getPath()));
        add(btnLastFiles);

        JButton btnFileBrowser = new JButton("...");
        btnFileBrowser.addActionListener(this);
        add(btnFileBrowser);

        setLastFiles(lastFiles);
    }

    @Override
    public void setLastFiles(List<File> value) {
        btnLastFiles.setLastFiles(value);
    }

    public File getValue() {
        String fileName = txtFilename.getText();
        File file = new File(fileName);
        return file;
    }

    public void setValue(File value) {
        txtFilename.setText(value.getPath());
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        // Create chooser.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");

        // Set current directory.
        final String currentPath = txtFilename.getText();
        if (currentPath.length() > 0) {

            final File currentFile = new File(currentPath);
            final File currentDir = currentFile.getParentFile();
            if (currentDir != null && currentDir.exists()) {
                fileChooser.setInitialDirectory(currentDir);
            }
        }

        // Open chooser.
        final FutureTask<File> task = new FutureTask<>(
                () -> fileChooser.showOpenDialog(null));
        Platform.runLater(task);
        try {
            File selectedFile = task.get();
            if (selectedFile == null)
                return;
            txtFilename.setText(selectedFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setToolTipText(String toolTip) {
        txtFilename.setToolTipText(toolTip);
    }

}
