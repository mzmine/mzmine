/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.SortableFeatureComboBox;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class FeatureResolverSetupDialog extends ParameterSetupDialogWithPreview {

  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChart;
  protected final SimpleXYChart<IonTimeSeriesToXYProvider> previewChartBadFeature;
  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;
  protected final NumberFormat mobilityFormat;
  private final PauseTransition delayedParameterUpdateListener;
  private final PauseTransition delayedFeatureUpdateListener;
  protected ComboBox<FeatureList> flistBox;
  protected SortableFeatureComboBox fBox;
  protected SortableFeatureComboBox fBoxBadFeature;
  protected BinningMobilogramDataAccess mobilogramBinning;
  protected Resolver resolver;
  private final Map<SimpleXYChart<IonTimeSeriesToXYProvider>, AbstractTask> updateTasksMap = new HashMap<>();

  public FeatureResolverSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);

    uf = MZmineCore.getConfiguration().getUnitFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();

    previewChart = new SimpleXYChart<>("Please select a good EIC",
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));
    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);

    previewChartBadFeature = new SimpleXYChart<>("Please select a noisy EIC",
        uf.format("Retention time", "min"), uf.format("Intensity", "a.u."));
    previewChartBadFeature.setDomainAxisNumberFormatOverride(rtFormat);
    previewChartBadFeature.setRangeAxisNumberFormatOverride(intensityFormat);

    // add pause to delay response to parameter changes
    delayedParameterUpdateListener = new PauseTransition(Duration.seconds(0.5));
    delayedParameterUpdateListener.setOnFinished(event -> updateWithCurrentParameters());
    delayedFeatureUpdateListener = new PauseTransition(Duration.seconds(0.1));
    delayedFeatureUpdateListener.setOnFinished(_ -> updateWithCurrentParameters());

    ObservableList<FeatureList> flists = FXCollections.observableArrayList(
        ProjectService.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    fBox = new SortableFeatureComboBox();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
            fBoxBadFeature.setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
            fBox.setSelectedFeature(findGoodEIC(
                (List<ModularFeatureListRow>) (List<? extends FeatureListRow>) newValue.getRows()));
            fBoxBadFeature.setSelectedFeature(findBadFeature(
                (List<ModularFeatureListRow>) (List<? extends FeatureListRow>) newValue.getRows()));
          } else {
            fBox.setItems(FXCollections.emptyObservableList());
            fBoxBadFeature.setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Feature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object);
      }

      @Override
      public Feature fromString(String string) {
        return null;
      }
    });
    fBox.selectedFeatureProperty().addListener(
        ((_, _, newValue) -> /*startUpdateThreadForChart(previewChart, newValue)*/ delayedFeatureUpdateListener.playFromStart()));

    fBoxBadFeature = new SortableFeatureComboBox();
    fBoxBadFeature.setConverter(new StringConverter<>() {
      @Override
      public String toString(Feature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object) + " (height / area = " + String.format("%.3f",
            object.getHeight() / object.getArea()) + ")";
      }

      @Override
      public Feature fromString(String string) {
        return null;
      }
    });
    fBoxBadFeature.selectedFeatureProperty().addListener(
        ((_, _, newValue) -> /*startUpdateThreadForChart(previewChartBadFeature, newValue)*/ delayedFeatureUpdateListener.playFromStart()));

    final BorderPane pnBadFeaturePreview = new BorderPane();
    pnBadFeaturePreview.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    previewChartBadFeature.setMinHeight(200);
    pnBadFeaturePreview.setCenter(previewChartBadFeature);
    pnBadFeaturePreview.setBottom(new HBox(new Label("Feature "), fBoxBadFeature));

    final BorderPane pnFeaturePreview = new BorderPane();
    previewChart.setMinHeight(200);
    GridPane pnControls = new GridPane(5, 5);
    pnControls.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    pnControls.add(new Label("Feature list "), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature "), 0, 1);
    pnControls.add(fBox, 1, 1);
    pnFeaturePreview.setCenter(previewChart);
    pnFeaturePreview.setBottom(pnControls);

    GridPane preview = new GridPane();
    preview.add(pnBadFeaturePreview, 0, 0, 2, 1);
    preview.add(pnFeaturePreview, 0, 1, 2, 1);
    preview.getRowConstraints()
        .add(new RowConstraints(200, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
    preview.getRowConstraints()
        .add(new RowConstraints(200, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
    preview.getColumnConstraints()
        .add(new ColumnConstraints(200, -1, -1, Priority.ALWAYS, HPos.LEFT, true));
    previewWrapperPane.setCenter(preview);
  }

  protected void startUpdateThreadForChart(SimpleXYChart<IonTimeSeriesToXYProvider> chart,
      Feature newValue) {
    if (newValue == null) {
      return;
    }
    // cancel old
    AbstractTask oldTask = updateTasksMap.get(chart);
    if (oldTask != null) {
      oldTask.cancel();
    }

    // do all of this and only update the chart once finished
    final AbstractTask updateTask = new UpdateTask(this, chart, newValue, resolver, );
    updateTasksMap.put(chart, updateTask);
    TaskService.getController().addTask(updateTask, TaskPriority.HIGH);
  }

  @Deprecated
  protected ResolvedPeak[] resolveFeature(Feature feature) {
    FeatureResolver resolver = ((GeneralResolverParameters) parameterSet).getResolver();
    if (feature == null) {
      return null;
    }
    ResolvedPeak[] resolvedFeatures = resolver.resolvePeaks(feature, parameterSet,
        FeatureDataUtils.DEFAULT_CENTER_FUNCTION, 0, 0);
    return resolvedFeatures;
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    // add a delay to accumulate changes then call updateWithCurrentParameters
    delayedParameterUpdateListener.playFromStart();
  }

  private void updateWithCurrentParameters() {
    updateParameterSetFromComponents();

    if (flistBox.getValue() != null) {
      resolver = ((GeneralResolverParameters) parameterSet).getResolver(parameterSet,
          (ModularFeatureList) flistBox.getValue());
    }

    List<String> errors = new ArrayList<>();
    if (parameterSet.checkParameterValues(errors, true)) {
      startUpdateThreadForChart(previewChart, fBox.getSelectedFeature());
      startUpdateThreadForChart(previewChartBadFeature, fBoxBadFeature.getSelectedFeature());
    }
  }

  @Override
  public void setOnPreviewShown(Runnable onPreviewShown) {
    super.setOnPreviewShown(onPreviewShown);
  }


  private ModularFeature findBadFeature(List<ModularFeatureListRow> rows) {
    final List<ModularFeatureListRow> sortedByArea = rows.stream()
        .sorted((r1, r2) -> Double.compare(r2.getMaxArea(), r1.getMaxArea())).toList();
    final List<ModularFeatureListRow> top20 = new ArrayList<>(
        sortedByArea.subList(0, Math.min(sortedByArea.size() - 1, 20)));

    // we a looking for a feature with a low height to area ratio -> big area but low height could
    // be a noisy chromatogram
    top20.sort(Comparator.comparingDouble(r -> r.getMaxHeight() / r.getMaxArea()));
    return top20.get(0).getBestFeature();
  }

  private ModularFeature findGoodEIC(List<ModularFeatureListRow> rows) {
    final List<ModularFeatureListRow> sortedByArea = rows.stream()
        .sorted((r1, r2) -> Double.compare(r2.getMaxArea(), r1.getMaxArea())).toList();
    final List<ModularFeatureListRow> top30 = new ArrayList<>(
        sortedByArea.subList(0, Math.min(sortedByArea.size() - 1, 30)));

    top30.sort(Comparator.comparingDouble(ModularFeatureListRow::getMaxHeight));
    return top30.get(top30.size() - 1).getBestFeature();
  }

}
