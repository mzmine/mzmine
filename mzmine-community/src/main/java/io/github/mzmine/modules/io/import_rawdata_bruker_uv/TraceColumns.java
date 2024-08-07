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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum TraceColumns {

  ID, DESCRIPTION, INSTRUMENT, INSTRUMENT_ID, TYPE, UNIT, TIME_OFFSET, COLOR;

  public String header() {
    return switch (this) {
      case ID -> "Id";
      case DESCRIPTION -> "Description";
      case INSTRUMENT -> "Instrument";
      case INSTRUMENT_ID -> "InstrumentId";
      case TYPE -> "Type";
      case UNIT -> "Unit";
      case TIME_OFFSET -> "TimeOffset";
      case COLOR -> "Color";
    };
  }

  public int valueType() {
    return switch (this) {
      case ID -> Types.INTEGER;
      case DESCRIPTION -> Types.CHAR;
      case INSTRUMENT -> Types.CHAR;
      case INSTRUMENT_ID -> Types.CHAR;
      case TYPE -> Types.INTEGER;
      case UNIT -> Types.INTEGER;
      case TIME_OFFSET -> Types.DOUBLE;
      case COLOR -> Types.INTEGER;
    };
  }

  public static Map<Integer, TraceColumns> findIndices(ResultSetMetaData metaData)
      throws SQLException {
    final Map<Integer, TraceColumns> indices = new HashMap<>();
    for (int i = 0; i < metaData.getColumnCount(); i++) {
      int finalI = i;
      final Optional<TraceColumns> value = Arrays.stream(values()).filter(c -> {
        try {
          return c.header().equals(metaData.getColumnName(finalI));
        } catch (SQLException e) {
          return false;
        }
      }).findFirst();
      if (value.isPresent()) {
        indices.put(i, value.get());
      }
    }
    return indices;
  }

  public void map(int index, ResultSet result, Trace trace) throws SQLException {
    switch (this) {
      case ID -> {
        trace.setId(result.getLong(index));
      }
      case DESCRIPTION -> {
        trace.setDescription(result.getString(index));
      }
      case INSTRUMENT -> {
        trace.setInstrument(result.getString(index));
      }
      case INSTRUMENT_ID -> {
        trace.setInstrumentId(result.getString(index));
      }
      case TYPE -> {
        trace.setType(result.getLong(index));
      }
      case UNIT -> {
        trace.setUnit(result.getLong(index));
      }
      case TIME_OFFSET -> {
        trace.setTimeOffset(result.getDouble(index));
      }
      case COLOR -> {
        trace.setColor(result.getLong(index));
      }
    }
  }
}
