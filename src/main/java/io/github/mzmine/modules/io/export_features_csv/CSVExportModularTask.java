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

package io.github.mzmine.modules.io.export_features_csv;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSVExportModularTask extends AbstractTask implements ProcessedItemsCounter {

  public static final String DATAFILE_PREFIX = "DATAFILE";
  private static final Logger logger = Logger.getLogger(CSVExportModularTask.class.getName());
  private final ModularFeatureList[] featureLists;
  // parameter values
  private final File fileName;
  private final String fieldSeparator;
  private final String idSeparator;
  private final String headerSeparator = ":";
  private final FeatureListRowsFilter rowFilter;
  private final boolean removeEmptyCols;
  // track number of exported items
  private final AtomicInteger exportedRows = new AtomicInteger(0);
  private int processedTypes = 0, totalTypes = 0;

  public CSVExportModularTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(CSVExportModularParameters.featureLists).getValue()
        .getMatchingFeatureLists();
    fileName = parameters.getParameter(CSVExportModularParameters.filename).getValue();
    fieldSeparator = parameters.getParameter(CSVExportModularParameters.fieldSeparator).getValue();
    idSeparator = parameters.getParameter(CSVExportModularParameters.idSeparator).getValue();
    this.rowFilter = parameters.getParameter(CSVExportModularParameters.filter).getValue();
    removeEmptyCols = parameters.getValue(CSVExportModularParameters.omitEmptyColumns);
  }

  /**
   * @param featureLists   feature lists to export
   * @param fileName       export file name
   * @param fieldSeparator separation of columns
   * @param idSeparator    identity field separation
   * @param rowFilter      Row filter
   */
  public CSVExportModularTask(ModularFeatureList[] featureLists, File fileName,
      String fieldSeparator, String idSeparator, FeatureListRowsFilter rowFilter,
      boolean removeEmptyCols, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    if (fieldSeparator.equals(idSeparator)) {
      throw new IllegalArgumentException(MessageFormat.format(
          "Column separator cannot equal the identity separator (currently {0})", idSeparator));
    }
    this.featureLists = featureLists;
    this.fileName = fileName;
    this.fieldSeparator = fieldSeparator;
    this.idSeparator = idSeparator;
    this.rowFilter = rowFilter;
    this.removeEmptyCols = removeEmptyCols;
  }

  @Override
  public int getProcessedItems() {
    return exportedRows.get();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalTypes == 0) {
      return 0;
    }
    return (double) processedTypes / (double) totalTypes;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists)
        + " to CSV file(s) (new format)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    String plNamePattern = "{}";
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (ModularFeatureList featureList : featureLists) {
      totalTypes += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (ModularFeatureList featureList : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }
      // check concurrent modification during export
      final int numRows = featureList.getNumberOfRows();
      final long numFeatures = featureList.streamFeatures().count();
      final long numMS2 = featureList.stream().filter(row -> row.hasMs2Fragmentation()).count();

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, "csv");

      // Open file

      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {
        exportFeatureList(featureList, writer);

      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.WARNING, String.format(
            "Error writing new CSV format to file: %s for feature list: %s. Message: %s",
            curFile.getAbsolutePath(), featureList.getName(), e.getMessage()), e);
        return;
      }

      checkConcurrentModification(featureList, numRows, numFeatures, numMS2);
      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  @SuppressWarnings("rawtypes")
  private void exportFeatureList(ModularFeatureList flist, BufferedWriter writer)
      throws IOException {
    final List<FeatureListRow> rows = flist.getRows().stream().filter(rowFilter::accept).toList();
    List<RawDataFile> rawDataFiles = flist.getRawDataFiles();

    List<DataType> rowTypes = flist.getRowTypes().values().stream().filter(this::filterType)
        .filter(type -> !removeEmptyCols || rowsContainData(type, rows, -1))
        .collect(Collectors.toList());

    List<DataType> featureTypes = flist.getFeatureTypes().values().stream().filter(this::filterType)
        .filter(type -> !removeEmptyCols || featuresContainData(type, rows, -1))
        .collect(Collectors.toList());

    // Write feature row headers
    StringBuilder header = new StringBuilder(getJoinedHeader(rowTypes, ""));
    for (RawDataFile raw : rawDataFiles) {
      header.append((header.length() == 0) ? "" : fieldSeparator)
          .append(getJoinedHeader(featureTypes, DATAFILE_PREFIX + headerSeparator + raw.getName()));
    }

    writer.append(header.toString());
    writer.newLine();

    // write data
    totalTypes = rowTypes.size() + featureTypes.size() * rawDataFiles.size();

    List<String[]> formattedCols = new ArrayList<>(totalTypes);

    // list string values for each type and sub type
    for (DataType rowType : rowTypes) {
      if (isCanceled()) {
        return;
      }

      addFormattedRowColumnsRecursively(formattedCols, rows, rowType);

      processedTypes++;
    }

    // add feature types for each raw data file
    for (RawDataFile raw : rawDataFiles) {
      for (DataType featureType : featureTypes) {
        if (isCanceled()) {
          return;
        }

        addFormattedFeatureColumnsRecursively(formattedCols, rows, raw, featureType);
        processedTypes++;
      }
    }

    for (int i = 0; i < rows.size(); i++) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      final int row = i;
      writer.append(
          formattedCols.stream().map(col -> col[row]).collect(Collectors.joining(fieldSeparator)));
      writer.newLine();

      exportedRows.incrementAndGet();
      processedTypes++;
    }
  }

  /**
   * Adds columns of formatted values for each column / sub column. missing values are replaced by
   * empty strings or default values
   *
   * @param formattedCols the target list
   * @param rows          the data
   * @param rowType       the feautre data type to be added (and its sub columns)
   */
  private void addFormattedRowColumnsRecursively(List<String[]> formattedCols,
      List<FeatureListRow> rows, DataType rowType) {
    if (rowType instanceof SubColumnsFactory subFactory) {
      int subCols = subFactory.getNumberOfSubColumns();
      for (int s = 0; s < subCols; s++) {
        // filter sub column - maybe excluded, no text, empty
        DataType<?> subType = subFactory.getType(s);
        if (!filterType(subType) || (removeEmptyCols && !rowsContainData(rowType, rows, s))) {
          continue;
        }
        // collect column data
        final int subIndex = s;
        String[] column = rows.stream().map(row -> getFormattedValue(row, subFactory, subIndex))
            .toArray(String[]::new);
        formattedCols.add(column);
      }
    } else {
      String[] column = rows.stream().map(row -> getFormattedValue(row, rowType))
          .toArray(String[]::new);
      formattedCols.add(column);
    }
  }

  /**
   * Adds columns of formatted values for each column / sub column. missing values are replaced by
   * empty strings or default values
   *
   * @param formattedCols the target list
   * @param rows          the data
   * @param raw           defines the feature
   * @param featureType   the feautre data type to be added (and its sub columns)
   */
  private void addFormattedFeatureColumnsRecursively(List<String[]> formattedCols,
      List<FeatureListRow> rows, RawDataFile raw, DataType featureType) {
    if (featureType instanceof SubColumnsFactory subFactory) {
      int subCols = subFactory.getNumberOfSubColumns();
      for (int s = 0; s < subCols; s++) {
        // filter sub column - maybe excluded, no text, empty
        DataType<?> subType = subFactory.getType(s);
        if (!filterType(subType) || (removeEmptyCols && !featuresContainData(featureType, rows,
            s))) {
          continue;
        }
        // collect column data
        final int subIndex = s;
        String[] column = rows.stream().map(row -> (ModularFeature) row.getFeature(raw))
            .map(feature -> getFormattedValue(feature, subFactory, subIndex))
            .toArray(String[]::new);
        formattedCols.add(column);
      }
    } else {
      String[] column = rows.stream().map(row -> (ModularFeature) row.getFeature(raw))
          .map(feature -> getFormattedValue(feature, featureType)).toArray(String[]::new);
      formattedCols.add(column);
    }
  }


  private <T extends DataType & SubColumnsFactory> String getFormattedValue(
      @Nullable ModularDataModel data, SubColumnsFactory subColFactory, int col) {
    if (data == null) {
      return "";
    }
    Object value = data == null ? null : data.get((DataType) subColFactory);
    if (value == null) {
      value = ((DataType) subColFactory).getDefaultValue();
    }
    return csvEscape(subColFactory.getFormattedSubColValue(col, value));
  }

  private String getFormattedValue(@Nullable ModularDataModel data, DataType type) {
    if (data == null) {
      return "";
    }
    Object value = data == null ? null : data.get(type);
    if (value == null) {
      value = type.getDefaultValue();
    }
    try {
      return csvEscape(type.getFormattedString(value));
    } catch (Exception e) {
      logger.log(Level.FINEST,
          "Cannot format value of type " + type.getClass().getName() + " value: " + value, e);
      return "";
    }
  }

  /**
   * @return true if type should be exported
   */
  public boolean filterType(DataType type) {
    return !(type instanceof NoTextColumn || type instanceof NullColumnType
        || type instanceof LinkedGraphicalType);
  }

  /**
   * @return true if any row contains data for type
   */
  private boolean rowsContainData(DataType type, List<FeatureListRow> rows, int sub) {
    for (FeatureListRow row : rows) {
      final Object mainVal = row.get(type);
      if (mainVal != null) {
        if (sub == -1 || type instanceof SubColumnsFactory subFactory
            && subFactory.getSubColValue(sub, mainVal) != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return true if any feature contains data
   */
  private boolean featuresContainData(DataType type, List<FeatureListRow> rows, int sub) {
    for (FeatureListRow row : rows) {
      for (ModularFeature feature : row.getFeatures()) {
        final Object mainVal = feature.get(type);
        if (mainVal != null) {
          if (sub == -1 || type instanceof SubColumnsFactory subFactory
              && subFactory.getSubColValue(sub, mainVal) != null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private String joinRowData(ModularFeatureListRow row, List<RawDataFile> raws,
      List<DataType> rowTypes, List<DataType> featureTypes) {
    StringBuilder b = new StringBuilder();
    joinData(b, row, rowTypes);

    // add feature types
    for (RawDataFile raw : raws) {
      ModularFeature feature = row.getFeature(raw);
      if (feature != null) {
        joinData(b, feature, featureTypes);
      } else {
        // no feature for this sample - add empty cells
        joinEmptyCells(b, featureTypes);
      }
    }
    return b.toString();
  }

  /**
   * Fills in empty cells for all data types and their sub types
   *
   * @param b         the string builder
   * @param dataTypes the list of types (with sub types) that are empty
   */
  private void joinEmptyCells(StringBuilder b, List<DataType> dataTypes) {
    for (DataType t : dataTypes) {
      if (t instanceof SubColumnsFactory subCols) {
        int numberOfSub = subCols.getNumberOfSubColumns();
        for (int i = 0; i < numberOfSub; i++) {
          DataType sub = subCols.getType(i);
          if (sub != null && !filterType(sub)) {
            continue;
          }
          b.append(fieldSeparator);
        }
      } else {
        b.append(fieldSeparator);
      }
    }
  }

  /**
   * @param b
   * @param data  {@link ModularFeatureListRow}, {@link ModularFeature} might be null if not set
   * @param types
   * @return
   */
  private void joinData(StringBuilder b, @Nullable ModularDataModel data, List<DataType> types) {
    for (DataType type : types) {
      if (type instanceof SubColumnsFactory subCols) {
        Object value = data == null ? null : data.get(type);
        if (value == null) {
          value = type.getDefaultValue();
        }
        int numberOfSub = subCols.getNumberOfSubColumns();
        for (int i = 0; i < numberOfSub; i++) {
          DataType<?> subType = subCols.getType(i);
          if (subType != null && !filterType(subType)) {
            continue;
          }
          String field = subCols.getFormattedSubColValue(i, value);
          if (b.length() != 0) {
            b.append(fieldSeparator);
          }
          b.append(csvEscape(field));
        }
      } else {
        Object value = data == null ? null : data.get(type);
        if (value == null) {
          value = type.getDefaultValue();
        }
        if (b.length() != 0) {
          b.append(fieldSeparator);
        }
        String str;
        try {
          str = type.getFormattedString(value);
        } catch (Exception e) {
          logger.log(Level.FINEST,
              "Cannot format value of type " + type.getClass().getName() + " value: " + value, e);
          str = "";
        }
        b.append(csvEscape(str));
      }
    }
  }


  /**
   * Join headers by field separator and sub data types by headerSeparator (Standard is colon :)
   *
   * @param types
   * @param prefix
   * @return
   */
  private String getJoinedHeader(List<DataType> types, String prefix) {
    StringBuilder b = new StringBuilder();
    for (DataType t : types) {
      String header =
          (prefix == null || prefix.isEmpty() ? "" : prefix + headerSeparator) + t.getUniqueID();
      if (t instanceof SubColumnsFactory subCols) {
        int numberOfSub = subCols.getNumberOfSubColumns();
        for (int i = 0; i < numberOfSub; i++) {
          DataType subType = subCols.getType(i);
          if (subType != null && !filterType(subType)) {
            continue;
          }
          String field = subCols.getUniqueID(i);
          if (b.length() != 0) {
            b.append(fieldSeparator);
          }
          b.append(csvEscape(header + headerSeparator + field));
        }
      } else {
        if (b.length() != 0) {
          b.append(fieldSeparator);
        }
        b.append(csvEscape(header));
      }
    }
    return b.toString();
  }

  private String csvEscape(String input) {
    return CSVUtils.escape(input, fieldSeparator);
  }


  private void checkConcurrentModification(FeatureList featureList, int numRows, long numFeatures,
      long numMS2) {
    final int numRowsEnd = featureList.getNumberOfRows();
    final long numFeaturesEnd = featureList.streamFeatures().count();
    final long numMS2End = featureList.stream().filter(row -> row.hasMs2Fragmentation()).count();

    if (numRows != numRowsEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) CSV export old=%d new=%d",
          featureList.getName(), numRows, numRowsEnd));
    }
    if (numFeatures != numFeaturesEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) CSV export old=%d new=%d",
          featureList.getName(), numFeatures, numFeaturesEnd));
    }
    if (numMS2 != numMS2End) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS WITH MS2 during featurelist (%s) CSV export old=%d new=%d",
          featureList.getName(), numMS2, numMS2End));
    }
  }
}
