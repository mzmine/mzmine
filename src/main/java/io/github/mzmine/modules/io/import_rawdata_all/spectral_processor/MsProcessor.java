/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_all.spectral_processor;

import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classes only used during data import to reduce the need for memory mapping and to filter data
 * before this
 */
public interface MsProcessor {

  /**
   * Process input data. This is usually called during import when a scan is read and finalized.
   *
   * @param metadataOnlyScan scan that may not contain all metadata. Only metadata needed to
   *                         process. This can be for example {@link MetadataOnlyScan} or
   *                         {@link SimpleBuildingScan}
   * @param spectrum         spectral data to process
   * @return resulting spectral data or input data if on no change
   */
  @NotNull SimpleSpectralArrays processScan(@Nullable final Scan metadataOnlyScan,
      @NotNull final SimpleSpectralArrays spectrum);

  @NotNull String description();
}
