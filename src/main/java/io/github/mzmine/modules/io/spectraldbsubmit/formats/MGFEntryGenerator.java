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
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

public class MGFEntryGenerator {

//  public static final List<DBEntryField> EXPORT_FIELDS = List.of(DBEntryField.SCAN_NUMBER,
//      DBEntryField.MS_LEVEL, DBEntryField.CHARGE, DBEntryField.NAME, DBEntryField.PRECURSOR_MZ,
//      DBEntryField.ION_TYPE, DBEntryField.RT, DBEntryField.CCS, DBEntryField.EXACT_MASS,
//      DBEntryField.FORMULA, DBEntryField.SMILES, DBEntryField.INCHI, DBEntryField.INCHIKEY,
//     DBEntryField.ISOLATION_WINDOW,
//
//      // instrument
//      DBEntryField.FRAGMENTATION_METHOD, DBEntryField.COLLISION_ENERGY, DBEntryField.INSTRUMENT, DBEntryField.INSTRUMENT_FIELDS
//      // MSn
//      DBEntryField.MSN_FRAGMENTATION_METHODS, DBEntryField.MSN_COLLISION_ENERGIES,
//      DBEntryField.MSN_PRECURSOR_MZS, DBEntryField.MSN_ISOLATION_WINDOWS,
//      // reference to dataset
//      DBEntryField.DATASET_ID, DBEntryField.USI,
//      //
//      DBEntryField.ACQUISITION,
//      //
//      DBEntryField.MERGED_SPEC_TYPE,
//      // quality
//      DBEntryField.QUALITY_CHIMERIC,
//      DBEntryField.SOFTWARE);

  /**
   * Creates a simple MSP nist format DB entry
   *
   * @param param
   * @param dps
   * @return
   */
  public static String createMGFEntry(LibrarySubmitIonParameters param, DataPoint[] dps) {

    LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param.getParameter(
        LibrarySubmitIonParameters.META_PARAM).getValue();

    boolean exportRT = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getValue();
    String ionMode =
        meta.getParameter(LibraryMetaDataParameters.IONMODE).getValue().equals(Polarity.Negative)
            ? "Negative" : "Positive";

    String def = "=";
    String br = "\n";
    StringBuilder s = new StringBuilder();
    s.append("BEGIN IONS").append(br);
    // tag spectrum from mzmine
    // ion specific
    s.append(DBEntryField.NAME.getMgfID() + def + meta.getParameter(
        LibraryMetaDataParameters.COMPOUND_NAME).getValue() + br);
    s.append(DBEntryField.INCHIKEY.getMgfID() + def + meta.getParameter(
        LibraryMetaDataParameters.INCHI_AUX).getValue() + br);
    s.append(DBEntryField.MS_LEVEL.getMgfID() + def + "MS" + meta.getParameter(
        LibraryMetaDataParameters.MS_LEVEL).getValue() + br);
    s.append(DBEntryField.INSTRUMENT_TYPE.getMgfID() + def + meta.getParameter(
        LibraryMetaDataParameters.INSTRUMENT).getValue() + br);
    s.append(DBEntryField.INSTRUMENT.getMgfID() + def + meta.getParameter(
        LibraryMetaDataParameters.INSTRUMENT_NAME).getValue() + br);
    s.append(DBEntryField.POLARITY.getMgfID() + def + ionMode + br);
    s.append(DBEntryField.COLLISION_ENERGY.getMgfID() + def + meta.getParameter(
        LibraryMetaDataParameters.FRAGMENTATION_METHOD).getValue() + br);
    s.append(
        DBEntryField.FORMULA.getMgfID() + def + meta.getParameter(LibraryMetaDataParameters.FORMULA)
            .getValue() + br);

    Double exact = meta.getParameter(LibraryMetaDataParameters.EXACT_MASS).getValue();
    if (exact != null) {
      s.append(DBEntryField.EXACT_MASS.getMgfID() + def + exact + br);
    }

    Double precursorMZ = param.getParameter(LibrarySubmitIonParameters.MZ).getValue();
    if (precursorMZ != null) {
      s.append(DBEntryField.PRECURSOR_MZ.getMgfID() + def + param.getParameter(
          LibrarySubmitIonParameters.MZ).getValue() + br);
    }

    String adduct = param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue();
    if (adduct != null && !adduct.trim().isEmpty()) {
      s.append(DBEntryField.ION_TYPE.getMgfID() + def + param.getParameter(
          LibrarySubmitIonParameters.ADDUCT).getValue() + br);
    }

    if (exportRT) {
      Double rt = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getEmbeddedParameter()
          .getValue();
      if (rt != null) {
        s.append(DBEntryField.RT.getMgfID() + def + rt + br);
      }
    }

    NumberFormat mzForm = new DecimalFormat("0.######");
    for (DataPoint dp : dps) {
      s.append(mzForm.format(dp.getMZ()) + " " + dp.getIntensity() + br);
    }
    s.append("END IONS").append(br);
    return s.toString();
  }

  /**
   * Creates a simple MSP nist format DB entry
   */
  public static String createMGFEntry(SpectralLibraryEntry entry) {
    String def = "=";
    String br = "\n";
    StringBuilder s = new StringBuilder();
    s.append("BEGIN IONS").append(br);
    // tag spectrum from mzmine
    // export sorted fields first, then the rest
    for (DBEntryField field : DBEntryField.values()) {
      String id = field.getMgfID();
      if (id == null || id.isBlank()) {
        continue;
      }
      entry.getField(field)
          .ifPresent(value -> s.append(field.getMgfID()).append(def).append(value).append(br));
    }
    entry.getField(DBEntryField.POLARITY).ifPresent(p -> {
      String pol = PolarityType.NEGATIVE.equals(p) ? "Negative" : "Positive";
      s.append(DBEntryField.POLARITY.getMgfID()).append(def).append(pol).append(br);
    });

    // num peaks and data
    DataPoint[] dps = entry.getDataPoints();

    NumberFormat mzForm = new DecimalFormat("0.######");
    // minimum intensity after formatting
    NumberFormat percentForm = new DecimalFormat("0.###");
    double minIntensity = 0.0005;

    double max = Arrays.stream(dps).mapToDouble(DataPoint::getIntensity).max().orElse(1d);
    for (DataPoint dp : dps) {
      double intensityPercent = dp.getIntensity() / max * 100.0;
      if (intensityPercent >= minIntensity) {
        s.append(mzForm.format(dp.getMZ())).append(" ").append(percentForm.format(intensityPercent))
            .append(br);
      }
    }
    s.append("END IONS").append(br);
    return s.toString();
  }
}
