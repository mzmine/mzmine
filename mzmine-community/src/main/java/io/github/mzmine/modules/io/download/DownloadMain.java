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

import io.github.mzmine.modules.io.download.DownloadMain.ProgressCallBack;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadMain implements ProgressCallBack {

  private static final Logger logger = Logger.getLogger(DownloadMain.class.getName());

  public static void main(String[] args) {
    new DownloadMain(
        "D:\\git\\mzmine3\\mzmine-community\\src\\main\\java\\io\\github\\mzmine\\modules\\io\\download\\download.txt",
        "https://raw.githubusercontent.com/mzmine/mzmine/master/mzmine-community/src/test/resources/rawdatafiles/additional/gc_orbi_a.mzML");
  }

  public DownloadMain(String localPath, String remoteURL) {
    try {
      URL url = URI.create(remoteURL).toURL();
      long size = contentLength(url);
      try (var rbc = new CallbackByteChannel(Channels.newChannel(url.openStream()), size, this); //
          var fos = new FileOutputStream(localPath)) {
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error during download: " + e.getMessage(), e);
    }
  }

  public void callback(CallbackByteChannel rbc, double progress) {
    System.out.println(rbc.getReadSoFar());
    System.out.println(progress);
  }

  long contentLength(URL url) {
    HttpURLConnection connection;
    long contentLength = -1;
    try {
      connection = (HttpURLConnection) url.openConnection();
//      connection.setRequestMethod("HEAD");
      contentLength = connection.getContentLengthLong();
    } catch (Exception e) {
    }
    return contentLength;
  }

  interface ProgressCallBack {

    public void callback(CallbackByteChannel rbc, double progress);
  }

  private class CallbackByteChannel implements ReadableByteChannel {

    ProgressCallBack delegate;
    long size;
    ReadableByteChannel rbc;
    long sizeRead;

    CallbackByteChannel(ReadableByteChannel rbc, long expectedSize, ProgressCallBack delegate) {
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
        progress = size > 0 ? (double) sizeRead / (double) size * 100.0 : -1.0;
        delegate.callback(this, progress);
      }
      return n;
    }
  }
}