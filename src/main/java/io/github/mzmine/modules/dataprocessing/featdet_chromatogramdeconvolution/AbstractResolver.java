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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractResolver implements Resolver {

  protected final ParameterSet generalParameters;
  protected final ResolvingDimension dimension;
  protected final ModularFeatureList flist;
  protected final RawDataFile file;
  protected BinningMobilogramDataAccess mobilogramDataAccess;

  protected AbstractResolver(@NotNull final ParameterSet parameters,
      @NotNull final ModularFeatureList flist) {
    this.generalParameters = parameters;
    dimension = parameters.getParameter(GeneralResolverParameters.dimension).getValue();
    this.flist = flist;
    file = flist.getRawDataFile(0);
  }

  @Override
  public @NotNull <T extends IonTimeSeries<? extends Scan>> List<T> resolve(@NotNull T series,
      @Nullable MemoryMapStorage storage) {
    // To properly resolve, we need the scans of the original series. In a full access, all scans
    // will be included, even the ones where the feature was not detected in. Furthermore, we need
    // the original series to know about the data type.
    final IonTimeSeries<? extends Scan> originalSeries =
        series instanceof FeatureDataAccess acc ? acc.getFeature().getFeatureData() : series;

    final List<T> resolved = new ArrayList<>();
    if (dimension == ResolvingDimension.RETENTION_TIME) {
      final List<Range<Double>> resolvedRanges = resolveRt(series);

      // make a new subseries for each resolved range.
      for (final Range<Double> range : resolvedRanges) {
        final List<? extends Scan> subList = originalSeries.getSpectra().stream()
            .filter(s -> range.contains((double) s.getRetentionTime())).toList();
        if (subList.isEmpty()) {
          continue;
        }
        if (originalSeries instanceof IonMobilogramTimeSeries trace) {
          resolved.add((T) trace.subSeries(null, (List<Frame>) subList, getMobilogramDataAccess()));
        } else if (originalSeries instanceof SimpleIonTimeSeries chrom) {
          resolved.add((T) chrom.subSeries(null, (List<Scan>) subList));
        } else {
          throw new IllegalStateException(
              "Resolving behaviour of " + originalSeries.getClass().getName() + " not specified.");
        }
      }
    } else if (dimension == ResolvingDimension.MOBILITY
        && originalSeries instanceof IonMobilogramTimeSeries originalTrace) {
      setSeriesToMobilogramDataAccess(series);
      final List<Range<Double>> resolvedRanges = resolveMobility(mobilogramDataAccess);

      // make a new sub series for each resolved range.
      for (Range<Double> resolvedRange : resolvedRanges) {
        final List<IonMobilitySeries> resolvedMobilograms = new ArrayList<>();
        for (IonMobilitySeries mobilogram : originalTrace.getMobilograms()) {
          // split every mobilogram
          final List<MobilityScan> subset = mobilogram.getSpectra().stream()
              .filter(scan -> resolvedRange.contains(scan.getMobility()))
              .collect(Collectors.toList());
          if (subset.isEmpty()) {
            continue;
          }
          // IonMobilitySeries are stored in ram until they are added to an IonMobilogramTimeSeries
          resolvedMobilograms.add((SimpleIonMobilitySeries) mobilogram.subSeries(null, subset));
        }
        if (resolvedMobilograms.isEmpty()) {
          continue;
        }

        resolved.add((T) IonMobilogramTimeSeriesFactory
            .of(storage, resolvedMobilograms, getMobilogramDataAccess()));
      }
    } else {
      throw new IllegalStateException(
          "Cannot resolve " + originalSeries.getClass().getName() + " in " + dimension
              + " mobility dimension.");
    }
    return resolved;
  }

  protected <T extends IonTimeSeries<? extends Scan>> void setSeriesToMobilogramDataAccess(
      @NotNull T series) {
    if (series instanceof SummedIntensityMobilitySeries mob) {
      getMobilogramDataAccess().setMobilogram(mob);
    } else if (series instanceof IonMobilogramTimeSeries mob) {
      if (!mob.getSpectrum(0).getDataFile().equals(file)) {
        throw new IllegalArgumentException("The given series is of a different raw data file.");
      }
      getMobilogramDataAccess().setMobilogram(mob.getSummedMobilogram());
    } else if (series instanceof FeatureDataAccess access && access.getFeature()
        .getFeatureData() instanceof IonMobilogramTimeSeries mob) {
      if (!mob.getSpectrum(0).getDataFile().equals(file)) {
        throw new IllegalArgumentException("The given series is of a different raw data file.");
      }
      getMobilogramDataAccess().setMobilogram(mob.getSummedMobilogram());
    } else {
      throw new IllegalArgumentException(
          "Unexpected type of ion series (" + series.getClass().getName()
              + "). Please contact the developers. ");
    }
  }

  public BinningMobilogramDataAccess getMobilogramDataAccess() {
    if (mobilogramDataAccess != null) {
      return mobilogramDataAccess;
    }

    if (file instanceof IMSRawDataFile imsFile) {
      mobilogramDataAccess = new BinningMobilogramDataAccess(imsFile,
          BinningMobilogramDataAccess.getPreviousBinningWith(flist, imsFile.getMobilityType()));
      return mobilogramDataAccess;
    }
    throw new RuntimeException("Could not initialize BinningMobilogramDataAccess.");
  }
}
