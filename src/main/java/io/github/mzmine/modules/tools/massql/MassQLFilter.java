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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.tools.massql;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import java.util.Map;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MassQLFilter {

  public static final double DEFAULT_MZ_TOLERANCE = 0.005;
  public static final double DEFAULT_PPM_TOLERANCE = 20d;

  private final Condition condition;
  private final Map<ConditionQualifier, Double> qualifiers;
  private final List<Double> values;

  // special case with mz tolerance
  private final MZTolerance mzTol;

  /**
   * @param condition  the condition
   * @param qualifiers qualifiers mapped to their values
   * @param values     conditions values
   */
  public MassQLFilter(Condition condition, Map<ConditionQualifier, Double> qualifiers,
      List<Double> values) {
    this.condition = condition;
    this.qualifiers = qualifiers;
    this.values = values;
    // search for mz tolerance
    Double ppm = DEFAULT_PPM_TOLERANCE;
    Double mz = DEFAULT_MZ_TOLERANCE;
    if(qualifiers != null) {
      for (var entry : qualifiers.entrySet()) {
        if (ConditionQualifier.TOLERANCEPPM == entry.getKey()) {
          ppm = entry.getValue();
        }
        if (ConditionQualifier.TOLERANCEMZ == entry.getKey()) {
          mz = entry.getValue();
        }
      }
    }
    mzTol = new MZTolerance(mz, ppm);
  }

  /**
   * @param row candidate row
   * @return true if the row matches all filters. false if one fails
   */
  public boolean accept(FeatureListRow row) {
    return switch (condition) {
      case RTMIN -> row.getAverageRT() >= values.get(0);
      case RTMAX -> row.getAverageRT() <= values.get(0);
      case SCANMIN -> row.getBestFeature().getRepresentativeScan().getScanNumber() >= values.get(0)
          .intValue();
      case SCANMAX -> row.getBestFeature().getRepresentativeScan().getScanNumber() <= values.get(0)
          .intValue();
      case CHARGE -> row.getRowCharge() == values.get(0).intValue();
      case POLARITY -> throw new UnsupportedOperationException(
          "Polarity is currently not supported");
      // precursor
      case MS2PREC -> mzTol.checkWithinTolerance(row.getAverageMZ(), values.get(0));
      // signals in MS2 spectra
      case MS2PROD -> row.getAllFragmentScans().stream()
          .anyMatch(scan -> containsFragmentIon(scan, values.get(0)));
      case MS2NL -> row.getAllFragmentScans().stream()
          .anyMatch(scan -> containsMzDelta(scan, values.get(0)));
    };
  }


  public boolean accept(Scan scan) {
    return switch (condition) {
      case RTMIN -> scan.getRetentionTime() >= values.get(0);
      case RTMAX -> scan.getRetentionTime() <= values.get(0);
      case SCANMIN -> scan.getScanNumber() >= values.get(0).intValue();
      case SCANMAX -> scan.getScanNumber() <= values.get(0).intValue();
      case CHARGE -> scan.getMSLevel() > 1 && scan.getPrecursorCharge() == values.get(0).intValue();
      case POLARITY -> throw new UnsupportedOperationException(
          "Polarity is currently not supported");
      // precursor
      case MS2PREC -> scan.getMSLevel() > 1 && mzTol
          .checkWithinTolerance(scan.getPrecursorMZ(), values.get(0));
      // signals in MS2 spectra
      case MS2PROD -> scan.getMSLevel() > 1 && containsFragmentIon(scan, values.get(0));
      case MS2NL -> scan.getMSLevel() > 1 && containsMzDelta(scan, values.get(0));
    };
  }

  public boolean containsFragmentIon(Scan scan, double targetMZ) {
    if (scan.getMSLevel() <= 1) {
      return false;
    }
    return containsMz(scan, targetMZ);
  }

  public boolean containsMz(Scan scan, double targetMZ) {
    double[] mzs = scan.getMzValues(new double[scan.getNumberOfDataPoints()]);
    for (double mz : mzs) {
      if (mzTol.checkWithinTolerance(mz, targetMZ)) {
        return true;
      }
    }
    return false;
  }

  public boolean containsMzDelta(Scan scan, double mzDelta) {
    double[] mzs = scan.getMzValues(new double[scan.getNumberOfDataPoints()]);
    double absDelta = Math.abs(mzDelta);
    for (int i = 0; i < mzs.length - 1; i++) {
      for (int k = 1; k < mzs.length; k++) {
        if (mzTol.checkWithinTolerance(absDelta, Math.abs(mzs[i] - mzs[k]))) {
          return true;
        }
      }
    }
    return false;
  }

}
