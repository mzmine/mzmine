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
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public class MGFEntryGenerator {

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
   * Creates a simple mgf format DB entry
   *
   * @return a {@link SpectrumString} to allow for further filtering if the spectrum is empty after
   * formatting the intensity values, removing 0 values after formatting.
   */
  public static SpectrumString createMGFEntry(SpectralLibraryEntry entry,
      @NotNull final IntensityNormalizer normalizer) {
    String br = "\n";
    StringBuilder s = new StringBuilder();
    s.append("BEGIN IONS").append(br);
    // tag spectrum from mzmine
    // export sorted fields first, then the rest
    for (DBEntryField field : DBEntryField.values()) {
      String id = field.getMgfID();
      if (id == null || id.isBlank()
          // skip num peaks to count actually exported number of signals
          || field == DBEntryField.NUM_PEAKS) {
        continue;
      }
      // write field if present
      entry.getField(field).ifPresent(value -> s.append(createValueLine(field, value)));
    }

    // num peaks and data
    DataPoint[] dps = entry.getDataPoints();

    SpectrumString spectralData = createSpectrumStringWithNumPeaks(normalizer, dps);
    s.append(createValueLine(DBEntryField.NUM_PEAKS, spectralData.numSignals()));
    s.append(spectralData.spectrum());
    s.append("END IONS").append(br);
    return new SpectrumString(s.toString(), spectralData.numSignals());
  }

  /**
   * @return spectral data as a string ending with a new line
   */
  private static SpectrumString createSpectrumStringWithNumPeaks(
      final @NotNull IntensityNormalizer normalizer, DataPoint[] dps) {
    NumberFormat mzForm = new DecimalFormat("0.######");
    // minimum intensity after formatting
    NumberFormat intensityForm = normalizer.createExportFormat();

    // normalize or keep original intensities
    // mzmine <= v4.4 was normalizing intensities to 100%
    // Parameter was added for better control and default changed to original intensities
    dps = normalizer.normalize(dps, true);

    // count actually exported data points
    StringBuilder dataBuilder = new StringBuilder();
    int numExportedValues = 0;
    for (DataPoint dp : dps) {
      String formattedIntensity = intensityForm.format(dp.getIntensity());

      if (NumberFormats.IsZero(formattedIntensity, intensityForm)) {
        continue; // skip data point if formatted intensity value is 0
      }

      dataBuilder.append(mzForm.format(dp.getMZ())).append(" ").append(formattedIntensity)
          .append("\n");
      numExportedValues++;
    }
    return new SpectrumString(dataBuilder.toString(), numExportedValues);
  }

  private static String createValueLine(final DBEntryField field, final Object value) {
    return field.getMgfID() + "=" + field.formatForMgf(value) + "\n";
  }
}
