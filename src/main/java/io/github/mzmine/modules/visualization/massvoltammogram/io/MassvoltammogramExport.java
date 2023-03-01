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

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramScan;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramUtils;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.ExtendedPlot3DPanel;
import io.github.mzmine.util.javafx.FxThreadUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FilenameUtils;

public class MassvoltammogramExport {

  public static void exportPlot(ExtendedPlot3DPanel plot) {

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
      MassvoltammogramExport.toCSV(plot, file.get());
    } else if (selectedFormat.equals("png")) {
      MassvoltammogramExport.toPNG(plot, file.get());
    }
  }

  public static void toPNG(ExtendedPlot3DPanel plot, File file) {

    //Saving the rendered picture to a png file.
    try {
      plot.toGraphicFile(file);
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }

  public static void toCSV(ExtendedPlot3DPanel plot, File file) {

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
    List<MassvoltammogramScan> scans = plot.getPlotData().getRawScansInMzRange();

    List<double[][]> scansAsArrays = new ArrayList<>();

    for (MassvoltammogramScan scan : scans) {

      scansAsArrays.add(scan.toArray());
    }

    //Exporting the data to csv files.
    for (double[][] scan : scansAsArrays) {

      //Initializing a file writer to export the csv file and naming the file.
      try (FileWriter writer = new FileWriter(
          folderPath + File.separator + fileName + "_" + scan[0][2] + "_mV.csv")) {

        //Filling the csv file with all data from the scan.
        for (double[] dataPoint : scan) {
          writer.append(String.valueOf(dataPoint[0])); //m/z-value
          writer.append(" ");
          writer.append(String.valueOf(dataPoint[1])); //intensity-value
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
