package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.util.List;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationMatchTab extends SimpleTab implements FeatureRowInterfaceFx {

  private final WeakAdapter weak = new WeakAdapter();
  private final FeatureTableFX table;
  private final ScrollPane scrollPane;
  private int matches = 0;

  public LipidAnnotationMatchTab(FeatureTableFX table) {
    super("Lipid annotation matches", false, false);
    setOnCloseRequest(event -> weak.dipose());
    this.table = table;
    scrollPane = new ScrollPane();
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setContent(scrollPane);

    weak.addListChangeListener(table.getSelectionModel().getSelectedItems(),
        c -> selectionChanged());
  }

  public static void addNewTab(final FeatureTableFX table) {
    MZmineCore.runLater(() -> {
      final LipidAnnotationMatchTab tab = new LipidAnnotationMatchTab(table);
      tab.selectionChanged();
      MZmineCore.getDesktop().addTab(tab);
    });
  }

  private void selectionChanged() {
    if (weak.isDisposed()) {
      return;
    }
    final ModularFeatureListRow selectedRow = table.getSelectedRow();
    if (selectedRow == null) {
      return;
    }
    setFeatureRow(selectedRow);
  }

  @Override
  public void setFeatureRows(final @NotNull List<? extends FeatureListRow> selectedRows) {
    matches = 0;
    GridPane pane = new GridPane();
    GridPane.setHgrow(pane, Priority.ALWAYS);
    GridPane.setVgrow(pane, Priority.ALWAYS);
    pane.setHgap(10);
    pane.setVgap(10);
    for (var row : selectedRows) {
      if (!(row instanceof ModularFeatureListRow selectedRow)) {
        continue;
      }
      final List<MatchedLipid> matchedLipids = selectedRow.get(LipidMatchListType.class);
      if (matchedLipids != null && !matchedLipids.isEmpty()) {
        int i = 0;
        for (MatchedLipid matchedLipid : matchedLipids) {
          LipidAnnotationMatchPane lipidAnnotationMatchPane = new LipidAnnotationMatchPane(
              matchedLipid);
          GridPane.setHgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
          GridPane.setVgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
          pane.add(lipidAnnotationMatchPane, 0, i);
          i++;
          matches++;
        }
      }
    }
    scrollPane.setContent(pane);
  }

  public void setFeatureRow(final ModularFeatureListRow selectedRow) {
    matches = 0;
    GridPane pane = new GridPane();
    GridPane.setHgrow(pane, Priority.ALWAYS);
    GridPane.setVgrow(pane, Priority.ALWAYS);
    pane.setHgap(10);
    pane.setVgap(10);
    List<MatchedLipid> matchedLipids = selectedRow.get(LipidMatchListType.class);
    int i = 0;
    for (MatchedLipid matchedLipid : matchedLipids) {
      LipidAnnotationMatchPane lipidAnnotationMatchPane = new LipidAnnotationMatchPane(
          matchedLipid);
      GridPane.setHgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
      GridPane.setVgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
      pane.add(lipidAnnotationMatchPane, 0, i);
      i++;
      matches++;
    }
    scrollPane.setContent(pane);
  }

  public boolean hasContent() {
    return matches > 0;
  }

}
