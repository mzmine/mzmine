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

package io.github.mzmine.util.spectraldb.parser.gnps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One entry of a GNPS2 json library. These carry the metadata in a nested object under mgf style
 * upper case keys and the signals as a plain json array, where the older GNPS exports put every key
 * on the entry itself and the signals in a peaks_json string.
 * <p>
 * The keys are the ones mgf uses, so they are resolved through {@link DBEntryField#forID(String)}
 * instead of being listed here. That keeps this working when GNPS2 adds a key that mzmine already
 * knows from another format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record Gnps2LibraryEntry(@NotNull Map<String, String> metadata,

                         @JsonProperty("peaks") @JsonDeserialize(using = PeakArrayDeserializer.class) double[][] spectrum) implements
    GnpsEntry {

  private static final Logger logger = Logger.getLogger(Gnps2LibraryEntry.class.getName());

  /**
   * Words used where a value is missing. Compared lower case.
   */
  private static final Set<String> PLACEHOLDERS = Set.of("n/a", "na", "nan", "none", "null", "-");

  @Override
  public SpectralLibraryEntry toSpectralLibraryEntry(@Nullable final SpectralLibrary library) {
    final MemoryMapStorage storage = library == null ? null : library.getStorage();
    final SpectralDBEntry entry = new SpectralDBEntry(storage, spectrum[0], spectrum[1]);

    for (final Map.Entry<String, String> pair : metadata.entrySet()) {
      final DBEntryField field = DBEntryField.forID(pair.getKey());
      if (field == null) {
        continue;
      }
      final String value = clean(pair.getValue());
      // assumption: json keeps its order, so where two keys map to the same field, for example
      // NAME and COMPOUND_NAME, the one written first wins
      if (value == null || entry.getField(field).isPresent()) {
        continue;
      }
      try {
        entry.putIfNotNull(field, field.convertValue(value));
      } catch (Exception e) {
        logger.log(Level.FINEST,
            () -> "Cannot convert %s of GNPS2 entry: %s".formatted(field, e.getMessage()));
      }
    }
    return entry;
  }

  /**
   * @return the value, or null if it is blank or one of the {@link #PLACEHOLDERS}
   */
  @Nullable
  private static String clean(@Nullable final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    final String trimmed = value.trim();
    return PLACEHOLDERS.contains(trimmed.toLowerCase()) ? null : trimmed;
  }
}
