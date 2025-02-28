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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class MZmineModularCsv {

  private static final Logger logger = Logger.getLogger(MZmineModularCsv.class.getName());

  private final Map<Column, ColumnData> data;

  private MZmineModularCsv(final Map<Column, ColumnData> data) {
    this.data = data;
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
          col = Column.forRowType(header, parts[2]);
        }
        data.put(col, ColumnData.create(col, colData));
      }

      //
      return new MZmineModularCsv(data);

    } catch (IOException | CsvException e) {
      logger.log(Level.WARNING, "Issue parsing modular csv file from " + file.getAbsolutePath(), e);
    }
    return null;
  }

  interface ColumnData {

    static ColumnData create(Column col, List<String> data) {
      if (col.type() instanceof NumberType<?> numberType) {
        return new NumberColumnData(col,
            data.stream().map(s -> StringUtils.parseDoubleOrElse(s, null)).toList());
      }
      return new StringColumnData(col, data);
    }

    record StringColumnData(Column col, List<String> data) implements ColumnData {

    }

    record NumberColumnData(Column col, List<Double> data) implements ColumnData {

      @Override
      public boolean checkEqual(final ColumnData obj, List<String> messages) {
        if (!(obj instanceof NumberColumnData other)) {
          return false;
        }

        for (int i = 0; i < data.size(); i++) {
          final Double a = data.get(i);
          final Double b = other.data.get(i);
          if (a == null && b == null) {
            continue;
          } else if (a == null) {
            if (messages != null) {
              messages.add(
                  "Number value does not equal: first was null, second was %f".formatted(b));
            }
            return false;
          } else if (b == null) {
            if (messages != null) {
              messages.add(
                  "Number value does not equal: first was %f, second was null".formatted(b));
            }
            return false;
          }

          if (!Precision.equalDoubleSignificance(a, b)) {
            if (messages != null) {
              messages.add("Number value does not equal: %f to %f".formatted(a, b));
            }
            return false;
          }
        }
        return true;
      }
    }

    boolean checkEqual(final ColumnData other, List<String> messages);
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
