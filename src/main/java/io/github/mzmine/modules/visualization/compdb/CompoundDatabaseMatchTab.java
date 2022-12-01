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

package io.github.mzmine.modules.visualization.compdb;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

public class CompoundDatabaseMatchTab extends MZmineTab {

  private final FeatureTableFX table;
  private final ScrollPane scrollPane;

  public static void addNewTab(final FeatureTableFX table) {
    MZmineCore.runLater(() -> {
      final CompoundDatabaseMatchTab tab = new CompoundDatabaseMatchTab(table);
      tab.selectionChanged();
      MZmineCore.getDesktop().addTab(tab);
    });
  }

  public CompoundDatabaseMatchTab(FeatureTableFX table) {
    super("Compound database matches", true, true);
    this.table = table;
    scrollPane = new ScrollPane();
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setContent(scrollPane);

    final ListChangeListener<TreeItem<ModularFeatureListRow>> listener = c -> selectionChanged();
    table.getSelectionModel().getSelectedItems().addListener(listener);
    setOnClosed(e -> table.getSelectionModel().getSelectedItems().removeListener(listener));
  }

  private void selectionChanged() {
    final ModularFeatureListRow selectedRow = table.getSelectedRow();
    if(selectedRow == null) {
      return;
    }
    GridPane pane = new GridPane();

    final List<CompoundDBAnnotation> compoundAnnotations = FeatureUtils.extractAllCompoundAnnotations(
        selectedRow);

    for (int i = 0, j = 0; i < compoundAnnotations.size(); i++) {
      CompoundDBAnnotation annotation = compoundAnnotations.get(i);
      final CompoundDatabaseMatchPane matchPane = new CompoundDatabaseMatchPane(annotation,
          selectedRow);
      pane.add(matchPane, 0, j++);
      pane.add(new Separator(Orientation.HORIZONTAL), 0, j++);
    }
    scrollPane.setContent(pane);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
