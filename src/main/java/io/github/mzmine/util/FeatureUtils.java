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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import java.text.Format;
import java.util.Arrays;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
/*
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualPeak;
 */

/**
 * Utilities for feature lists
 *
 */
public class FeatureUtils {

  private static final FeatureListRowSorter ascMzRowSorter =
      new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending);

  /**
   * Common utility method to be used as Peak.toString() method in various Peak implementations
   *
   * @param feature FeatureOld to be converted to String
   * @return String representation of the peak
   */
  public static String peakToString(Feature feature) {
    StringBuffer buf = new StringBuffer();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
    buf.append("m/z ");
    buf.append(mzFormat.format(feature.getMZ()));
    buf.append(" (");
    buf.append(timeFormat.format(feature.getRT()));
    buf.append(" min) [" + feature.getRawDataFile().getName() + "]");
    return buf.toString();
  }

  /**
   * Compares identities of two feature list rows. 1) if preferred identities are available, they
   * must be same 2) if no identities are available on both rows, return true 3) otherwise all
   * identities on both rows must be same
   *
   * @return True if identities match between rows
   *
   */
  public static boolean compareIdentities(FeatureListRow row1, FeatureListRow row2) {

    if ((row1 == null) || (row2 == null))
      return false;

    // If both have preferred identity available, then compare only those
    PeakIdentity row1PreferredIdentity = row1.getPreferredPeakIdentity();
    PeakIdentity row2PreferredIdentity = row2.getPreferredPeakIdentity();
    if ((row1PreferredIdentity != null) && (row2PreferredIdentity != null)) {
      return row1PreferredIdentity.getName().equals(row2PreferredIdentity.getName());
    }

    // If no identities at all for both rows, then return true
    PeakIdentity[] row1Identities = row1.getPeakIdentities();
    PeakIdentity[] row2Identities = row2.getPeakIdentities();
    if ((row1Identities.length == 0) && (row2Identities.length == 0))
      return true;

    // Otherwise compare all against all and require that each identity has
    // a matching identity on the other row
    if (row1Identities.length != row2Identities.length)
      return false;
    boolean sameID = false;
    for (PeakIdentity row1Identity : row1Identities) {
      sameID = false;
      for (PeakIdentity row2Identity : row2Identities) {
        if (row1Identity.getName().equals(row2Identity.getName())) {
          sameID = true;
          break;
        }
      }
      if (!sameID)
        break;
    }

    return sameID;
  }

  /**
   * Compare charge state of the best MS/MS precursor masses
   *
   * @param row1 PeaklistRow 1
   * @param row2 PeakListRow 2
   *
   * @return true, same charge state
   */
  public static boolean compareChargeState(FeatureListRow row1, FeatureListRow row2) {

    assert ((row1 != null) && (row2 != null));

    int firstCharge = row1.getBestPeak().getCharge();
    int secondCharge = row2.getBestPeak().getCharge();

    return (firstCharge == 0) || (secondCharge == 0) || (firstCharge == secondCharge);

  }

  // TODO PeakListRows to FeatureListRows
  // S
  /**
   * Returns true if feature list row contains a compound identity matching to id
   *
   */
  public static boolean containsIdentity(FeatureListRow row, PeakIdentity id) {

    for (PeakIdentity identity : row.getPeakIdentities()) {
      if (identity.getName().equals(id.getName()))
        return true;
    }

    return false;
  }

  /**
   * Copies properties such as identification results and comments from the source row to the target
   * row.
   */
  public static void copyFeatureListRowProperties(FeatureListRow source, FeatureListRow target) {

    // Combine the comments
    String targetComment = target.getComment();
    if ((targetComment == null) || (targetComment.trim().length() == 0)) {
      targetComment = source.getComment();
    } else {
      if ((source.getComment() != null) && (source.getComment().trim().length() > 0))
        targetComment += "; " + source.getComment();
    }
    target.setComment(targetComment);

    // TODO:
    // Copy all peak identities, if these are not already present
    /*
    for (PeakIdentity identity : source.getPeakIdentities()) {
      if (!containsIdentity(target, identity))
        target.addPeakIdentity(identity, false);
    }
    */

    // Set the preferred identity
    target.setPreferredPeakIdentity(source.getPreferredPeakIdentity());

  }

  // TODO FeatureOld to Feature
  /**
   * Copies properties such as isotope pattern and charge from the source peak to the target peak
   */
  public static void copyPeakProperties(Feature source, Feature target) {

    // Copy isotope pattern
    IsotopePattern originalPattern = source.getIsotopePattern();
    if (originalPattern != null)
      target.setIsotopePattern(originalPattern);

    // Copy charge
    int charge = source.getCharge();
    target.setCharge(charge);

  }

  // TODO FeatureOld to Feature
  /**
   * Finds a combined m/z range that covers all given features
   */
  public static Range<Double> findMZRange(Feature features[]) {

    Range<Double> mzRange = null;

    for (Feature p : features) {
      if (mzRange == null) {
        mzRange = p.getRawDataPointsMZRange();
      } else {
        mzRange = mzRange.span(p.getRawDataPointsMZRange());
      }
    }

    return mzRange;

  }

  // TODO FeatureOld(ManualPeak) to Feature
  /**
   * Integrates over a given m/z and rt range within a raw data file.
   *
   * @param dataFile
   * @param rtRange
   * @param mzRange
   * @return The result of the integration.
   */
  public static double integrateOverMzRtRange(RawDataFile dataFile, Range<Double> rtRange,
      Range<Double> mzRange) {
    /*
    ManualPeak newPeak = new ManualPeak(dataFile);
    boolean dataPointFound = false;

    int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

    for (int scanNumber : scanNumbers) {

      // Get next scan
      Scan scan = dataFile.getScan(scanNumber);

      // Find most intense m/z peak
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

      if (basePeak != null) {
        if (basePeak.getIntensity() > 0)
          dataPointFound = true;
        newPeak.addDatapoint(scan.getScanNumber(), basePeak);
      } else {
        final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
        DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
        newPeak.addDatapoint(scan.getScanNumber(), fakeDataPoint);
      }

    }

    if (dataPointFound) {
      newPeak.finalizePeak();
      return newPeak.getArea();
    } else {
      return 0.0;
    }
    */
    // REMOVE
    return Double.NaN;
    // REMOVE
  }

  // TODO FeatureOld to Feature
  /**
   *
   * @param row The row.
   * @return The average retention time range of all features contained in this peak list row across
   *         all raw data files. Empty range (0,0) if the row is null or has no feature assigned to
   *         it.
   */
  public @Nonnull static Range<Float> getPeakListRowAvgRtRange(FeatureListRow row) {

    if (row == null || row.getBestPeak() == null)
      return Range.closed(0.f, 0.f);

    int size = row.getFeatures().size();
    double[] lower = new double[size];
    double[] upper = new double[size];

    Feature[] f = row.getFeatures().toArray(new Feature[0]);

    for (int i = 0; i < size; i++) {
      if (f[i] == null)
        continue;

      Range<Float> r = f[i].getRawDataPointsRTRange();

      lower[i] = r.lowerEndpoint();
      upper[i] = r.upperEndpoint();
    }

    float avgL = 0, avgU = 0;
    for (int i = 0; i < size; i++) {
      avgL += lower[i];
      avgU += upper[i];
    }
    avgL /= size;
    avgU /= size;

    return Range.closed(avgL, avgU);
  }

  // TODO PeakListRow to FeatureListRow
  /**
   * Creates a copy of a PeakListRow.
   *
   * @param row A row.
   * @return A copy of row.
   */
  public static FeatureListRow copyPeakRow(final FeatureListRow row) {
    // TODO: generalize beyond modular
    // Copy the feature list row.
    final FeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) row.getFeatureList());
    copyFeatureListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getFeatures()) {
      final Feature newPeak = new ModularFeature((ModularFeatureList) peak.getFeatureList());
      copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getRawDataFile(), newPeak);
    }

    return newRow;
  }

  // TODO PeakListRow to FeatureListRow
  /**
   * Creates a copy of an array of PeakListRows.
   *
   * @param rows The rows to be copied.
   * @return A copy of rows.
   */
  public static FeatureListRow[] copyPeakRows(final FeatureListRow[] rows) {
    FeatureListRow[] newRows = new FeatureListRow[rows.length];

    for (int i = 0; i < newRows.length; i++) {
      newRows[i] = copyPeakRow(rows[i]);
    }

    return newRows;
  }

  // TODO PeakListRow to FeatureListRow
  /**
   * Convenience method to sort an array of PeakListRows by ascending m/z
   *
   * @param rows
   * @return Array sorted by ascending m/z.
   */
  public static FeatureListRow[] sortRowsMzAsc(FeatureListRow[] rows) {
    Arrays.sort(rows, ascMzRowSorter);
    return rows;
  }
}
