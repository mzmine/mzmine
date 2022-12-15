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
