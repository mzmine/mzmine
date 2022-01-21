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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSVExportModularTask extends AbstractTask {

  public static final String DATAFILE_PREFIX = "DATAFILE";
  private static final Logger logger = Logger.getLogger(CSVExportModularTask.class.getName());
  private final ModularFeatureList[] featureLists;
  // parameter values
  private final File fileName;
  private final String fieldSeparator;
  private final String idSeparator;
  private final String headerSeparator = ":";
  private final FeatureListRowsFilter filter;
  private int processedRows = 0, totalRows = 0;

  public CSVExportModularTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(CSVExportModularParameters.featureLists).getValue()
        .getMatchingFeatureLists();
    fileName = parameters.getParameter(CSVExportModularParameters.filename).getValue();
    fieldSeparator = parameters.getParameter(CSVExportModularParameters.fieldSeparator).getValue();
    idSeparator = parameters.getParameter(CSVExportModularParameters.idSeparator).getValue();
    this.filter = parameters.getParameter(CSVExportModularParameters.filter).getValue();
  }

  /**
   * @param featureLists   feature lists to export
   * @param fileName       export file name
   * @param fieldSeparator separation of columns
   * @param idSeparator    identity field separation
   * @param filter         Row filter
   */
  public CSVExportModularTask(ModularFeatureList[] featureLists, File fileName,
      String fieldSeparator, String idSeparator, FeatureListRowsFilter filter,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    if (fieldSeparator.equals(idSeparator)) {
      throw new IllegalArgumentException(MessageFormat.format(
          "Column separator cannot equal the identity separator (currently {0})", idSeparator));
    }
    this.featureLists = featureLists;
    this.fileName = fileName;
    this.fieldSeparator = fieldSeparator;
    this.idSeparator = idSeparator;
    this.filter = filter;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
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
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (ModularFeatureList featureList : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

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
    List<RawDataFile> rawDataFiles = flist.getRawDataFiles();

    List<DataType> rowTypes = flist.getRowTypes().values().stream().filter(this::filterType)
        .collect(Collectors.toList());

    List<DataType> featureTypes = flist.getFeatureTypes().values().stream().filter(this::filterType)
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
    for (FeatureListRow row : flist.getRows()) {
      if (!filter.accept(row)) {
        processedRows++;
        continue;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }
      writer.append(joinRowData((ModularFeatureListRow) row, rawDataFiles, rowTypes, featureTypes));
      writer.newLine();

      processedRows++;
    }
  }

  public boolean filterType(DataType type) {
    return !(type instanceof NoTextColumn || type instanceof NullColumnType
             || type instanceof LinkedGraphicalType);
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
      String header = (prefix == null || prefix.isEmpty() ? "" : prefix + headerSeparator)
                      + t.getHeaderString();
      if (t instanceof SubColumnsFactory subCols) {
        int numberOfSub = subCols.getNumberOfSubColumns();
        for (int i = 0; i < numberOfSub; i++) {
          DataType subType = subCols.getType(i);
          if (subType != null && !filterType(subType)) {
            continue;
          }
          String field = subCols.getHeader(i);
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
}
