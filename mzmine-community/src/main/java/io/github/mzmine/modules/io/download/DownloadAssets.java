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

package io.github.mzmine.modules.io.download;

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
      new DownloadAsset(ExternalAsset.MSnLib, "20240411-full", true,
          "https://zenodo.org/api/records/11163381/files-archive"),
      new DownloadAsset(ExternalAsset.MSnLib, "20240411-ms2", false,
          "https://zenodo.org/records/11163381/files/20231031_nihnp_library_neg_all_lib_MS2.mgf?download=1"),
      new DownloadAsset(ExternalAsset.MSnLib, "20240411-msn", false,
          "https://zenodo.org/records/11163381/files/20231031_nihnp_library_neg_all_lib_MSn.mgf?download=1")
      // models

      //
  ));

}
