/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables;


import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataColumn;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataTable;
import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectraAcquisitionStepsTable extends TDFDataTable<Long> {

  /**
   * This is not actually used in the query, we just have it for consistency purposes.
   */
  private static final String spectraAcquisitionTable = "SpectraAcquisitionStepsTable";
  private final TDFDataColumn<Long> idCol;

  // spectra table
  private final TDFDataColumn<Double> rtCol = new TDFDataColumn<>(BafSpectraTable.RT_COL);
  private final TDFDataColumn<Long> segment = new TDFDataColumn<>(BafSpectraTable.SEGMENT_COL);
  private final TDFDataColumn<Long> acquisitionKey = new TDFDataColumn<>(
      BafSpectraTable.AQUISITION_KEY_COL);
  private final TDFDataColumn<Long> mzAcqRangeLowerCol = new TDFDataColumn<>(
      BafSpectraTable.MZ_ACQ_RANGE_LOWER_COL);
  private final TDFDataColumn<Long> mzAcqRangeUpper = new TDFDataColumn<>(
      BafSpectraTable.MZ_ACQ_RANGE_UPPER_COL);
  private final TDFDataColumn<Double> sumIntensityCol = new TDFDataColumn<>(
      BafSpectraTable.SUM_INTENSITY_COL);
  private final TDFDataColumn<Double> maxIntensityCol = new TDFDataColumn<>(
      BafSpectraTable.MAX_INTENSITY_COL);
  private final TDFDataColumn<Long> profileMzIdCol = new TDFDataColumn<>(
      BafSpectraTable.PROFILE_MZ_ID_COL);
  private final TDFDataColumn<Long> profileIntensityCol = new TDFDataColumn<>(
      BafSpectraTable.PROFILE_INTENSITY_ID_COL);
  private final TDFDataColumn<Long> lineMzIdCol = new TDFDataColumn<>(
      BafSpectraTable.LINE_MZ_ID_COL);
  private final TDFDataColumn<Long> lineIntensityCol = new TDFDataColumn<>(
      BafSpectraTable.LINE_INTENSITY_ID_COL);
  private final TDFDataColumn<Long> lineAreaIdCol = new TDFDataColumn<>(
      BafSpectraTable.LINE_AREA_ID_COL);

  //acq mode table
  private final TDFDataColumn<Long> polarityCol = new TDFDataColumn<>(
      BafAcqusitionKeysTable.POLAIRTY_COL);
  private final TDFDataColumn<Long> scanModeCol = new TDFDataColumn<>(
      BafAcqusitionKeysTable.SCAN_MODE_COL);
  private final TDFDataColumn<Long> acquisitionModeCol = new TDFDataColumn<>(
      BafAcqusitionKeysTable.ACQUISITION_MODE_COL);
  private final TDFDataColumn<Long> msLevelCol = new TDFDataColumn<>(
      BafAcqusitionKeysTable.MsLevelCol);

  // in isCid/bbCID data, there is no steps table. So we cannot get the ce from there, but we can
  // extract it from the variables table.
  private final TDFDataColumn<Double> fallbackCeColumn = new TDFDataColumn<>("fallbackCE");

  private final boolean importProfile;
  private MassSpectrumType preferredSpectrumType = MassSpectrumType.CENTROIDED;


  public SpectraAcquisitionStepsTable(boolean importProfileIfPossible) {
    super(spectraAcquisitionTable, BafSpectraTable.ID_COL);
    this.importProfile = importProfileIfPossible;

    idCol = (TDFDataColumn<Long>) getColumn(BafSpectraTable.ID_COL);
    columns.addAll(
        Arrays.asList(rtCol, segment, acquisitionKey, mzAcqRangeLowerCol, mzAcqRangeUpper,
            sumIntensityCol, maxIntensityCol, profileMzIdCol, profileIntensityCol, lineMzIdCol,
            lineIntensityCol, lineAreaIdCol, polarityCol, scanModeCol, acquisitionModeCol,
            msLevelCol, fallbackCeColumn));
  }

  @Override
  protected String getColumnHeadersForQuery() {
    final String spectraTable = BafSpectraTable.NAME;
    final String acqTable = BafAcqusitionKeysTable.NAME;
    final String variableTable = BafVariables.NAME;

    final String spectraString = new BafSpectraTable().columns().stream()
        .map(col -> "%s.%s".formatted(spectraTable, col.getCoulumnName()))
        .collect(Collectors.joining(", "));
    final String acqString = new BafAcqusitionKeysTable().columns().stream()
        .filter(col -> !col.getCoulumnName().equals(BafAcqusitionKeysTable.ID_COL))
        .map(col -> "%s.%s".formatted(acqTable, col.getCoulumnName()))
        .collect(Collectors.joining(", "));
    final String variableTableString =
        variableTable + "." + BafVariables.VALUE_COL + " AS " + fallbackCeColumn.getCoulumnName();

    return String.join(", ", spectraString, acqString, variableTableString);
  }

  @Override
  protected String getQueryText(String columnHeadersForQuery) {
    final String spectraTable = BafSpectraTable.NAME;
    final String acqTable = BafAcqusitionKeysTable.NAME;

    return "SELECT " + columnHeadersForQuery + " FROM " + spectraTable + //
        " LEFT JOIN " + acqTable + " ON " + spectraTable + "." + BafSpectraTable.AQUISITION_KEY_COL
        + "=" + acqTable + "." + BafAcqusitionKeysTable.ID_COL + //
        // documentation says 5 is a constant value. could also get it from the SupportedVariables table otherwise.
        " LEFT JOIN " + BafVariables.NAME + " ON " + spectraTable + "." + BafSpectraTable.ID_COL
        + "=" + BafVariables.NAME + "." + BafVariables.SPECTRUM_COL + " AND " + BafVariables.NAME
        + "." + BafVariables.VARIABLE_COL + "=5" + //
        " ORDER BY " + spectraTable + "." + BafSpectraTable.ID_COL;
  }

  @Override
  public boolean executeQuery(Connection connection) {
    final boolean b = super.executeQuery(connection);

    if (b) {
      if (containsProfileData() && importProfile) {
        preferredSpectrumType = MassSpectrumType.PROFILE;
      } else if (containsCentroidData()) {
        preferredSpectrumType = MassSpectrumType.CENTROIDED;
      } else if (containsProfileData()) {
        preferredSpectrumType = MassSpectrumType.PROFILE;
      } else {
        throw new RuntimeException(
            "No valid spectrum type found. (File contains neither centroid nor profile data.)");
      }
    }
    return b;
  }

  public boolean containsCentroidData() {
    return lineMzIdCol.stream().anyMatch(id -> id != null && id != 0) && lineIntensityCol.stream()
        .anyMatch(id -> id != null && id != 0);
  }

  public boolean containsProfileData() {
    return profileMzIdCol.stream().anyMatch(id -> id != null && id != 0)
        && profileIntensityCol.stream().anyMatch(id -> id != null && id != 0);
  }

  public int getId(int index) {
    return idCol.get(index).intValue();
  }

  public float getRt(int index) {
    return rtCol.get(index).floatValue() / 60f;
  }

  public double getAcqMzRangeLower(int index) {
    return mzAcqRangeLowerCol.get(index);
  }

  public double getAcqMzRangeUpper(int index) {
    return mzAcqRangeUpper.get(index);
  }

  public long getMzIds(int index, @NotNull final MassSpectrumType spectrumType) {
    return switch (spectrumType) {
      case CENTROIDED -> lineMzIdCol.get(index);
      case PROFILE -> profileMzIdCol.get(index);
      default -> throw new RuntimeException("No mz data found.");
    };
  }

  public long getIntensityIds(int index, @NotNull final MassSpectrumType spectrumType) {
    return switch (spectrumType) {
      case CENTROIDED -> lineIntensityCol.get(index);
      case PROFILE -> profileIntensityCol.get(index);
      default -> throw new RuntimeException("No intensity data found.");
    };
  }

  public PolarityType getPolarity(int index) {
    return switch (polarityCol.get(index).intValue()) {
      case 0 -> PolarityType.POSITIVE;
      case 1 -> PolarityType.NEGATIVE;
      default -> throw new RuntimeException(
          "Illegal polarity value. %d".formatted(polarityCol.get(index)));
    };
  }

  /**
   * @return 0 = ms1, 2 = msms, 4 = in source cid, 5 = broadband cid
   */
  public int getScanMode(int index) {
    return scanModeCol.get(index).intValue();
  }

  /**
   * @return 0 = ms1, 2 = msms, 4 = in source cid, 5 = broadband cid
   */
  public int getMsLevel(int index) {
    return switch (msLevelCol.get(index).intValue()) {
      case 0 -> {
        yield switch (getScanMode(index)) {
          case 0 -> 1;
          case 2 -> 2;
          case 4 -> 2;
          case 5 -> 2;
          default -> throw new RuntimeException("Unknown scan mode.");
        };
      }
      case 1 -> 2;
      default -> throw new IllegalStateException("Unknown ms level.");
    };
  }

  public int getNumberOfScans() {
    return idCol.size();
  }

  public MassSpectrumType getPreferredSpectrumType() {
    return preferredSpectrumType;
  }

  private boolean scanHasData(int index) {
    final Long lineMzId = lineMzIdCol.get(index);
    final Long profileMzId = profileMzIdCol.get(index);

    return (lineMzId != null && lineMzId != 0) || (profileMzId != null && profileMzId != 0);
  }

  /**
   * @return The spectrum type to read for the requested scan index. Null if no data is available
   * for the scan (=no data saved). The return may differ from {@link #getPreferredSpectrumType()}
   * in case the preferred data type is not available.
   */
  @Nullable
  public MassSpectrumType getSpectrumType(int index) {
    if (!scanHasData(index)) {
      return null;
    }

    return switch (getPreferredSpectrumType()) {
      case CENTROIDED ->
          lineMzIdCol.get(index) != null ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE;
      case PROFILE -> profileMzIdCol.get(index) != null ? MassSpectrumType.PROFILE
          : MassSpectrumType.CENTROIDED;
      default -> throw new RuntimeException("Invalid spectrum type: " + getPreferredSpectrumType());
    };
  }

  public Double getFallbackCe(int index) {
    return fallbackCeColumn.get(index);
  }
}
