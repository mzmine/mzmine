/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.massvoltammogram.io;

import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramPlotPanel;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramScan;
import io.github.mzmine.util.javafx.FxThreadUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.commons.io.FilenameUtils;

public class MassvoltammogramExport {

  private MassvoltammogramExport() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Method to export the massvoltammogram to different file formats.
   *
   * @param massvoltammogram The massvoltammogram to be exported.
   */
  public static void exportPlot(Massvoltammogram massvoltammogram) {

    //Initializing a file chooser and a file to save the selected path to.
    final FileChooser fileChooser = new FileChooser();
    final AtomicReference<File> file = new AtomicReference<>(null);
    final FileChooser.ExtensionFilter extensionFilterPNG = new ExtensionFilter(
        "Portable Network Graphics", ".png");
    final FileChooser.ExtensionFilter extensionFilterCSV = new ExtensionFilter("CSV-File", ".csv");
    fileChooser.getExtensionFilters().add(extensionFilterCSV);
    fileChooser.getExtensionFilters().add(extensionFilterPNG);

    //Opening dialog to choose the path to save the png files to.
    FxThreadUtil.runOnFxThreadAndWait(() -> file.set(fileChooser.showSaveDialog(null)));
    if (file.get() == null) {
      return;
    }
    final String selectedFormat = FilenameUtils.getExtension(file.get().getName());

    if (selectedFormat.equals("csv")) {
      MassvoltammogramExport.toCSV(massvoltammogram, file.get());
    } else if (selectedFormat.equals("png")) {
      MassvoltammogramExport.toPNG(massvoltammogram, file.get());
    }
  }

  /**
   * Method to export the massvoltammogram to a png-file.
   *
   * @param massvoltammogram The massvoltammogram to be exported.
   * @param file             The path the massvoltammogram will be exported to.
   */
  private static void toPNG(Massvoltammogram massvoltammogram, File file) {

    MassvoltammogramPlotPanel plot = massvoltammogram.getPlot();

    //Extracting the buffered frame as an image.
    Image image = plot.createImage(plot.getWidth(), plot.getHeight());
    plot.paint(image.getGraphics());
    image = new ImageIcon(image).getImage();

    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
        BufferedImage.TYPE_INT_RGB);
    Graphics graphics = bufferedImage.createGraphics();
    graphics.drawImage(image, 0, 0, Color.WHITE, null);
    graphics.dispose();

    //Saving the buffered image to a png file.
    try {
      ImageIO.write(bufferedImage, "PNG", file);
    } catch (IllegalArgumentException | IOException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Method to export the massvoltammograms scan sto single csv-files.
   *
   * @param massvoltammogram The massvoltammogram to be exported.
   * @param file             the pht the massvoltammogram will be exported to.
   */
  private static void toCSV(Massvoltammogram massvoltammogram, File file) {

    //Getting the file name and path.
    final String fileName = FilenameUtils.removeExtension(file.getName());
    final String folderPath = FilenameUtils.removeExtension(file.getAbsolutePath());

    //Creating a new folder at the selected directory.
    try {
      Files.createDirectory(Paths.get(folderPath));
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }

    //Getting the data to export from the PlotPanel.
    List<MassvoltammogramScan> scans = massvoltammogram.getRawScansInMzRange();

    //Exporting the data to csv files.
    for (MassvoltammogramScan scan : scans) {

      //Initializing a file writer to export the csv file and naming the file.
      try (FileWriter writer = new FileWriter(
          folderPath + File.separator + fileName + "_" + scan.getPotential() + "_mV.csv")) {

        //Filling the csv file with all data from the scan.
        for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {
          writer.append(String.valueOf(scan.getMz(i))); //m/z-value
          writer.append(" ");
          writer.append(String.valueOf(scan.getIntensity(i))); //intensity-value
          writer.append("\n");
        }
        writer.flush();
      }
      //Handling the exception from the file writer.
      catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }
}
