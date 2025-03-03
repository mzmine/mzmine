package io.github.mzmine.modules.tools.output_compare;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.maths.Precision;
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


  interface ColumnData<T> {

    static ColumnData create(Column col, List<String> data) {
      if (col.type() instanceof NumberType<?> numberType) {
        return new NumberColumnData(col,
            data.stream().map(s -> StringUtils.parseDoubleOrElse(s, null)).toList());
      }
      return new StringColumnData(col, data);
    }


    boolean checkEqual(final ColumnData other, List<String> messages);

    Column col();

    List<T> data();

    default int numRows() {
      return data().size();
    }

    // implementation
    record StringColumnData(Column col, List<String> data) implements ColumnData<String> {

      @Override
      public boolean checkEqual(final ColumnData obj, List<String> messages) {
        final String colID = "header: %s, type: %s;".formatted(col.header,
            col.type != null ? col.type.getClass().getSimpleName() : "null");
        if (!(obj instanceof StringColumnData other)) {
          if (messages != null) {
            messages.add(
                colID + " Column type does not equal. This was String and other was Number.");
          }
          return false;
        }

        // max rows
        int numRows = Math.max(numRows(), obj.numRows());

        for (int row = 0; row < numRows; row++) {
          // get value is safe for out of index --> null
          final String a = getValue(row);
          final String b = other.getValue(row);
          if (a == null && b == null) {
            continue;
          } else if (a == null) {
            if (messages != null) {
              messages.add(
                  "%s Number value does not equal: first was null, second was %s".formatted(colID,
                      b));
            }
            return false;
          } else if (b == null) {
            if (messages != null) {
              messages.add(
                  "%s Number value does not equal: first was %s, second was null".formatted(colID,
                      b));
            }
            return false;
          }

          if (!Objects.equals(a, b)) {
            if (messages != null) {
              messages.add("%s String value does not equal: %s to %s".formatted(colID, a, b));
            }
            return false;
          }
        }
        return true;
      }
    }


    default T getValue(int row) {
      if (row < 0 || row >= numRows()) {
        return null;
      }
      return data().get(row);
    }

    record NumberColumnData(Column col, List<Double> data) implements ColumnData<Double> {

      @Override
      public boolean checkEqual(final ColumnData obj, List<String> messages) {
        final String colID = "header: %s, type: %s; ".formatted(col.header,
            col.type != null ? col.type.getClass().getSimpleName() : "null");
        if (!(obj instanceof NumberColumnData other)) {
          if (messages != null) {
            messages.add(
                colID + " Column type does not equal. This was Number and other was String.");
          }
          return false;
        }

        // max rows
        int numRows = Math.max(numRows(), obj.numRows());

        for (int row = 0; row < numRows; row++) {
          // get value is safe for out of index --> null
          final Double a = getValue(row);
          final Double b = other.getValue(row);
          if (a == null && b == null) {
            continue;
          } else if (a == null) {
            if (messages != null) {
              messages.add(
                  "%s Number value does not equal: first was null, second was %f".formatted(colID,
                      b));
            }
            return false;
          } else if (b == null) {
            if (messages != null) {
              messages.add(
                  "%s Number value does not equal: first was %f, second was null".formatted(colID,
                      b));
            }
            return false;
          }

          if (!Precision.equalDoubleSignificance(a, b)) {
            if (messages != null) {
              messages.add("%s Number value does not equal: %f to %f".formatted(colID, a, b));
            }
            return false;
          }
        }
        return true;
      }
    }

  }

  record Column(String header, String uniqueTypeId, @Nullable DataType type,
                @Nullable String rawFile) {

    public static Column forFeatureType(final String header, final String uniqueTypeId,
        @Nullable final String rawFile) {
      return new Column(header, uniqueTypeId, DataTypes.getTypeForId(uniqueTypeId), rawFile);
    }

    public static Column forRowType(final String header, final String uniqueTypeId) {
      return new Column(header, uniqueTypeId, DataTypes.getTypeForId(uniqueTypeId), null);
    }
  }
}
