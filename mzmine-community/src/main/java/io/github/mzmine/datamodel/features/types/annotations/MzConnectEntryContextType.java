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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the MZconnect entry identifier behind a user-facing context action label.
 */
public class MzConnectEntryContextType extends StringType {

  @FunctionalInterface
  public interface EntryContextHandler {

    void openEntryContext(@NotNull String entryId);
  }

  private static volatile @Nullable EntryContextHandler handler;

  public static void setHandler(final @Nullable EntryContextHandler newHandler) {
    handler = newHandler;
  }

  public static boolean openEntryContext(final @Nullable Object value) {
    final String entryId;
    if (value instanceof final String stringValue) {
      entryId = stringValue;
    } else if (value instanceof final Number numericValue) {
      entryId = numericValue.toString();
    } else {
      return false;
    }

    final String trimmedEntryId = entryId.trim();
    if (trimmedEntryId.isEmpty()) {
      return false;
    }

    final EntryContextHandler currentHandler = handler;
    if (currentHandler == null) {
      return false;
    }

    currentHandler.openEntryContext(trimmedEntryId);
    return true;
  }

  @Override
  public @NotNull String getUniqueID() {
    return "mzconnect_entry_context";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "MZconnect context";
  }

  @Override
  public @NotNull String getFormattedString(final String value, final boolean export) {
    return export ? value : "Open historical context";
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(final @Nullable FeatureTableFX table,
      @NotNull final ModularFeatureListRow row, @NotNull final List<RawDataFile> file,
      @Nullable final DataType<?> superType, final @Nullable Object value) {
    return () -> openEntryContext(value);
  }
}
