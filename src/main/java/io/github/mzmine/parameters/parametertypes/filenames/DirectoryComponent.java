/*
 * Copyright 2006-2022 The MZmine Development Team
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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;

/**
 *
 */
public class DirectoryComponent extends BorderPane {

  // Text field width.
  private static final int TEXT_FIELD_COLUMNS = 15;

  // Text field font.
  private static final Font SMALL_FONT = new Font("SansSerif", 10);

  // Chooser title.
  private static final String TITLE = "Select Directory";

  // Text field.
  private final TextField txtDirectory;

  /**
   * Create the component.
   */
  public DirectoryComponent() {

    // Create text field.
    txtDirectory = new TextField();
    txtDirectory.setPrefColumnCount(TEXT_FIELD_COLUMNS);
    txtDirectory.setFont(SMALL_FONT);

    // Chooser button.
    final Button btnFileBrowser = new Button("...");
    btnFileBrowser.setOnAction(e -> {
      // Create chooser.
      DirectoryChooser fileChooser = new DirectoryChooser();
      fileChooser.setTitle(TITLE);

      // Set current directory.
      final String currentPath = txtDirectory.getText();
      if (currentPath.length() > 0) {

        final File currentFile = new File(currentPath);
        final File currentDir = currentFile.getParentFile();
        if (currentDir != null && currentDir.exists()) {
          fileChooser.setInitialDirectory(currentDir);
        }
      }

      // Open chooser.
      File selectedFile = fileChooser.showDialog(null);
      if (selectedFile == null) {
        return;
      }
      txtDirectory.setText(selectedFile.getPath());

    });

    setCenter(txtDirectory);
    setRight(btnFileBrowser);
  }

  public File getValue() {

    return new File(txtDirectory.getText());
  }

  public void setValue(final File value) {
    txtDirectory.setText(value == null ? "" : value.getPath());
  }

  public void setToolTipText(final String text) {
    txtDirectory.setTooltip(new Tooltip(text));
  }

}
