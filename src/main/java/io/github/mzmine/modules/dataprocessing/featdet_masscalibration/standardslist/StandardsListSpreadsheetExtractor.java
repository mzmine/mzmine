/*
 * Copyright 2006-2021 The MZmine Development Team
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
 *
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
 * StandardsListExtractor for xls and xlsx spreadsheets
 * uses sheet at specified index, first sheet available by default
 * expects columns at fixed positions for storing needed data
 * first column is retention time (min) and second column is ion formula
 * third column is optional name
 * first row (column headers) is skipped
 */
public class StandardsListSpreadsheetExtractor implements StandardsListExtractor {

  protected static final int retentionTimeColumn = 0;
  protected static final int ionFormulaColumn = 1;
  protected static final int nameColumn = 2;

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
          float retentionTime = (float) retentionCell.getNumericCellValue();
          String molecularFormula = ionCell.getStringCellValue();
          StandardsListItem calibrant = new StandardsListItem(molecularFormula, retentionTime);
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

    logger.info("Extracted " + extractedData.size() + " standard molecules from " + rowIndex + " rows");
    if (extractedData.size() + 1 < rowIndex) {
      logger.warning("Skipped " + (rowIndex - extractedData.size() - 1) + " rows when reading standards list in" +
              " spreadsheet " + filename);
    }

    return new StandardsList(extractedData);
  }

  @Override
  public void closeInputStreams() {
    try {
      spreadsheet.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
