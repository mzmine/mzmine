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

import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assets that can be downloaded by {@link FileDownloadTask}. Usually defined in
 * {@link DownloadAssets}
 *
 * @param extAsset      the external asset that defines additional fixed properties of this asset
 * @param version       the version
 * @param requiresUnzip unzip after download
 * @param mainFileName  the main file that is selected after unziping. Otherwise the first file
 * @param recordId      the zenodo record ID, prefer latest ID which can be extracted from zenodo
 *                      the Cite all versions DOI on each record: for 10.5281/zenodo.12628368 the
 *                      record ID would be 12628368 which points to the latest version.
 */
public record ZenodoDownloadAsset(@NotNull ExternalAsset extAsset, @Nullable String version,
                                  boolean requiresUnzip, @Nullable String mainFileName,
                                  @NotNull String recordId,
                                  @NotNull String fileNameRegEx) implements DownloadAsset {

  /**
   * @return true if all files should be downloaded
   */
  public boolean isAllFiles() {
    return StringUtils.isBlank(fileNameRegEx) || fileNameRegEx.equals(".*");
  }

  /**
   * The zenodo record api URL
   *
   * @return
   */
  @Override
  public String url() {
    return "https://zenodo.org/api/records/" + recordId;
  }

  public String getDownloadDescription() {
    if (version == null) {
      return "Download %s from Zenodo record %s".formatted(extAsset, recordId);
    }
    return "Download %s version %s from Zenodo record %s".formatted(extAsset, version, recordId);
  }

  public String getLabel(boolean includeUrl) {
    if (version == null) {
      return "%s, Zenodo: %s".formatted(extAsset, recordId);
    }
    return "%s (%s), Zenodo: %s".formatted(extAsset, version, recordId);
  }

  /**
   * Estimated file name as in download directory of {@link ExternalAsset#getDownloadToDir()} and
   * mainFileName or url file name. If file is unzipped - then the final file name may be different.
   * In this case use the mainFileName to determine the final file.
   *
   * @return estimated filename based on download directory and mainFileName or URL
   */
  @NotNull
  public File getEstimatedFinalFile() {
    File dir = extAsset.getDownloadToDir();
    if (mainFileName != null) {
      return new File(dir, mainFileName);
    }
    var fileName = FileAndPathUtil.getFileNameFromUrl(recordId);
    return new File(dir, fileName);
  }
}
