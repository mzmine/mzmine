package io.github.mzmine.modules.visualization.compdb;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.ArrayList;
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
    GridPane pane = new GridPane();

    final List<CompoundDBAnnotation> compoundAnnotations = new ArrayList<>();
    final Collection<DataType> dataTypes = selectedRow.getTypes().values();
    for (DataType dataType : dataTypes) {
      if (dataType instanceof ListWithSubsType<?> listType && dataType instanceof AnnotationType) {
        final List<?> list = selectedRow.get(listType);
        if (list != null && !list.isEmpty()) {
          list.stream().filter(c -> c instanceof CompoundDBAnnotation)
              .forEach(c -> compoundAnnotations.add((CompoundDBAnnotation) c));
        }
      }
    }

    for (int i = 0; i < compoundAnnotations.size(); i++) {
      CompoundDBAnnotation annotation = compoundAnnotations.get(i);
      final CompoundDatabaseMatchPane matchPane = new CompoundDatabaseMatchPane(annotation,
          selectedRow);
      pane.add(matchPane, 0, i++);
      pane.add(new Separator(Orientation.HORIZONTAL), 0, i++);
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
