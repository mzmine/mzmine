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

package io.github.mzmine.datamodel.features.types.annotations;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummaryOrder;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.gui.MZmineWindow;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.compdb.CompoundDatabaseMatchTab;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableOwner;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralIdentificationResultsTab;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationSummaryType extends DataType<AnnotationSummary> implements
    GraphicalColumType<AnnotationSummary>, NoTextColumn {

  @Override
  public @NotNull String getUniqueID() {
    return "annotation_quality_summary";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "AQS";
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(final @Nullable FeatureTableFX table,
      @NotNull final ModularFeatureListRow row, @NotNull final List<RawDataFile> file,
      @Nullable final DataType<?> superType, final @Nullable Object value) {

    final DataType<?> mainType;
    if (superType instanceof PreferredAnnotationType
        && row.getPreferredAnnotation() instanceof FeatureAnnotation a) {
      mainType = DataTypes.get(a.getDataType());
    } else {
      mainType = superType;
    }

    final FeatureTableOwner masterTableOwner = table.getTableOwner();
    return () -> FxThread.runLater(() -> {
      switch (mainType) {
        case CompoundDatabaseMatchesType _ -> {
          CompoundDatabaseMatchTab tab = new CompoundDatabaseMatchTab(table);
          MZmineCore.getDesktop().addTab(tab);
        }
        case SpectralLibraryMatchesType s -> MZmineCore.getDesktop()
            .addTab(new SpectralIdentificationResultsTab(table, s.getClass()));
        case AnalogSpectralLibraryMatchesType a -> MZmineCore.getDesktop()
            .addTab(new SpectralIdentificationResultsTab(table, a.getClass()));
        case LipidMatchListType _ -> {
          final LipidAnnotationQCDashboardTab tab = new LipidAnnotationQCDashboardTab();
          // master is complex dashboard - open in other window
          if (masterTableOwner.isOtherComplexDashboard()) {
            new MZmineWindow().addTab(tab);
          } else {
            MZmineCore.getDesktop().addTab(tab);
          }

          // Wire bidirectional cross-dashboard link. linkTo(..., true) pushes the source's current
          // selectedFeatureLists / selectedRows / selectedCompoundRow into the target on creation,
          // so no separate setFeatureList seed is needed. Both directions are active by default;
          // the user can disable either direction from the link popover.
          final FxFeatureTableController sourceCtrl = FxFeatureTableController.controllerFor(table);
          final FxFeatureTableController lipidCtrl = tab.getController()
              .getFeatureTableController();
          if (sourceCtrl != null) {
            sourceCtrl.linkTo(lipidCtrl, true);
            lipidCtrl.linkTo(sourceCtrl, true);
          }
        }
        case null, default -> {
        }
      }
    });
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {
    final TreeTableColumn<ModularFeatureListRow, AnnotationSummary> column = new TreeTableColumn<>(
        getHeaderString());
    column.setUserData(this);
    if (parentType != null) {
      // parent type set -> is a sub type of an annotation/list type. get annotation from there
      column.setCellValueFactory(
          new Callback<CellDataFeatures<ModularFeatureListRow, AnnotationSummary>, ObservableValue<AnnotationSummary>>() {
            // cache the AnnotationSummary because sorting and isotope pattern score is expensive
            // first time sorting is slow and from then on it is as fast as all the other columns
            private record RowAnnotationSummary(FeatureListRow row, FeatureAnnotation annotation) {

            }

            private final Map<RowAnnotationSummary, AnnotationSummary> cache = new HashMap<>();

            @Override
            public ObservableValue<AnnotationSummary> call(
                CellDataFeatures<ModularFeatureListRow, AnnotationSummary> cdf) {
              final ModularFeatureListRow row = cdf.getValue().getValue();
              final Object value = row.get((DataType<?>) parentType);
              final FeatureAnnotation annotation;
              if (value instanceof List list) {
                annotation =
                    list != null && !list.isEmpty() ? (FeatureAnnotation) list.getFirst() : null;
              } else if (value instanceof FeatureAnnotation a) {
                annotation = a;
              } else {
                annotation = null;
              }

              if (annotation == null) {
                return new ReadOnlyObjectWrapper<>();
              }
              final RowAnnotationSummary cacheKey = new RowAnnotationSummary(row, annotation);
              final AnnotationSummary summary = cache.computeIfAbsent(cacheKey,
                  _ -> AnnotationSummary.of(row, annotation));
              return new ReadOnlyObjectWrapper<>(summary);
            }
          });
    } else {
      // currently not used but in case this type was added directly to the row, then use the preferred annotation
      column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(
          CompoundAnnotationUtils.getBestAnnotationSummary(cdf.getValue().getValue())));
    }

    column.setCellFactory(col -> new MicroChartCell());
    column.setMinWidth(45);
    column.setPrefWidth(45);
    column.setSortable(true);
    // flip sorting so that first click gives correct sorting
    column.setComparator(AnnotationSummaryOrder.MZMINE.getComparatorLowFirst());
//    column.setMaxWidth(60);

    return (TreeTableColumn) column;
  }

  @Override
  public Property<AnnotationSummary> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<AnnotationSummary> getValueClass() {
    return AnnotationSummary.class;
  }

  @Override
  public @Nullable Node createCellContent(@NotNull ModularFeatureListRow row,
      AnnotationSummary cellData, @Nullable RawDataFile raw, AtomicDouble progress) {
    throw new IllegalStateException("Statement should be unreachable due to custom cell factory.");
  }

  /// Thin wrapper around {@link AnnotationSummaryChart} that adapts it to a {@link TreeTableCell}.
  /// Cell-only concerns live here (empty/visible state, cell-size constraints); the chart node owns
  /// the canvas, the palette, and the tooltip content.
  private static class MicroChartCell extends
      TreeTableCell<ModularFeatureListRow, AnnotationSummary> {

    private final AnnotationSummaryChart chart = new AnnotationSummaryChart();

    public MicroChartCell() {
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      setGraphic(chart);
      setMinHeight(50);
      // The chart owns its tooltip, but we forward visibility/emptiness through setAnnotation,
      // which clears the tooltip content when the cell has no data.
      chart.prefWidthProperty().bind(widthProperty().subtract(getGraphicTextGap() * 2));
      chart.prefHeightProperty().bind(heightProperty().subtract(getGraphicTextGap() * 2));
    }

    @Override
    protected void updateItem(AnnotationSummary item, boolean empty) {
      super.updateItem(item, empty);
      chart.setAnnotation(empty || !isVisible() ? null : item);
    }
  }

}
