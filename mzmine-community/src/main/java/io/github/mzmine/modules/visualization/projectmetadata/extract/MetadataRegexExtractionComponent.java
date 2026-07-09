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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionUtils.GroupMatch;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterComponent;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor component for {@link MetadataRegexExtractionParameter}. Shows the mappings as rows of a
 * grid (one bold header row to save space), a single shared {@link MetadataValueMappingEditor} bound
 * to the selected row, and a live preview that lists selected files and loaded raw data files with
 * the selected mapping's matched group highlighted. The first grid column marks the selected row.
 */
public class MetadataRegexExtractionComponent extends VBox implements
    ParameterComponent<List<MetadataRegexMapping>> {

  // limit how many files are rendered in the preview to keep the dialog responsive
  private static final int MAX_PREVIEW_FILES = 1000;
  private static final int PREVIEW_VIEWPORT_HEIGHT = 340;
  private static final Color HIGHLIGHT_COLOR = Color.web("#E8590C");
  private static final String[] HEADERS = {"", "Source", "Target column", "Type", "Regex",
      "Default", ""};
  // tooltips parallel to HEADERS – reuses the same strings defined on the input fields
  private static final String[] HEADER_TOOLTIPS = {"", MetadataRegexMappingRow.TOOLTIP_SOURCE,
      MetadataRegexMappingRow.TOOLTIP_COLUMN, MetadataRegexMappingRow.TOOLTIP_TYPE,
      MetadataRegexMappingRow.TOOLTIP_REGEX, MetadataRegexMappingRow.TOOLTIP_DEFAULT, ""};
  // the regex column absorbs the free horizontal space so the grid fills the dialog width
  private static final int GROW_COLUMN = 4;

  private final List<MetadataRegexMappingRow> rows = new ArrayList<>();
  private final GridPane grid = new GridPane();
  private final VBox previewBox = FxLayout.newVBox(Insets.EMPTY);
  /// files already loaded into mzmine
  private final List<RawDataFile> loadedPreviewFiles;
  /// files selected by drag and drop or in the data import or wizard
  private final ObservableList<File> selectedFiles;
  private final List<String> columnSuggestions;
  private final MetadataValueMappingEditor valueEditor;

  private @Nullable MetadataRegexMappingRow selected;

  public MetadataRegexExtractionComponent() {
    this(FXCollections.observableArrayList());
  }

  public MetadataRegexExtractionComponent(@NotNull final ObservableList<File> selectedFiles) {
    super(FxLayout.DEFAULT_SPACE);
    setPadding(new Insets(FxLayout.DEFAULT_SPACE));
    setMaxWidth(Double.MAX_VALUE);
    this.selectedFiles = selectedFiles;

    loadedPreviewFiles =
        ProjectService.getProject() != null ? ProjectService.getProject().getCurrentRawDataFiles()
            : List.of();
    columnSuggestions = buildColumnSuggestions();

    grid.setHgap(FxLayout.DEFAULT_SPACE);
    grid.setVgap(FxLayout.DEFAULT_SPACE);
    grid.setMaxWidth(Double.MAX_VALUE);
    setupGridColumns();

    valueEditor = new MetadataValueMappingEditor(this::previewInputStrings, this::refreshPreview);
    this.selectedFiles.addListener((ListChangeListener<File>) _ -> refreshFileDependentPreview());

    final var addButton = FxButtons.createButton("Add mapping", "Add another column mapping",
        () -> addRow(MetadataRegexMapping.createDefault()));
    final var header = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY,
        FxLabels.newBoldLabel("Regex column mappings"), addButton);

    // do not wrap the preview lines; show a horizontal scroll bar instead
    final ScrollPane previewScroll = new ScrollPane(previewBox);
    previewScroll.setFitToWidth(false);
    previewScroll.setFitToHeight(true);
    previewScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    previewScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    previewScroll.setMinViewportHeight(PREVIEW_VIEWPORT_HEIGHT);
    previewScroll.setPrefViewportHeight(PREVIEW_VIEWPORT_HEIGHT);
    final BooleanExpression dragMessageVisible = Bindings.createBooleanBinding(
        () -> loadedPreviewFiles.isEmpty() && this.selectedFiles.isEmpty(), this.selectedFiles);
    final StackPane previewWrapper = FxIconUtil.createDragAndDropWrapper(previewScroll,
        dragMessageVisible, "Drop files here to preview the metadata extraction.");
    previewWrapper.setMinHeight(PREVIEW_VIEWPORT_HEIGHT);
    initPreviewDragAndDrop(previewWrapper);
    final TitledPane previewPane = FxLayout.newTitledPane(
        "Preview - matched group highlighted in selected and loaded files", previewWrapper);
    final Accordion previewAccordion = FxLayout.newAccordion(true, previewPane);
    VBox.setVgrow(previewAccordion, Priority.ALWAYS);

    getChildren().addAll(header, grid, new Separator(), FxLayout.wrapInBorder(valueEditor),
        previewAccordion);
    rebuildGrid();
    refreshPreview();
  }

  // existing metadata columns (plus the common reserved targets) offered as autocomplete options
  private static @NotNull List<String> buildColumnSuggestions() {
    final LinkedHashSet<String> suggestions = new LinkedHashSet<>();
    // always offer the reserved sample type and run date columns as targets
    suggestions.add(MetadataColumn.SAMPLE_TYPE_HEADER);
    suggestions.add(MetadataColumn.DATE_HEADER);
    if (ProjectService.getProject() != null) {
      ProjectService.getMetadata().getColumns().stream().map(MetadataColumn::getTitle)
          // the filename column is synthetic and cannot be written to
          .filter(title -> !MetadataColumn.FILENAME_HEADER.equals(title)).forEach(suggestions::add);
    }
    return List.copyOf(suggestions);
  }

  // -------------------------------------------------------------------------------------------- grid

  // one column (the regex column) grows to absorb free width; the others size to their content
  private void setupGridColumns() {
    for (int col = 0; col < HEADERS.length; col++) {
      final ColumnConstraints cc = new ColumnConstraints();
      if (col == GROW_COLUMN) {
        cc.setHgrow(Priority.ALWAYS);
        cc.setFillWidth(true);
      }
      grid.getColumnConstraints().add(cc);
    }
  }

  private void rebuildGrid() {
    grid.getChildren().clear();
    for (int col = 0; col < HEADERS.length; col++) {
      if (!HEADERS[col].isEmpty()) {
        final Label headerLabel = FxLabels.newBoldLabel(HEADERS[col]);
        if (!HEADER_TOOLTIPS[col].isEmpty()) {
          headerLabel.setTooltip(new Tooltip(HEADER_TOOLTIPS[col]));
        }
        grid.add(headerLabel, col, 0);
      }
    }
    for (int row = 0; row < rows.size(); row++) {
      final Node[] cells = rows.get(row).gridCells();
      for (int col = 0; col < cells.length; col++) {
        grid.add(cells[col], col, row + 1);
      }
    }
  }

  private @NotNull MetadataRegexMappingRow createRow(@NotNull final MetadataRegexMapping mapping) {
    final MetadataRegexMappingRow row = new MetadataRegexMappingRow(mapping, columnSuggestions,
        this::previewInputStrings);
    row.setOnActivate(this::selectRow);
    row.setOnChange(() -> {
      if (selected == row) {
        // a column name change updates the value-editor header (without rebuilding its fields)
        valueEditor.updateHeader();
        refreshPreview();
      }
    });
    row.setOnRemove(this::handleRemove);
    return row;
  }

  private void addRow(@NotNull final MetadataRegexMapping mapping) {
    final MetadataRegexMappingRow row = createRow(mapping);
    rows.add(row);
    rebuildGrid();
    selectRow(row);
  }

  private void selectRow(@Nullable final MetadataRegexMappingRow row) {
    selected = row;
    for (final MetadataRegexMappingRow r : rows) {
      r.setSelected(r == row);
    }
    valueEditor.setTarget(row);
    refreshPreview();
  }

  private void handleRemove(@NotNull final MetadataRegexMappingRow row) {
    final String column = row.toMapping().columnName();
    final String label =
        column.isBlank() ? "this mapping" : "the mapping for column '" + column + "'";
    if (!DialogLoggerUtil.showDialogYesNo("Remove mapping", "Remove " + label + "?")) {
      return;
    }
    rows.remove(row);
    rebuildGrid();
    if (selected == row) {
      selectRow(rows.isEmpty() ? null : rows.getFirst());
    }
  }

  // ----------------------------------------------------------------------------------------- preview

  public @NotNull ObservableList<File> getSelectedFiles() {
    return selectedFiles;
  }

  private @NotNull List<String> previewInputStrings(@NotNull final MetadataRegexMapping mapping) {
    return previewInputStrings(mapping.inputSource());
  }

  private @NotNull List<String> previewInputStrings(@NotNull final RegexInputSource source) {
    final LinkedHashMap<String, String> inputs = new LinkedHashMap<>();
    for (final File file : selectedFiles) {
      if (file != null) {
        inputs.putIfAbsent(fileKey(file), source.extract(file));
      }
    }
    for (final RawDataFile raw : loadedPreviewFiles) {
      inputs.putIfAbsent(fileKey(raw.getAbsoluteFilePath()), source.extract(raw));
    }
    return List.copyOf(inputs.values());
  }

  private void initPreviewDragAndDrop(@NotNull final StackPane previewWrapper) {
    previewWrapper.setOnDragOver(event -> {
      if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      event.consume();
    });

    previewWrapper.setOnDragDropped(event -> {
      if (event.getDragboard().hasFiles()) {
        addSelectedFilesSkipDuplicates(event.getDragboard().getFiles());
        event.setDropCompleted(true);
      }
      event.consume();
    });
  }

  private void addSelectedFilesSkipDuplicates(@NotNull final Collection<File> files) {
    final LinkedHashMap<String, File> uniqueFiles = new LinkedHashMap<>();
    for (final File file : selectedFiles) {
      if (file != null) {
        uniqueFiles.putIfAbsent(fileKey(file), file);
      }
    }

    final LinkedHashSet<String> loadedFileKeys = new LinkedHashSet<>();
    for (final RawDataFile raw : loadedPreviewFiles) {
      loadedFileKeys.add(fileKey(raw.getAbsoluteFilePath()));
    }

    for (final File file : files) {
      if (file == null) {
        continue;
      }
      final String key = fileKey(file);
      if (!loadedFileKeys.contains(key)) {
        uniqueFiles.putIfAbsent(key, file);
      }
    }
    selectedFiles.setAll(uniqueFiles.values());
  }

  private static @NotNull String fileKey(@NotNull final File file) {
    return file.getAbsoluteFile().toPath().normalize().toString().toLowerCase(Locale.ROOT);
  }

  private void refreshFileDependentPreview() {
    refreshPreview();
    valueEditor.refreshResults();
  }

  private void refreshPreview() {
    previewBox.getChildren().clear();

    final MetadataRegexMapping mapping = selected != null ? selected.toMapping() : null;
    final RegexInputSource source =
        mapping != null ? mapping.inputSource() : RegexInputSource.FILE_NAME;
    final List<String> previewInputs = previewInputStrings(source);

    if (previewInputs.isEmpty()) {
      previewBox.getChildren().add(FxLabels.newItalicLabel(
          "No raw data files are loaded or selected. Drop files here to preview the matches."));
      return;
    }

    final boolean hasRegex = mapping != null && !mapping.regex().isBlank();

    final String headerText = mapping == null ? "Select a mapping to highlight its matches."
        : "Highlighting %s of column '%s' (%s)".formatted(
            hasRegex ? "the matched group" : "(no regex yet)",
            mapping.columnName().isBlank() ? "?" : mapping.columnName(), source.toString());
    previewBox.getChildren().add(FxLabels.newItalicLabel(headerText));

    final int shown = Math.min(previewInputs.size(), MAX_PREVIEW_FILES);
    for (int i = 0; i < shown; i++) {
      previewBox.getChildren().add(buildPreviewLine(previewInputs.get(i), mapping, hasRegex));
    }
    if (previewInputs.size() > shown) {
      previewBox.getChildren().add(
          FxLabels.newItalicLabel("... and %d more files".formatted(previewInputs.size() - shown)));
    }
  }

  private @NotNull TextFlow buildPreviewLine(@NotNull final String input,
      @Nullable final MetadataRegexMapping mapping, final boolean hasRegex) {
    final GroupMatch match =
        hasRegex ? SampleMetadataExtractionUtils.firstGroupMatch(mapping, input) : null;

    final TextFlow flow;
    if (match != null) {
      final Text pre = FxTexts.text(input.substring(0, match.start()));
      final Text hit = FxTexts.colored(
          FxTexts.boldText(input.substring(match.start(), match.end())), HIGHLIGHT_COLOR);
      hit.setUnderline(true);
      final Text post = FxTexts.text(input.substring(match.end()));
      flow = FxTextFlows.newTextFlow(pre, hit, post);
    } else {
      flow = FxTextFlows.newTextFlow(FxTexts.text(input));
      if (hasRegex) {
        flow.getChildren().add(FxTexts.colored(FxTexts.italicText("  (no match)"), Color.GRAY));
      }
    }

    if (hasRegex) {
      final String value = SampleMetadataExtractionUtils.extractValue(mapping, input);
      flow.getChildren().add(FxTexts.colored(FxTexts.text("   →   "), Color.GRAY));
      flow.getChildren()
          .add(FxTexts.colored(FxTexts.boldText(value == null ? "(empty)" : value), Color.GRAY));
    }
    return flow;
  }

  // ----------------------------------------------------------------------------- ParameterComponent

  @Override
  public List<MetadataRegexMapping> getValue() {
    return rows.stream().map(MetadataRegexMappingRow::toMapping).toList();
  }

  @Override
  public void setValue(@Nullable final List<MetadataRegexMapping> value) {
    rows.clear();
    if (value != null) {
      value.forEach(mapping -> rows.add(createRow(mapping)));
    }
    if (rows.isEmpty()) {
      // always start with one row so the editor is usable
      rows.add(createRow(MetadataRegexMapping.createDefault()));
    }
    rebuildGrid();
    selectRow(rows.getFirst());
  }
}
