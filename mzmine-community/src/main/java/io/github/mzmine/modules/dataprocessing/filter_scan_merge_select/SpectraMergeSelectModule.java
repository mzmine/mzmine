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

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.merging.SpectraMerger;

/**
 * These modules provide options to select and merge fragmentation mass spectra. The
 * {@link SpectraMergeSelectParameter} is the parameter to setup everything.
 * <p>
 * There are simple preset based setups in {@link PresetSimpleSpectraMergeSelectParameters} and
 * {@link PresetAdvancedSpectraMergeSelectParameters} for simple merging setup of commonly used
 * combinations. The {@link InputSpectraSelectParameters} allows selecting all or the most intense
 * input spectra without merging. The {@link AdvancedSpectraMergeSelectParameters} allow the full
 * setup of merging and input spectra selection but is quite complex. Therefore, some modules may
 * choose to hide this option or even some of the simpler presets.
 * <p>
 * Finally, the actual selection and merging are done by {@link FragmentScanSelection} and
 * {@link SpectraMerger}.
 */
public interface SpectraMergeSelectModule extends MZmineModule {


}
