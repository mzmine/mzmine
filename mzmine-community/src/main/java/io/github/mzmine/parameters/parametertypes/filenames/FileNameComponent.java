/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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


import io.github.mzmine.modules.io.download.DownloadAsset;
import io.github.mzmine.modules.io.download.DownloadAssetButton;
import io.github.mzmine.modules.io.download.ExternalAsset;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class FileNameComponent extends HBox implements LastFilesComponent {

  private static final Logger logger = Logger.getLogger(FileNameComponent.class.getName());
  private final TextField txtFilename;
  private final LastFilesButton btnLastFiles;
  private final FileSelectionType type;
  private final List<ExtensionFilter> filters;


  public FileNameComponent(List<File> lastFiles, FileSelectionType type,
      final List<ExtensionFilter> filters) {
    this(lastFiles, type, filters, null);
  }

  public FileNameComponent(final List<File> lastFiles, final FileSelectionType type,
      final List<ExtensionFilter> filters, final ExternalAsset extAsset,
      final List<DownloadAsset> downloadLinks) {
    this(lastFiles, type, filters, null, extAsset, downloadLinks);
  }


  public FileNameComponent(List<File> lastFiles, FileSelectionType type,
      final List<ExtensionFilter> filters, @Nullable Consumer<File> exportExamples) {
    this(lastFiles, type, filters, exportExamples, null, List.of());
  }

  private FileNameComponent(final List<File> lastFiles, final FileSelectionType type,
      final List<ExtensionFilter> filters, @Nullable Consumer<File> exportExamples,
      final ExternalAsset extAsset, final List<DownloadAsset> downloadLinks) {
    this.type = type;
    this.filters = filters;

    txtFilename = new TextField();
    //txtFilename.setFont(smallFont);

    // last used files chooser button
    // on click - set file name to textField
    btnLastFiles = new LastFilesButton("last", file -> txtFilename.setText(file.getPath()));

    Button btnFileBrowser = new Button("Select");
    btnFileBrowser.setOnAction(e -> {
      var selectedFile = openSelectDialog(lastFiles, type, filters);

      if (selectedFile == null) {
        return;
      }
      txtFilename.setText(selectedFile.getPath());
    });

    getChildren().addAll(txtFilename, btnLastFiles, btnFileBrowser);
    if (exportExamples != null) {
      Button button = new Button("Example");
      Tooltip.install(button, new Tooltip("Export an example file with expected format"));
      button.setOnAction(event -> {
        var selectedFile = openSelectDialog(lastFiles, FileSelectionType.SAVE, filters);

        if (selectedFile == null) {
          return;
        }
        exportExamples.accept(selectedFile);
      });
      getChildren().add(button);
    }
    if (extAsset != null) {
      var downloadButton = new DownloadAssetButton(extAsset, downloadLinks);
      downloadButton.setOnDownloadFinished(file -> setValue(file));
      getChildren().add(downloadButton);
    }

    setAlignment(Pos.CENTER_LEFT);
    setSpacing(5);
    HBox.setHgrow(txtFilename, Priority.ALWAYS);
    setLastFiles(lastFiles);
    initDragDropped();
  }

  public File openSelectDialog(final List<File> lastFiles, final FileSelectionType type,
      final List<ExtensionFilter> filters) {
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
    return selectedFile;
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

  private void initDragDropped() {
    txtFilename.setOnDragOver(e -> {
      if (e.getGestureSource() != this && e.getGestureSource() != txtFilename && e.getDragboard()
          .hasFiles()) {
        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      e.consume();
    });
    txtFilename.setOnDragDropped(e -> {
      if (e.getDragboard().hasFiles()) {
        final List<File> files = e.getDragboard().getFiles();
        final List<String> patterns = filters.stream().flatMap(f -> f.getExtensions().stream())
            .map(extension -> extension.toLowerCase().replace("*", "").toLowerCase()).toList();

        // use the first match of the dropped file
        for (File file : files) {
          if (patterns.stream()
              .anyMatch(filter -> file.getAbsolutePath().toLowerCase().endsWith(filter))) {
            txtFilename.setText(file.getPath());
            break;
          }
        }

        e.setDropCompleted(true);
        e.consume();
      }
    });
  }
}
