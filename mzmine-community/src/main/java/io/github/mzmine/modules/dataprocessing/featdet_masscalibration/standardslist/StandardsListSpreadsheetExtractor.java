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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;


/**
 * StandardsListExtractor for xls and xlsx spreadsheets uses sheet at specified index, first sheet
 * available by default expects columns at fixed positions for storing needed data first column is
 * retention time (min) and second column is ion formula third column is optional name first row
 * (column headers) is skipped
 */
public class StandardsListSpreadsheetExtractor implements StandardsListExtractor {

  protected static final int retentionTimeColumn = 0;
  protected static final int ionFormulaColumn = 1;
  protected static final int nameColumn = 2;
  protected static final int mzColumn = 3;

  protected Logger logger = Logger.getLogger(this.getClass().getName());

  protected String filename;
  protected int sheetIndex;

  protected Workbook spreadsheet;

  protected ArrayList<StandardsListItem> extractedData;

  /**
   * Creates the extractor
   *
   * @param filename   spreadsheet filename
   * @param sheetIndex sheet index to use
   * @throws IOException exception thrown when issues opening given file occur
   */
  public StandardsListSpreadsheetExtractor(String filename, int sheetIndex) throws IOException {
    this.filename = filename;
    this.sheetIndex = sheetIndex;

    this.spreadsheet = WorkbookFactory.create(new FileInputStream(filename));
  }

  public StandardsListSpreadsheetExtractor(String filename) throws IOException {
    this(filename, 0);
  }

  /**
   * Extracts standards list caching underlying list data
   *
   * @return new standards list object
   * @throws IllegalArgumentException thrown when sheet index is out of range available
   */
  public StandardsList extractStandardsList() {
    logger.fine("Extracting standards list " + filename + " sheet index " + sheetIndex);

    if (this.extractedData != null) {
      logger.fine("Using cached list");
      return new StandardsList(this.extractedData);
    }
    this.extractedData = new ArrayList<>();

    Sheet sheet = spreadsheet.getSheetAt(sheetIndex);

    int rowIndex = -1;
    for (Row row : sheet) {
      rowIndex++;

      if (rowIndex == 0) {
        continue;
      } else {
        try {
          Cell retentionCell = row.getCell(retentionTimeColumn);
          Cell ionCell = row.getCell(ionFormulaColumn);
          Cell nameCell = row.getCell(nameColumn);
          Cell mzCell = row.getCell(mzColumn);
          float retentionTime = (float) retentionCell.getNumericCellValue();
          String molecularFormula = ionCell.getStringCellValue();
          Double mz = mzCell != null && !mzCell.getStringCellValue().isBlank() ? Double.parseDouble(
              mzCell.getStringCellValue().trim()) : null;
          StandardsListItem calibrant = new StandardsListItem(molecularFormula, retentionTime, mz);
          try {
            if (nameCell != null && nameCell.getStringCellValue().trim().isEmpty() == false) {
              calibrant.setName(nameCell.getStringCellValue());
            }
          } catch (Exception ex) {
          }

          extractedData.add(calibrant);
        } catch (Exception e) {
          logger.fine("Exception occurred when reading row index " + rowIndex);
          logger.fine(e.toString());
        }

      }

    }

    logger.info(
        "Extracted " + extractedData.size() + " standard molecules from " + rowIndex + " rows");
    if (extractedData.size() + 1 < rowIndex) {
      logger.warning("Skipped " + (rowIndex - extractedData.size() - 1)
          + " rows when reading standards list in" + " spreadsheet " + filename);
    }

    return new StandardsList(extractedData);
  }

}
