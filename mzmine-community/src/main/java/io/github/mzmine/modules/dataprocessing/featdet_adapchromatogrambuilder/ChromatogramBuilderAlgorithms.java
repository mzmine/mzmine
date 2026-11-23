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

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastAutoChromatogramBuilderAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastChromatogramBuilderAlgorithm;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

/**
 * Algorithms of the {@link ModularADAPChromatogramBuilderModule}.
 */
public enum ChromatogramBuilderAlgorithms implements ModuleOptionsEnum<MZmineModule> {
  /**
   * The fast builder with all parameters determined from the data for a sensitivity, the default
   */
  FAST_AUTO,
  /**
   * The fast builder with user defined parameters, the m/z tolerance may be estimated
   */
  FAST,
  /**
   * The ADAP chromatogram builder of mzmine before 4.11, old batch steps load with this algorithm
   */
  LEGACY_ADAP;

  @Override
  public Class<? extends MZmineModule> getModuleClass() {
    return switch (this) {
      case FAST_AUTO -> FastAutoChromatogramBuilderAlgorithm.class;
      case FAST -> FastChromatogramBuilderAlgorithm.class;
      case LEGACY_ADAP -> LegacyAdapChromatogramBuilderAlgorithm.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case FAST_AUTO -> "fast_auto";
      case FAST -> "fast";
      case LEGACY_ADAP -> "legacy_adap";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case FAST_AUTO -> "Fast (auto)";
      case FAST -> "Fast";
      case LEGACY_ADAP -> "mzmine <4.11";
    };
  }

  /**
   * @return true for the algorithms of the fast builder
   */
  public boolean isFast() {
    return switch (this) {
      case FAST_AUTO, FAST -> true;
      case LEGACY_ADAP -> false;
    };
  }
}
