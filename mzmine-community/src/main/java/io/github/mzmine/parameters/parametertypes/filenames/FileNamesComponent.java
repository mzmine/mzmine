/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import com.google.common.collect.ImmutableList;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.modules.io.download.DownloadAsset;
import io.github.mzmine.modules.io.download.DownloadAssetButton;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileNamesComponent extends BorderPane {

  public static final Font smallFont = new Font("SansSerif", 10);

  private final TextArea txtFilename;
  private final CheckBox useSubFolders;

  private final List<ExtensionFilter> filters;
  private final Path defaultDir;
  private final List<DownloadAsset> assets;
  private final DoubleProperty dragMessageOpacity = new SimpleDoubleProperty(0.3);
  // this mapper is applied when all * files button is clicked. Input is all files and directories
  // matching the filter and the function may apply transformation like Bruker path validation
  private final @NotNull Function<File[], File[]> allFilesMapper;


  /**
   * @param allFilesMapper this mapper is applied when all * files button is clicked. Input is all
   *                       files and directories matching the filter and the function may apply
   *                       transformation like Bruker path validation
   */
  public FileNamesComponent(List<ExtensionFilter> filters, Path defaultDir,
      @Nullable String dragPrompt, @NotNull Function<File[], File[]> allFilesMapper) {
    this(filters, defaultDir, List.of(), dragPrompt, allFilesMapper);
  }

  /**
   * @param allFilesMapper this mapper is applied when all * files button is clicked. Input is all
   *                       files and directories matching the filter and the function may apply
   *                       transformation like Bruker path validation
   */
  public FileNamesComponent(List<ExtensionFilter> filters, Path defaultDir,
      @NotNull List<DownloadAsset> assets, @Nullable String dragPrompt,
      @NotNull Function<File[], File[]> allFilesMapper) {
    this.filters = ImmutableList.copyOf(filters);
    this.defaultDir = defaultDir;
    this.assets = assets;
    this.allFilesMapper = allFilesMapper;

    txtFilename = new TextArea();
    txtFilename.setPrefColumnCount(65);
    txtFilename.setPrefRowCount(6);
    txtFilename.setFont(smallFont);
    initDragDropped();

    final StackPane stack = FxIconUtil.createDragAndDropWrapper(txtFilename,
        txtFilename.textProperty().isEmpty(), dragPrompt, dragMessageOpacity);

    Button btnFileBrowser = new Button("Select files");
    btnFileBrowser.setMaxWidth(Double.MAX_VALUE);
    btnFileBrowser.setOnAction(e -> {
      // Create chooser.
      FileChooser fileChooser = new FileChooser();
      if (defaultDir != null) {
        fileChooser.setInitialDirectory(defaultDir.toFile());
      }
      fileChooser.setTitle("Select files");

      fileChooser.getExtensionFilters().addAll(this.filters);

      String[] currentPaths = txtFilename.getText().split("\n");
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
    btnClear.setMaxWidth(Double.MAX_VALUE);
    btnClear.setOnAction(e -> txtFilename.setText(""));

    GridPane buttonGrid = new GridPane();
    ColumnConstraints b1 = new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE,
        USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.CENTER, true);
    ColumnConstraints b2 = new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE,
        USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.CENTER, true);
    buttonGrid.getColumnConstraints().addAll(b1, b2);

    buttonGrid.setHgap(1);
    buttonGrid.setVgap(3);
    buttonGrid.setPadding(new Insets(0, 0, 0, 5));

    int row = 0;
    // add asset button if assets available
    if (!assets.isEmpty()) {
      var downloadButton = new DownloadAssetButton(assets);
      downloadButton.setOnDownloadFinished(
          files -> FxThread.runLater(() -> addFilesSkipDuplicates(files)));
      buttonGrid.add(downloadButton, 0, row, 2, 1);
      row++;
    }

    buttonGrid.add(btnFileBrowser, 0, row);
    buttonGrid.add(btnClear, 1, row);
    row++;
    buttonGrid.add(useSubFolders, 0, row, 2, 1);
    row++;

    List<Button> directoryButtons = createFromDirectoryBtns(filters);
    buttonGrid.add(directoryButtons.removeFirst(), 0, row, 2, 1);
    row++;
    for (int i = 0; i < directoryButtons.size(); i++) {
      buttonGrid.add(directoryButtons.get(i), i % 2, row + 1 + i / 2);
      directoryButtons.get(i).getParent().layout();
    }
    buttonGrid.layout();

    // main gridpane
    this.setCenter(stack);
    this.setRight(buttonGrid);
  }

  /**
   * @return duplicates or empty list
   */
  private synchronized Set<File> addFilesSkipDuplicates(final List<File> files) {
    File[] old = getValue();
    if (old == null || old.length == 0) {
      Set<File> duplicates = CollectionUtils.streamDuplicates(files.stream())
          .collect(Collectors.toSet());
      setValue(files.stream().distinct().toArray(File[]::new));
      return duplicates;
    }
    Supplier<Stream<File>> supplier = () -> Stream.concat(Arrays.stream(old), files.stream());
    var duplicates = CollectionUtils.streamDuplicates(supplier.get()).collect(Collectors.toSet());
    setValue(supplier.get().distinct().toArray(File[]::new));
    return duplicates;
  }

  private List<Button> createFromDirectoryBtns(List<ExtensionFilter> filters) {
    List<String> allExtensions = filters.stream().map(ExtensionFilter::getExtensions)
        .flatMap(Collection::stream).filter(ext -> !"*.*".equals(ext)).toList();
    ExtensionFilter allFilters = new ExtensionFilter("All", allExtensions);

    List<Button> btns = new ArrayList<>();
    // create from folder button
    createButton(btns, allFilters, true);

    // add buttons for single format filters
    List<ExtensionFilter> singleFormatFilters = filters.stream()
        .filter(f -> f.getExtensions().stream().map(String::toLowerCase).distinct().count() == 1)
        .toList();
    for (ExtensionFilter filter : singleFormatFilters) {
      createButton(btns, filter, false);
    }
    return btns;
  }

  private void createButton(final List<Button> btns, final ExtensionFilter filter,
      final boolean isAllFilter) {
    if (filter.getExtensions().isEmpty() || filter.getExtensions().get(0).equals("*.*")) {
      return;
    }
    String name = isAllFilter ? "All in folder" : "All " + filter.getExtensions().get(0);

    Button btnFromDirectory = new Button(name);
    btnFromDirectory.setMinWidth(USE_COMPUTED_SIZE);
    btnFromDirectory.setPrefWidth(USE_COMPUTED_SIZE);
    btnFromDirectory.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    btnFromDirectory.setTooltip(new Tooltip(
        "All files in the selected folder (optionally include all sub folders if checked above)"));
    btns.add(btnFromDirectory);
    btnFromDirectory.setOnAction(_ -> {
      // Create chooser.
      DirectoryChooser fileChooser = new DirectoryChooser();
      fileChooser.setTitle("Select a folder");
      setInitialDirectory(fileChooser);

      // Open chooser.
      File dir = fileChooser.showDialog(null);
      if (dir == null) {
        return;
      }

      // list all files in sub directories
      final @NotNull File[] matchingFilesAndDirs = FileAndPathUtil.findFilesInDirFlat(dir, filter,
          true, useSubFolders.isSelected());

      // raw data files need post processing of matching files to validate bruker file paths
      // other filter just filters out directories and only keeps files
      final File[] mappedFiles = allFilesMapper.apply(matchingFilesAndDirs);

      setValue(mappedFiles);
    });
  }

  /**
   * When creating a new chooser set the initial directory to the currently selected files
   *
   * @param fileChooser target chooser
   */
  private void setInitialDirectory(DirectoryChooser fileChooser) {
    String[] currentPaths = txtFilename.getText().split("\n");
    if (currentPaths.length > 0) {
      File currentFile = new File(currentPaths[0].trim());
      File currentDir = currentFile.getParentFile();
      if (currentDir != null && currentDir.exists()) {
        fileChooser.setInitialDirectory(currentDir);
      }
    }
  }

  public File[] getValue() {
    String[] fileNameStrings = txtFilename.getText().split("\n");
    List<File> files = new ArrayList<>();
    for (String fileName : fileNameStrings) {
      if (fileName.isBlank()) {
        continue;
      }
      files.add(new File(fileName.trim()));
    }
    return files.toArray(new File[0]);
  }

  public void setValue(@Nullable File[] value) {
    if (value == null) {
      txtFilename.setText("");
      return;
    }
    txtFilename.setText(Arrays.stream(value).filter(Objects::nonNull).map(File::getPath)
        .collect(Collectors.joining("\n")));
  }

  public void setToolTipText(String toolTip) {
    txtFilename.setTooltip(new Tooltip(toolTip));
  }

  private void initDragDropped() {
    txtFilename.setOnDragOver(e -> {
      dragMessageOpacity.set(0.6);
      if (e.getGestureSource() != this && e.getGestureSource() != txtFilename && e.getDragboard()
          .hasFiles()) {
        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      e.consume();
    });
    txtFilename.setOnDragExited(_ -> {
      dragMessageOpacity.set(0.3);
    });
    txtFilename.setOnDragDropped(e -> {
      dragMessageOpacity.set(0.3);
      if (e.getDragboard().hasFiles()) {
        final List<File> files = e.getDragboard().getFiles();

        StringBuilder sb = new StringBuilder(txtFilename.getText());
        if (!sb.toString().endsWith("\n")) {
          sb.append("\n");
        }

        final List<String> patterns = filters.stream().flatMap(f -> f.getExtensions().stream())
            .map(extension -> extension.toLowerCase().replace("*", "").toLowerCase()).toList();

        for (File file : files) {
          if (patterns.stream()
              .anyMatch(filter -> file.getAbsolutePath().toLowerCase().endsWith(filter))) {
            sb.append(file.getPath()).append("\n");
          }
        }

        txtFilename.setText(sb.toString());

        e.setDropCompleted(true);
        e.consume();
      }
    });
  }
}
