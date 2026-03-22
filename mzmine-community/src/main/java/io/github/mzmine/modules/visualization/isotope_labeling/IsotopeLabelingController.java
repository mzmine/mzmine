/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controller for isotope labeling visualization
 */
public class IsotopeLabelingController extends FxController<IsotopeLabelingModel> implements
    SelectedFeatureListsBinding, SelectedRowsBinding {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingController.class.getName());

  private final IsotopeLabelingViewBuilder viewBuilder;
  private final Region view;
  private Map<Integer, List<FeatureListRow>> allIsotopeClusters = new HashMap<>();

  public IsotopeLabelingController(@Nullable FeatureList featureList) {
    super(new IsotopeLabelingModel());

    // Initialize feature list if provided
    if (featureList != null) {
      model.setFeatureLists(List.of(featureList));

      // Extract labeling parameters so that visualization can group files correctly
      model.extractLabelingParamsFromFeatureList(featureList);

      // Extract all isotope clusters via the model
      allIsotopeClusters = model.extractIsotopeClusters(featureList);

      if (!allIsotopeClusters.isEmpty()) {
        logger.info("Found " + allIsotopeClusters.size() + " isotope clusters in feature list: "
            + featureList.getName());
      }
    }

    viewBuilder = new IsotopeLabelingViewBuilder(model);
    view = viewBuilder.build();

    initializeListeners();

    // Initial update of view with clusters
    Platform.runLater(() -> viewBuilder.updateClusterList(allIsotopeClusters));
  }

  private void initializeListeners() {
    // Listen for feature list changes
    model.featureListsProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue != null && !newValue.isEmpty()) {
        FeatureList featureList = newValue.get(0);
        model.extractLabelingParamsFromFeatureList(featureList);
        allIsotopeClusters = model.extractIsotopeClusters(featureList);

        Platform.runLater(() -> viewBuilder.updateClusterList(allIsotopeClusters));

        updateVisualization();
      }
    });

    // Listen for changes in visualization settings or selected clusters
    PropertyUtils.onChange(this::updateVisualization, model.visualizationTypeProperty(),
        model.selectedClustersProperty(), model.maxIsotopologuesProperty(),
        model.normalizeToBaseIsotopologueProperty(), model.showSignificanceMarkersProperty());
    
    model.selectedRowsProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue == null || newValue.isEmpty()) {
        return;
      }
      List<Integer> clusterIds = new ArrayList<>();
      for (FeatureListRow row : newValue) {
        Integer clusterId = IsotopeLabelingModel.getIsotopeClusterId(row);
        if (clusterId != null && !clusterIds.contains(clusterId)) {
          clusterIds.add(clusterId);
        }
      }
      if (!clusterIds.isEmpty()) {
        model.setSelectedClusters(clusterIds);
      }
    });
  }

  private void updateVisualization() {
    if (allIsotopeClusters.isEmpty() || model.getSelectedClusters() == null
        || model.getSelectedClusters().isEmpty()) {
      return;
    }
    onTaskThreadDelayed(new IsotopeLabelingUpdateTask(model, allIsotopeClusters));
  }

  @Override
  public ObjectProperty<List<FeatureList>> selectedFeatureListsProperty() {
    return model.featureListsProperty();
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  public Region getView() {
    return view;
  }

  @Override
  protected @NotNull FxViewBuilder<IsotopeLabelingModel> getViewBuilder() {
    return viewBuilder;
  }

  /**
   * Get all available isotope clusters
   *
   * @return Map of cluster IDs to lists of feature list rows
   */
  public Map<Integer, List<FeatureListRow>> getAllIsotopeClusters() {
    return allIsotopeClusters;
  }

  /**
   * Get the model used by this controller
   *
   * @return The model
   */
  public IsotopeLabelingModel getModel() {
    return model;
  }
}
