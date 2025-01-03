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

package io.github.mzmine.modules.io.download;

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fixed details on external assets. See also {@link DownloadAsset} to define new downloads of
 * specific versions.
 */
public enum AssetGroup {
  // cannot download those assets directly but only provide link
  ThermoRawFileParser, MSCONVERT,

  // spectral libraries
  MSnLib, GNPS_LIB, MONA_LIB, MASSBANK_EU,

  // models
  MS2DEEPSCORE, DREAMS;

  @Override
  public String toString() {
    // always lower case for folders
    return super.toString().toLowerCase();
  }

  /**
   * @return clear text label to be used in UI
   */
  public String getLabel() {
    return switch (this) {
      case ThermoRawFileParser -> "ThermoRawFileParser";
      case MSCONVERT -> "MSconvert";
      case MSnLib -> "MSnLib";
      case GNPS_LIB -> "GNPS";
      case MONA_LIB -> "MoNA";
      case MASSBANK_EU -> "MassBank EU";
      case MS2DEEPSCORE -> "MS2Deepscore";
      case DREAMS -> "DreaMS";
    };
  }

  public File getDownloadToDir() {
    return FileAndPathUtil.resolveInDownloadResourcesDir(
        getCategory().toString() + "/" + this.toString());
  }

  public AssetCategory getCategory() {
    return switch (this) {
      case ThermoRawFileParser, MSCONVERT -> AssetCategory.TOOLS;
      case MSnLib, GNPS_LIB, MONA_LIB, MASSBANK_EU -> AssetCategory.SPECTRAL_LIBRARIES;
      case MS2DEEPSCORE, DREAMS -> AssetCategory.MODELS;
    };
  }

  /**
   * The website with download instructions
   */
  @Nullable
  public String getDownloadInfoPage() {
    return switch (this) {
      case ThermoRawFileParser ->
          "https://github.com/pluskal-lab/ThermoRawFileParserMacLinux/releases";
      case MSCONVERT -> "https://proteowizard.sourceforge.io/download.html";
      // libraries
      case MSnLib -> "https://zenodo.org/records/11163380";
      case GNPS_LIB -> "https://external.gnps2.org/gnpslibrary";
      case MONA_LIB -> "https://mona.fiehnlab.ucdavis.edu/downloads";
      case MASSBANK_EU -> "https://github.com/MassBank/MassBank-data/releases/latest";
      case MS2DEEPSCORE -> "https://zenodo.org/records/12628368";
      case DREAMS -> "https://huggingface.co/roman-bushuiev/DreaMS";
    };
  }

  /**
   * @return list of download options. Empty list if no direct download is possible
   */
  @NotNull
  public List<DownloadAsset> getDownloadAssets() {
    return DownloadAssets.ASSETS.stream().filter(a -> a.extAsset() == this).toList();
  }

}
