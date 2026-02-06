/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.chemaudit;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ChemAuditQualityCategoryType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ChemAuditQualityIndicatorType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ChemAuditRawJsonType;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChemAuditResultsTab extends SimpleTab implements FeatureRowInterfaceFx {

  private final WeakAdapter weak = new WeakAdapter();
  private final FeatureTableFX table;
  private final ScrollPane scrollPane;
  private final BorderPane root;
  private final RadioButton selectedOnlyToggle;
  private final RadioButton allRowsToggle;
  private final TextField filterField;
  private final Label summaryLabel;
  private int matches = 0;

  public ChemAuditResultsTab(@Nullable FeatureTableFX table) {
    super("ChemAudit results", true, true);
    setOnCloseRequest(event -> weak.dipose());

    this.table = table;
    root = new BorderPane();
    scrollPane = new ScrollPane();
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

    final ToggleGroup scopeGroup = new ToggleGroup();
    selectedOnlyToggle = new RadioButton("Selected rows");
    selectedOnlyToggle.setToggleGroup(scopeGroup);
    selectedOnlyToggle.setSelected(true);
    allRowsToggle = new RadioButton("All ChemAudit rows");
    allRowsToggle.setToggleGroup(scopeGroup);

    filterField = new TextField();
    filterField.setPromptText("Filter by row, name, SMILES, category...");
    summaryLabel = new Label("No ChemAudit results");

    final HBox topBar = new HBox(12, selectedOnlyToggle, allRowsToggle, new Separator(
        Orientation.VERTICAL), filterField);
    topBar.setPadding(new Insets(6, 8, 6, 8));
    HBox.setHgrow(filterField, Priority.ALWAYS);

    final VBox header = new VBox(4, topBar, summaryLabel);
    header.setPadding(new Insets(4, 8, 4, 8));

    root.setTop(header);
    root.setCenter(scrollPane);
    setContent(root);

    if (table != null) {
      weak.addListChangeListener(table, table.getSelectionModel().getSelectedItems(),
          c -> selectionChanged());
    }
    scopeGroup.selectedToggleProperty().addListener((_, _, __) -> refreshView());
    filterField.textProperty().addListener((_, _, __) -> refreshView());
    selectionChanged();
  }

  public static void addNewTab(@Nullable final FeatureTableFX table) {
    FxThread.runLater(() -> {
      final ChemAuditResultsTab tab = new ChemAuditResultsTab(table);
      tab.selectionChanged();
      MZmineCore.getDesktop().addTab(tab);
    });
  }

  private void selectionChanged() {
    if (weak.isDisposed() || table == null) {
      return;
    }
    refreshView();
  }

  @Override
  public void setFeatureRows(final @NotNull List<? extends FeatureListRow> selectedRows) {
    renderRows(selectedRows);
  }

  @Override
  public boolean hasContent() {
    return matches > 0;
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // title updated by selected row
  }

  private static boolean hasChemAuditData(@NotNull final CompoundDBAnnotation annotation) {
    return annotation.get(ChemAuditRawJsonType.class) != null;
  }

  private void refreshView() {
    final List<? extends FeatureListRow> rows = selectedOnlyToggle.isSelected()
        ? getSelectedRows()
        : getAllRows();
    renderRows(rows);
  }

  private List<? extends FeatureListRow> getSelectedRows() {
    if (table == null) {
      return List.of();
    }
    final var selected = table.getSelectedRows();
    return selected != null ? selected : List.of();
  }

  private List<? extends FeatureListRow> getAllRows() {
    if (table == null) {
      return List.of();
    }
    final ModularFeatureList flist = table.getFeatureList();
    return flist != null ? flist.getRows() : List.of();
  }

  private void renderRows(final @NotNull List<? extends FeatureListRow> rows) {
    matches = 0;
    GridPane pane = new GridPane();
    int j = 0;
    final String filter = normalizedFilter();

    for (var row : rows) {
      if (!(row instanceof ModularFeatureListRow selectedRow)) {
        continue;
      }
      final List<CompoundDBAnnotation> compoundAnnotations = FeatureUtils.extractAllCompoundAnnotations(
          selectedRow);
      for (CompoundDBAnnotation annotation : compoundAnnotations) {
        if (!hasChemAuditData(annotation) || !matchesFilter(annotation, selectedRow, filter)) {
          continue;
        }
        final ChemAuditResultPane matchPane = new ChemAuditResultPane(annotation, selectedRow);
        pane.add(matchPane, 0, j++);
        pane.add(new Separator(Orientation.HORIZONTAL), 0, j++);
        matches++;
      }
    }

    scrollPane.setContent(pane);
    summaryLabel.setText(matches == 0 ? "No ChemAudit results"
        : "Showing " + matches + " ChemAudit result" + (matches == 1 ? "" : "s"));

    if (selectedOnlyToggle.isSelected()) {
      setSubTitle(rows.stream().map(FeatureUtils::rowToString).collect(Collectors.joining(", ")));
    } else {
      setSubTitle("All ChemAudit annotations");
    }
  }

  private String normalizedFilter() {
    final String text = filterField.getText();
    return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
  }

  private boolean matchesFilter(@NotNull final CompoundDBAnnotation annotation,
      @NotNull final ModularFeatureListRow row, @NotNull final String filter) {
    if (filter.isBlank()) {
      return true;
    }
    final String rowId = String.valueOf(row.getID());
    final String name = annotation.get(CompoundNameType.class);
    final String smiles = annotation.get(SmilesStructureType.class);
    final String category = annotation.get(ChemAuditQualityCategoryType.class);
    final String indicator = annotation.get(ChemAuditQualityIndicatorType.class);

    return containsIgnoreCase(rowId, filter)
        || containsIgnoreCase(name, filter)
        || containsIgnoreCase(smiles, filter)
        || containsIgnoreCase(category, filter)
        || containsIgnoreCase(indicator, filter);
  }

  private boolean containsIgnoreCase(@Nullable final String value, @NotNull final String filter) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
  }
}
