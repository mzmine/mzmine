/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.filenames;


import java.io.File;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 *
 */
public class FileNameComponent extends HBox implements LastFilesComponent {

  //public static final Font smallFont = new Font("SansSerif", 10);

  private final TextField txtFilename;
  private final LastFilesButton btnLastFiles;
  private final FileSelectionType type;

  public FileNameComponent(List<File> lastFiles, FileSelectionType type,
      final List<ExtensionFilter> filters) {
    // setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

    this.type = type;

    txtFilename = new TextField();
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

    getChildren().addAll(txtFilename, btnLastFiles, btnFileBrowser);
    setAlignment(Pos.CENTER_LEFT);
    setSpacing(5);
    HBox.setHgrow(txtFilename, Priority.ALWAYS);
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
    if (!allowEmptyString && fileName.trim().isEmpty()) {
      return null;
    }
    return getValue();
  }

  public void setToolTipText(String toolTip) {
    txtFilename.setTooltip(new Tooltip(toolTip));
  }

}
