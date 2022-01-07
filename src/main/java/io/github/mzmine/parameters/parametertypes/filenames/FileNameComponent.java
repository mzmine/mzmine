/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes.filenames;


import java.io.File;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 *
 */
public class FileNameComponent extends FlowPane implements LastFilesComponent {

  //public static final Font smallFont = new Font("SansSerif", 10);

  private TextField txtFilename;
  private LastFilesButton btnLastFiles;
  private FileSelectionType type;

  public FileNameComponent(int textfieldcolumns, List<File> lastFiles, FileSelectionType type,
      final List<ExtensionFilter> filters) {
    // setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

    this.type = type;

    txtFilename = new TextField();
    txtFilename.setPrefColumnCount(textfieldcolumns);
    //txtFilename.setFont(smallFont);

    // last used files chooser button
    // on click - set file name to textField
    btnLastFiles = new LastFilesButton("last", file -> txtFilename.setText(file.getPath()));

    Button btnFileBrowser = new Button("Select");
    btnFileBrowser.setOnAction(e -> {
      // Create chooser.
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select file");
      if (filters != null) {
        fileChooser.getExtensionFilters().addAll(filters);
      }

      // Set current directory.
      boolean initDirFound = false;
      final String currentPath = txtFilename.getText();
      try {
        if (currentPath.length() > 0) {

          final File currentFile = new File(currentPath);
          final File currentDir = currentFile.getParentFile();
          if (currentDir != null && currentDir.exists()) {
            fileChooser.setInitialDirectory(currentDir);
            initDirFound = true;
          }
        }
      } catch (Exception ex) {
      }

      if (!initDirFound && lastFiles != null && !lastFiles.isEmpty()) {
        final File lastDir = lastFiles.get(0).getParentFile();
        if (lastDir != null && lastDir.exists()) {
          fileChooser.setInitialDirectory(lastDir);
        }
      }
      // Open chooser.
      File selectedFile = null;
      if (type == FileSelectionType.OPEN) {
        selectedFile = fileChooser.showOpenDialog(null);
      } else {
        selectedFile = fileChooser.showSaveDialog(null);
      }

      if (selectedFile == null) {
        return;
      }
      txtFilename.setText(selectedFile.getPath());
    });

    HBox hBox = new HBox(txtFilename, btnLastFiles, btnFileBrowser);
    hBox.setSpacing(7d);
    hBox.setAlignment(Pos.CENTER_LEFT);
    super.getChildren().add(hBox);

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
    txtFilename.setText(value != null ? value.getPath() : "");
  }

  public File getValue(boolean allowEmptyString) {
    String fileName = txtFilename.getText();
    if (allowEmptyString == false && fileName.trim().isEmpty()) {
      return null;
    }
    return getValue();
  }

  public void setToolTipText(String toolTip) {
    txtFilename.setTooltip(new Tooltip(toolTip));
  }

}
