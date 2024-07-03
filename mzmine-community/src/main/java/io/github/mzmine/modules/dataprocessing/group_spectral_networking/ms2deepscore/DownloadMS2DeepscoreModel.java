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
import java.nio.file.Paths;
import java.util.Objects;
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

  private static File getResourceModelFile(String fileName) {
    URI modelsFolder;
    try {
      modelsFolder = Objects.requireNonNull(
          DownloadMS2DeepscoreModel.class.getClassLoader().getResource("models")).toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    File ModelsFolderPath = Paths.get(modelsFolder).toFile();
    return new File(ModelsFolderPath, fileName);
  }

  public static File download_settings() {
    String fileURL = "https://zenodo.org/records/12628369/files/settings.json?download=1";
    File file = getResourceModelFile("ms2deepscore_model_settings.json");
    downloadFile(fileURL, file);
    System.out.println(file);
    return file;
  }

  public static File download_model() {
    String fileURL = "https://zenodo.org/records/12628369/files/ms2deepscore_model_java.pt?download=1";
    File file = getResourceModelFile("ms2deepscore_model.pt");
    downloadFile(fileURL, file);
    return file;
  }
}