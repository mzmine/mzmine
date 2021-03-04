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

import com.google.common.collect.ImmutableList;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class FileNamesComponent extends FlowPane {

  public static final Font smallFont = new Font("SansSerif", 10);

  private TextArea txtFilename;
  private CheckBox useSubFolders;

  private final List<ExtensionFilter> filters;

  public FileNamesComponent(List<ExtensionFilter> filters) {

    this.filters = ImmutableList.copyOf(filters);

    // setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

    txtFilename = new TextArea();
    txtFilename.setPrefColumnCount(40);
    txtFilename.setPrefRowCount(6);
    txtFilename.setFont(smallFont);

    Button btnFileBrowser = new Button("Select");
    btnFileBrowser.setOnAction(e -> {
      // Create chooser.
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select files");

      fileChooser.getExtensionFilters().addAll(this.filters);

      String currentPaths[] = txtFilename.getText().split("\n");
      if (currentPaths.length > 0) {
        File currentFile = new File(currentPaths[0].trim());
        File currentDir = currentFile.getParentFile();
        if (currentDir != null && currentDir.exists()) {
          fileChooser.setInitialDirectory(currentDir);
        }
      }

      // Open chooser.
      List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
      if (selectedFiles == null) {
        return;
      }
      setValue(selectedFiles.toArray(new File[0]));
    });

    useSubFolders = new CheckBox("In sub folders");
    useSubFolders.setSelected(false);

    Button btnClear = new Button("Clear");
    btnClear.setOnAction(e -> {
      txtFilename.setText("");
    });

    VBox vbox = new VBox(btnFileBrowser, btnClear, useSubFolders);

    List<Button> directoryButtons = createFromDirectoryBtns(filters);
    vbox.getChildren().addAll(directoryButtons);

    HBox hbox = new HBox(txtFilename, vbox);
    getChildren().addAll(hbox);
  }

  private List<Button> createFromDirectoryBtns(List<ExtensionFilter> filters) {
    List<Button> btns = new ArrayList<>();
    for (ExtensionFilter filter : filters) {
      if (filter.getExtensions().isEmpty() || filter.getExtensions().get(0).equals("*.*")) {
        continue;
      }
      String name =
          filter.getExtensions().size() > 3 ? "Multiple" : "All " + filter.getExtensions().get(0);

      Button btnFromDirectory = new Button(name);
      btnFromDirectory.setTooltip(new Tooltip("All files in folder (sub folders)"));
      btns.add(btnFromDirectory);
      btnFromDirectory.setOnAction(e -> {
        // Create chooser.
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select a folder");

        String currentPaths[] = txtFilename.getText().split("\n");
        if (currentPaths.length > 0) {
          File currentFile = new File(currentPaths[0].trim());
          File currentDir = currentFile.getParentFile();
          if (currentDir != null && currentDir.exists()) {
            fileChooser.setInitialDirectory(currentDir);
          }
        }

        // Open chooser.
        File dir = fileChooser.showDialog(null);
        if (dir == null) {
          return;
        }

        List<File[]> filesInDir = FileAndPathUtil
            .findFilesInDir(dir, filter, useSubFolders.isSelected());
        // all files in dir or sub dirs
        setValue(filesInDir.stream().flatMap(sub -> Arrays.stream(sub)).toArray(File[]::new));
      });
    }
    return btns;
  }

  public File[] getValue() {
    String fileNameStrings[] = txtFilename.getText().split("\n");
    List<File> files = new ArrayList<>();
    for (String fileName : fileNameStrings) {
      if (fileName.trim().equals("")) {
        continue;
      }
      files.add(new File(fileName.trim()));
    }
    return files.toArray(new File[0]);
  }

  public void setValue(File[] value) {
    if (value == null) {
      txtFilename.setText("");
      return;
    }
    StringBuilder b = new StringBuilder();
    for (File file : value) {
      b.append(file.getPath());
      b.append("\n");
    }
    txtFilename.setText(b.toString());
  }

  public void actionPerformed(ActionEvent event) {

  }

  public void setToolTipText(String toolTip) {
    txtFilename.setTooltip(new Tooltip(toolTip));
  }

}
