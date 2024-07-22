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
import java.time.Instant;

/**
 * Task to download a single file. If task is cancelled or on error - file download is stopped and
 * already downloaded parts are discarded
 */
public class FileDownloadTask extends AbstractTask implements DownloadProgressCallback {

  private final String downloadUrl;
  private final String localFile;
  public volatile DownloadStatus downloadStatus = DownloadStatus.WAITING;
  public String description = "Waiting for download";
  private double progress;
  private FileDownloader fileDownloader;

  /**
   * @param downloadUrl    URL defines file to download
   * @param localDirectory this is the local path, a directory.
   */
  protected FileDownloadTask(String downloadUrl, String localDirectory) {
    super(Instant.now());
    this.downloadUrl = downloadUrl;
    this.localFile = localDirectory;
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
    fileDownloader = new FileDownloader(downloadUrl, localFile, this);
    fileDownloader.downloadFileBlocking();

    if (fileDownloader.getStatus() == DownloadStatus.SUCCESS) {
      setStatus(TaskStatus.FINISHED);
    } else {
      error("Error while downloading file " + localFile + " from " + downloadUrl);
    }
  }

  @Override
  public void onProgress(final long totalBytes, final long bytesRead, final double progress) {
    this.progress = progress;
    if (totalBytes <= 0) {
      description = "Downloading to %s (%.1f MB done)".formatted(localFile, toMB(bytesRead));
    }
    description = "Downloading to %s (%.1f / %.1f MB)".formatted(localFile, toMB(bytesRead),
        toMB(totalBytes));
  }

  private static float toMB(final long bytesRead) {
    return bytesRead / 1000000f;
  }


}
