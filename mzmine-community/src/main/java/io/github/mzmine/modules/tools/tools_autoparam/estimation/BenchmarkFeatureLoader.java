/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureWithIsotopeTraces;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loads benchmark targets from computed file statistics or an optional user-supplied CSV file.
 */
public final class BenchmarkFeatureLoader {

  private BenchmarkFeatureLoader() {
  }

  public static @Nullable List<FeatureRecord> fromStatistics(
      @Nullable List<@NotNull DataFileStatistics> statistics) {
    if (statistics == null) {
      return null;
    }

    return statistics.stream().map(DataFileStatistics::featureStatistics).flatMap(List::stream)
        .map(FeatureStatistics::getBestEnvelope).map(FeatureWithIsotopeTraces::isotopeTraces)
        .flatMap(List::stream).map(
            feature -> new FeatureRecord(feature.getRawDataFile(), feature.getMZ(), feature.getRT(),
                feature.getMobility())).toList();
  }

  public static @NotNull List<FeatureRecord> fromFile(
      @Nullable List<@NotNull DataFileStatistics> statistics, @NotNull File benchmarkFile,
      @NotNull List<ImportType<?>> types) {
    final Character separator = CSVParsingUtils.autoDetermineSeparator(benchmarkFile);
    final SimpleStringProperty errorMessage = new SimpleStringProperty();

    try {
      final List<String[]> csvData = CSVParsingUtils.readData(benchmarkFile, separator.toString());
      final List<ImportType<?>> lineIds = CSVParsingUtils.findLineIds(types, csvData.getFirst(),
          errorMessage);
      final ColumnIndices columns = findColumns(lineIds);
      final List<FeatureRecord> records = new ArrayList<>();
      for (int row = 1; row < csvData.size(); row++) {
        addRecords(records, statistics, csvData.get(row), columns);
      }
      return records;
    } catch (IOException | CsvException e) {
      throw new RuntimeException("Cannot read benchmark features from " + benchmarkFile, e);
    }
  }

  private static @NotNull ColumnIndices findColumns(@NotNull List<ImportType<?>> lineIds) {
    int mzIndex = -1;
    int rtIndex = -1;
    Integer mobilityIndex = null;
    for (final ImportType<?> lineId : lineIds) {
      switch (lineId.getDataType()) {
        case MZType _ -> mzIndex = lineId.getColumnIndex();
        case RTType _ -> rtIndex = lineId.getColumnIndex();
        case MobilityType _ -> mobilityIndex = lineId.getColumnIndex();
        default -> {
        }
      }
    }
    if (mzIndex < 0 || rtIndex < 0) {
      throw new IllegalArgumentException("MZ and RT columns were not found");
    }
    return new ColumnIndices(mzIndex, rtIndex, mobilityIndex);
  }

  private static void addRecords(@NotNull List<FeatureRecord> records,
      @Nullable List<@NotNull DataFileStatistics> statistics, String @NotNull [] row,
      @NotNull ColumnIndices columns) {
    final double mz = Double.parseDouble(row[columns.mzIndex()]);
    final float rt = Float.parseFloat(row[columns.rtIndex()]);
    final Float mobility =
        columns.mobilityIndex() == null ? null : Float.parseFloat(row[columns.mobilityIndex()]);
    if (statistics == null) {
      records.add(new FeatureRecord(null, mz, rt, mobility));
      return;
    }
    statistics.stream().map(DataFileStatistics::file)
        .map(file -> new FeatureRecord(file, mz, rt, mobility)).forEach(records::add);
  }

  private record ColumnIndices(int mzIndex, int rtIndex, @Nullable Integer mobilityIndex) {

  }
}
