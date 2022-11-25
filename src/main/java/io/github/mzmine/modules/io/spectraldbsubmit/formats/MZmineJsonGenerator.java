/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.formats;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import java.util.List;

/**
 * Json for MZmine json library entry submission
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MZmineJsonGenerator {

  /**
   * Whole JSON entry
   *
   * @param param
   * @param dps
   * @return
   */
  public static String generateJSON(LibrarySubmitIonParameters param, DataPoint[] dps) {
    LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param.getParameter(
        LibrarySubmitIonParameters.META_PARAM).getValue();

    boolean exportRT = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getValue();

    JsonObjectBuilder json = Json.createObjectBuilder();
    // tag spectrum from mzmine2
    json.add(DBEntryField.SOFTWARE.getMZmineJsonID(), "mzmine2");
    // ion specific
    Double precursorMZ = param.getParameter(LibrarySubmitIonParameters.MZ).getValue();
    if (precursorMZ != null) {
      json.add(DBEntryField.PRECURSOR_MZ.getMZmineJsonID(), precursorMZ);
    }

    Integer charge = param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue();
    if (charge != null) {
      json.add(DBEntryField.CHARGE.getMZmineJsonID(), charge);
    }

    String adduct = param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue();
    if (adduct != null && !adduct.trim().isEmpty()) {
      json.add(DBEntryField.ION_TYPE.getMZmineJsonID(), adduct);
    }

    if (exportRT) {
      Double rt = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getEmbeddedParameter()
          .getValue();
      if (rt != null) {
        json.add(DBEntryField.RT.getMZmineJsonID(), rt);
      }
    }

    // add data points array
    json.add("peaks", genJSONData(dps));

    // add meta data
    for (Parameter<?> p : meta.getParameters()) {
      if (!p.getName().equals(LibraryMetaDataParameters.EXPORT_RT.getName())) {
        String key = p.getName();
        Object value = p.getValue();
        addToJson(json, key, value);
      }
    }

    // return Json.createObjectBuilder().add("spectrum",
    // json.build()).build().toString();
    return json.build().toString();
  }

  private static void addToJson(final JsonObjectBuilder json, final String key, Object value) {
    if (value instanceof Double) {
      if (Double.compare(0d, (Double) value) == 0) {
        json.add(key, 0);
      } else {
        json.add(key, (Double) value);
      }
    } else if (value instanceof Float) {
      if (Float.compare(0f, (Float) value) == 0) {
        json.add(key, 0);
      } else {
        json.add(key, (Float) value);
      }
    } else if (value instanceof Integer) {
      json.add(key, (Integer) value);
    } else {
      if (value == null || (value instanceof String && ((String) value).isEmpty())) {
        value = "N/A";
      }
      json.add(key, value.toString());
    }
  }

  private static void addToJson(final JsonArrayBuilder json, Object value) {
    if (value instanceof Double) {
      if (Double.compare(0d, (Double) value) == 0) {
        json.add(0);
      } else {
        json.add((Double) value);
      }
    } else if (value instanceof Float) {
      if (Float.compare(0f, (Float) value) == 0) {
        json.add(0);
      } else {
        json.add((Float) value);
      }
    } else if (value instanceof Integer) {
      json.add((Integer) value);
    } else {
      if (value == null || (value instanceof String && ((String) value).isEmpty())) {
        value = "N/A";
      }
      json.add(value.toString());
    }
  }

  /**
   * JSON of data points array
   *
   * @param dps
   * @return
   */
  public static JsonArray genJSONData(DataPoint[] dps) {
    JsonArrayBuilder data = Json.createArrayBuilder();
    JsonArrayBuilder signal = Json.createArrayBuilder();
    for (DataPoint dp : dps) {
      // round to digits. thats more than enough
      signal.add(((int) (dp.getMZ() * 1000000)) / 1000000.0);
      signal.add(dp.getIntensity());
      data.add(signal.build());
    }
    return data.build();
  }

  public static String generateJSON(final SpectralLibraryEntry entry) {
    JsonObjectBuilder json = Json.createObjectBuilder();
    // tag spectrum from mzmine3
    json.add(DBEntryField.SOFTWARE.getMZmineJsonID(), "mzmine3");

    for (var metafield : entry.getFields().entrySet()) {
      String id = metafield.getKey().getMZmineJsonID();
      if (id == null || id.isBlank()) {
        continue;
      }
      Object value = metafield.getValue();
      switch (value) {
        case Double cv -> json.add(id, cv);
        case Float cv -> json.add(id, cv);
        case Integer cv -> json.add(id, cv);
        case Long cv -> json.add(id, cv);
        case Boolean cv -> json.add(id, cv);
        case List list -> json.add(id, listToJsonArray(list));
        default -> json.add(id, value.toString());
      }
    }
    entry.getField(DBEntryField.POLARITY)
        .ifPresent(value -> json.add(DBEntryField.POLARITY.getMZmineJsonID(), value.toString()));

    DataPoint[] dps = entry.getDataPoints();
    json.add(DBEntryField.NUM_PEAKS.getMZmineJsonID(), dps.length);

    // add data points array
    json.add("peaks", genJSONData(dps));

    return json.build().toString();
  }

  private static JsonArray listToJsonArray(final List list) {
    JsonArrayBuilder array = Json.createArrayBuilder();
    for (var o : list) {
      addToJson(array, o);
    }
    return array.build();
  }
}
