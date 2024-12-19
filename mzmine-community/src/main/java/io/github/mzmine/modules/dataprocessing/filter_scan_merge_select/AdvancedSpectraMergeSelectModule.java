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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Advanced settings should give all options that are available to merging and selecting fragment
 * scans. All the other preset based setups currently generate advanced parameters and the
 * {@link FragmentScanSelection} and {@link SpectraMerger} are setup from this parameter set
 */
public class AdvancedSpectraMergeSelectModule implements SpectraMergeSelectModule {

  @Override
  public @NotNull String getName() {
    return "Spectra merging (advanced)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AdvancedSpectraMergeSelectParameters.class;
  }
}
