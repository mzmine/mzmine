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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.modules.io.download.DownloadUtils.HandleExistingFiles;
import io.github.mzmine.modules.io.download.ZenodoRecord.ZenFile;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.web.HttpResponseException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task to download a single file. If task is cancelled or on error - file download is stopped and
 * already downloaded parts are discarded. Optionally unzips file and deletes the zip as defined in
 * {@link DownloadAsset}
 */
public class FileDownloadTask extends AbstractTask implements DownloadProgressCallback {

  private static final Logger logger = Logger.getLogger(FileDownloadTask.class.getName());
  private final File localDirectory;
  // sometimes URL may be specified during runtime like for zenodo we need to grab the latest url
  @Nullable
  private final String downloadUrl;
  @Nullable
  private DownloadAsset asset;

  public volatile DownloadStatus downloadStatus = DownloadStatus.WAITING;
  public String description = "Waiting for download";
  private int totalFiles = 1;
  private int finishedFiles = 0;
  private double progress;
  private FileDownloader fileDownloader;

  // resulting file after download and optional unzip
  private List<File> downloadedFiles;
  // skip files
  private List<File> skipFiles = List.of();


  public FileDownloadTask(final DownloadAsset asset) {
    this(asset, asset.extAsset().getDownloadToDir());
  }

  public FileDownloadTask(final DownloadAsset asset, final File downloadToDir) {
    this(asset.url(), downloadToDir);
    this.asset = asset;
  }

  /**
   * @param downloadUrl    URL defines file to download
   * @param localDirectory this is the local path, a directory.
   */
  public FileDownloadTask(@Nullable String downloadUrl, String localDirectory) {
    this(downloadUrl, new File(localDirectory));
  }

  /**
   * @param downloadUrl    URL defines file to download
   * @param localDirectory this is the local path, a directory.
   */
  public FileDownloadTask(@Nullable String downloadUrl, File localDirectory) {
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
    if (fileDownloader != null) {
      fileDownloader.cancel();
    }
    if (downloadedFiles != null && !downloadedFiles.isEmpty()) {
      for (final File file : downloadedFiles) {
        try {
          file.delete();
        } catch (Exception ex) {
        }
      }
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    // progress might be -1
    return (finishedFiles + Math.max(0, progress)) / (double) totalFiles;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (asset instanceof ZenodoDownloadAsset zenodoAsset) {
      downloadedFiles = downloadFromZenodoBlocking(zenodoAsset);
    } else {
      downloadFromUrl();
    }
    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  /**
   * Download from URL and unzip if defined in asset
   */
  private void downloadFromUrl() {
    // download file and block thread. Still reacts to task status changes
    fileDownloader = new FileDownloader(downloadUrl, localDirectory, this);
    fileDownloader.downloadFileBlocking();

    if (fileDownloader.getStatus() == DownloadStatus.SUCCESS) {
      downloadedFiles = List.of(fileDownloader.getLocalFile());

      // requires unzip?
      if (asset != null && asset.requiresUnzip()) {
        unzipFile(fileDownloader.getLocalFile());
      }
    } else if (fileDownloader.getStatus() == DownloadStatus.ERROR_REMOVE) {
      error("Error while downloading file " + localDirectory.getAbsolutePath() + " from "
          + downloadUrl);
    }
  }

  private List<File> downloadFromZenodoBlocking(final ZenodoDownloadAsset asset) {
    try {
      List<File> resultingFiles = new ArrayList<>();
      var record = ZenodoService.getWebRecord(asset);
      if (!asset.isAllFiles()) {
        record = record.applyNameFilter(asset.fileNameRegEx());
        if (record.files().isEmpty()) {
          logger.warning(
              "No files in zenodo record %s matching pattern %s. Check out: %s".formatted(
                  record.recid(), asset.fileNameRegEx(), record.links().selfHtml()));
          return List.of();
        }
      }

      // already downloaded files
      List<File> existing = record.files().stream().map(f -> new File(localDirectory, f.name()))
          .filter(File::exists).toList();
      boolean skipExisting = false;
      if (!existing.isEmpty()) {
        var result = DownloadUtils.showUseExistingFilesDialog(existing);
        skipExisting = result == HandleExistingFiles.USE_EXISTING;
        if (!skipExisting) {
          // download all again
          existing = List.of();
        }
      }
      if (skipExisting) {
        if (existing.size() == record.files().size()) {
          // all are existing
          return existing;
        }
      }
      setSkipFiles(existing);

      // never download full archive if some files already exist
      if (asset.isAllFiles() && existing.isEmpty()) {
        resultingFiles.addAll(downloadZenodoArchive(record));
      } else {
        // download files individually after file filter
        totalFiles = record.files().size();
        int countMatchingFiles = 0;
        for (final ZenFile file : record.files()) {
          File destination = new File(localDirectory, file.name());
          if (existing.contains(destination)) {
            continue; // skip existing
          }
          countMatchingFiles++;
          if (isCanceled()) {
            return List.of();
          }
          fileDownloader = new FileDownloader(file.link(), localDirectory, this, file.name());
          fileDownloader.downloadFileBlocking();
          finishedFiles++;
          progress = 0;
          if (asset.requiresUnzip()) {
            resultingFiles.addAll(unzipFile(fileDownloader.getLocalFile()));
          } else {
            resultingFiles.add(fileDownloader.getLocalFile());
          }
        }
        if (countMatchingFiles == 0) {
          logger.warning("No files in zenodo record %s downloaded".formatted(record.recid()));
        }
      }

      return resultingFiles;
    } catch (IOException | InterruptedException | HttpResponseException e) {
      error("Error retrieving zendodo record: " + asset.recordId());
      return List.of();
    }
  }

  /**
   * Download whole archive and unzip
   */
  private List<File> downloadZenodoArchive(final ZenodoRecord record) {
    fileDownloader = new FileDownloader(record.getArchiveZipUrl(), localDirectory, this);
    fileDownloader.downloadFileBlocking();
    if (fileDownloader.getStatus() == DownloadStatus.SUCCESS) {
      return unzipFile(fileDownloader.getLocalFile());
    } else if (fileDownloader.getStatus() == DownloadStatus.ERROR_REMOVE) {
      error("Error while downloading file " + localDirectory.getAbsolutePath() + " from "
            + downloadUrl);
    }
    return List.of();
  }

  private List<File> unzipFile(final File fileName) {
    if (fileName == null) {
      return List.of();
    }

    description = "Unzipping file " + fileName;

    File directory = fileName.getParentFile();

    List<File> files;
    try {
      files = ZipUtils.unzipFile(fileName, directory);
      downloadedFiles = files;
    } catch (FileNotFoundException e) {
      error("File not found Error while unzipping file " + fileName + " from " + downloadUrl, e);
      return List.of();
    } catch (IOException e) {
      error("IOError while unzipping file " + fileName + " from " + downloadUrl, e);
      return List.of();
    }

    // remove original file
    try {
      fileName.delete();
    } catch (Exception e) {
    }
    return requireNonNullElse(files, List.of());
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
    List<File> files = new ArrayList<>();
    if (skipFiles != null) {
      files.addAll(skipFiles);
    }
    if (downloadedFiles != null) {
      files.addAll(downloadedFiles);
    }
    return files;
  }

  public void setSkipFiles(final List<File> existing) {
    skipFiles = requireNonNullElse(existing, List.of());
  }
}
