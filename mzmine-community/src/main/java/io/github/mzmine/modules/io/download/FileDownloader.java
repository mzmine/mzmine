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

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 * Download a single file. If stopped then removes downloaded file. Preferred use by
 * {@link FileDownloadTask}
 */
public class FileDownloader {

  private static final Logger logger = Logger.getLogger(FileDownloader.class.getName());
  private static final long PACKAGE_BYTE_SIZE = 10_000_000L;
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(FileDownloader.class);
  private final String downloadUrl;
  private final File localFile;
  private final DownloadProgressCallback progressCallback;
  private DownloadStatus status = DownloadStatus.WAITING;

  /**
   * @param downloadUrl      remote file URL to download
   * @param localDirectory   local file directory to copy file into
   * @param progressCallback called on each progress change
   */
  public FileDownloader(String downloadUrl, String localDirectory,
      DownloadProgressCallback progressCallback) {
    this(downloadUrl, new File(localDirectory), progressCallback);
  }

  /**
   * @param downloadUrl      remote file URL to download
   * @param localDirectory   local file directory to copy file into
   * @param progressCallback called on each progress change
   */
  public FileDownloader(String downloadUrl, File localDirectory,
      DownloadProgressCallback progressCallback) {
    this.downloadUrl = downloadUrl;

    // strip query parameters from URL with split at ?
    String fileName = FileAndPathUtil.getFileNameFromUrl(downloadUrl);
    this.localFile = new File(localDirectory, fileName);
    this.progressCallback = progressCallback;
  }

  public void downloadFileBlocking() {
    try {
      FileAndPathUtil.createDirectory(localFile.getParentFile());
      URL url = URI.create(downloadUrl).toURL();
      long totalBytes = contentLength(url);
      try (var rbc = new CountingByteChannel(Channels.newChannel(url.openStream()), totalBytes,
          progressCallback); //
          var fos = new FileOutputStream(localFile)) {
        setStatus(DownloadStatus.DOWNLOADING);

        log.info("Starting to download %s to %s".formatted(downloadUrl, localFile));
        // transfer packages to react to potential cancel calls
        int currentPackage = 0;
        long actuallyReadBytes = 0;
        do {
          if (status.isRemove()) {
            break;
          }
          actuallyReadBytes = fos.getChannel()
              .transferFrom(rbc, currentPackage * PACKAGE_BYTE_SIZE, PACKAGE_BYTE_SIZE);
          currentPackage++;
        } while (actuallyReadBytes > 0);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error during download: " + e.getMessage(), e);
      setStatus(DownloadStatus.ERROR_REMOVE);
    }
    // handle the final status
    if (status.isRemove()) {
      tryCleanLocalFile(localFile);
    } else if (status == DownloadStatus.DOWNLOADING) {
      setStatus(DownloadStatus.SUCCESS);
    }
  }

  private void tryCleanLocalFile(final File localFile) {
    try {
      // clean up file if download was interrupted
      Files.deleteIfExists(localFile.toPath());
      logger.info(
          "Successfully deleted temporary download file after download was stopped: " + status);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to delete file from unfinished download: " + localFile, e);
    }
  }

  public File getLocalFile() {
    return localFile;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void cancel() {
    log.info("Cancelling file download " + localFile);
    setStatus(DownloadStatus.CANCEL_REMOVE);
  }

  private void setStatus(final DownloadStatus status) {
    if (this.status.isRemove()) {
      return; // cannot change away from error or cancel
    }
    this.status = status;
  }

  public DownloadStatus getStatus() {
    return status;
  }

  long contentLength(URL url) {
    HttpURLConnection connection;
    long contentLength = -1;
    try {
      connection = (HttpURLConnection) url.openConnection();
//      connection.setRequestMethod("HEAD");
      contentLength = connection.getContentLengthLong();
    } catch (Exception e) {
      logger.log(Level.INFO, "Cannot read file size. Will download without progress view: " + url,
          e);
    }
    return contentLength;
  }

  private static class CountingByteChannel implements ReadableByteChannel {

    private final DownloadProgressCallback delegate;
    long size;
    ReadableByteChannel rbc;
    long sizeRead;

    CountingByteChannel(ReadableByteChannel rbc, long expectedSize,
        DownloadProgressCallback delegate) {
      this.delegate = delegate;
      this.size = expectedSize;
      this.rbc = rbc;
    }

    public void close() throws IOException {
      rbc.close();
    }

    public long getReadSoFar() {
      return sizeRead;
    }

    public boolean isOpen() {
      return rbc.isOpen();
    }

    public int read(ByteBuffer bb) throws IOException {
      int n;
      double progress;
      if ((n = rbc.read(bb)) > 0) {
        sizeRead += n;
        progress = size > 0 ? (double) sizeRead / (double) size : -1.0;
        delegate.onProgress(size, sizeRead, progress);
      }
      return n;
    }
  }
}