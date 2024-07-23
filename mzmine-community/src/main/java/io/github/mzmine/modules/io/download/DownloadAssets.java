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
 * Definition of hard coded download assets
 */
public class DownloadAssets {

  /**
   * May be changed from outside to account for more downloadable assets
   */
  public static final List<DownloadAsset> ASSETS = new ArrayList<>(List.of(
      // tools
      new DownloadAsset(ExternalAsset.ThermoRawFileParser, "1.4.4", true, "ThermoRawFileParser.exe",
          "https://github.com/compomics/ThermoRawFileParser/releases/download/v1.4.4/ThermoRawFileParser1.4.4.zip"),
      // libraries
      new DownloadAsset(ExternalAsset.MSnLib, "20240411", true,
          "https://zenodo.org/api/records/11163381/files-archive")
      // models

      //
  ));

}
