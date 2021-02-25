/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.filenames;


import java.io.File;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

/**
 */
public class FileNameComponent extends FlowPane implements LastFilesComponent {

  public static final Font smallFont = new Font("SansSerif", 10);

  private TextField txtFilename;
  private LastFilesButton btnLastFiles;
  private FileSelectionType type;

  public FileNameComponent(int textfieldcolumns, List<File> lastFiles, FileSelectionType type) {
    // setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

    this.type = type;

    txtFilename = new TextField();
    txtFilename.setPrefColumnCount(textfieldcolumns);
    txtFilename.setFont(smallFont);

    // last used files chooser button
    // on click - set file name to textField
    btnLastFiles = new LastFilesButton("last", file -> txtFilename.setText(file.getPath()));

    Button btnFileBrowser = new Button("...");
    btnFileBrowser.setOnAction(e -> {
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
      File selectedFile = null;
      if(type == FileSelectionType.OPEN)
        selectedFile = fileChooser.showOpenDialog(null);
      else
        selectedFile = fileChooser.showSaveDialog(null);
      
      if (selectedFile == null)
        return;
      txtFilename.setText(selectedFile.getPath());
    });

    getChildren().addAll(txtFilename, btnLastFiles, btnFileBrowser);

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

  public File getValue(boolean allowEmptyString) {
    String fileName = txtFilename.getText();
    if (allowEmptyString == false && fileName.trim().isEmpty()) {
      return null;
    }
    return getValue();
  }

  public void setValue(File value) {
    txtFilename.setText(value.getPath());
  }

  public void setToolTipText(String toolTip) {
    txtFilename.setTooltip(new Tooltip(toolTip));
  }

}
