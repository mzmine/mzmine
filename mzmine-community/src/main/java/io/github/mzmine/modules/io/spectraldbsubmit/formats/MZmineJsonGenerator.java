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

import com.fasterxml.jackson.core.JsonGenerator;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.io.SemverVersionReader;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.MZmineJsonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Json for MZmine json library entry submission
 * <p>
 * One json object per entry, which is what
 * {@link MZmineJsonParser} reads back, either one per line
 * or wrapped in a json array.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MZmineJsonGenerator {

  /**
   * Entries are small, so the writer only has to avoid the initial regrowth of the default buffer.
   */
  private static final int INITIAL_ENTRY_CHARS = 4096;

  private MZmineJsonGenerator() {
  }

  /**
   * Whole JSON entry
   */
  public static String generateJSON(@NotNull final LibrarySubmitIonParameters param,
      final DataPoint @NotNull [] dps, @NotNull final IntensityNormalizer normalizer) {
    final LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param.getParameter(
        LibrarySubmitIonParameters.META_PARAM).getValue();

    final boolean exportRT = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getValue();

    return write(gen -> {
      // tag spectrum from mzmine
      final String version = String.valueOf(SemverVersionReader.getMZmineVersion());
      gen.writeStringField(DBEntryField.SOFTWARE.getMZmineJsonID(), "mzmine-" + version);

      // ion specific
      final Double precursorMZ = param.getParameter(LibrarySubmitIonParameters.MZ).getValue();
      if (precursorMZ != null) {
        gen.writeNumberField(DBEntryField.PRECURSOR_MZ.getMZmineJsonID(), precursorMZ);
      }

      final Integer charge = param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue();
      if (charge != null) {
        gen.writeNumberField(DBEntryField.CHARGE.getMZmineJsonID(), charge);
      }

      final String adduct = param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue();
      if (adduct != null && !adduct.trim().isEmpty()) {
        gen.writeStringField(DBEntryField.ION_TYPE.getMZmineJsonID(), adduct);
      }

      if (exportRT) {
        final Double rt = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT)
            .getEmbeddedParameter().getValue();
        if (rt != null) {
          gen.writeNumberField(DBEntryField.RT.getMZmineJsonID(), rt);
        }
      }

      // add data points array
      gen.writeFieldName("peaks");
      writeDataPoints(gen, dps, normalizer);

      // add meta data
      for (final Parameter<?> p : meta.getParameters()) {
        if (!p.getName().equals(LibraryMetaDataParameters.EXPORT_RT.getName())) {
          gen.writeFieldName(p.getName());
          writeValue(gen, p.getValue());
        }
      }
    });
  }

  public static String generateJSON(@NotNull final SpectralLibraryEntry entry,
      @NotNull final IntensityNormalizer normalizer) {
    final PolarityType polarity = entry.getPolarity();
    final DataPoint[] dps = entry.getDataPoints();

    return write(gen -> {
      // tag spectrum from mzmine
      final String version = String.valueOf(SemverVersionReader.getMZmineVersion());
      gen.writeStringField(DBEntryField.SOFTWARE.getMZmineJsonID(), "mzmine-" + version);

      // polarity and the signal count are taken from the spectrum, not from the stored fields.
      // They are still written in the place the entry has them so exported libraries keep their
      // key order
      boolean polarityWritten = !polarity.isDefined();
      boolean numPeaksWritten = false;
      for (final Entry<DBEntryField, Object> metafield : entry.getFields().entrySet()) {
        final DBEntryField field = metafield.getKey();
        final String id = field.getMZmineJsonID();
        final Object value = metafield.getValue();
        if (id == null || id.isBlank() || value == null || field == DBEntryField.SOFTWARE) {
          continue;
        }
        if (field == DBEntryField.POLARITY && polarity.isDefined()) {
          gen.writeStringField(id, polarity.toString());
          polarityWritten = true;
        } else if (field == DBEntryField.NUM_PEAKS) {
          gen.writeNumberField(id, dps.length);
          numPeaksWritten = true;
        } else {
          gen.writeFieldName(id);
          writeEntryValue(gen, value);
        }
      }

      if (!polarityWritten) {
        gen.writeStringField(DBEntryField.POLARITY.getMZmineJsonID(), polarity.toString());
      }
      if (!numPeaksWritten) {
        gen.writeNumberField(DBEntryField.NUM_PEAKS.getMZmineJsonID(), dps.length);
      }

      // add data points array
      gen.writeFieldName("peaks");
      writeDataPoints(gen, dps, normalizer);
    });
  }

  /**
   * Opens the object, lets the caller write the fields and returns the finished json.
   */
  private static String write(@NotNull final EntryWriter content) {
    final StringWriter out = new StringWriter(INITIAL_ENTRY_CHARS);
    try (JsonGenerator gen = JsonUtils.FACTORY.createGenerator(out)) {
      gen.writeStartObject();
      content.write(gen);
      gen.writeEndObject();
    } catch (IOException e) {
      // StringWriter never fails, so this can only be a malformed entry
      throw new UncheckedIOException(e);
    }
    return out.toString();
  }

  /**
   * JSON of data points array, each signal as its own [mz, intensity] array.
   */
  private static void writeDataPoints(@NotNull final JsonGenerator gen, DataPoint @NotNull [] dps,
      @NotNull final IntensityNormalizer normalizer) throws IOException {
    dps = normalizer.normalize(dps, false);

    gen.writeStartArray(dps.length);
    for (final DataPoint dp : dps) {
      gen.writeStartArray(2);
      // round to digits. thats more than enough. Has to go through long, an int saturates at
      // 2147.483647 and would cut every m/z above that
      gen.writeNumber(((long) (dp.getMZ() * 1000000)) / 1000000.0);
      gen.writeNumber(dp.getIntensity());
      gen.writeEndArray();
    }
    gen.writeEndArray();
  }

  /**
   * Writes a value of an exported library entry. Numbers keep their own type here, so a float field
   * like the retention time is written as the float it is instead of the wider double it would
   * widen to.
   */
  private static void writeEntryValue(@NotNull final JsonGenerator gen, @NotNull final Object value)
      throws IOException {
    switch (value) {
      case Double v -> gen.writeNumber(v.doubleValue());
      case Float v -> gen.writeNumber(v.floatValue());
      case Integer v -> gen.writeNumber(v.intValue());
      case Long v -> gen.writeNumber(v.longValue());
      case Boolean v -> gen.writeBoolean(v);
      case List<?> list -> writeList(gen, list);
      // assumption: anything else is described well enough by its toString, like the enums and
      // records used for polarity, the ion type or the retention index
      default -> gen.writeString(value.toString());
    }
  }

  /**
   * Writes a value of a submitted entry, where the user may have left fields empty. Used for the
   * parameter values and for the elements of every list, so the field name has to be written by the
   * caller.
   */
  private static void writeValue(@NotNull final JsonGenerator gen, @Nullable final Object value)
      throws IOException {
    switch (value) {
      // a zero is written as an int, as it was before, so that an unset number does not
      // end up as 0.0 in the submitted entry
      case Double v when Double.compare(0d, v) == 0 -> gen.writeNumber(0);
      case Double v -> gen.writeNumber(v.doubleValue());
      case Float v when Float.compare(0f, v) == 0 -> gen.writeNumber(0);
      case Float v -> gen.writeNumber(v.floatValue());
      case Integer v -> gen.writeNumber(v.intValue());
      case List<?> list -> writeList(gen, list);
      // missing and empty values have to be filled, the receiving side expects a value per field
      case null -> gen.writeString("N/A");
      default ->
          gen.writeString(value instanceof String s && s.isBlank() ? "N/A" : value.toString());
    }
  }

  private static void writeList(@NotNull final JsonGenerator gen, @NotNull final List<?> list)
      throws IOException {
    gen.writeStartArray(list.size());
    for (final Object o : list) {
      writeValue(gen, o);
    }
    gen.writeEndArray();
  }

  /**
   * Writes the fields of one entry into an already opened json object.
   */
  @FunctionalInterface
  private interface EntryWriter {

    void write(@NotNull JsonGenerator gen) throws IOException;
  }
}
