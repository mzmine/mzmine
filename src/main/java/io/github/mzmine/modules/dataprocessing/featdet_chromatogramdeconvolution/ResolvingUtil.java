/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResolvingUtil {

  /**
   * Resolves the data with the given resolver and directly processes it to new {@link
   * IonTimeSeries} which can be used for visualisation or feature building.
   *
   * @param resolver
   * @param data
   * @param storage   May be null, if the values shall be stored in ram (e.g. previews)
   * @param dimension
   * @return
   */
  public static List<IonTimeSeries<? extends Scan>> resolve(@Nonnull final XYResolver resolver,
      @Nonnull final IonTimeSeries<? extends Scan> data, @Nullable final MemoryMapStorage storage,
      @Nonnull final ResolvingDimension dimension) {
    final double[][] extractedData = extractData(data, dimension);

    final List<IonTimeSeries<? extends Scan>> resolvedSeries = new ArrayList<>();
    final Collection<List<ResolvedValue<Double, Double>>> resolvedData = resolver
        .resolve(extractedData[0], extractedData[1]);
    for (List<ResolvedValue<Double, Double>> resolvedValues : resolvedData) {
      // 3 points to a triangle
      if (resolvedValues.size() < 3) {
        continue;
      }
      // find start and end of the cut
      ResolvedValue<Double, Double> firstPair = resolvedValues.get(0);
      ResolvedValue<Double, Double> lastPair = resolvedValues.get(resolvedValues.size() - 1);

      if (dimension == ResolvingDimension.RETENTION_TIME) {
        // make a sub list of the original scans
        List<? extends Scan> subset = data.getSpectra().stream().filter(
            s -> Double.compare(s.getRetentionTime(), firstPair.x) >= 0
                && Double.compare(s.getRetentionTime(), lastPair.x) <= 0).collect(
            Collectors.toList());
        if (data instanceof SimpleIonTimeSeries) {
          SimpleIonTimeSeries resolved = ((SimpleIonTimeSeries) data)
              .subSeries(storage, (List<Scan>) subset);
          resolvedSeries.add(resolved);
        } else if (data instanceof IonMobilogramTimeSeries) {
          IonMobilogramTimeSeries resolved = ((IonMobilogramTimeSeries) data)
              .subSeries(storage, (List<Frame>) subset);
          resolvedSeries.add(resolved);
        } else {
          throw new IllegalArgumentException("Resolving behaviour of " + data.getClass().getName()
              + " not specified.");
        }

      } else if (dimension == ResolvingDimension.MOBILITY
          && data instanceof IonMobilogramTimeSeries) {
        List<SimpleIonMobilitySeries> resolvedMobilograms = new ArrayList<>();
        for (SimpleIonMobilitySeries mobilogram : ((IonMobilogramTimeSeries) data)
            .getMobilograms()) {
          // make a sub list of the original scans
          List<MobilityScan> subset = mobilogram.getSpectra().stream()
              .filter(s -> Double.compare(s.getMobility(), firstPair.x) >= 0
                  && Double.compare(s.getMobility(), lastPair.x) <= 0).collect(
                  Collectors.toList());
          if (subset.size() < 3) {
            continue;
          }
          SimpleIonMobilitySeries resolvedMobilogram = (SimpleIonMobilitySeries) mobilogram
              .subSeries(storage, subset);
          resolvedMobilograms.add(resolvedMobilogram);
        }
        if(resolvedMobilograms.isEmpty()) {
          continue;
        }

        IonMobilogramTimeSeries resolved = new SimpleIonMobilogramTimeSeries(
            storage, resolvedMobilograms);
        resolvedSeries.add(resolved);

      } else {
        throw new IllegalArgumentException("Resolving behaviour of " + data.getClass().getName()
            + " not specified.");
      }
    }
    return resolvedSeries;
  }

  /**
   * Extracts the data from a series with regard to the requested dimension.
   *
   * @param data
   * @param dimension
   * @return 2d array of the requested dimension. [0][n] will represent the domain dimension, [1][n]
   * will represent the range dimension.
   */
  public static double[][] extractData(@Nonnull IonTimeSeries<? extends Scan> data,
      @Nonnull ResolvingDimension dimension) {
    double[] xdata;
    double[] ydata;
    if (dimension == ResolvingDimension.RETENTION_TIME) {
      xdata = new double[data.getNumberOfValues()];
      ydata = new double[data.getNumberOfValues()];
      for (int j = 0; j < data.getNumberOfValues(); j++) {
        xdata[j] = data.getSpectra().get(j).getRetentionTime();
        ydata[j] = data.getIntensity(j);
      }
    } else {
      if (data instanceof IonMobilogramTimeSeries mobData) {
        SummedIntensityMobilitySeries summedMobilogram = mobData.getSummedMobilogram();
        xdata = new double[summedMobilogram.getNumberOfValues()];
        ydata = new double[summedMobilogram.getNumberOfValues()];

        // tims 1/k0 decreases with scan number, drift time increases
        MobilityType mt = mobData.getSpectra().get(0).getMobilityType();
        int operation = mt == MobilityType.TIMS ? -1 : +1;
        int dataIndex = 0;
        for (int j = (mt == MobilityType.TIMS ? summedMobilogram.getNumberOfValues() - 1 : 0);
            j < summedMobilogram.getNumberOfValues() && j >= 0; j += operation) {
          xdata[dataIndex] = summedMobilogram.getMobility(j);
          ydata[dataIndex] = summedMobilogram.getIntensity(j);
          dataIndex++;
        }
      } else {
        throw new IllegalArgumentException(
            "Cannot resolve ion mobility data for " + data.getClass().getName()
                + ". No mobility dimension.");
      }
    }
    return new double[][]{xdata, ydata};
  }

}
