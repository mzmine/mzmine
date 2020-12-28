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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.Mobilogram;
import io.github.mzmine.datamodel.impl.MobilityDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SavitzkyGolayFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MobilogramUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NotFiniteNumberException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;

public class MobilogramSmootherTask extends AbstractTask {

  private final List<Frame> frames;
  private final double[] weights;
  private final int totalFrames;
  private int processedFrames;


  public MobilogramSmootherTask(List<Frame> frames, ParameterSet parameters) {
    this.frames = frames;
    weights =
        SavitzkyGolayFilter.getNormalizedWeights(
            parameters.getParameter(MobilogramSmootherParameters.filterWidth).getValue());

    totalFrames = frames.size();
    processedFrames = 0;
    setStatus(TaskStatus.WAITING);
  }

  @Nullable
  public static SimpleMobilogram loessSmoothMobilogram(Mobilogram mobilogram,
      LoessInterpolator loess) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    // loess needs x val in ascending order, tims has descending mobility
    Collections.reverse(dataPoints);

    double[] smoothedIntensity;
    try {
      smoothedIntensity = loess
          .smooth(dataPoints.stream().mapToDouble(MobilityDataPoint::getMobility).toArray(),
              dataPoints.stream().mapToDouble(MobilityDataPoint::getIntensity).toArray());
    } catch (DimensionMismatchException | NoDataException
        | NotFiniteNumberException | NumberIsTooSmallException e) {
      return null;
    }

    SimpleMobilogram smoothedMobilogram = new SimpleMobilogram(mobilogram.getMobilityType(),
        mobilogram.getRawDataFile());
    for (int i = 0; i < dataPoints.size(); i++) {
      MobilityDataPoint dp = dataPoints.get(i);

      smoothedMobilogram.addDataPoint(new MobilityDataPoint(dp.getMZ(),
          smoothedIntensity[i], dp.getMobility(), dp.getScanNum()));
    }
    smoothedMobilogram.calc();
    return MobilogramUtils.removeZeroIntensityDataPoints(smoothedMobilogram);
  }

  @Nullable
  public static SimpleMobilogram sgSmoothMobilogram(Mobilogram mobilogram,
      double[] weights) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    // tims has descending mobility
    Collections.reverse(dataPoints);

    final double[] smoothedIntensity = SavitzkyGolayFilter
        .convolve(dataPoints.stream().mapToDouble(MobilityDataPoint::getIntensity).toArray(),
            weights);

    SimpleMobilogram smoothedMobilogram = new SimpleMobilogram(mobilogram.getMobilityType(),
        mobilogram.getRawDataFile());

    final double scaleFactor =
        mobilogram.getMaximumIntensity() / Arrays.stream(smoothedIntensity).max().getAsDouble();

    for (int i = 0; i < dataPoints.size(); i++) {
      final MobilityDataPoint dp = dataPoints.get(i);
      double newIntensity = (smoothedIntensity[i]) * scaleFactor;
      newIntensity = (newIntensity > 0) ? newIntensity : 0; // SG can cause artifacts

      smoothedMobilogram.addDataPoint(new MobilityDataPoint(dp.getMZ(),
          newIntensity, dp.getMobility(), dp.getScanNum()));
    }

    smoothedMobilogram.calc();
    return smoothedMobilogram;
  }

  @Override
  public String getTaskDescription() {
    return "Smoothing mobilograms of frame " + processedFrames + "/" + totalFrames;
  }

  @Override
  public double getFinishedPercentage() {
    return (double) processedFrames / totalFrames;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    for (Frame frame : frames) {
      List<Mobilogram> smoothedMobilograms = new ArrayList<>(frame.getMobilograms().size());

      for (Mobilogram mobilogram : frame.getMobilograms()) {
        if (isCanceled()) {
          return;
        }

        SimpleMobilogram smoothedMobilogram = sgSmoothMobilogram(mobilogram, weights);
        smoothedMobilograms.add(smoothedMobilogram);
      }

      frame.clearMobilograms();
      smoothedMobilograms.forEach(frame::addMobilogram);

      processedFrames++;
    }

    setStatus(TaskStatus.FINISHED);
  }
}
