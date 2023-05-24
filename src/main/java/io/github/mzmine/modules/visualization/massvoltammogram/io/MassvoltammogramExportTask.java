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

import com.itextpdf.text.DocumentException;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramPlotPanel;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramScan;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.swing.SwingExportUtil;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MassvoltammogramExportTask extends AbstractTask {

  //Logger.
  private static final Logger logger = Logger.getLogger(MassvoltammogramExportTask.class.getName());

  //Export variables.
  private final Massvoltammogram massvoltammogram;  //The massvoltammogram that will be exported.
  private final String filePath; //The path the massvoltammogram will be exported to.
  private final String fileFormat;  //The file format the massvoltammogram will be exported to.
  private final Boolean bgTransparent;

  //Progress.
  private int numExportedScans;
  private int numTotalScans;


  public MassvoltammogramExportTask(Massvoltammogram massvoltammogram,
      MassvoltammogramExportParameters parameters) {
    super(null, Instant.now());

    this.massvoltammogram = massvoltammogram;
    this.filePath = parameters.getValue(MassvoltammogramExportParameters.path).getAbsolutePath();
    this.fileFormat = parameters.getValue(MassvoltammogramExportParameters.fileFormat);
    this.bgTransparent = parameters.getValue(MassvoltammogramExportParameters.transparency);

    run();
  }

  public void run() {

    //Starting the export.
    setStatus(TaskStatus.PROCESSING);

    switch (fileFormat) {
      case "CSV" -> toCSV();
      case "XLSX" -> toXLSX();
      case "PNG" -> toPNG();
      case "JPG" -> toJPG();
      case "EMF" -> toEMF();
      case "PDF" -> toPDF();
      default -> cancel();
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Method to export the massvoltammogram to a png-file.
   */
  private void toPNG() {

    MassvoltammogramPlotPanel plot = massvoltammogram.getPlot();

    //Setting the background transparent if chosen.
    plot.setBackgroundTransparent(bgTransparent);

    //Writing the plot to a buffered image
    BufferedImage img = new BufferedImage(plot.getWidth(), plot.getHeight(),
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = img.createGraphics();
    plot.paint(graphics);
    graphics.drawImage(img, 0, 0, null);
    graphics.dispose();

    //Saving the buffered image to a png file.
    File file = new File(filePath + ".png");
    try {
      ImageIO.write(img, "PNG", file);
    } catch (IllegalArgumentException | IOException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    plot.setBackgroundTransparent(false);
  }

  /**
   * Method to export the massvoltammogram to a jpg-file.
   */
  private void toJPG() {

    MassvoltammogramPlotPanel plot = massvoltammogram.getPlot();

    //Writing the plot to a buffered image
    BufferedImage img = new BufferedImage(plot.getWidth(), plot.getHeight(),
        BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = img.createGraphics();
    plot.paint(graphics);
    graphics.drawImage(img, 0, 0, null);
    graphics.dispose();

    //Saving the buffered image to a png file.
    File file = new File(filePath + ".jpg");
    try {
      ImageIO.write(img, "JPG", file);
    } catch (IllegalArgumentException | IOException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  /**
   * Method to export the massvoltammograms scan sto single csv-files.
   */
  private void toCSV() {

    //Getting the file name.
    final String fileName = FilenameUtils.getName(filePath);

    //Creating a new folder at the selected directory.
    try {
      Files.createDirectory(Paths.get(filePath));
    } catch (IOException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    //Getting the data to export from the PlotPanel.
    List<MassvoltammogramScan> scans = massvoltammogram.getRawScansInMzRange();

    //Setting the total number of scans to calculate the finished percentage.
    numTotalScans = scans.size();

    //Exporting the data to csv files.
    for (MassvoltammogramScan scan : scans) {

      //Initializing a file writer to export the csv file and naming the file.
      try (FileWriter writer = new FileWriter(
          filePath + File.separator + fileName + "_" + scan.getPotential() + "_mV.csv")) {

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
      catch (IOException e) {
        e.printStackTrace();
        logger.log(Level.WARNING, e.getMessage(), e);
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
      File file = new File(filePath + ".xlsx");
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      xlsxWorkbook.write(fileOutputStream);

    } catch (IOException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  /**
   * Method to export the massvoltammogram to a emf-file.
   */
  private void toEMF() {

    File file = new File(filePath + ".emf");
    try {
      SwingExportUtil.writeToEMF(massvoltammogram.getPlot(), file);

    } catch (IOException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage());
    }
  }

  /**
   * Method to export the massvoltammogram to a pdf-file.
   */
  private void toPDF() {

    File file = new File(filePath + ".pdf");
    try {
      SwingExportUtil.writeToPDF(massvoltammogram.getPlot(), file);

    } catch (IOException | DocumentException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, e.getMessage());
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
    return "Exporting the massvoltammogram.";
  }

  /**
   * @return Returns the progress of the export. Returns 0 if no progress could be calculated.
   */
  @Override
  public double getFinishedPercentage() {

    if (numTotalScans > 0) {
      return (double) numExportedScans / (double) numTotalScans;
    } else {
      return 0;
    }
  }
}