/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.rawfilemetadata;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.rawfiletypes.CvTermType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.ProjectChangeEvent;
import io.github.mzmine.project.impl.ProjectChangeListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

/**
 * Read only table of the file metadata of all raw data files in the project, see
 * {@link RawDataFile#getFileMetadata()}. One row per file and one column per metadata type that is
 * present in any file.
 */
public class AcquisitionMetadataTab extends SimpleTab {

  public static final String TITLE = "Acquisition metadata";

  private final @NotNull MZmineProject project;
  private final @NotNull ObservableList<RawDataFile> files = FXCollections.observableArrayList();
  private final @NotNull TableView<RawDataFile> table = new TableView<>(files);
  private final @NotNull ProjectChangeListener projectListener;

  public AcquisitionMetadataTab() {
    super(TITLE, false, false);
    project = ProjectService.getProject();

    table.getSelectionModel().setCellSelectionEnabled(true);
    table.setPlaceholder(new Label("No raw MS data files in the project"));
    setContent(new BorderPane(table));

    projectListener = new ProjectChangeListener() {
      @Override
      public void dataFilesChanged(final ProjectChangeEvent<RawDataFile> event) {
        FxThread.runLater(() -> update());
      }
    };
    project.addProjectListener(projectListener);
    setOnClosed(_ -> {
      project.removeProjectListener(projectListener);
      setOnClosed(null);
    });

    update();
  }

  private static @NotNull TableColumn<RawDataFile, String> createColumn(
      @NotNull final DataType<?> type) {
    final TableColumn<RawDataFile, String> column = TableColumns.createColumn("", 100,
        raw -> new ReadOnlyStringWrapper(format(raw.getFileMetadata(), type)));

    // header as label to show the controlled vocabulary term as tooltip
    final Label header = new Label(type.getHeaderString());
    final String tooltip =
        type instanceof CvTermType cv ? "%s\n%s (%s)".formatted(type.getHeaderString(),
            cv.getCvName(), cv.getCvAccession()) : type.getHeaderString();
    header.setTooltip(new Tooltip(tooltip));
    column.setGraphic(header);
    return column;
  }

  private static <T> @NotNull String format(@NotNull final ModularDataModel metadata,
      @NotNull final DataType<T> type) {
    final T value = metadata.get(type);
    return value == null ? "" : type.getFormattedString(value, false);
  }

  private static @NotNull String formatDate(final LocalDateTime date) {
    return Objects.toString(date, "");
  }

  /**
   * Rebuilds columns and rows from the current raw data files of the project.
   */
  private void update() {
    final List<RawDataFile> raws = project.getCurrentRawDataFiles().stream()
        .sorted(Comparator.comparing(RawDataFile::getName)).toList();

    // union of all metadata types, files may provide different metadata
    final List<DataType<?>> types = raws.stream()
        .flatMap(raw -> raw.getFileMetadata().getTypes().stream())
        .<DataType<?>>map(type -> (DataType<?>) type).distinct()
        .sorted(Comparator.comparing(DataType::getHeaderString)).toList();

    final List<TableColumn<RawDataFile, ?>> columns = new ArrayList<>();
    columns.add(TableColumns.createColumn("Raw data file", 150,
        raw -> new ReadOnlyStringWrapper(raw.getName())));
    columns.add(TableColumns.createColumn("Acquisition date", 130,
        raw -> new ReadOnlyStringWrapper(formatDate(raw.getStartTimeStamp()))));
    for (final DataType<?> type : types) {
      columns.add(createColumn(type));
    }

    table.getColumns().setAll(columns);
    files.setAll(raws);
  }
}
