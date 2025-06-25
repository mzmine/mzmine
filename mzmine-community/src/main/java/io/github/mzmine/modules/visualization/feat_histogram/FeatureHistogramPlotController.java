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

package io.github.mzmine.modules.visualization.feat_histogram;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

/**
 * A histogram for features of one or multiple feature lists
 */
public class FeatureHistogramPlotController extends
    FxController<FeatureHistogramPlotModel> implements SelectedFeatureListsBinding {

  private static final Logger logger = Logger.getLogger(
      FeatureHistogramPlotController.class.getName());

  private final FeatureHistogramPlotViewBuilder viewBuilder;

  public FeatureHistogramPlotController() {
    this(List.of());
  }

  public FeatureHistogramPlotController(@NotNull List<FeatureList> featureLists) {
    super(new FeatureHistogramPlotModel());

    viewBuilder = new FeatureHistogramPlotViewBuilder(model);
    initializeListeners();

    // after initializing everything
    model.setFeatureLists(featureLists);
  }

  private void initializeListeners() {
    logger.info("Initializing feature histogram plot listeners.");
    PropertyUtils.onChange(this::computeDataset, model.featureListsProperty(),
        model.selectedTypeProperty());

    // first listen for changes to typeChoices
    model.getTypeChoices().addListener((ListChangeListener<NumberType>) _ -> {
      logger.fine(() -> "Type choices changed in feature histogram");
      var selectedType = model.getSelectedType();
      if (selectedType == null) {
        logger.info("Selected type was null");
        return;
      }
      if (!model.getTypeChoices().contains(selectedType)) {
        logger.info("Selected type removed due to unavailability");
        model.setSelectedType(null); // remove as its not present
      }
    });

    // listen to changes to featureLists - extract all number types
    model.featureListsProperty().subscribe((_) -> {
      logger.info(() -> "Feature lists changed");
      List<NumberType> types = requireNonNullElse(model.getFeatureLists(),
          new ArrayList<FeatureList>()).stream().map(FeatureList::getFeatureTypes)
          .flatMap(Collection::stream).filter(NumberType.class::isInstance)
          .map(NumberType.class::cast)
          .sorted(Comparator.comparing(dt -> dt.getHeaderString().toLowerCase())).toList();
      model.getTypeChoices().setAll(types);
    });
  }

  private void computeDataset() {
    logger.info(() -> "Computing dataset");
    // wait and update
    onTaskThreadDelayed(new FeatureHistogramPlotUpdateTask(model));
  }

  @Override
  public ObjectProperty<List<FeatureList>> selectedFeatureListsProperty() {
    return model.featureListsProperty();
  }

  public Region getView() {
    return viewBuilder.build();
  }

  @Override
  protected @NotNull FxViewBuilder<FeatureHistogramPlotModel> getViewBuilder() {
    return viewBuilder;
  }

}
