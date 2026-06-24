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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectors;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.loess.LoessSmoothingParameters;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.project.ProjectService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntegrationDashboardController extends FxController<IntegrationDashboardModel> {

  public IntegrationDashboardController() {
    super(new IntegrationDashboardModel());

    model.featureListProperty().subscribe(flist -> {
      model.getFeatureTableFx().setFeatureList(flist);

      final MetadataTable metadata = ProjectService.getMetadata();
      final MetadataColumn<?> sortingCol = model.getRawFileSortingColumn();
      model.setSortedFiles(flist.getRawDataFiles().stream().sorted(Comparator.comparing(
              file -> Objects.requireNonNullElse(metadata.getValue(sortingCol, file), "").toString()))
          .toList());

      model.postProcessingMethodProperty().set(extractPostProcessingMethod(flist));
    });

    model.rawFileSortingColumnProperty().subscribe(col -> model.setSortedFiles(
        model.getFeatureList().getRawDataFiles().stream().sorted(Comparator.comparing(
            file -> Objects.requireNonNullElse(ProjectService.getMetadata().getValue(col, file), "")
                .toString())).toList()));

    // the offset may never be larger than the number of files
    model.sortedFilesProperty().subscribe(files -> model.setGridPaneFileOffset(
        Math.max(Math.min(model.getGridPaneFileOffset(), files.size() - 1), 0)));
    PropertyUtils.onChange(() -> onTaskThread(new FeatureIntegrationDataCalcTask(model)),
        model.rowProperty(), model.applyPostProcessingProperty());
  }

  private static SmoothingAlgorithm extractSmoother(ModularFeatureList flist) {
    final List<ParameterSet> smoothingParams = ParameterUtils.getModuleCalls(
            flist.getAppliedMethods(), SmoothingModule.class).stream()
        .map(FeatureListAppliedMethod::getParameters).toList();
    for (ParameterSet ps : smoothingParams) {
      // find the smoothing step that smoothed RT
      final ModuleOptionsEnumComboParameter<FeatureSmoothingOptions> smoothingParam = ps.getParameter(
          SmoothingParameters.smoothingAlgorithm);
      OptionalParameter<IntegerParameter> rtSmoothing = smoothingParam.getEmbeddedParameters()
          .getParameter(LoessSmoothingParameters.rtSmoothing);
      if (!rtSmoothing.getValue()) {
        continue;
      }
      return FeatureSmoothingOptions.createSmoother(ps);
    }
    return null;
  }

  private static @Nullable BaselineCorrector extractBaselineCorrector(ModularFeatureList flist) {
    final FeatureListAppliedMethod blMethod = ParameterUtils.getLatestModuleCall(
        flist.getAppliedMethods(), BaselineCorrectionModule.class);
    BaselineCorrector corrector = null;
    if (blMethod != null) {
      final BaselineCorrectors correctorParam = blMethod.getParameters()
          .getParameter(BaselineCorrectionParameters.correctionAlgorithm).getValue();
      corrector = correctorParam.getModuleInstance()
          .newInstance(blMethod.getParameters(), flist.getMemoryMapStorage(), flist);
    }
    return corrector;
  }

  @Override
  protected @NotNull FxViewBuilder<IntegrationDashboardModel> getViewBuilder() {
    return new IntegrationDashboardViewBuilder(model);
  }

  public void setFeatureList(@Nullable ModularFeatureList flist) {
    if (flist == null) {
      return;
    }
    model.setFeatureList(flist);
  }

  public <S extends Scan, T extends IonTimeSeries<S>> Function<@NotNull T, @NotNull T> extractPostProcessingMethod(
      ModularFeatureList flist) {
    final SmoothingAlgorithm smoother = extractSmoother(flist);
    // todo: baseline corrector needs the full chromatogram, only a subset is computed in the dashboard
    final BaselineCorrector corrector = null;
//    final BaselineCorrector corrector = extractBaselineCorrector(flist);

    return its -> {
      if (!model.isApplyPostProcessing()) {
        return its;
      }

      final T smoothedRt;
      if (smoother != null) {
        final double @Nullable [] rtSmoothedIntensities = smoother.smoothRt(its);
        smoothedRt = (T) its.copyAndReplace(flist.getMemoryMapStorage(), rtSmoothedIntensities);
      } else {
        smoothedRt = its;
      }

      final T blCorrected;
      if (corrector != null) {
        blCorrected = corrector.correctBaseline(smoothedRt);
      } else {
        blCorrected = smoothedRt;
      }
      return blCorrected;
    };
  }
}
