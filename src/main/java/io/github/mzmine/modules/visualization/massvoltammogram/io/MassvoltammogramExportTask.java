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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramPlotPanel;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramScan;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.javafx.FxThreadUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MassvoltammogramExportTask implements Task {

  //Export variables.
  private final Massvoltammogram massvoltammogram;  //The massvoltammogram that will be exported.
  private File file; //The file the massvoltammogram will be exported to

  //Task variables.
  private TaskStatus taskStatus;
  private List<TaskStatusListener> listeners;
  private String errorMessage = null;

  //Finished percentage.
  private int numExportedScans;
  private int numTotalScans;


  public MassvoltammogramExportTask(Massvoltammogram massvoltammogram) {

    //Setting the massvoltammogram to be exported.
    this.massvoltammogram = massvoltammogram;

    //Adding the ExportTask to the TaskController.
    MZmineCore.getTaskController().addTask(this, getTaskPriority());

    //Running the ExportTask
    setStatus(TaskStatus.PROCESSING);
  }

  /**
   * Method to export the massvoltammogram to different file formats.
   */
  public void run() {

    //Initializing a file chooser and a file to save the selected path to.
    final FileChooser fileChooser = new FileChooser();
    final AtomicReference<File> chosenFile = new AtomicReference<>(null);

    //Generating the extension filters.
    final FileChooser.ExtensionFilter extensionFilterPNG = new ExtensionFilter(
        "Portable Network Graphics", ".png");
    final FileChooser.ExtensionFilter extensionFilterCSV = new ExtensionFilter("CSV-File", ".csv");
    final FileChooser.ExtensionFilter extensionFilterXLSX = new ExtensionFilter("Excel-File",
        ".xlsx");
    fileChooser.getExtensionFilters()
        .addAll(Arrays.asList(extensionFilterCSV, extensionFilterXLSX, extensionFilterPNG));

    //Opening dialog to choose the path to save the file to.
    FxThreadUtil.runOnFxThreadAndWait(() -> chosenFile.set(fileChooser.showSaveDialog(null)));

    //Cancelling the task if no file was chosen.
    if (chosenFile.get() == null) {
      cancel();
      return;
    }
    this.file = chosenFile.get();

    //Exporting the massvoltammogram to the selected format.
    final String selectedFormat = FilenameUtils.getExtension(chosenFile.get().getName());
    switch (selectedFormat) {
      case "csv" -> toCSV();
      case "png" -> toPNG();
      case "xlsx" -> toXLSX();
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Method to export the massvoltammogram to a png-file.
   */
  private void toPNG() {

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
   */
  private void toCSV() {

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

    //Setting the total number of scans to calculate the finished percentage.
    numTotalScans = scans.size();

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

      //Updating the number of exported scans.
      numExportedScans++;
    }
  }

  /**
   * Method to export the massvoltammograms data to a single xlsx-file.
   */
  private void toXLSX() {

    //Creating an excel-workbook.
    try (XSSFWorkbook xlsxWorkbook = new XSSFWorkbook()) {
      XSSFSheet sheet = xlsxWorkbook.createSheet("Massvoltammogram");

      //Getting all the massvoltammograms scans.
      List<MassvoltammogramScan> scans = massvoltammogram.getRawScansInMzRange();

      //Setting the total number of scans to calculate the finished percentage.
      numTotalScans = scans.size();

      //Adding the potential values to the first row.
      Row firstRow = sheet.createRow(0);
      int firstRowCellCounter = 1;
      for (MassvoltammogramScan scan : scans) {

        Cell potentialCell = firstRow.createCell(firstRowCellCounter);
        potentialCell.setCellValue(scan.getPotential());

        firstRowCellCounter += 2;
      }

      //Writing the intensity and mz-values of all MassvoltammogramScans to the excel-file
      int columnCounter = 0;
      for (MassvoltammogramScan scan : scans) {

        //Going over all datapoints in the given scan.
        for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {

          //Adding the datapoint to an already existing row in the excel-file.
          if (sheet.getRow(i + 1) != null) {
            Row row = sheet.getRow(i + 1);
            addDatapointToExcelRow(scan, row, columnCounter, i);
          }

          //Adding a new row in the excel-file for the data if no row is found.
          else {
            Row row = sheet.createRow(i + 1);
            addDatapointToExcelRow(scan, row, columnCounter, i);
          }
        }
        //Increasing the column counter.
        columnCounter += 2;

        //Updating the number of exported scans.
        numExportedScans++;
      }

      //Writing the excel-file to disk.
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      xlsxWorkbook.write(fileOutputStream);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method to add a datapoint of a MassvoltammogramScan to an excel-row.
   *
   * @param scan           The MassvoltammogramScan.
   * @param row            The excel-row.
   * @param columnNumber   The number of the column the datapoint will be added to.
   * @param datapointIndex The index of the datapoint in the MassvoltammogramScan.
   */
  private void addDatapointToExcelRow(MassvoltammogramScan scan, Row row, int columnNumber,
      int datapointIndex) {

    //Creating the cells.
    Cell mzCell = row.createCell(columnNumber);
    Cell intensityCell = row.createCell(columnNumber + 1);

    //Adding the data.
    mzCell.setCellValue(scan.getMz(datapointIndex));
    intensityCell.setCellValue(scan.getIntensity(datapointIndex));
  }

  @Override
  public String getTaskDescription() {
    return "Exporting the massvoltammogram to disk.";
  }

  @Override
  public double getFinishedPercentage() {

    if (numTotalScans > 0) {

      return (double) numExportedScans / (double) numTotalScans;
    } else {
      return 0;
    }
  }

  @Override
  public TaskStatus getStatus() {
    return taskStatus;
  }

  public final void setStatus(TaskStatus newStatus) {
    TaskStatus oldStatus = taskStatus;
    this.taskStatus = newStatus;
    if (listeners != null && !taskStatus.equals(oldStatus)) {
      for (TaskStatusListener listener : listeners) {
        listener.taskStatusChanged(this, taskStatus, oldStatus);
      }
    }
  }

  @Override
  public final String getErrorMessage() {
    return errorMessage;
  }

  public final void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  @Override
  public void cancel() {
    setStatus(TaskStatus.CANCELED);
  }

  @Override
  public void addTaskStatusListener(TaskStatusListener list) {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    listeners.add(list);
  }

  @Override
  public boolean removeTaskStatusListener(TaskStatusListener list) {
    if (listeners != null) {
      return listeners.remove(list);
    } else {
      return false;
    }
  }

  @Override
  public void clearTaskStatusListener() {
    if (listeners != null) {
      listeners.clear();
    }
  }
}
