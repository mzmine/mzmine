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

package io.github.mzmine.modules.io.export_features_csv;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedDataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.io.export_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.beans.property.Property;

public class CSVExportModularTask extends AbstractTask {

  public static final String DATAFILE_PREFIX = "DATAFILE";

  private ModularFeatureList[] featureLists;
  private int processedRows = 0, totalRows = 0;

  // parameter values
  private File fileName;
  private String plNamePattern = "{}";
  private String fieldSeparator;
  private String idSeparator;
  private String headerSeparator = ":";
  private FeatureListRowsFilter filter;

  public CSVExportModularTask(ParameterSet parameters) {
    this.featureLists =
        parameters.getParameter(CSVExportModularParameters.featureLists).getValue()
            .getMatchingFeatureLists();
    fileName = parameters.getParameter(CSVExportModularParameters.filename).getValue();
    fieldSeparator = parameters.getParameter(
        CSVExportModularParameters.fieldSeparator).getValue();
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
      String fieldSeparator,
      String idSeparator, FeatureListRowsFilter filter) {
    super();
    if (fieldSeparator.equals(idSeparator)) {
      throw new IllegalArgumentException(MessageFormat
          .format("Column separator cannot equal the identity separator (currently {0})",
              idSeparator));
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
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to CSV file(s) (new format)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (ModularFeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (ModularFeatureList featureList : featureLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, "csv");

      // Open file

      try (BufferedWriter writer = Files
          .newBufferedWriter(curFile.toPath(), StandardCharsets.UTF_8)) {
        exportFeatureList(featureList, writer, curFile);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        return;
      }

      // Cancel?
      if (isCanceled()) {
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

  private void exportFeatureList(ModularFeatureList flist, BufferedWriter writer, File fileName)
      throws IOException {
    List<RawDataFile> rawDataFiles = flist.getRawDataFiles();

    List<DataType> rowTypes = flist.getRowTypes().values().stream()
        .filter(this::filterType)
        .collect(Collectors.toList());

    List<DataType> featureTypes = flist.getFeatureTypes().values().stream()
        .filter(this::filterType)
        .collect(Collectors.toList());

    // Write feature row headers
    String header = getJoinedHeader(rowTypes, "");
    for (RawDataFile raw : rawDataFiles) {
      header += (header.isEmpty() ? "" : fieldSeparator) + getJoinedHeader(featureTypes,
          DATAFILE_PREFIX + headerSeparator + raw.getName());
    }

    writer.append(header);
    writer.newLine();

    // write data
    for (FeatureListRow row : flist.getRows()) {
      if (!filter.filter(row)) {
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
        || type instanceof LinkedDataType);
  }

  private String joinRowData(ModularFeatureListRow row,
      List<RawDataFile> raws, List<DataType> rowTypes, List<DataType> featureTypes) {
    StringBuilder b = new StringBuilder();
    joinData(b, row, rowTypes);

    // add feature types
    for (RawDataFile raw : raws) {
      ModularFeature feature = row.getFeature(raw);
      joinData(b, feature, featureTypes);
    }
    return b.toString();
  }

  /**
   * @param b
   * @param data  {@link ModularFeatureListRow}, {@link ModularFeature}, {@link
   *              ModularTypeProperty}
   * @param types
   * @return
   */
  private void joinData(StringBuilder b, ModularDataModel data, List<DataType> types) {
    for (DataType type : types) {
      if (type instanceof ModularType) {
        ModularType modType = (ModularType) type;
        ModularTypeProperty modProp = data.get(modType);
        // join all the sub types of a modular data type
        List<DataType> filteredSubTypes = modType.getSubDataTypes().stream()
            .filter(this::filterType).collect(Collectors.toList());
        joinData(b, modProp, filteredSubTypes);
      } else if (type instanceof SubColumnsFactory subCols) {
        Property property = data.get(type);
        Object value = property == null? null : property.getValue();
        int numberOfSub = subCols.getNumberOfSubColumns();
        for (int i = 0; i < numberOfSub; i++) {
          String field = subCols.getFormattedSubColValue(i, null, null, value, null);
          if (!b.isEmpty()) {
            b.append(fieldSeparator);
          }
          b.append(field == null ? "" : field);
        }
      } else {
        Property property = data.get(type);
        if (!b.isEmpty()) {
          b.append(fieldSeparator);
        }
        b.append(escapeStringForCSV(type.getFormattedString(property)));
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
      String header = (prefix == null || prefix.isEmpty() ? "" : prefix + headerSeparator) + t
          .getHeaderString();
      if (t instanceof ModularType) {
        ModularType modType = (ModularType) t;
        // join all the sub headers
        List<DataType> filteredSubTypes = modType.getSubDataTypes().stream()
            .filter(this::filterType).collect(Collectors.toList());
        header = getJoinedHeader(filteredSubTypes, header);
        if (!b.isEmpty()) {
          b.append(fieldSeparator);
        }
        b.append(header);
      } else if (t instanceof SubColumnsFactory subCols) {
        int numberOfSub = subCols.getNumberOfSubColumns();
        for (int i = 0; i < numberOfSub; i++) {
          String field = subCols.getHeader(i);
          if (!b.isEmpty()) {
            b.append(fieldSeparator);
          }
          b.append(field == null ? "" : (header + headerSeparator + field));
        }
      } else {
        if (!b.isEmpty()) {
          b.append(fieldSeparator);
        }
        b.append(header);
      }
    }
    return b.toString();
  }

  private String escapeStringForCSV(final String inputString) {
    if (inputString == null) {
      return "";
    }

    // Remove all special characters (particularly \n would mess up our CSV
    // format).
    String result = inputString.replaceAll("[\\p{Cntrl}]", " ");

    // Skip too long strings (see Excel 2007 specifications)
    if (result.length() >= 32766) {
      result = result.substring(0, 32765);
    }

    // If the text contains fieldSeparator, we will add
    // parenthesis
    if (result.contains(fieldSeparator) || result.contains("\"")) {
      result = "\"" + result.replaceAll("\"", "'") + "\"";
    }

    return result;
  }
}
