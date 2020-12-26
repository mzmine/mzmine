/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.MIN_RELATIVE_HEIGHT;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Mobilogram;
import io.github.mzmine.datamodel.impl.MobilityDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMobilogram;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SavitzkyGolayFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithMobilogramPreview;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.deconvolution.impl.LocalMinimumResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MobilogramSmootherSetupDialog extends ParameterSetupDialogWithMobilogramPreview {

  private Set<Integer> previousDataSetNums;

  public MobilogramSmootherSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    previousDataSetNums = new HashSet<>();

  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();

    updateParameterSetFromComponents();
    Mobilogram mobilogram = controller.getSelectedMobilogram();

    List<String> errorMessages = new ArrayList<>();
    if (!parameterSet.checkParameterValues(errorMessages) || mobilogram == null) {
      return;
    }

    final double[] weights = SavitzkyGolayFilter.getNormalizedWeights(
        parameterSet.getParameter(MobilogramSmootherParameters.filterWidth).getValue());
    Mobilogram smoothed = MobilogramSmootherTask.sgSmoothMobilogram(mobilogram, weights);

    previousDataSetNums.forEach(num -> controller.getMobilogramChart().removeDataSet(num));

    PreviewMobilogram preview = new PreviewMobilogram(smoothed, "smoothed");
    previousDataSetNums.add(controller.getMobilogramChart().addDataset(preview));
    Set<Mobilogram> mobilograms = resolveMobilogram(mobilogram, smoothed, parameterSet);
    int counter = 1;
    for (Mobilogram mob : mobilograms) {
      PreviewMobilogram prev = new PreviewMobilogram(mob, "Deconv " + counter);
      previousDataSetNums.add(controller.getMobilogramChart().addDataset(new ColoredXYDataset(prev),
          new ColoredXYShapeRenderer()));
      counter++;
    }
  }

  @Override
  public void onMobilogramSelectionChanged(Mobilogram newMobilogram) {
    parametersChanged();
  }

  private Set<Mobilogram> resolveMobilogram(Mobilogram originalMobilogram,
      Mobilogram smoothedMobilogram, ParameterSet parameters) {

    // deconvolve the smoothed mobilogram, but construct new mobilograms from the original data
    // points
    List<MobilityDataPoint> dataPoints = smoothedMobilogram.getDataPoints().stream()
        .sorted(Comparator.comparingDouble(MobilityDataPoint::getMobility))
        .collect(Collectors.toList());
    double[] mobilities = new double[dataPoints.size()];
    double[] intensities = new double[dataPoints.size()];
    int[] indices = new int[dataPoints.size()];

    for (int i = 0; i < dataPoints.size(); i++) {
      mobilities[i] = dataPoints.get(i).getMobility();
      intensities[i] = dataPoints.get(i).getIntensity();
      indices[i] = i; // cannot use scan number bc. mobility decreases with increasing scannum in
      // TIMS
    }

    final double minAbsHeight =
        parameters.getParameter(MobilogramSmootherParameters.MIN_ABSOLUTE_HEIGHT).getValue();
    final double minRelHeight = parameters.getParameter(MIN_RELATIVE_HEIGHT).getValue();
    final double chromatographicThresholdLevel =
        MathUtils.calcQuantile(intensities,
            parameters.getParameter(MobilogramSmootherParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL)
                .getValue());
    final Range<Double> peakDuration =
        parameters.getParameter(MobilogramSmootherParameters.PEAK_DURATION).getValue();
    final float searchRTRange =
        parameters.getParameter(MobilogramSmootherParameters.SEARCH_MOBILITY_RANGE).getValue()
            .floatValue();
    final double minRatio =
        parameters.getParameter(MobilogramSmootherParameters.MIN_RATIO).getValue();

    double maximumIntensity = Arrays.stream(intensities).max().getAsDouble();
    final double minHeight = Math.max(minAbsHeight, minRelHeight * maximumIntensity);

    LocalMinimumResolver resolver = new LocalMinimumResolver(peakDuration, searchRTRange, minRatio,
        minHeight, chromatographicThresholdLevel);

    Collection<? extends Collection<Integer>> resolved = resolver
        .resolveToIndices(mobilities, intensities, indices);

    Set<Mobilogram> resolvedMobilogram = new HashSet<>();
    List<MobilityDataPoint> originalDataPoints =
        originalMobilogram.getDataPoints().stream()
            .sorted(Comparator.comparingDouble(MobilityDataPoint::getMobility)).collect(
            Collectors.toList());
    logger.info("-----");
    for (Collection<Integer> indicesSet : resolved) {
      logger.info("Number of deconvolved mobilograms size: " + indicesSet.size());

      if (indicesSet.isEmpty()) {
        continue;
      }

      Set<MobilityDataPoint> newDps = new HashSet<>();
      indicesSet.forEach(index -> newDps.add(originalDataPoints.get(index)));
//          originalDataPoints.stream().filter(dp -> scanNumbers.contains(dp.getScanNum())).collect(
//              Collectors.toSet());

      List<MobilityDataPoint> sortedDps = newDps.stream().sorted(Comparator.comparingInt(
          MobilityDataPoint::getScanNum)).collect(Collectors.toList());

      SimpleMobilogram newMobilogram = new SimpleMobilogram(originalMobilogram.getMobilityType(),
          originalMobilogram.getRawDataFile());
      sortedDps.forEach(dp -> newMobilogram.addDataPoint(dp));
      newMobilogram.calc();
      resolvedMobilogram.add(newMobilogram);
    }

    return resolvedMobilogram;
  }
}
