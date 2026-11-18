/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_scans;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Controls whether the output file is overwritten or appended to on each export run.
 *
 * <p>The default is {@link #APPEND} to preserve the behaviour of batch files saved before this
 * parameter existed (old batches did not store the parameter, so
 * {@link ExportScansParameters#handleLoadedParameters} restores it to {@code APPEND}).
 */
public enum FileWriterOption implements UniqueIdSupplier {

  /**
   * Append new scans to the end of an existing file. Preserves the original behaviour and is the
   * default for backward-compatibility with saved batch files that predate this parameter.
   */
  APPEND,

  /**
   * Truncate (overwrite) the output file before writing. Use this when a clean file per run is
   * desired.
   */
  OVERWRITE;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case APPEND -> "append";
      case OVERWRITE -> "overwrite";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case APPEND -> "Append to existing file";
      case OVERWRITE -> "Overwrite existing file";
    };
  }

  /** Convenience method used in {@link ExportScansTask}. */
  public boolean isAppend() {
    return this == APPEND;
  }
}
