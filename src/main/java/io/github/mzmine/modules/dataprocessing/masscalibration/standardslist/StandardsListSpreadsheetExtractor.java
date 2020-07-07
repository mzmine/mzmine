/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.standardslist;

import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * StandardsListExtractor for xls and xlsx spreadsheets
 * uses sheet at specified index, first sheet available by default
 * expects fixed case-insensitive column names
 * 'retention time (min)' and 'ion formula'
 * for storing needed data
 */
public class StandardsListSpreadsheetExtractor implements StandardsListExtractor {

  protected static final String retentionTimeColumnName = "retention time (min)";
  protected static final String molecularFormulaColumnName = "ion formula";

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
   * @throws RuntimeException         thrown when required column names are missing
   */
  public StandardsList extractStandardsList() {
    logger.fine("Extracting standards list " + filename + " sheet index " + sheetIndex);

    if (this.extractedData != null) {
      logger.fine("Using cached list");
      return new StandardsList(this.extractedData);
    }
    this.extractedData = new ArrayList<StandardsListItem>();

    Sheet sheet = spreadsheet.getSheetAt(sheetIndex);

    int rowIndex = 0;
    int retentionTimeCellIndex = -1;
    int molecularFormulaCellIndex = -1;

    for (Row row : sheet) {

      if (rowIndex == 0) {
        int cellIndex = 0;
        for (Cell cell : row) {

          try {
            String value = cell.getStringCellValue();
            if (value.equalsIgnoreCase(retentionTimeColumnName)) {
              retentionTimeCellIndex = cellIndex;
            } else if (value.equalsIgnoreCase(molecularFormulaColumnName)) {
              molecularFormulaCellIndex = cellIndex;
            }
          } catch (Exception e) {
          }

          cellIndex++;
        }

        if (retentionTimeCellIndex == -1 || molecularFormulaCellIndex == -1) {
          throw new RuntimeException(String.format("Spreadsheet %s missing retention time [%s]"
                  + " or molecular formula [%s] columns",
                  filename, retentionTimeColumnName, molecularFormulaColumnName));
        }
      } else {

        try {
          Cell retentionCell = row.getCell(retentionTimeCellIndex);
          Cell molecularCell = row.getCell(molecularFormulaCellIndex);
          double retentionTime = retentionCell.getNumericCellValue();
          String molecularFormula = molecularCell.getStringCellValue();
          extractedData.add(new StandardsListItem(molecularFormula, retentionTime));
        } catch (Exception e) {
          logger.fine("Exception occurred when reading row index " + rowIndex);
          logger.fine(e.toString());
        }

      }

      rowIndex++;
    }

    logger.info("Extracted " + extractedData.size() + " standard molecules from " + rowIndex + " rows");
    if (extractedData.size() + 1 < rowIndex) {
      logger.warning("Skipped " + (rowIndex - extractedData.size() - 1) + " rows when reading standards list in" +
              " spreadsheet " + filename);
    }

    return new StandardsList(extractedData);
  }
}
