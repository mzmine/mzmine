package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationMatchTab extends SimpleTab implements FeatureRowInterfaceFx {

  private final WeakAdapter weak = new WeakAdapter();
  private final FeatureTableFX table;
  private final ScrollPane scrollPane;
  private int matches = 0;

  public LipidAnnotationMatchTab(FeatureTableFX table) {
    super("Lipid annotation matches", true, true);
    setOnCloseRequest(event -> weak.dipose());
    this.table = table;
    scrollPane = new ScrollPane();
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(true);
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
    int j = 0;
    for (var row : selectedRows) {
      if (!(row instanceof ModularFeatureListRow selectedRow)) {
        continue;
      }
      final List<MatchedLipid> matchedLipids = selectedRow.get(LipidMatchListType.class);
      if (matchedLipids != null && !matchedLipids.isEmpty()) {
        final LipidSpectrumChart lipidSpectrumChart = new LipidSpectrumChart(selectedRow, null,
            false);
        GridPane.setHgrow(lipidSpectrumChart, Priority.ALWAYS);
        GridPane.setVgrow(lipidSpectrumChart, Priority.ALWAYS);
        pane.add(lipidSpectrumChart, 0, j++);
        pane.add(new Separator(Orientation.HORIZONTAL), 0, j++);
        matches++;
      }
    }
    scrollPane.setContent(pane);
  }

  public void setFeatureRow(final ModularFeatureListRow selectedRow) {
    final LipidSpectrumChart lipidSpectrumChart = new LipidSpectrumChart(selectedRow, null, false);
    GridPane.setHgrow(lipidSpectrumChart, Priority.ALWAYS);
    GridPane.setVgrow(lipidSpectrumChart, Priority.ALWAYS);
    GridPane pane = new GridPane();
    GridPane.setHgrow(pane, Priority.ALWAYS);
    GridPane.setVgrow(pane, Priority.ALWAYS);
    pane.add(lipidSpectrumChart, 0, 1);
    pane.add(new Separator(Orientation.HORIZONTAL), 0, 1);
    scrollPane.setContent(pane);
  }

  public boolean hasContent() {
    return matches > 0;
  }

}
