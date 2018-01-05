/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Vector;
import javax.swing.JFileChooser;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.files.FileTypeFilter;

public class XSSFExcelWriterReader {
  // static
  private static JFileChooser chooser;

  // ###################################################################################
  // READER STUFF
  private Vector<InputStream> listInStream = new Vector<InputStream>();
  private Vector<XSSFWorkbook> listWb = new Vector<XSSFWorkbook>();

  /**
   * Reads the given .xlsx-file and returns a Workbook
   *
   * @param file the file to open
   * @return a XSSFWorkbook or null
   * @see
   */
  public XSSFWorkbook openExistingWorkbook(File file) throws InvalidFormatException, IOException {
    // Get the workbook instance for XLS file
    InputStream inputFS = new FileInputStream(file);
    if (inputFS != null) {
      XSSFWorkbook workbook = new XSSFWorkbook(inputFS);
      // in list
      listInStream.add(inputFS);
      listWb.add(workbook);
      // return
      return workbook;
    } else
      return null;
  }

  /**
   * Closes the given workbook (Not saving!)
   *
   * @param wb workbook to close
   * @return true if succeed
   * @return false if not
   * @see
   */
  public boolean closeWorkbook(XSSFWorkbook wb) throws InvalidFormatException, IOException {
    //
    for (int i = 0; i < listWb.size(); i++) {
      if (listWb.get(i) == wb) {
        listWb.remove(i);
        listInStream.remove(i).close();
        return true;
      }
    }
    return false;
  }

  /**
   * Closes all loaded workbooks (Not saving!)
   * 
   * @return true if succeed
   * @return false if not
   * @see
   */
  public boolean closeAllWorkbooks() throws InvalidFormatException, IOException {
    //
    for (int i = 0; i < listWb.size(); i++) {
      listWb.remove(i);
      listInStream.remove(i).close();
    }
    return false;
  }


  // ###################################################################################
  // WRITER STUFF

  /**
   * Writes the data to a cell and returns the cell.
   *
   * @param sheet the sheet to write on
   * @param icol index of column
   * @param irow index of row
   * @param data data to write in cell (String, Date, Boolean, Double, Float, Integer, Number)
   * @return the given cell at icol;irow
   * @see
   */
  public Cell writeToCell(XSSFSheet sheet, int icol, int irow, Object data) {
    Cell cell = getCell(sheet, icol, irow);
    // write data
    if (data instanceof Date)
      cell.setCellValue((Date) data);
    else if (data instanceof Boolean)
      cell.setCellValue((Boolean) data);
    else if (data instanceof String)
      cell.setCellValue((String) data);
    else if ((data instanceof Double))
      cell.setCellValue((Double) data);
    else if ((data instanceof Number))
      cell.setCellValue(((Number) data).doubleValue());
    else if ((data instanceof Integer))
      cell.setCellValue(((Integer) data).doubleValue());
    else if ((data instanceof Float))
      cell.setCellValue(((Float) data).doubleValue());
    return cell;
  }

  /**
   * Returns the cell of a given sheet at icol;irow. Former values are overwritten.
   *
   * @param sheet the sheet to write on
   * @param icol index of column
   * @param irow index of row
   * @return the given cell at icol;irow
   * @see
   */
  public Cell getCell(XSSFSheet sheet, int icol, int irow) {
    // try to get row
    Row row = sheet.getRow(irow);
    // if not exist: create row
    if (row == null)
      row = sheet.createRow(irow);
    // get cell
    return row.createCell(icol);
  }

  /**
   * Returns the index of the first empty column
   *
   * @param sheet the sheet to write on
   * @return a column index
   * @see
   */
  public int getFirstEmptyColumn(XSSFSheet sheet) {
    Row row = sheet.getRow(0);
    if (row == null)
      return 0;
    int lastcoll = row.getLastCellNum();
    return (lastcoll == -1) ? 0 : lastcoll;
  }

  /**
   * Returns an existing sheet. If there is no sheet with this name a new will be created
   * 
   * @param name name of sheet
   * @return an existing or new sheet
   * @see
   */
  public XSSFSheet getSheet(XSSFWorkbook wb, String name) {
    // try to get row
    XSSFSheet sheet = wb.getSheet(name);
    // if not exist: create row
    if (sheet == null)
      sheet = wb.createSheet(name);
    // get cell
    return sheet;
  }

  /**
   * Saves the given workbook to a file. The file has to end with .xlsx
   *
   * @param file the path and name (with suffix (.xlsx)) the file will be saved to (*.xlsx)
   * @param wb the workbook with data
   * @return
   * @see
   */
  public boolean saveWbToFile(File file, XSSFWorkbook wb) {
    try {
      if (FileAndPathUtil.createDirectory(file.getParentFile())) {
        FileOutputStream out = new FileOutputStream(file);
        wb.write(out);
        out.close();
        return true;
      } else
        return false;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * exports a data[rows][columns] array
   * 
   * @param realFilePath
   * @param data
   * @param rowsFirst true: [rows][cols] false [cols][rows]
   */
  public XSSFWorkbook exportDataArrayToFile(File file, String sheetname, Object[][] data,
      boolean rowsFirst) {
    // open wb
    XSSFWorkbook wb = new XSSFWorkbook();
    XSSFSheet sheet = getSheet(wb, sheetname);
    // write to wb
    for (int r = 0; r < data.length; r++) {
      // all columns
      for (int c = 0; c < data[r].length; c++) {
        if (data[r][c] != null) {
          if (rowsFirst)
            writeToCell(sheet, c, r, data[r][c]);
          else
            writeToCell(sheet, r, c, data[r][c]);
        }
      }
    }

    // save wb
    saveWbToFile(file, wb);
    return wb;
  }

  /**
   * writes a data[rows][columns] array to the given wb
   * 
   * @param realFilePath
   * @param data
   * @param rowsFirst true: [rows][cols] false [cols][rows]
   */
  public void writeDataArrayToSheet(XSSFSheet sheet, Object[][] data, int ic, int ir,
      boolean rowsFirst) {
    // write to wb
    for (int r = 0; r < data.length; r++) {
      // all columns
      for (int c = 0; c < data[r].length; c++) {
        if (data[r][c] != null) {
          if (rowsFirst)
            writeToCell(sheet, c + ic, r + ir, data[r][c]);
          else
            writeToCell(sheet, r + ic, c + ir, data[r][c]);
        }
      }
    }
  }

  /**
   * writes a data[rows][columns] array to the given wb
   * 
   * @param realFilePath
   * @param data
   * @param rowsFirst true: [rows][cols] false [cols][rows]
   */
  public void writeDataArrayToSheet(XSSFSheet sheet, double[][] data, int ic, int ir,
      boolean rowsFirst) {
    // write to wb
    for (int r = 0; r < data.length; r++) {
      // all columns
      for (int c = 0; c < data[r].length; c++) {
        if (data[r][c] != Double.NaN) {
          if (rowsFirst)
            writeToCell(sheet, c + ic, r + ir, data[r][c]);
          else
            writeToCell(sheet, r + ic, c + ir, data[r][c]);
        }
      }
    }
  }


  /**
   * writes a boolean[rows][columns] array to the given sheet as binary 1:0
   * 
   * @param realFilePath
   * @param data
   */
  public void writeBooleanArrayToSheet(XSSFSheet sheet, boolean[][] data, int ic, int ir) {
    // write to wb
    for (int r = 0; r < data.length; r++) {
      // all columns
      for (int c = 0; c < data[r].length; c++) {
        writeToCell(sheet, c + ic, r + ir, data[r][c] ? 1 : 0);
      }
    }
  }


  /**
   * writes a data array to one column
   * 
   * @param data
   * @param inColumn in column or inRow?
   */
  public void writeDataArrayToSheet(XSSFSheet sheet, Object[] data, int ic, int ir,
      boolean inColumn) {
    // write to wb
    for (int r = 0; r < data.length; r++) {
      if (data[r] != null) {
        if (inColumn)
          writeToCell(sheet, ic, r + ir, data[r]);
        else
          writeToCell(sheet, ic + r, ir, data[r]);
      }
    }
  }

  public void writeDataArrayToSheet(XSSFSheet sheet, double[] data, int ic, int ir,
      boolean inColumn) {
    // write to wb
    for (int r = 0; r < data.length; r++) {
      if (data[r] != Double.NaN) {
        if (inColumn)
          writeToCell(sheet, ic, r + ir, data[r]);
        else
          writeToCell(sheet, ic + r, ir, data[r]);
      }
    }
  }

  /**
   * writes a data array to one column
   * 
   * @param data
   */
  public void writeDataArrayToSheet(XSSFSheet sheet, Vector<Object> data, int ic, int ir) {
    writeDataArrayToSheet(sheet, data, ic, ir, true);
  }

  public void writeDataArrayToSheet(XSSFSheet sheet, Vector<Object> data, int ic, int ir,
      boolean inColumn) {
    // write to wb
    for (int r = 0; r < data.size(); r++) {
      if (data.get(r) != null) {
        writeToCell(sheet, ic, r + ir, data.get(r));
      }
    }
  }

  /**
   * writes one line to a sheet
   * 
   * @param sheet
   * @param title
   */
  public void writeLine(XSSFSheet sheet, String[] line, int ic, int ir) {
    for (int c = 0; c < line.length; c++) {
      if (line[c] != null) {
        writeToCell(sheet, c + ic, ir, line[c]);
      }
    }
  }

  /**
   * writes regression data to sheets
   * 
   * @param sheet
   * @param reg
   * @param dp
   * @param quantifier
   */
  public void writeRegressionToSheet(XSSFSheet sheet, SimpleRegression reg, double[][] dp) {
    //
    writeToCell(sheet, 0, 1, "c = (I-intercept)/slope");
    writeToCell(sheet, 0, 2, "intercept = ");
    writeToCell(sheet, 1, 2, reg.getIntercept());
    writeToCell(sheet, 0, 2, "slope = ");
    writeToCell(sheet, 1, 2, reg.getSlope());
    writeToCell(sheet, 0, 3, "R^2 = ");
    writeToCell(sheet, 1, 3, reg.getRSquare());
    writeToCell(sheet, 0, 3, "R^2 = ");
    // datapoints
    writeToCell(sheet, 4, 0, "regression");
    writeToCell(sheet, 4, 1, "x");
    writeToCell(sheet, 5, 1, "intensity");
    writeDataArrayToSheet(sheet, dp, 4, 2, true);
  }

  /**
   * A file chooser for xlsx workbooks
   * 
   * @return
   */
  public static JFileChooser getChooser() {
    if (chooser == null) {
      chooser = new JFileChooser();
      chooser.addChoosableFileFilter(new FileTypeFilter("xlsx", "Excel workbook"));
    }
    return chooser;
  }

}
