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

package io.github.mzmine.modules.io.download;

import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assets that can be downloaded by {@link FileDownloadTask}. Usually defined in
 * {@link DownloadAssets} as versions of an {@link AssetGroup}
 */
public sealed interface DownloadAsset permits UrlDownloadAsset, ZenodoDownloadAsset {


  @Nullable String url();

  AssetGroup extAsset();

  String version();

  boolean requiresUnzip();

  /**
   * @return the main file name that should be picked or null
   */
  @Nullable String mainFileName();

  default String getDownloadDescription() {
    if (version() == null) {
      return "Download %s from %s".formatted(extAsset(), url());
    }
    return "Download %s version %s from %s".formatted(extAsset(), version(), url());
  }

  default String getLabel(boolean includeUrl) {
    var extAsset = extAsset().getLabel();
    if (!includeUrl || url() == null) {
      if (version() == null) {
        return extAsset;
      }
      return "%s (%s)".formatted(extAsset, version());
    }

    if (version() == null) {
      return "%s, %s".formatted(extAsset, url());
    }
    return "%s (%s), %s".formatted(extAsset, version(), url());
  }


  /**
   * Estimated file name as in download directory of {@link AssetGroup#getDownloadToDir()} and
   * mainFileName or url file name. If file is unzipped - then the final file name may be different.
   * In this case use the mainFileName to determine the final file.
   *
   * @return estimated filename based on download directory and mainFileName or URL
   */
  @NotNull List<File> getEstimatedFinalFiles();


  final class Builder<T extends DownloadAsset> {

    private final @NotNull AssetGroup extAsset;
    private @Nullable String version;
    private boolean requiresUnzip = false;
    private @Nullable String mainFileName = null;
    private @NotNull String fileNamePattern = ".*";
    // implementation specific fields
    private @Nullable String zenodoRecordId;
    private @Nullable String url;

    public Builder(@NotNull final AssetGroup extAsset) {
      this.extAsset = extAsset;
    }

    public static Builder<ZenodoDownloadAsset> ofZenodo(@NotNull AssetGroup extAsset,
        @NotNull String recordId) {
      Builder<ZenodoDownloadAsset> builder = new Builder<>(extAsset);
      builder.zenodoRecordId = recordId;
      return builder;
    }

    public static Builder<UrlDownloadAsset> ofURL(@NotNull AssetGroup extAsset,
        @NotNull String url) {
      Builder<UrlDownloadAsset> builder = new Builder<>(extAsset);
      builder.url = url;
      return builder;
    }

    public Builder<T> version(final @Nullable String version) {
      this.version = version;
      return this;
    }

    public Builder<T> requiresUnzip(final boolean requiresUnzip) {
      this.requiresUnzip = requiresUnzip;
      return this;
    }

    public Builder<T> mainFileName(final @Nullable String mainFileName) {
      this.mainFileName = mainFileName;
      return this;
    }

    public Builder<T> fileNamePattern(final @NotNull String fileNamePattern) {
      this.fileNamePattern = fileNamePattern;
      return this;
    }

    @SuppressWarnings("unchecked")
    public T create() {
      if (zenodoRecordId != null) {
        return (T) new ZenodoDownloadAsset(extAsset, version, requiresUnzip, mainFileName,
            zenodoRecordId, fileNamePattern);
      } else if (url != null) {
        return (T) new UrlDownloadAsset(extAsset, version, requiresUnzip, mainFileName, url);
      }
      throw new IllegalStateException(
          "Creation of download asset not handled, maybe new type of downloader?");
    }
  }

}
