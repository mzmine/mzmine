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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.util.List;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationMatchTabOld extends SimpleTab implements FeatureRowInterfaceFx {

  private final WeakAdapter weak = new WeakAdapter();
  private final FeatureTableFX table;
  private final ScrollPane scrollPane;
  private int matches = 0;

  public LipidAnnotationMatchTabOld(FeatureTableFX table) {
    super("Lipid annotation matches", false, false);
    setOnCloseRequest(event -> weak.dipose());
    this.table = table;
    scrollPane = new ScrollPane();
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setContent(scrollPane);
    weak.addListChangeListener(table.getSelectionModel().getSelectedItems(), c -> selectionChanged());
  }

  public static void addNewTab(final FeatureTableFX table) {
    FxThread.runLater(() -> {
      final LipidAnnotationMatchTabOld tab = new LipidAnnotationMatchTabOld(table);
      tab.selectionChanged();
      MZmineCore.getDesktop().addTab(tab);
    });
  }

  private void selectionChanged() {
    if (weak.isDisposed()) {
      return;
    }
    final ModularFeatureListRow selectedRow = table.getSelectedRow();
    if (selectedRow == null || selectedRow.get(LipidMatchListType.class) == null) {
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
    int i = 0;
    for (var row : selectedRows) {
      if (!(row instanceof ModularFeatureListRow selectedRow)) {
        continue;
      }
      final List<MatchedLipid> matchedLipids = selectedRow.get(LipidMatchListType.class);
      if (matchedLipids != null && !matchedLipids.isEmpty()) {
        for (MatchedLipid matchedLipid : matchedLipids) {
          if (matchedLipid.getComment() == null || matchedLipid.getComment().isEmpty()) {
            LipidAnnotationMatchPaneOld lipidAnnotationMatchPane = new LipidAnnotationMatchPaneOld(
                matchedLipid);
            GridPane.setHgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
            GridPane.setVgrow(lipidAnnotationMatchPane, Priority.ALWAYS);
            pane.add(lipidAnnotationMatchPane, 0, i);
            i++;
            matches++;
          }
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
    if (matchedLipids != null && !matchedLipids.isEmpty()) {
      int i = 0;
      for (MatchedLipid matchedLipid : matchedLipids) {
        if (matchedLipid.getComment() == null || matchedLipid.getComment().isEmpty()) {
          LipidAnnotationMatchPaneOld lipidAnnotationMatchPane = new LipidAnnotationMatchPaneOld(
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

  public boolean hasContent() {
    return matches > 0;
  }

}
