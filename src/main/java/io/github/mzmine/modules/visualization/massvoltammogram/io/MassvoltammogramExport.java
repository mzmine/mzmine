/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package io.github.mzmine.modules.visualization.massvoltammogram.io;

import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramScan;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.ExtendedPlot3DPanel;
import io.github.mzmine.util.javafx.FxThreadUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FilenameUtils;

public class MassvoltammogramExport {

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
  public static void toPNG(Massvoltammogram massvoltammogram, File file) {

    ExtendedPlot3DPanel plot = massvoltammogram.getPlot();

    //Saving the rendered picture to a png file.
    try {
      plot.toGraphicFile(file);
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }

  /**
   * Method to export the massvoltammograms scan sto single csv-files.
   *
   * @param massvoltammogram The massvoltammogram to be exported.
   * @param file             the pht the massvoltammogram will be exported to.
   */
  public static void toCSV(Massvoltammogram massvoltammogram, File file) {

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
