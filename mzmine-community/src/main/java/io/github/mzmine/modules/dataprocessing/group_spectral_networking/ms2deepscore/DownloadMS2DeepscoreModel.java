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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.io.FileUtils;

public class DownloadMS2DeepscoreModel {

  /**
   * Downloads a file from a URL
   *
   * @param fileURL  HTTP URL of the file to be downloaded
   * @param saveFile path of the directory to save the file
   */
  private static void downloadFile(String fileURL, File saveFile) {
    if (!saveFile.exists()) {
      try {
        URL url = new URI(fileURL).toURL();
        FileUtils.copyURLToFile(url, saveFile);
      } catch (URISyntaxException | IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static File downloadSettings(File saveDirectory) {
    String fileURL = "https://zenodo.org/records/12628369/files/settings.json?download=1";
    File settingsFile = new File(saveDirectory, "ms2deepscore_model_settings.json");
    downloadFile(fileURL, settingsFile);
    return settingsFile;
  }

  public static File downloadModel(File saveDirectory) {
    String fileURL = "https://zenodo.org/records/12628369/files/ms2deepscore_model_java.pt?download=1";
    File modelFile = new File(saveDirectory, "ms2deepscore_model.pt");
    downloadFile(fileURL, modelFile);
    return modelFile;
  }
}