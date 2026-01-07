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

package io.github.mzmine.modules.io.import_feature_networks;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.SimpleRowsRelationship;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.exceptions.MissingColumnException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ImportFeatureNetworksSimpleTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      ImportFeatureNetworksSimpleTask.class.getName());

  final R2RNetworkingMaps maps = new R2RNetworkingMaps();
  private final String[] cols = SimpleExternalNetworkEdge.getColumnHeadersLowerCase();
  private final File inputFile;
  private final ModularFeatureList featureList;
  private char separator;
  private @NotNull Int2ObjectMap<FeatureListRow> rowIdMap;

  public ImportFeatureNetworksSimpleTask(final File inputFile, final ModularFeatureList featureList,
      final ParameterSet parameters, final Instant callDate) {
    super(null, callDate, parameters, ImportFeatureNetworksSimpleModule.class);
    this.inputFile = inputFile;
    this.featureList = featureList;
  }

  private boolean loadEdges(final CSVReader csvReader)
      throws IOException, CsvValidationException, MissingColumnException {
    List<String> errors = new ArrayList<>();

    Map<String, Integer> colIndex = null;
    String[] line;
    while ((line = csvReader.readNext()) != null) {
      if (colIndex == null) {
        // throws exception on fail
        colIndex = CSVParsingUtils.extractColumnIndicesStrict(cols, line);
      } else {
        try {
          var edge = parseLine(line, colIndex);
          addEdgeToFeatureLists(errors, edge);
          if (errors.size() >= 10) {
            setErrorMessage("Failed to load file " + inputFile.getAbsolutePath());
            setStatus(TaskStatus.ERROR);
            logger.warning("Errors during edge parsing: " + String.join("\n", errors));
            return false;
          }

        } catch (Exception e) {
          errors.add(e.getMessage());
          if (errors.size() >= 10) {
            setErrorMessage("Failed to load file " + inputFile.getAbsolutePath());
            setStatus(TaskStatus.ERROR);
            logger.warning("Errors during edge parsing: " + String.join("\n", errors));
            return false;
          }
        }
      }
    }
    if (!errors.isEmpty()) {
      logger.warning("Errors during edge parsing: " + String.join("\n", errors));
    }
    return true;
  }

  private void addEdgeToFeatureLists(final List<String> errors,
      final SimpleExternalNetworkEdge edge) {
    FeatureListRow a = rowIdMap.get(edge.id1());
    FeatureListRow b = rowIdMap.get(edge.id2());

    if (a == null) {
      errors.add("Wrong feature list? Missing row with id " + edge.id1());
    }
    if (b == null) {
      errors.add("Wrong feature list? Missing row with id " + edge.id1());
    }
    if (b == null || a == null) {
      return;
    }

    var r2r = new SimpleRowsRelationship(a, b, edge.score(), edge.edgeType(), edge.annotation());
    maps.addRowsRelationship(a, b, r2r);
  }

  private SimpleExternalNetworkEdge parseLine(final String[] line,
      final Map<String, Integer> colIndex) {
    if (line.length == 0) {
      return null;
    }
    // map values into correct order
    List<String> values = Arrays.stream(cols).map(col -> line[colIndex.get(col)]).toList();
    return SimpleExternalNetworkEdge.parse(values);
  }


  @Override
  public String getTaskDescription() {
    return "Importing networks from tabular data";
  }

  @Override
  protected void process() {
    rowIdMap = FeatureListUtils.getRowIdMap(featureList);

    try (Reader reader = Files.newBufferedReader(inputFile.toPath())) {
      separator = ',';
      try (CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(
          new RFC4180ParserBuilder().withSeparator(separator).build()).build()) {
        loadEdges(csvReader);

        if (isCanceled()) {
          return;
        }
        featureList.addRowMaps(maps);

      }
    } catch (MissingColumnException e) {
      setErrorMessage("CSV missing columns error: " + e.getMessage());
      logger.log(Level.WARNING, "CSV missing columns error: " + e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return;
    } catch (CsvValidationException e) {
      setErrorMessage("CSV parsing error: " + e.getMessage());
      logger.log(Level.WARNING, "CSV parsing error " + e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return;
    } catch (IOException e) {
      setErrorMessage("Could not read file: " + e.getMessage());
      logger.log(Level.WARNING, "Could not load file", e);
      setStatus(TaskStatus.ERROR);
      return;
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }
}
