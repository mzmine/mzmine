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
import io.github.mzmine.modules.visualization.projectmetadata.extract.SampleMetadataExtractionUtils.GroupMatch;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterComponent;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor component for {@link MetadataRegexExtractionParameter}. Shows the mappings as rows of a
 * grid (one bold header row to save space), a single shared {@link MetadataValueMappingEditor} bound
 * to the selected row, and a live preview that lists all loaded raw data files with the selected
 * mapping's matched group highlighted. The first grid column marks the selected row.
 */
public class MetadataRegexExtractionComponent extends VBox implements
    ParameterComponent<List<MetadataRegexMapping>> {

  // limit how many files are rendered in the preview to keep the dialog responsive
  private static final int MAX_PREVIEW_FILES = 1000;
  private static final Color HIGHLIGHT_COLOR = Color.web("#E8590C");
  private static final String[] HEADERS = {"", "Source", "Target column", "Type", "Regex",
      "Default", "", ""};

  private final List<MetadataRegexMappingRow> rows = new ArrayList<>();
  private final GridPane grid = new GridPane();
  private final VBox previewBox = FxLayout.newVBox(Insets.EMPTY);
  private final List<RawDataFile> previewFiles;
  private final List<String> columnSuggestions;
  private final MetadataValueMappingEditor valueEditor;

  private @Nullable MetadataRegexMappingRow selected;

  public MetadataRegexExtractionComponent() {
    super(FxLayout.DEFAULT_SPACE);
    setPadding(new Insets(FxLayout.DEFAULT_SPACE));

    previewFiles =
        ProjectService.getProject() != null ? ProjectService.getProject().getCurrentRawDataFiles()
            : List.of();
    columnSuggestions = buildColumnSuggestions();

    grid.setHgap(FxLayout.DEFAULT_SPACE);
    grid.setVgap(FxLayout.DEFAULT_SPACE);

    valueEditor = new MetadataValueMappingEditor(previewFiles, this::refreshPreview);

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
    previewScroll.setPrefViewportHeight(220);
    final TitledPane previewPane = FxLayout.newTitledPane(
        "Preview – matched group highlighted in loaded files", previewScroll);
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

  private void rebuildGrid() {
    grid.getChildren().clear();
    for (int col = 0; col < HEADERS.length; col++) {
      if (!HEADERS[col].isEmpty()) {
        grid.add(FxLabels.newBoldLabel(HEADERS[col]), col, 0);
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
        previewFiles);
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

  private void refreshPreview() {
    previewBox.getChildren().clear();

    if (previewFiles.isEmpty()) {
      previewBox.getChildren().add(FxLabels.newItalicLabel(
          "No raw data files are loaded. Import data files to preview the matches."));
      return;
    }

    final MetadataRegexMapping mapping = selected != null ? selected.toMapping() : null;
    final RegexInputSource source =
        mapping != null ? mapping.inputSource() : RegexInputSource.FILE_NAME;
    final boolean hasRegex = mapping != null && !mapping.regex().isBlank();

    final String headerText = mapping == null ? "Select a mapping to highlight its matches."
        : "Highlighting %s of column '%s' (%s)".formatted(
            hasRegex ? "the matched group" : "(no regex yet)",
            mapping.columnName().isBlank() ? "?" : mapping.columnName(), source.toString());
    previewBox.getChildren().add(FxLabels.newItalicLabel(headerText));

    final int shown = Math.min(previewFiles.size(), MAX_PREVIEW_FILES);
    for (int i = 0; i < shown; i++) {
      previewBox.getChildren()
          .add(buildPreviewLine(previewFiles.get(i), mapping, source, hasRegex));
    }
    if (previewFiles.size() > shown) {
      previewBox.getChildren().add(
          FxLabels.newItalicLabel("… and %d more files".formatted(previewFiles.size() - shown)));
    }
  }

  private @NotNull TextFlow buildPreviewLine(@NotNull final RawDataFile raw,
      @Nullable final MetadataRegexMapping mapping, @NotNull final RegexInputSource source,
      final boolean hasRegex) {
    final String input = source.extract(raw);

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
