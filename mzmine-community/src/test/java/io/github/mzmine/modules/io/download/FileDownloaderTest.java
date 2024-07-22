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

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class FileDownloaderTest {

  private static final Logger logger = Logger.getLogger(FileDownloaderTest.class.getName());

  @Test
  void testDownload() {
    var fileDownloader = new FileDownloader(
        "https://raw.githubusercontent.com/mzmine/mzmine/master/mzmine-community/src/test/resources/rawdatafiles/additional/gc_orbi_a.mzML",
        // change output folder
        "testdownload/folder/", (totalBytes, bytesRead, progress) -> {
      String description = "Downloading to %s (%.1f / %.1f MB done)".formatted("test",
          bytesRead / 1000000f, totalBytes / 1000000f);
      System.out.println(description);
    });

    fileDownloader.downloadFileBlocking();

    var localFile = Path.of(fileDownloader.getLocalFile()).toFile();
    Assertions.assertTrue(localFile.exists());
    try {
      FileUtils.delete(localFile);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to delete local file " + localFile.getAbsolutePath() + "\n" + e.getMessage(), e);
    }
  }

}