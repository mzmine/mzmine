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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.header;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An instrumentConfiguration of the mzML header with its components and software reference.
 */
public class MzMLInstrumentConfiguration extends MzMLParamContainer {

  private final @NotNull List<MzMLParamContainer> sources = new ArrayList<>();
  private final @NotNull List<MzMLParamContainer> analyzers = new ArrayList<>();
  private final @NotNull List<MzMLParamContainer> detectors = new ArrayList<>();
  private @Nullable String softwareRef;

  public MzMLInstrumentConfiguration(@NotNull final String id) {
    super(id);
  }

  public @NotNull List<MzMLParamContainer> getSources() {
    return sources;
  }

  public @NotNull List<MzMLParamContainer> getAnalyzers() {
    return analyzers;
  }

  public @NotNull List<MzMLParamContainer> getDetectors() {
    return detectors;
  }

  public @Nullable String getSoftwareRef() {
    return softwareRef;
  }

  public void setSoftwareRef(@Nullable final String softwareRef) {
    this.softwareRef = softwareRef;
  }
}
