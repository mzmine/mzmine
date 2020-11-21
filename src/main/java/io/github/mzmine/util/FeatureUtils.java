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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.Format;
import java.util.Arrays;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;

/**
 * Utilities for feature lists
 *
 */
public class FeatureUtils {

  private static final FeatureListRowSorter ascMzRowSorter =
      new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending);

  /**
   * Common utility method to be used as Feature.toString() method in various Feature implementations
   *
   * @param feature Feature to be converted to String
   * @return String representation of the feature
   */
  public static String featureToString(Feature feature) {
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
    ObservableList<PeakIdentity> row1Identities = row1.getPeakIdentities();
    ObservableList<PeakIdentity> row2Identities = row2.getPeakIdentities();
    if ((row1Identities.isEmpty()) && (row2Identities.isEmpty()))
      return true;

    // Otherwise compare all against all and require that each identity has
    // a matching identity on the other row
    if (row1Identities.size() != row2Identities.size())
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
   * @param row1 FeatureListRow 1
   * @param row2 FeatureListRow 2
   *
   * @return true, same charge state
   */
  public static boolean compareChargeState(FeatureListRow row1, FeatureListRow row2) {

    assert ((row1 != null) && (row2 != null));

    int firstCharge = row1.getBestFeature().getCharge();
    int secondCharge = row2.getBestFeature().getCharge();

    return (firstCharge == 0) || (secondCharge == 0) || (firstCharge == secondCharge);

  }

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

    // Copy all feature identities, if these are not already present
    for (PeakIdentity identity : source.getPeakIdentities()) {
      if (!containsIdentity(target, identity))
        target.addPeakIdentity(identity, false);
    }


    // Set the preferred identity
    target.setPreferredPeakIdentity(source.getPreferredPeakIdentity());

  }

  /**
   * Copies properties such as isotope pattern and charge from the source feature to the target feature
   */
  public static void copyFeatureProperties(Feature source, Feature target) {

    // Copy isotope pattern
    IsotopePattern originalPattern = source.getIsotopePattern();
    if (originalPattern != null)
      target.setIsotopePattern(originalPattern);

    // Copy charge
    int charge = source.getCharge();
    target.setCharge(charge);

  }

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

  /**
   * Integrates over a given m/z and rt range within a raw data file.
   *
   * @param dataFile
   * @param rtRange
   * @param mzRange
   * @return The result of the integration.
   */
  public static double integrateOverMzRtRange(RawDataFile dataFile, Range<Float> rtRange,
      Range<Double> mzRange) {
    ManualFeature newFeature = new ManualFeature(dataFile);
    boolean dataPointFound = false;

    int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

    for (int scanNumber : scanNumbers) {

      // Get next scan
      Scan scan = dataFile.getScan(scanNumber);

      // Find most intense m/z feature
      DataPoint baseFeature = ScanUtils.findBasePeak(scan, mzRange);

      if (baseFeature != null) {
        if (baseFeature.getIntensity() > 0)
          dataPointFound = true;
        newFeature.addDatapoint(scan.getScanNumber(), baseFeature);
      } else {
        final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
        DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
        newFeature.addDatapoint(scan.getScanNumber(), fakeDataPoint);
      }

    }

    if (dataPointFound) {
      newFeature.finalizeFeature();
      return newFeature.getArea();
    } else {
      return 0.0;
    }
  }


  /**
   *
   * @param row The row.
   * @return The average retention time range of all features contained in this feature list row across
   *         all raw data files. Empty range (0,0) if the row is null or has no feature assigned to
   *         it.
   */
  public @Nonnull static Range<Float> getFeatureListRowAvgRtRange(FeatureListRow row) {

    if (row == null || row.getBestFeature() == null)
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

  /**
   * Creates a copy of a FeatureListRow.
   *
   * @param row A row.
   * @return A copy of row.
   */
  public static FeatureListRow copyFeatureRow(final FeatureListRow row) {
    // TODO: generalize beyond modular
    // Copy the feature list row.
    final FeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) row.getFeatureList());
    copyFeatureListRowProperties(row, newRow);

    // Copy the features.
    for (final Feature feature : row.getFeatures()) {
      final Feature newFeature = new ModularFeature((ModularFeatureList) feature.getFeatureList());
      copyFeatureProperties(feature, newFeature);
      newRow.addFeature(feature.getRawDataFile(), newFeature);
    }

    return newRow;
  }

  /**
   * Creates a copy of an array of FeatureListRows.
   *
   * @param rows The rows to be copied.
   * @return A copy of rows.
   */
  public static FeatureListRow[] copyFeatureRows(final FeatureListRow[] rows) {
    FeatureListRow[] newRows = new FeatureListRow[rows.length];

    for (int i = 0; i < newRows.length; i++) {
      newRows[i] = copyFeatureRow(rows[i]);
    }

    return newRows;
  }

  /**
   * Convenience method to sort an array of FeatureListRows by ascending m/z
   *
   * @param rows
   * @return Array sorted by ascending m/z.
   */
  public static FeatureListRow[] sortRowsMzAsc(FeatureListRow[] rows) {
    Arrays.sort(rows, ascMzRowSorter);
    return rows;
  }
}
