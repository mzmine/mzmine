/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the query spectra of one MSPepSearch run as an MSP file.
 * <p>
 * The ion polarity goes in as {@code Ion_mode: P} or {@code Ion_mode: N}. Probed against
 * MSPepSearch 0.9.7.5 and the NIST 26 tandem libraries with {@code /MatchPolarity}: the single
 * letters, a {@code Precursor_type} ending in + or - and a signed {@code Charge: -1} are all read,
 * while {@code Ion_mode: NEGATIVE} and {@code Charge: 1-} are silently ignored and leave the
 * spectrum positive. See {@link MsPepSearchCommand} for why a run must not mix polarities.
 */
final class MsPepSearchQueryWriter {

  private MsPepSearchQueryWriter() {
  }

  /**
   * Writes the spectra in the given order, which is the order MSPepSearch numbers them in.
   */
  static void write(@NotNull final File file, @NotNull final List<MspSpectrum> spectra)
      throws IOException {

    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {

      for (final MspSpectrum spectrum : spectra) {

        writer.write("Name: " + spectrum.name());
        writer.newLine();

        if (spectrum.precursorMz() != null) {
          writer.write("PrecursorMZ: " + spectrum.precursorMz());
          writer.newLine();
        }

        final String ionMode = ionMode(spectrum.polarity());
        if (ionMode != null) {
          writer.write("Ion_mode: " + ionMode);
          writer.newLine();
        }

        if (spectrum.nominalMw() != null) {
          writer.write("MW: " + spectrum.nominalMw());
          writer.newLine();
        }

        if (spectrum.rt() != null) {
          writer.write("RT: " + spectrum.rt());
          writer.newLine();
        }

        writer.write("Num Peaks: " + spectrum.dataPoints().length);
        writer.newLine();

        for (final DataPoint dataPoint : spectrum.dataPoints()) {
          writer.write(dataPoint.getMZ() + "\t" + dataPoint.getIntensity());
          writer.newLine();
        }

        writer.newLine();
      }
    }
  }

  /**
   * @return the {@code Ion_mode} value MSPepSearch reads, or null if the polarity is unknown and
   * the field must be left out.
   */
  private static @Nullable String ionMode(@Nullable final PolarityType polarity) {
    if (polarity == null) {
      return null;
    }
    return switch (polarity) {
      case POSITIVE -> "P";
      case NEGATIVE -> "N";
      case NEUTRAL, ANY, UNKNOWN -> null;
    };
  }

  /**
   * One spectrum of the query file.
   *
   * @param name        the {@code Name:}, which MSPepSearch echoes in its {@code Unknown} column.
   * @param precursorMz the precursor m/z, only written for the accurate mass MS/MS searches.
   * @param polarity    the ion polarity, written only if positive or negative.
   * @param nominalMw   the nominal molecular weight, only needed by the hybrid search.
   * @param rt          the retention time in minutes, informational only.
   * @param dataPoints  the peaks.
   */
  record MspSpectrum(@NotNull String name, @Nullable Double precursorMz,
                     @Nullable PolarityType polarity, @Nullable Integer nominalMw,
                     @Nullable Float rt, @NotNull DataPoint[] dataPoints) {

  }
}
