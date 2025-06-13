/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.SelectedFilesBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrParameters;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PseudoSpectrumVisualizerController extends
    FxController<PseudoSpectrumVisualizerModel> implements SelectedRowsBinding,
    SelectedFilesBinding {

  private static final Logger logger = Logger.getLogger(
      PseudoSpectrumVisualizerController.class.getName());

  public PseudoSpectrumVisualizerController() {
    super(new PseudoSpectrumVisualizerModel());

    initListeners();
  }

  private void initListeners() {
    // extract last mz tolerance from feature list
    model.featureListProperty().subscribe(
        (_, flist) -> model.setMzTolerance(extractMzToleranceFromPreviousMethods(flist)));

    // update the tic plot if row or raw data file changes
    PropertyUtils.onChange(this::updateDatasets, model.selectedFileProperty(),
        model.selectedRowProperty());
  }

  private void updateDatasets() {
    onTaskThreadDelayed(new PseudoSpectrumVisualizerUpdateTask(model));
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  @Override
  public ObjectProperty<List<RawDataFile>> selectedRawFilesProperty() {
    return model.selectedFilesProperty();
  }

  @Override
  protected @NotNull FxViewBuilder<PseudoSpectrumVisualizerModel> getViewBuilder() {
    return new PseudoSpectrumVisualizerViewBuilder(model);
  }

  public void setFeature(@Nullable Feature feature) {
    setRow(feature == null ? null : feature.getRow());
    setRawFile(feature == null ? null : feature.getRawDataFile());
  }

  public void setRawFile(@Nullable RawDataFile raw) {
    model.selectedFilesProperty().setValue(raw == null ? List.of() : List.of(raw));
  }

  public void setRow(@Nullable FeatureListRow row) {
    setRawFile(null); // resets to best feature of row
    model.selectedRowsProperty().setValue(row == null ? List.of() : List.of(row));
  }


  /*
   *  Extract mz tolerance from DIA correlation or GC clustering module
   * */
  public static MZTolerance extractMzToleranceFromPreviousMethods(FeatureList flist) {
    try {
      final ObservableList<FeatureListAppliedMethod> appliedMethods = flist.getAppliedMethods();
      // for DIA correlation
      final Optional<MZTolerance> ms2Tolerance = ParameterUtils.getValueFromAppliedMethods(
          appliedMethods, DiaMs2CorrParameters.class, DiaMs2CorrParameters.ms2ScanToScanAccuracy);
      if (ms2Tolerance.isPresent()) {
        return ms2Tolerance.get();
      }

      // for GC-EI workflow use tolerance of chromatogram building, because deconvolution does
      // not use mz tol parameter
      boolean hasChromatograms = appliedMethods.stream().anyMatch(
          appliedMethod -> appliedMethod.getParameters().getClass()
              .equals(ADAPChromatogramBuilderParameters.class));
      if (hasChromatograms) {
        return ParameterUtils.getValueFromAppliedMethods(appliedMethods,
                ADAPChromatogramBuilderParameters.class, ADAPChromatogramBuilderParameters.mzTolerance)
            .orElse(new MZTolerance(0.005, 15));
      }
    } catch (Exception e) {
      logger.log(Level.WARNING,
          " Could not extract previously used mz tolerance, will apply default settings. "
              + e.getMessage());
    }
    return new MZTolerance(0.005, 15);
  }

  public void setColor(Color color) {
    model.setColor(color);
  }

}
