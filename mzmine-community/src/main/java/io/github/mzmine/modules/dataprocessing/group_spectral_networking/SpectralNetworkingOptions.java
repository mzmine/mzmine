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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor.NoPrecursorCosineSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams.DreaMSNetworkingModule;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;

public enum SpectralNetworkingOptions implements ModuleOptionsEnum {

  MODIFIED_COSINE, MS2_DEEPSCORE, COSINE_NO_PRECURSOR, DREAMS;

  @Override
  public String toString() {
    return getStableId();
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case MS2_DEEPSCORE -> MS2DeepscoreNetworkingModule.NAME;
      case MODIFIED_COSINE -> ModifiedCosineSpectralNetworkingModule.NAME;
      case COSINE_NO_PRECURSOR -> NoPrecursorCosineSpectralNetworkingModule.NAME;
      case DREAMS -> DreaMSNetworkingModule.NAME;
    };
  }


  @Override
  public Class<? extends MZmineModule> getModuleClass() {
    return switch (this) {
      case MODIFIED_COSINE -> ModifiedCosineSpectralNetworkingModule.class;
      case MS2_DEEPSCORE -> MS2DeepscoreNetworkingModule.class;
      case COSINE_NO_PRECURSOR -> NoPrecursorCosineSpectralNetworkingModule.class;
      case DREAMS -> DreaMSNetworkingModule.class;
    };
  }

}
