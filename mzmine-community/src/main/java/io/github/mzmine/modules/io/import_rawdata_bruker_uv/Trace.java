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

package io.github.mzmine.modules.io.import_rawdata_bruker_uv;

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public record Trace(Long id, String description, String instrument, String instrumentId, Long type,
                    Long unit, Double timeOffset, Long color) {

  public static List<Trace> readFromSql(Connection con) throws SQLException {
    final String query = "SELECT Id,Description,Instrument,InstrumentId,Type,Unit,TimeOffset,Color FROM TraceSources";

    List<Trace> traces = new ArrayList<>();
    try (var statement = con.createStatement()) {
      final ResultSet results = statement.executeQuery(query);

      while (results.next()) {
        final Trace trace = new Trace(results.getLong(1), results.getString(2),
            results.getString(3), results.getString(4), results.getLong(5), results.getLong(6),
            results.getDouble(7), results.getLong(8));

        traces.add(trace);
      }

      results.close();
    }

    return traces;
  }

  public boolean isValid() {
    // other types are not interesting
    if (id == null || description == null || instrument == null || type == null || unit == null) {
      return false;
    }

//    if ((getChomatogramType() == ChromatogramType.UNKNOWN && !"Pump_pressure".equals(description))
//        || getChomatogramType().equals(ChromatogramType.UNKNOWN)) {
//      return false;
//    }
    return true;
  }

  public ChromatogramType getChomatogramType() {
    if (type == null) {
      return ChromatogramType.UNKNOWN;
    }
    return switch (type.intValue()) {
      case 0 -> ChromatogramType.UNKNOWN; // None
      case 1 -> ChromatogramType.BPC; // any MS
      case 2, 3 -> deriveTypeFromUnit(); // Trace from DAD/PDA, does not have to be actual UV signal
      case 4 -> ChromatogramType.PRESSURE; // pump pressure
      case 5 -> ChromatogramType.UNKNOWN; // solvent composition
      case 6 -> ChromatogramType.FLOW_RATE; // flow rate
      case 7 -> ChromatogramType.UNKNOWN; // temperature
      default -> ChromatogramType.UNKNOWN; // user defined
    };
  }

  /**
   * More finely grained check for Traces from UV detector (Type = 3) May be a temperature
   * chromatogram or so. If it's related to UV, we return {@link ChromatogramType#ABSORPTION} and
   * {@link ChromatogramType#UNKNOWN} otherwise.
   */
  private ChromatogramType deriveTypeFromUnit() {
    return switch (getConvertedRangeLabel()) {
      case "Absorbance", "Intensity" -> ChromatogramType.ABSORPTION;
      default -> ChromatogramType.UNKNOWN;
    };
  }

  public String getConvertedRangeUnit() {
    return BrukerUtils.unitToString(unit().intValue());
  }

  public String getConvertedRangeLabel() {
    return BrukerUtils.unitToLabel(unit.intValue());
  }
}
