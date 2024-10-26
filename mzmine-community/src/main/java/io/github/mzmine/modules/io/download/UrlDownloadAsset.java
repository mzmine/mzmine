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
 * @param url           the download URL
 */
public record UrlDownloadAsset(@NotNull ExternalAsset extAsset, String version,
                               boolean requiresUnzip, @Nullable String mainFileName,
                               String url) implements DownloadAsset {

  public UrlDownloadAsset(final ExternalAsset extAsset, final String version,
      final boolean requiresUnzip, final String url) {
    this(extAsset, version, requiresUnzip, null, url);
  }
}
