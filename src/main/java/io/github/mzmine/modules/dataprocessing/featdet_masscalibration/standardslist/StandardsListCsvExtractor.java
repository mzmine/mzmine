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

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * StandardsListExtractor for csv files
 * expects columns at fixed positions for storing needed data
 * first column is retention time (min) and second column is ion formula
 * third column is optional name
 * first row (column headers) is skipped
 */
public class StandardsListCsvExtractor implements StandardsListExtractor {

  protected static final int retentionTimeColumn = 0;
  protected static final int ionFormulaColumn = 1;
  protected static final int nameColumn = 2;

  protected Logger logger = Logger.getLogger(this.getClass().getName());

  protected String filename;

  protected LabeledCSVParser csvReader;

  protected ArrayList<StandardsListItem> extractedData;

  /**
   * Creates the extractor
   *
   * @param filename csv filename
   * @throws IOException exception thrown when issues opening given file occur
   */
  public StandardsListCsvExtractor(String filename) throws IOException {
    this.filename = filename;
    this.csvReader = new LabeledCSVParser(new CSVParser(new FileInputStream(filename)));
  }

  /**
   * Extracts standards list caching underlying list data
   *
   * @return new standards list object
   * @throws IOException thrown when issues extracting from the file occur
   */
  public StandardsList extractStandardsList() throws IOException {
    logger.fine("Extracting standards list " + filename);

    if (this.extractedData != null) {
      logger.fine("Using cached list");
      return new StandardsList(this.extractedData);
    }
    this.extractedData = new ArrayList<StandardsListItem>();

    String[] lineValues;
    while ((lineValues = csvReader.getLine()) != null) {
      try {
        String retentionTimeString = lineValues[retentionTimeColumn];
        String molecularFormula = lineValues[ionFormulaColumn];
        String name = nameColumn < lineValues.length ? lineValues[nameColumn] : null;
        float retentionTime = (float) Double.parseDouble(retentionTimeString);
        StandardsListItem calibrant = new StandardsListItem(molecularFormula, retentionTime);
        if (name != null && name.trim().isEmpty() == false) {
          calibrant.setName(name);
        }
        extractedData.add(calibrant);
      } catch (Exception e) {
        logger.fine("Exception occurred when reading row index " + csvReader.getLastLineNumber());
        logger.fine(e.toString());
      }
    }

    logger.info("Extracted " + extractedData.size() + " standard molecules from "
            + csvReader.getLastLineNumber() + " rows");
    if (extractedData.size() < csvReader.getLastLineNumber()) {
      logger.warning("Skipped " + (csvReader.getLastLineNumber() - extractedData.size())
              + " rows when reading standards list in csv file " + filename);
    }

    return new StandardsList(extractedData);
  }

  @Override
  public void closeInputStreams() {
    try {
      csvReader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
