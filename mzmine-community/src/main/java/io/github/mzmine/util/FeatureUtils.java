/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureAnnotationPriority;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.PolarityTypeType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import static io.github.mzmine.util.annotations.CompoundAnnotationUtils.getTypeValue;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for feature lists
 */
public class FeatureUtils {

  private static final Logger logger = Logger.getLogger(FeatureUtils.class.getName());

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
    if (feature == null) {
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
  public static double integrateOverMzRtRange(RawDataFile dataFile, List<Scan> scans,
      Range<Float> rtRange, Range<Double> mzRange) {
    final IonTimeSeries<?> series = IonTimeSeriesUtils.extractIonTimeSeries(dataFile, scans,
        mzRange, rtRange, null);

    return FeatureDataUtils.calculateArea(series);
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
   * {@link ListWithSubsType} within the {@link FeatureListRow}'s
   * {@link io.github.mzmine.datamodel.features.ModularDataModel}.
   *
   * @param selectedRow The row
   * @return List of all annotations.
   */
  @NotNull
  public static List<CompoundDBAnnotation> extractAllCompoundAnnotations(
      FeatureListRow selectedRow) {
    final List<CompoundDBAnnotation> compoundAnnotations = new ArrayList<>();
    final Set<DataType> dataTypes = selectedRow.getTypes();
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

  public static boolean isImsFeature(@Nullable Feature f) {

    return f != null && f.getRawDataFile() instanceof IMSRawDataFile
        && f.getFeatureData() instanceof IonMobilogramTimeSeries;
  }

  /**
   * Extracts the best (most confident) {@link FeatureAnnotation} from a feature/row.
   *
   * @param m The row/feature.
   * @return The annotation or null.
   */
  @Nullable
  public static FeatureAnnotation getBestFeatureAnnotation(ModularDataModel m) {
    final List<SpectralDBAnnotation> specDb = m.get(SpectralLibraryMatchesType.class);
    if (specDb != null && !specDb.isEmpty()) {
      return specDb.get(0);
    }

    final List<CompoundDBAnnotation> comp = m.get(CompoundDatabaseMatchesType.class);
    if (comp != null && !comp.isEmpty()) {
      return comp.get(0);
    }

    return null;
  }


  /**
   * Extracts a sub-value for any data type from annotations that implement {@link ListWithSubsType}
   * and {@link AnnotationType}, e.g. {@link CompoundDatabaseMatchesType} and
   * {@link SpectralLibraryMatchesType}. (Basically all annotations should implement this) Can be
   * used to create a consensus formula, annotation or else.
   *
   * @param featureListRow The row.
   * @param theType        The sub data type of which to extract the value from.
   * @param <K>            The class of the annotation that contains the data type theType.
   * @param <V>            The value of the data type in the annotation class K.
   * @return A mapping of annotation list type to the sub data type value.
   */
  public static <K extends ListWithSubsType & AnnotationType, V> Map<K, V> extractSubValueFromAllAnnotations(
      FeatureListRow featureListRow, Class<? extends DataType<V>> theType) {

    return extractSubValueFromAllAnnotations(featureListRow, DataTypes.get(theType));
  }

  /**
   * Extracts a sub-value for any data type from annotations that implement {@link ListWithSubsType}
   * and {@link AnnotationType}, e.g. {@link CompoundDatabaseMatchesType} and
   * {@link SpectralLibraryMatchesType}. (Basically all annotations should implement this) Can be
   * used to create a consensus formula, annotation or else.
   *
   * @param featureListRow The row.
   * @param theType        The sub data type of which to extract the value from.
   * @param <K>            The class of the annotation that contains the data type theType.
   * @param <V>            The value of the data type in the annotation class K.
   * @return A mapping of annotation list type to the sub data type value.
   */
  public static <K extends ListWithSubsType & AnnotationType, V> Map<K, V> extractSubValueFromAllAnnotations(
      FeatureListRow featureListRow, DataType<V> theType) {
    final Map<K, V> result = new HashMap<>();

    // get ALL DataTypes from the feature list
    final Set<DataType> dataTypes = featureListRow.getTypes();

    for (DataType<?> type : dataTypes) {

      // filter for ListWithSubsType which are an annotation
      if (!(type instanceof ListWithSubsType<?> listType)
          || !(listType instanceof AnnotationType)) {
        continue;
      }

      // get the actual value of the ListWithSubsType stored in the feature list, we know its a list
      final List<?> annotationList = featureListRow.get(listType);

      if (annotationList == null || annotationList.isEmpty()) {
        continue;
      }

      // get the list of subTypes
      final List<DataType> subDataTypeList = listType.getSubDataTypes();

      // check if the searched data type exists in the listWithSubsType
      final int subColIndex = subDataTypeList.indexOf(theType);
      if (subColIndex == -1) {
        continue;
      }

      try {
        V value = (V) listType.getSubColValue(subColIndex, annotationList);
        result.put((K) listType, value);
      } catch (ClassCastException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }

    return result;
  }

  /**
   * Get the absolute/unsigned charge of this row. Checks the row charge first, then the instrument
   * detected charge in the most intense fragment scan and then the best feature then the
   * annotation, which at times may be wrong charge.
   */
  @NotNull
  public static OptionalInt extractBestAbsoluteChargeState(@Nullable FeatureListRow row) {
    return extractBestAbsoluteChargeState(row,
        row != null ? row.getMostIntenseFragmentScan() : null);
  }

  /**
   * Get the absolute/unsigned charge of this row. Checks the row charge first, then the supplied
   * scan and then the best feature then the annotation, which at times may be wrong charge.
   */
  @NotNull
  public static OptionalInt extractBestAbsoluteChargeState(@Nullable FeatureListRow row,
      @Nullable Scan scan) {
    var annotation = CompoundAnnotationUtils.getBestFeatureAnnotation(row).orElse(null);
    return extractBestAbsoluteChargeState(row, scan, annotation);
  }

  /**
   * Get the absolute/unsigned charge of this row. Checks the row charge first, then the supplied
   * scan and then the best feature then the annotation, which at times may be wrong charge.
   */
  @NotNull
  public static OptionalInt extractBestAbsoluteChargeState(@Nullable FeatureListRow row,
      @Nullable Scan scan, @Nullable FeatureAnnotation annotation) {
    return extractBestChargeState(row, scan, annotation).stream().map(Math::abs).findFirst();
  }

  /**
   * Get the charge of this row. Checks the row charge first, then the supplied scan and then the
   * best feature then the annotation, which at times may be wrong charge. This charge state may or
   * may not be charged. Therefore, use
   * {@link #extractBestAbsoluteChargeState(FeatureListRow, Scan, FeatureAnnotation)} for abs charge
   * or {@link #extractBestSignedChargeState(FeatureListRow, MassSpectrum, FeatureAnnotation)} for a
   * charge that also uses the polarity to determine sign
   */
  @NotNull
  private static OptionalInt extractBestChargeState(@Nullable FeatureListRow row,
      @Nullable MassSpectrum spec, @Nullable FeatureAnnotation annotation) {
    final Integer rowCharge = row != null ? row.getRowCharge() : null;
    if (rowCharge != null && rowCharge != 0) {
      return OptionalInt.of(rowCharge);
    }
    if (spec instanceof Scan scan && scan.getMsMsInfo() instanceof DDAMsMsInfo dda) {
      final Integer precursorCharge = dda.getPrecursorCharge();
      if (precursorCharge != null && precursorCharge != 0) {
        return OptionalInt.of(precursorCharge);
      }
    }
    final Integer featureCharge = row != null ? row.getBestFeature().getCharge() : null;
    if (featureCharge != null && featureCharge != 0) {
      return OptionalInt.of(featureCharge);
    }
    // via annotation
    return CompoundAnnotationUtils.extractAbsCharge(annotation);
  }

  /**
   * Extracts the best polarity from the row. Checks the supplied scan first, then the row fragment
   * scan and then the best feature fragment scan, last checks the annotation (which may sometimes
   * be wrong polarity, therefore last). Optional.empty if no polarity is found.
   */
  public static Optional<PolarityType> extractBestPolarity(@Nullable FeatureListRow row) {
    return extractBestPolarity(row, null);
  }

  /**
   * Extracts the best polarity from the row. Checks the supplied scan first, then the row fragment
   * scan and then the best feature fragment scan, last checks the annotation (which may sometimes
   * be wrong polarity, therefore last). Optional.empty if no polarity is found.
   */
  public static Optional<PolarityType> extractBestPolarity(@Nullable FeatureListRow row,
      @Nullable Scan scan) {
    var annotation = CompoundAnnotationUtils.getBestFeatureAnnotation(row).orElse(null);
    return extractBestPolarity(row, scan, annotation);
  }

  /**
   * Extracts the best polarity from the row. Checks the supplied scan first, then the row fragment
   * scan and then the best feature fragment scan, last checks the annotation (which may sometimes
   * be wrong polarity, therefore last). Optional.empty if no polarity is found.
   */
  public static Optional<PolarityType> extractBestPolarity(@Nullable FeatureListRow row,
      @Nullable MassSpectrum scan, @Nullable FeatureAnnotation annotation) {
    PolarityType polarity = scan instanceof Scan s ? s.getPolarity() : null;
    if (PolarityType.isDefined(polarity)) {
      return Optional.of(polarity);
    }
    if (row != null) {
      polarity = ScanUtils.getPolarity(row.getMostIntenseFragmentScan());
      if (PolarityType.isDefined(polarity)) {
        return Optional.of(polarity);
      }

      polarity = ScanUtils.getPolarity(row.getBestFeature().getRepresentativeScan());
      if (PolarityType.isDefined(polarity)) {
        return Optional.of(polarity);
      }
    }

    if (annotation != null) {
      polarity = getTypeValue(annotation, PolarityTypeType.class);
      if (PolarityType.isDefined(polarity)) {
        return Optional.of(polarity);
      }
      Integer charge = getTypeValue(annotation, ChargeType.class);
      if (charge != null && charge != 0) {
        return Optional.of(PolarityType.fromInt(charge));
      }
    }

    return Optional.empty();
  }

  /**
   * Takes into account the charge and polarity to provide charge state.
   *
   * @return signed int charge state
   */
  @NotNull
  public static OptionalInt extractBestSignedChargeState(@Nullable FeatureListRow row,
      @Nullable Scan scan) {
    var annotation = CompoundAnnotationUtils.getBestFeatureAnnotation(row).orElse(null);
    return extractBestSignedChargeState(row, scan, annotation);
  }

  /**
   * Takes into account the charge and polarity to provide charge state.
   *
   * @return signed int charge state
   */
  @NotNull
  public static OptionalInt extractBestSignedChargeState(@Nullable FeatureListRow row,
      @Nullable MassSpectrum scan, @Nullable FeatureAnnotation annotation) {
    // just use positive as default here
    final Optional<PolarityType> pol = extractBestPolarity(row, scan, annotation);
    return extractBestChargeState(row, scan, annotation).stream().map(charge -> {
      // apply polarity to abs charge if present - otherwise take signed charge
      if (pol.isPresent()) {
        return Math.abs(charge) * pol.get().getSign();
      } else {
        return charge;
      }
    }).findFirst();
  }

  public static List<IonType> extractAllIonTypes(FeatureListRow row) {

    final List<IonType> allIonTypes = Arrays.stream(FeatureAnnotationPriority.values())
        .flatMap(type -> {
          final Object o = row.get(type.getAnnotationType());
          if (!(o instanceof List<?> annotations)) {
            return Stream.empty();
          }
          return switch (type) {
            case MANUAL, FORMULA, LIPID -> Stream.empty();
            case SPECTRAL_LIBRARY, EXACT_COMPOUND -> {
              List<FeatureAnnotation> featureAnnotations = (List<FeatureAnnotation>) annotations;
              yield featureAnnotations.stream().map(FeatureAnnotation::getAdductType);
            }
          };
        }).toList();

    return allIonTypes;
  }

  /**
   * Extracts precursor mz from match, row mz, or scan.getPrecursorMz depending which first is
   * available
   */
  @NotNull
  public static Optional<Double> getPrecursorMz(@Nullable final FeatureAnnotation match,
      @Nullable final FeatureListRow row, @Nullable final MassSpectrum scan) {
    if (match != null) {
      Double mz = match.getPrecursorMZ();
      if (mz != null) {
        return Optional.of(mz);
      }
    }

    if (row != null) {
      Double mz = row.getAverageMZ();
      if (mz != null) {
        return Optional.of(mz);
      }
    }

    if (scan instanceof Scan s) {
      Double mz = s.getPrecursorMz();
      if (mz != null) {
        return Optional.of(mz);
      }
    }

    return Optional.empty();
  }

  /**
   * Finds the best adduct definition for a match (optional) and a row (optional). If the match is
   * undefined and only row is searched, all {@link FeatureAnnotation} for this row will be
   * searched
   *
   * @param match first priority if set
   * @param row   second priority {@link IonIdentity} otherwise the FeatureAnnotations if no match
   *              is defined as first argument
   * @return
   */
  public static @NotNull Optional<IonType> extractBestIonIdentity(
      final @Nullable FeatureAnnotation match, final @Nullable FeatureListRow row) {
    IonType ion = match != null ? match.getAdductType() : null;
    if (ion != null) {
      return Optional.of(ion);
    }
    if (row != null) {
      var identity = row.getBestIonIdentity();
      if (identity != null) {
        return Optional.ofNullable(identity.getIonType());
      }
      // try to get from match - but only if match was not defined
      if (match == null) {
        return CompoundAnnotationUtils.streamFeatureAnnotations(row)
            .map(FeatureAnnotation::getAdductType).filter(Objects::nonNull).findFirst();
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if this feature has MRM data (and is not null).
   */
  public static boolean isMrm(@Nullable Feature f) {
    return f instanceof ModularFeature mf && mf.isMrm();
  }
}
