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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ZipUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task to download a single file. If task is cancelled or on error - file download is stopped and
 * already downloaded parts are discarded. Optionally unzips file and deletes the zip as defined in
 * {@link DownloadAsset}
 */
public class FileDownloadTask extends AbstractTask implements DownloadProgressCallback {

  private final String downloadUrl;
  private final File localDirectory;
  @Nullable
  private DownloadAsset asset;

  public volatile DownloadStatus downloadStatus = DownloadStatus.WAITING;
  public String description = "Waiting for download";
  private double progress;
  private FileDownloader fileDownloader;

  // resulting file after download and optional unzip
  private List<File> downloadedFiles;


  public FileDownloadTask(final DownloadAsset asset) {
    this(asset.url(), asset.extAsset().getDownloadToDir());
    this.asset = asset;
  }

  /**
   * @param downloadUrl    URL defines file to download
   * @param localDirectory this is the local path, a directory.
   */
  public FileDownloadTask(String downloadUrl, String localDirectory) {
    this(downloadUrl, new File(localDirectory));
  }

  /**
   * @param downloadUrl    URL defines file to download
   * @param localDirectory this is the local path, a directory.
   */
  public FileDownloadTask(String downloadUrl, File localDirectory) {
    super(Instant.now());
    this.downloadUrl = downloadUrl;
    this.localDirectory = localDirectory;
    addTaskStatusListener((_, _, _) -> {
      if (downloadStatus == DownloadStatus.CANCEL_REMOVE) {
        // already removing
        return;
      }
      if (isCanceled()) {
        cancelDownloadAndRemoveFile();
      }
    });
  }

  public void cancelDownloadAndRemoveFile() {
    downloadStatus = DownloadStatus.CANCEL_REMOVE;
    if (!isCanceled()) {
      cancel();
    }
    fileDownloader.cancel();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // download file and block thread. Still reacts to task status changes
    fileDownloader = new FileDownloader(downloadUrl, localDirectory, this);
    fileDownloader.downloadFileBlocking();

    if (fileDownloader.getStatus() == DownloadStatus.SUCCESS) {
      downloadedFiles = List.of(fileDownloader.getLocalFile());

      // requires unzip?
      if (asset != null && asset.requiresUnzip()) {
        unzipFile();
      }

      setStatus(TaskStatus.FINISHED);
    } else if (fileDownloader.getStatus() == DownloadStatus.ERROR_REMOVE) {
      error("Error while downloading file " + localDirectory.getAbsolutePath() + " from "
            + downloadUrl);
    }
  }

  private void unzipFile() {
    File fileName = fileDownloader.getLocalFile();
    if (fileName == null) {
      return;
    }

    description = "Unzipping file " + fileName;

    File directory = fileName.getParentFile();

    try (var zis = new ZipInputStream(new FileInputStream(fileName))) {
      downloadedFiles = ZipUtils.unzipStream(zis, directory);
    } catch (FileNotFoundException e) {
      error("File not found Error while unzipping file " + fileName + " from " + downloadUrl, e);
      return;
    } catch (IOException e) {
      error("IOError while unzipping file " + fileName + " from " + downloadUrl, e);
      return;
    }

    // remove original file
    fileName.delete();
  }

  @Override
  public void onProgress(final long totalBytes, final long bytesRead, final double progress) {
    this.progress = progress;
    if (totalBytes <= 0) {
      description = "Downloading to %s (%.1f MB done)".formatted(localDirectory, toMB(bytesRead));
    } else {
      description = "Downloading to %s (%.1f / %.1f MB)".formatted(localDirectory, toMB(bytesRead),
          toMB(totalBytes));
    }
  }

  private static float toMB(final long bytesRead) {
    return bytesRead / 1000000f;
  }

  /**
   * @return List of downloaded files. After unzipping this may be multiple files
   */
  @NotNull
  public List<File> getDownloadedFiles() {
    return downloadedFiles;
  }
}
