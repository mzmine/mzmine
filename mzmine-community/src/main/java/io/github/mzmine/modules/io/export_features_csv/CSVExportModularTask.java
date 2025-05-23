/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_csv;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberRangeType;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSVExportModularTask extends AbstractTask implements ProcessedItemsCounter {

  public static final String DATAFILE_PREFIX = "datafile";
  private static final Logger logger = Logger.getLogger(CSVExportModularTask.class.getName());
  private final ModularFeatureList[] featureLists;
  // parameter values
  private final File fileName;
  private final String fieldSeparator;
  private final String idSeparator;
  private final String headerSeparator = ":";
  private final FeatureListRowsFilter rowFilter;
  private final boolean removeEmptyCols;
  private final ParameterSet parameters;
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
    this.parameters = parameters;
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
    parameters = null;
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

    if (!substitute && featureLists.length > 1) {
      setErrorMessage("""
          Cannot export multiple feature lists to the same CSV file. Please use "{}" pattern in filename.\
          This will be replaced with the feature list name to generate one file per feature list.
          """);
      setStatus(TaskStatus.ERROR);
      return;
    }

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

      if (parameters != null) { // if this is null, the external constructor was used.
        featureList.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(CSVExportModularModule.class, parameters,
                getModuleCallDate()));
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
    final List<FeatureListRow> rows = flist.getRows().stream().filter(rowFilter::accept)
        .sorted(FeatureListRowSorter.DEFAULT_ID).toList();
    List<RawDataFile> rawDataFiles = flist.getRawDataFiles();

    List<DataType> rowTypes = flist.getRowTypes().stream().filter(this::filterType)
        .filter(type -> !removeEmptyCols || typeContainData(type, rows, false, -1))
        .collect(Collectors.toList());

    List<DataType> featureTypes = flist.getFeatureTypes().stream().filter(this::filterType)
        .filter(type -> !removeEmptyCols || typeContainData(type, rows, true, -1))
        .collect(Collectors.toList());

    final Map<DataType, List<DataType>> rowsSubTypesIndex = indexSubTypes(rowTypes, rows);

    // Write feature row headers
    StringBuilder header = new StringBuilder(
        getJoinedHeader(rowTypes, "", rows, false, rowsSubTypesIndex));
    for (RawDataFile raw : rawDataFiles) {
      header.append((header.length() == 0) ? "" : fieldSeparator).append(
          getJoinedHeader(featureTypes, DATAFILE_PREFIX + headerSeparator + raw.getName(), rows,
              true, null));
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

      addFormattedColumnsRecursively(formattedCols, rows, null, rowType, rowsSubTypesIndex);

      processedTypes++;
    }

    // add feature types for each raw data file
    for (RawDataFile raw : rawDataFiles) {
      for (DataType featureType : featureTypes) {
        if (isCanceled()) {
          return;
        }

        addFormattedColumnsRecursively(formattedCols, rows, raw, featureType, null);
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
   * Checks all rows and their data types for sub types, that may or may not be listed in the
   * {@link SubColumnsFactory#getType(int)} method.
   *
   * @return A map of main type (in the row) to all it's sub types in the entries.
   */
  private Map<DataType, List<DataType>> indexSubTypes(List<DataType> types,
      List<? extends ModularDataModel> rows) {
    Map<DataType, Set<DataType>> subTypes = new LinkedHashMap<>();
    for (DataType mainType : types) {
      if (!(mainType instanceof SubColumnsFactory subFactory)
          || subFactory instanceof NumberRangeType) {
        continue;
      }
      final int definedSubTypes = subFactory.getNumberOfSubColumns();
      final Set<DataType> typesList = subTypes.computeIfAbsent(mainType,
          _ -> new LinkedHashSet<>());
      for (int i = 0; i < definedSubTypes; i++) {
        // check if we keep the "static" sub types
        DataType subType = subFactory.getType(i);
        if (!filterType(subType) || (removeEmptyCols && !typeContainData(mainType, rows, false,
            i))) {
          continue;
        }
        typesList.add(subFactory.getType(i));
      }

      for (ModularDataModel row : rows) {
        final Object entry = row.get(mainType);
        if (entry == null) {
          continue;
        }

        final Object first;
        if (entry instanceof List list) {
          if (list.isEmpty()) {
            continue;
          }
          // the modular csv export only exports the first entry of the annotations, so we only need to check the first
          first = list.getFirst();
        } else {
          first = entry;
        }

        switch (first) {
          case ModularDataModel modular -> typesList.addAll(modular.getTypes());
          case SimpleCompoundDBAnnotation annotation -> typesList.addAll(annotation.getTypes());
          default -> {
          }
        }
      }
    }

    return subTypes.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> List.copyOf(entry.getValue())));
  }

  /**
   * Adds columns of formatted values for each column / sub column. missing values are replaced by
   * empty strings or default values
   *
   * @param formattedCols the target list
   * @param rows          the data
   * @param raw           defines the feature
   * @param type          the feature data type to be added (and its sub columns)
   */
  private void addFormattedColumnsRecursively(List<String[]> formattedCols,
      @NotNull final List<FeatureListRow> rows, @Nullable RawDataFile raw,
      @NotNull final DataType type, @Nullable final Map<DataType, List<DataType>> subTypesIndex) {

    if (type instanceof SubColumnsFactory subFactory && subTypesIndex != null
        && subTypesIndex.get(type) != null) {
      // explicitly indexed types
      final List<DataType> subTypes = subTypesIndex.get(type);
      for (DataType subType : subTypes) {
        // collect column data
        String[] column = getDataStream(rows, raw, false).map(
            data -> getFormattedValue(data, subFactory, subType)).toArray(String[]::new);
        formattedCols.add(column);
      }
    } else if (type instanceof SubColumnsFactory subFactory) {
      // only "static" sub columns for this type, e.g. for number types as they are a dirty hack
      final int subCols = subFactory.getNumberOfSubColumns();
      for (int s = 0; s < subCols; s++) {
        // filter sub column - maybe excluded, no text, empty
        DataType<?> subType = subFactory.getType(s);
        if (!filterType(subType) || (removeEmptyCols && !typeContainData(type, rows, raw != null,
            s))) {
          continue;
        }
        // collect column data
        final int subIndex = s;
        String[] column = getDataStream(rows, raw, false).map(
            data -> getFormattedValue(data, subFactory, subIndex)).toArray(String[]::new);
        formattedCols.add(column);
      }
    } else {
      String[] column = getDataStream(rows, raw, false).map(data -> getFormattedValue(data, type))
          .toArray(String[]::new);
      formattedCols.add(column);
    }
  }

  /**
   * Data stream for rows or all features
   *
   * @param source      the data source. Assumed to be a row if the raw file is set or allRawFiles
   *                    is true.
   * @param raw         defines to get a feature type column
   * @param allRawFiles or all columns of this type for each raw file
   * @return stream of data column
   */
  private <T extends ModularDataModel> Stream<? extends ModularDataModel> getDataStream(
      List<T> source, RawDataFile raw, boolean allRawFiles) {
    if (allRawFiles) {
      return source.stream().flatMap(row -> ((ModularFeatureListRow) row).getFeatures().stream());
    } else if (raw == null) {
      return source.stream();
    } else {
      return source.stream().map(row -> ((ModularFeatureListRow) row).getFeature(raw));
    }
  }

  /**
   * Get a formatted sub column value by the sub column index. This is required for
   * {@link NumberRangeType}s.
   */
  private String getFormattedValue(@Nullable ModularDataModel data, SubColumnsFactory subColFactory,
      int col) {
    Object value = data == null ? null : data.get((DataType) subColFactory);
    if (value == null) {
      value = ((DataType) subColFactory).getDefaultValue();
    }
    return csvEscape(subColFactory.getFormattedSubColExportValue(col, value));
  }

  /**
   * Get a formatted sub column value by the sub column data type. This can be used if the sub
   * columns have been dynamically indexed to include "hidden" types, that are not included in the
   * {@link SubColumnsFactory#getNumberOfSubColumns()} method.
   */
  private String getFormattedValue(@Nullable ModularDataModel data, SubColumnsFactory subColFactory,
      DataType subCol) {
    Object value = data == null ? null : data.get((DataType) subColFactory);
    if (value == null) {
      value = ((DataType) subColFactory).getDefaultValue();
    }
    final Object subColValue = subColFactory.getSubColValue(subCol, value);
    return csvEscape(subCol.getFormattedString(subColValue, true));
  }

  private String getFormattedValue(@Nullable ModularDataModel data, DataType type) {
    Object value = data == null ? null : data.get(type);
    if (value == null) {
      value = type.getDefaultValue();
    }
    try {
      return csvEscape(type.getFormattedExportString(value));
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
   * @param sub         sub column index
   * @param rows        data source
   * @param featureType defines if row or feature type (true)
   * @return true if any row or feature contains data
   */
  private boolean typeContainData(DataType type, List<? extends ModularDataModel> rows,
      boolean featureType, int sub) {
    final Stream<? extends ModularDataModel> dataStream = getDataStream(rows, null, featureType);
    return dataStream.anyMatch(data -> modelContainData(data, type, sub));
  }

  /**
   * @param sub         sub column index
   * @param rows        data source
   * @param featureType defines if row or feature type (true)
   * @return true if any row or feature contains data
   */
  private boolean typeContainData(DataType type, List<? extends ModularDataModel> rows,
      boolean featureType, DataType sub) {
    final Stream<? extends ModularDataModel> dataStream = getDataStream(rows, null, featureType);
    return dataStream.anyMatch(data -> modelContainData(data, type, sub));
  }

  /**
   * @return true if any row contains data for type
   */
  private boolean modelContainData(ModularDataModel data, DataType type, int sub) {
    final Object mainVal = data.get(type);
    if (sub == -1) {
      return containsData(mainVal);
    }
    if (type instanceof SubColumnsFactory subFactory) {
      return containsData(subFactory.getSubColValue(sub, mainVal));
    }
    throw new IllegalStateException("Reached invalid case when checking for data");
  }

  /**
   * @return true if any row contains data for type
   */
  private boolean modelContainData(ModularDataModel data, @Nullable DataType type,
      @Nullable DataType sub) {
    final Object mainVal = data.get(type);
    if (sub == null) {
      return containsData(mainVal);
    }
    if (type instanceof SubColumnsFactory subFactory) {
      return containsData(subFactory.getSubColValue(sub, mainVal));
    }
    throw new IllegalStateException("Reached invalid case when checking for data");
  }

  private boolean containsData(Object val) {
    return val != null && !(val instanceof String sval && sval.isBlank());
  }


  /**
   * Join headers by field separator and sub data types by headerSeparator (Standard is colon :)
   *
   * @param types
   * @param prefix
   * @return
   */
  private String getJoinedHeader(List<DataType> types, String prefix, List<FeatureListRow> rows,
      boolean isFeatureType, @Nullable final Map<DataType, List<DataType>> subTypesIndex) {
    StringBuilder b = new StringBuilder();

    for (DataType mainType : types) {
      String header = (prefix == null || prefix.isEmpty() ? "" : prefix + headerSeparator)
          + mainType.getUniqueID();
      if (mainType instanceof SubColumnsFactory subCols) {
        if (subTypesIndex != null && subTypesIndex.get(mainType) != null) {
          // when processing row types which may have "unknown" sub columns
          final List<DataType> subTypes = subTypesIndex.get(mainType);
          for (DataType subType : subTypes) {
            // types in here are already checked for containing data in the indexSubTypes method
            String field = subType.getUniqueID();
            if (b.length() != 0) {
              b.append(fieldSeparator);
            }
            b.append(csvEscape(header + headerSeparator + field));
          }

        } else { // when processing feature types
          int numberOfSub = subCols.getNumberOfSubColumns();
          for (int i = 0; i < numberOfSub; i++) {
            DataType subType = subCols.getType(i);
            if (!filterType(subType) || (removeEmptyCols && !typeContainData(mainType, rows,
                isFeatureType, i))) {
              continue;
            }
            String field = subCols.getUniqueID(i);
            if (b.length() != 0) {
              b.append(fieldSeparator);
            }
            b.append(csvEscape(header + headerSeparator + field));
          }
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
