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

import static io.github.mzmine.modules.io.download.DownloadAsset.Builder.ofURL;
import static io.github.mzmine.modules.io.download.DownloadAsset.Builder.ofZenodo;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of hard coded download assets. The list of assets may be extended by additional assets
 * from an external list of assets in the future.
 */
public class DownloadAssets {

  /**
   * May be changed from outside to account for more downloadable assets
   */
  public static final List<DownloadAsset> ASSETS = new ArrayList<>(List.of(
      // tools
      // libraries
      ofZenodo(AssetGroup.MSnLib, "11163380").version("ms2-latest").fileNamePattern(".*_ms2.json")
          .create(),
      ofZenodo(AssetGroup.MSnLib, "11163380").version("msn-latest").fileNamePattern(".*_msn.json")
          .create(), ofZenodo(AssetGroup.MSnLib, "11163380").version("ms2-pos-latest")
          .fileNamePattern(".*pos_ms2.json").create(),
      ofZenodo(AssetGroup.MSnLib, "11163380").version("msn-pos-latest")
          .fileNamePattern(".*pos_msn.json").create(),
      ofZenodo(AssetGroup.MSnLib, "11163380").version("ms2-neg-latest")
          .fileNamePattern(".*neg_ms2.json").create(),
      ofZenodo(AssetGroup.MSnLib, "11163380").version("msn-neg-latest")
          .fileNamePattern(".*neg_msn.json").create(),

      // other libraries
//      ofURL(AssetGroup.MASSBANK_EU,
//          "https://github.com/MassBank/MassBank-data/releases/download/2024.06/MassBank.json").version(
//          "2024.06").create(), //
      ofURL(AssetGroup.GNPS_LIB,
          "https://external.gnps2.org/gnpslibrary/ALL_GNPS_NO_PROPOGATED.json").version(
          "All-no propagated").create(), //
//      ofURL(ExternalAsset.MONA_LIB,
//          "https://mona.fiehnlab.ucdavis.edu/rest/downloads/retrieve/7609a87b-5df1-4343-afe9-2016a3e79516").version(
//          "LC-MS2 positive").create(), //
//      ofURL(ExternalAsset.MONA_LIB,
//          "https://mona.fiehnlab.ucdavis.edu/rest/downloads/retrieve/cd081cf8-6707-4dd3-bccc-48b1b14af859").version(
//          "LC-MS2 negative").create(), //

      // models
      ofZenodo(AssetGroup.MS2DEEPSCORE, "12628368").version("ms2deepscore-latest")
          .mainFileName("ms2deepscore_model_java.pt").create(),

      ofURL(AssetGroup.DREAMS,
              "https://huggingface.co/roman-bushuiev/DreaMS/resolve/main/DreaMS_embedding_model_torchscript.zip")
              .requiresUnzip(true)
              .mainFileName("DreaMS_embedding_model_torchscript.pt")
            .create()
  ));

  public static List<DownloadAsset> forAssetGroup(final AssetCategory group) {
    return ASSETS.stream().filter(a -> a.extAsset().getCategory() == group).toList();
  }
}
