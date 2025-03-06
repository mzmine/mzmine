package io.github.mzmine.modules.tools.output_compare;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MZmineModularCsv(Map<Column, ColumnData> data, int numRows) {

  private static final Logger logger = Logger.getLogger(MZmineModularCsv.class.getName());

  public int numColumns() {
    return data.size();
  }

  public List<String> getUniqueRawFiles() {
    return data.keySet().stream().map(Column::rawFile).filter(Objects::nonNull).distinct().toList();
  }

  public List<DataType> getRowTypes() {
    return extractUniqueDataTypes(getRowTypeColumns());
  }

  public List<DataType> getFeatureTypes() {
    return extractUniqueDataTypes(getFeatureTypeColumns());
  }

  public static @NotNull List<DataType> extractUniqueDataTypes(
      final Collection<ColumnData> columns) {
    return columns.stream().map(col -> col.col().type()).filter(Objects::nonNull).distinct()
        .toList();
  }

  public List<ColumnData> getRowTypeColumns() {
    return data.values().stream().filter(cd -> cd.col().rawFile() == null).toList();
  }

  public List<ColumnData> getFeatureTypeColumns() {
    return data.values().stream().filter(cd -> cd.col().rawFile() != null).toList();
  }

  public Collection<Column> columns() {
    return data.keySet();
  }

  public static @Nullable MZmineModularCsv parseFile(final File file) {
    try {
      List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
      String[] headers = rows.getFirst();
      rows = rows.subList(1, rows.size());

      Map<Column, ColumnData> data = HashMap.newHashMap(headers.length);

      for (int i = 0; i < headers.length; i++) {
        // extract data
        final int c = i;
        final List<String> colData = rows.stream().map(cells -> cells[c]).toList();

        // header parsing
        final String header = headers[i];
        final String[] parts = header.split(":");

        // feature data
        final Column col;
        if ("datafile".equals(parts[0])) {
          col = Column.forFeatureType(header, parts[2], parts[1]);
        }
        // rows data
        else {
          col = Column.forRowType(header, parts[0]);
        }
        data.put(col, ColumnData.create(col, colData));
      }

      //
      final int numRows = data.values().stream().mapToInt(ColumnData::numRows).findFirst()
          .orElse(0);
      return new MZmineModularCsv(data, numRows);

    } catch (IOException | CsvException e) {
      logger.log(Level.WARNING, "Issue parsing modular csv file from " + file.getAbsolutePath(), e);
    }
    return null;
  }

  /**
   * Pair all row columns from this and other table.
   *
   * @return the list of {@link ColumnData} pairs
   */
  public List<ColumnData[]> pairRowColumns(final MZmineModularCsv other) {
    return pairColumns(getRowTypeColumns(), other.getRowTypeColumns());
  }

  /**
   * Pair all feature columns from this and other table.
   *
   * @return the list of {@link ColumnData} pairs
   */
  public List<ColumnData[]> pairFeatureColumns(final MZmineModularCsv other) {
    return pairColumns(getFeatureTypeColumns(), other.getFeatureTypeColumns());
  }


  /**
   * Pair all columns from two lists
   *
   * @return the list of {@link ColumnData} pairs
   */
  public static List<ColumnData[]> pairColumns(final List<ColumnData> first,
      final List<ColumnData> second) {
    final Map<Column, ColumnData> firstMap = first.stream()
        .collect(Collectors.toMap(ColumnData::col, cd -> cd));
    final Map<Column, ColumnData> secondMap = second.stream()
        .collect(Collectors.toMap(ColumnData::col, cd -> cd));

    // collect all columns first
    final HashSet<Column> columns = new HashSet<>(firstMap.keySet());
    columns.addAll(secondMap.keySet());

    return columns.stream().map(col -> new ColumnData[]{firstMap.get(col), secondMap.get(col)})
        .toList();
  }


  /**
   * Column can act as a key in a map to map the data
   *
   * @param header       as in file
   * @param uniqueTypeId the first type ID
   * @param type         the first type
   * @param rawFile      a datafile if this is a feature type
   */
  record Column(@NotNull String header, @NotNull String uniqueTypeId, @Nullable DataType type,
                @Nullable String rawFile) {

    public static Column forFeatureType(final String header, final String uniqueTypeId,
        @Nullable final String rawFile) {
      return new Column(header, uniqueTypeId, DataTypes.getTypeForId(uniqueTypeId), rawFile);
    }

    public static Column forRowType(final String header, final String uniqueTypeId) {
      return new Column(header, uniqueTypeId, DataTypes.getTypeForId(uniqueTypeId), null);
    }

    boolean isFeatureType() {
      return rawFile != null;
    }

    boolean isRowType() {
      return rawFile == null;
    }

    @NotNull String getTypeClassName() {
      return type == null ? "null" : type.getClass().getSimpleName();
    }
  }
}
