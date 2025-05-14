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
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingParameters;
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

      // Extract all isotope clusters
      allIsotopeClusters = extractIsotopeClusters(featureList);

      // If clusters were found, add them to the view
      if (!allIsotopeClusters.isEmpty()) {
        logger.info("Found " + allIsotopeClusters.size() + " isotope clusters in feature list: "
            + featureList.getName());
      }
    }

    viewBuilder = new IsotopeLabelingViewBuilder(model);
    view = viewBuilder.build();

    initializeListeners();

    // Initial update of view with clusters
    Platform.runLater(() -> {
      viewBuilder.updateClusterList(allIsotopeClusters);
    });
  }

  /**
   * Extract isotope clusters from a feature list
   *
   * @param featureList Feature list to extract clusters from
   * @return Map of cluster IDs to lists of rows
   */
  private Map<Integer, List<FeatureListRow>> extractIsotopeClusters(FeatureList featureList) {
    Map<Integer, List<FeatureListRow>> clusters = new HashMap<>();

    if (featureList == null) {
      return clusters;
    }

    // Check if feature list has the required types
    if (!featureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)
        || !featureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      logger.warning("Feature list does not have the required isotope labeling annotations");
      return clusters;
    }

    // Group rows by cluster ID
    for (FeatureListRow row : featureList.getRows()) {
      if (row instanceof ModularFeatureListRow modRow) {
        Integer clusterId = modRow.get(UntargetedLabelingParameters.isotopeClusterType);

        if (clusterId != null) {
          // Add row to its cluster
          clusters.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(row);
        }
      }
    }

    // Sort rows within each cluster by isotopologue rank
    for (List<FeatureListRow> clusterRows : clusters.values()) {
      clusterRows.sort((r1, r2) -> {
        if (r1 instanceof ModularFeatureListRow modRow1
            && r2 instanceof ModularFeatureListRow modRow2) {
          Integer rank1 = modRow1.get(UntargetedLabelingParameters.isotopologueRankType);
          Integer rank2 = modRow2.get(UntargetedLabelingParameters.isotopologueRankType);

          if (rank1 != null && rank2 != null) {
            return Integer.compare(rank1, rank2);
          }
        }
        return 0;
      });
    }

    return clusters;
  }

  private void initializeListeners() {
    // Listen for feature list changes
    model.featureListsProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue != null && !newValue.isEmpty()) {
        FeatureList featureList = newValue.get(0);
        allIsotopeClusters = extractIsotopeClusters(featureList);

        // Update view with new clusters
        Platform.runLater(() -> {
          viewBuilder.updateClusterList(allIsotopeClusters);
        });

        updateVisualization();
      }
    });

    // Listen for changes in visualization type or selected clusters
    PropertyUtils.onChange(this::updateVisualization, model.visualizationTypeProperty(),
        model.selectedClustersProperty(), model.maxIsotopologuesProperty(),
        model.normalizeToBaseIsotopologueProperty());

    // Listen for changes in selected rows to update selected clusters
    model.selectedRowsProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue != null && !newValue.isEmpty()) {
        // Extract cluster IDs from selected rows
        List<Integer> clusterIds = new ArrayList<>();

        for (FeatureListRow row : newValue) {
          if (row instanceof ModularFeatureListRow modRow) {
            Integer clusterId = modRow.get(UntargetedLabelingParameters.isotopeClusterType);
            if (clusterId != null && !clusterIds.contains(clusterId)) {
              clusterIds.add(clusterId);
            }
          }
        }

        // Update selected clusters if any were found
        if (!clusterIds.isEmpty()) {
          model.setSelectedClusters(clusterIds);
        }
      }
    });
  }

  private void updateVisualization() {
    // Only update if we have clusters and at least one is selected
    if (allIsotopeClusters.isEmpty() || model.getSelectedClusters() == null
        || model.getSelectedClusters().isEmpty()) {
      return;
    }

    // Schedule background task to update visualization
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
   * Get the model used by this controller Required for IsotopeLabelingModule to set parameters
   *
   * @return The model
   */
  public IsotopeLabelingModel getModel() {
    return model;
  }
}