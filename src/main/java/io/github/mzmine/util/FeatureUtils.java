/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for feature lists
 */
public class FeatureUtils {

  private static final FeatureListRowSorter ascMzRowSorter = new FeatureListRowSorter(
      SortingProperty.MZ, SortingDirection.Ascending);

  /**
   * Common utility method to be used as Feature.toString() method in various Feature
   * implementations
   *
   * @param feature Feature to be converted to String
   * @return String representation of the feature
   */
  public static String featureToString(@Nullable Feature feature) {
    if(feature == null) {
      return "null";
    }
    StringBuffer buf = new StringBuffer();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    buf.append("m/z ");
    buf.append(mzFormat.format(feature.getMZ()));

    final Float averageRT = feature.getRT();
    if (averageRT != null) {
      Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
      buf.append(" (");
      buf.append(timeFormat.format(averageRT));
      buf.append(" min)");
    }

    final Float mobility = feature.getMobility();
    if (mobility != null) {
      Format mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
      buf.append(" [");
      buf.append(mobilityFormat.format(mobility));
      buf.append(" ");
      final MobilityType unit = feature.getMobilityUnit();
      if (unit != null) {
        buf.append(unit.getUnit());
      }
      buf.append("]");
    }

    buf.append(" : ");
    buf.append(feature.getRawDataFile().getName());
    return buf.toString();
  }

  public static String rowToString(FeatureListRow row) {
    StringBuffer buf = new StringBuffer();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();

    buf.append("#");
    buf.append(row.getID());

    buf.append(" m/z ");
    buf.append(mzFormat.format(row.getAverageMZ()));

    final Float averageRT = row.getAverageRT();
    if (averageRT != null) {
      Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
      buf.append(" (");
      buf.append(timeFormat.format(averageRT));
      buf.append(" min)");
    }

    final Float mobility = row.getAverageMobility();
    if (mobility != null) {
      Format mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
      buf.append(" [");
      buf.append(mobilityFormat.format(mobility));
      buf.append(" ");
      final MobilityType unit = row.getBestFeature().getMobilityUnit();
      if (unit != null) {
        buf.append(unit.getUnit());
      }
      buf.append("]");
    }

    return buf.toString();
  }

  /**
   * Compares identities of two feature list rows. 1) if preferred identities are available, they
   * must be same 2) if no identities are available on both rows, return true 3) otherwise all
   * identities on both rows must be same
   *
   * @return True if identities match between rows
   */
  public static boolean compareIdentities(FeatureListRow row1, FeatureListRow row2) {

    if ((row1 == null) || (row2 == null)) {
      return false;
    }

    // If both have preferred identity available, then compare only those
    FeatureIdentity row1PreferredIdentity = row1.getPreferredFeatureIdentity();
    FeatureIdentity row2PreferredIdentity = row2.getPreferredFeatureIdentity();
    if ((row1PreferredIdentity != null) && (row2PreferredIdentity != null)) {
      return row1PreferredIdentity.getName().equals(row2PreferredIdentity.getName());
    }

    // If no identities at all for both rows, then return true
    List<FeatureIdentity> row1Identities = row1.getPeakIdentities();
    List<FeatureIdentity> row2Identities = row2.getPeakIdentities();
    if ((row1Identities.isEmpty()) && (row2Identities.isEmpty())) {
      return true;
    }

    // Otherwise compare all against all and require that each identity has
    // a matching identity on the other row
    if (row1Identities.size() != row2Identities.size()) {
      return false;
    }
    boolean sameID = false;
    for (FeatureIdentity row1Identity : row1Identities) {
      sameID = false;
      for (FeatureIdentity row2Identity : row2Identities) {
        if (row1Identity.getName().equals(row2Identity.getName())) {
          sameID = true;
          break;
        }
      }
      if (!sameID) {
        break;
      }
    }

    return sameID;
  }

  /**
   * Compare charge state of the best MS/MS precursor masses
   *
   * @param row1 FeatureListRow 1
   * @param row2 FeatureListRow 2
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
   */
  public static boolean containsIdentity(FeatureListRow row, FeatureIdentity id) {

    for (FeatureIdentity identity : row.getPeakIdentities()) {
      if (identity.getName().equals(id.getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Finds a combined m/z range that covers all given features
   */
  public static Range<Double> findMZRange(Feature[] features) {

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

    Scan[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

    for (Scan scan : scanNumbers) {
      // Find most intense m/z feature
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

      if (basePeak != null) {
        if (basePeak.getIntensity() > 0) {
          dataPointFound = true;
        }
        newFeature.addDatapoint(scan, basePeak);
      } else {
        final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
        DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
        newFeature.addDatapoint(scan, fakeDataPoint);
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
   * @param row The row.
   * @return The average retention time range of all features contained in this feature list row
   * across all raw data files. Empty range (0,0) if the row is null or has no feature assigned to
   * it.
   */
  public @NotNull
  static Range<Float> getFeatureListRowAvgRtRange(FeatureListRow row) {

    if (row == null || row.getBestFeature() == null) {
      return Range.closed(0.f, 0.f);
    }

    int size = row.getFeatures().size();
    double[] lower = new double[size];
    double[] upper = new double[size];

    Feature[] f = row.getFeatures().toArray(new Feature[0]);

    for (int i = 0; i < size; i++) {
      if (f[i] == null) {
        continue;
      }

      Range<Float> r = f[i].getRawDataPointsRTRange();

      lower[i] = r.lowerEndpoint();
      upper[i] = r.upperEndpoint();
    }

    float avgL = 0, avgU = 0;
    for (int i = 0; i < size; i++) {
      avgL += (float) lower[i];
      avgU += (float) upper[i];
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
  public static ModularFeatureListRow copyFeatureRow(final ModularFeatureListRow row) {
    return copyFeatureRow(row.getFeatureList(), row, true);
  }

  /**
   * Create a copy of a feature list row.
   *
   * @param newFeatureList
   * @param row            the row to copy.
   * @return the newly created copy.
   */
  private static ModularFeatureListRow copyFeatureRow(ModularFeatureList newFeatureList,
      final ModularFeatureListRow row, boolean copyFeatures) {
    // Copy the feature list row.
    final ModularFeatureListRow newRow = new ModularFeatureListRow(newFeatureList, row,
        copyFeatures);

    // TODO this should actually be already copied in the feature list row constructor (all DataTypes are)
//     if (row.getFeatureInformation() != null) {
//      SimpleFeatureInformation information =
//              new SimpleFeatureInformation(new HashMap<>(row.getFeatureInformation().getAllProperties()));
//      newRow.setFeatureInformation(information);
//    }

    return newRow;
  }

  /**
   * Creates a copy of an array of FeatureListRows.
   *
   * @param rows The rows to be copied.
   * @return A copy of rows.
   */
  public static ModularFeatureListRow[] copyFeatureRows(final ModularFeatureListRow[] rows) {
    ModularFeatureListRow[] newRows = new ModularFeatureListRow[rows.length];
    for (int i = 0; i < newRows.length; i++) {
      newRows[i] = copyFeatureRow(rows[i]);
    }
    return newRows;
  }

  public static ModularFeatureListRow[] copyFeatureRows(ModularFeatureList newFeatureList,
      final ModularFeatureListRow[] rows, boolean copyFeatures) {
    ModularFeatureListRow[] newRows = new ModularFeatureListRow[rows.length];
    for (int i = 0; i < newRows.length; i++) {
      newRows[i] = copyFeatureRow(newFeatureList, rows[i], copyFeatures);
    }
    return newRows;
  }

  public static List<ModularFeatureListRow> copyFeatureRows(
      final List<ModularFeatureListRow> rows) {
    return rows.stream().map(row -> copyFeatureRow(row)).collect(Collectors.toList());
  }

  public static List<ModularFeatureListRow> copyFeatureRows(ModularFeatureList newFeatureList,
      final List<ModularFeatureListRow> rows, boolean copyFeatures) {
    return rows.stream().map(row -> copyFeatureRow(newFeatureList, row, copyFeatures))
        .collect(Collectors.toList());
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

  /**
   * Builds simple modular feature from manual feature using mz and rt range.
   *
   * @param featureList
   * @param dataFile
   * @param rtRange
   * @param mzRange
   * @return The result of the integration.
   */
  public static ModularFeature buildSimpleModularFeature(ModularFeatureList featureList,
      RawDataFile dataFile, Range<Float> rtRange, Range<Double> mzRange) {

    // Get MS1 scans in RT range.
    Scan[] scanRange = dataFile.getScanNumbers(1, rtRange);

    // Feature parameters.
    DataPoint targetDP[] = new DataPoint[scanRange.length];
    double targetMZ;
    float targetRT, targetHeight, targetArea;
    targetMZ = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2;
    targetRT = (float) (rtRange.upperEndpoint() + rtRange.lowerEndpoint()) / 2;
    targetHeight = targetArea = 0;
    Scan representativeScan = null;
    List<Scan> allMS2fragmentScanNumbers = ScanUtils.streamAllMS2FragmentScans(dataFile, rtRange,
        mzRange).toList();

    // Get target data points, height, and estimated area over range.
    for (int i = 0; i < scanRange.length; i++) {
      Scan scan = scanRange[i];
      double mz = targetMZ;
      double intensity = 0;

      // Get base peak for target.
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

      // If peak exists, get data point values.
      if (basePeak != null) {
        mz = basePeak.getMZ();
        intensity = basePeak.getIntensity();
      }

      // Add data point to array.
      targetDP[i] = new SimpleDataPoint(mz, intensity);

      // Update feature height and scan.
      if (intensity > targetHeight) {
        targetHeight = (float) intensity;
        representativeScan = scan;
      }

      // Skip area calculation for last datapoint.
      if (i == scanRange.length - 1) {
        break;
      }

      // Get next scan for area calculation.
      Scan nextScan = scanRange[i + 1];
      DataPoint nextBasePeak = ScanUtils.findBasePeak(scan, mzRange);
      double nextIntensity = 0;

      if (nextBasePeak != null) {
        nextIntensity = nextBasePeak.getIntensity();
      }

      // Naive area under the curve calculation.
      double rtDifference = nextScan.getRetentionTime() - scan.getRetentionTime();
      rtDifference *= 60;
      targetArea += (float) (rtDifference * (intensity + nextIntensity) / 2);
    }

    if (targetHeight != 0) {

      // Set intensity range with maximum height in range.
      Range intensityRange = Range.open((float) 0.0, targetHeight);

      // Build new feature for target.
      ModularFeature newPeak = new ModularFeature(featureList, dataFile, targetMZ, targetRT,
          targetHeight, targetArea, scanRange, targetDP, FeatureStatus.DETECTED, representativeScan,
          allMS2fragmentScanNumbers, rtRange, mzRange, intensityRange);

      return newPeak;
    } else {
      return null;
    }
  }

  /**
   * Loops over all {@link DataType}s in a {@link FeatureListRow}. Extracts all annotations derived
   * from a {@link CompoundDBAnnotation} in all {@link AnnotationType}s derived from the
   * {@link ListWithSubsType} within the {@link FeatureListRow}'s {@link
   * io.github.mzmine.datamodel.features.ModularDataModel}.
   *
   * @param selectedRow The row
   * @return List of all annotations.
   */
  @NotNull
  public static List<CompoundDBAnnotation> extractAllCompoundAnnotations(
      FeatureListRow selectedRow) {
    final List<CompoundDBAnnotation> compoundAnnotations = new ArrayList<>();
    final Collection<DataType> dataTypes = selectedRow.getTypes().values();
    for (DataType dataType : dataTypes) {
      if (dataType instanceof ListWithSubsType<?> listType && dataType instanceof AnnotationType) {
        final List<?> list = selectedRow.get(listType);
        if (list != null && !list.isEmpty()) {
          list.stream().filter(c -> c instanceof CompoundDBAnnotation)
              .forEach(c -> compoundAnnotations.add((CompoundDBAnnotation) c));
        }
      }
    }
    return compoundAnnotations;
  }
}
