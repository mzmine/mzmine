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

package io.github.mzmine.datamodel.features.rawfiletypes;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lookup of raw file metadata {@link DataType}s by their controlled vocabulary accession. All types
 * implementing {@link CvTermType} are registered automatically, so adding a new type is enough to
 * make it available to importers that map cvParams by accession.
 */
public final class RawFileMetadataTypes {

  private static Map<String, DataType<?>> byAccession;

  private RawFileMetadataTypes() {
  }

  /**
   * @return unmodifiable map of CV accession to data type
   */
  public static synchronized @NotNull Map<String, DataType<?>> byAccession() {
    if (byAccession == null) {
      final Map<String, DataType<?>> map = new HashMap<>();
      for (final DataType<?> type : DataTypes.getInstances()) {
        if (type instanceof CvTermType cv) {
          final DataType<?> old = map.put(cv.getCvAccession(), type);
          if (old != null) {
            throw new IllegalStateException(
                "Multiple data types with CV accession %s: %s and %s".formatted(cv.getCvAccession(),
                    old.getClass().getName(), type.getClass().getName()));
          }
        }
      }
      byAccession = Map.copyOf(map);
    }
    return byAccession;
  }

  public static @Nullable DataType<?> forAccession(@Nullable final String accession) {
    if (accession == null) {
      return null;
    }
    return byAccession().get(accession);
  }
}
