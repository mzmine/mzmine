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
