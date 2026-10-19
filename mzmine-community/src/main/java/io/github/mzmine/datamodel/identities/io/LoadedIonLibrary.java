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

package io.github.mzmine.datamodel.identities.io;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.time.LocalDateTime;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An ion library read from a file, together with the index the file referenced each ion type by.
 * Other formats embed an ion library and then only reference its ion types by that index, so they
 * need them resolved exactly as they were written - never by looking at the order of
 * {@link #library()}, which may be a different, already known library instance, and never by
 * re-deriving an order from a sorting that is free to change between versions.
 *
 * @param savedDate       when the file was written
 * @param library         the loaded library, possibly an already known instance with the same
 *                        content but a different ion order
 * @param ionTypesByIndex the position an ion type had in the file to the ion type, so index 0 is
 *                        the first ion type of the file
 */
public record LoadedIonLibrary(@NotNull LocalDateTime savedDate, @NotNull IonLibrary library,
                               @NotNull Map<Integer, IonType> ionTypesByIndex) {

  /**
   * @return the ion type the file stored at this index or null if the index is not in the file
   */
  public @Nullable IonType ionType(final int index) {
    return ionTypesByIndex.get(index);
  }
}
