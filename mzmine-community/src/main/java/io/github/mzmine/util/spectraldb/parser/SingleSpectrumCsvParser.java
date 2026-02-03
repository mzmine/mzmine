/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.spectraldb.parser;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.ParsingUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the default format tsv/csv for spectra when using the right click in spectrum chart,
 * export data to clipboard. Two columns: mz, intensity
 * <p>
 * Headers are for now disregarded and mz and intensity always need to be the first and second
 * column. Additional columns are disregarded.
 */
public class SingleSpectrumCsvParser {

  private static final Logger logger = Logger.getLogger(SingleSpectrumCsvParser.class.getName());

  @Nullable
  public static MassList readFile(@NotNull String file) {
    return readFile(new File(file));
  }

  @Nullable
  public static MassList readFile(@NotNull File file) {
    try {
      final List<String[]> lines = CSVParsingUtils.readDataAutoSeparator(file);
      if (lines.isEmpty()) {
        return null;
      }

      // header is optional - otherwise require 2 columns
      final String[] header = lines.getFirst();
      if (header.length < 2) {
        logger.fine("Empty header in CSV file " + file.getAbsolutePath());
        return null;
      }

      boolean hasHeader =
          ParsingUtils.stringToDouble(header[0]) == null; // is a string not a number

      int startRow = hasHeader ? 1 : 0;

      DoubleList mzs = new DoubleArrayList();
      DoubleList intensities = new DoubleArrayList();

      boolean hadEmptyEndRow = false;
      for (int row = startRow; row < lines.size(); row++) {
        try {
          final String[] line = lines.get(row);
          if (line.length < 2) {
            hadEmptyEndRow = true;
            continue; // only allow empty rows now, if one full row follows, fail
          }
          if(hadEmptyEndRow) {
            logger.warning("Data after empty line in CSV file is not allowed, file: " + file.getAbsolutePath());
            return null;
          }
          double mz = Double.parseDouble(line[0].trim());
          double intensity = Double.parseDouble(line[1].trim());
          mzs.add(mz);
          intensities.add(intensity);
        } catch (Exception e) {
          logger.log(Level.WARNING,
              "Could not parse numbers for mz intensity. Need to be the first two columns in this order. "
                  + e.getMessage(), e);
          return null;
        }
      }

      // this is only done if the mzs were unsorted
      SimpleSpectralArrays sorted = DataPointUtils.ensureSortingMzAscendingDefault(
          new SimpleSpectralArrays(mzs.toDoubleArray(), intensities.toDoubleArray()));
      // simple mass list ensures sorting
      return new SimpleMassList(null, sorted.mzs(), sorted.intensities());
    } catch (IOException | CsvException e) {
      logger.log(Level.WARNING, "Error reading file " + file.getAbsolutePath(), e);
      return null;
    }
  }
}
