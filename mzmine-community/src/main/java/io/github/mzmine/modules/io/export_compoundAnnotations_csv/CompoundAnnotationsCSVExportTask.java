/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.MethodType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.UsiType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class CompoundAnnotationsCSVExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      CompoundAnnotationsCSVExportTask.class.getName());
  private final FeatureList[] featureLists;
  private final File fileName;
  private final int topNMatches;

  private String fieldSeparator = ",";


  private int processedRows = 0, totalRows = 0;

  public CompoundAnnotationsCSVExportTask(ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureLists = parameters.getParameter(CompoundAnnotationsCSVExportParameters.featureLists)
        .getValue().getMatchingFeatureLists();
    fileName = parameters.getValue(CompoundAnnotationsCSVExportParameters.filename);
    topNMatches = parameters.getValue(CompoundAnnotationsCSVExportParameters.topNMatches);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting annotations of feature list(s) " + Arrays.toString(featureLists)
           + " to CSV file(s) ";
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    String plNamePattern = "{}";
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (FeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = FileAndPathUtil.getRealFilePath(new File(newFilename), "csv");
      }

      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {

        exportFeatureList(featureList, writer);

      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error during compound annotations csv export to " + curFile);
        logger.log(Level.WARNING,
            "Error during compound annotations csv export of feature list: " + featureList.getName()
            + ": " + e.getMessage(), e);
        return;
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }


  }

  private void exportFeatureList(FeatureList featureList, BufferedWriter writer)
      throws IOException {
    try {
      // Create a list of column DataTypes
      var columns = Stream.of(IDType.class, CompoundNameType.class, IonTypeType.class,
              ScoreType.class, PrecursorMZType.class, MobilityType.class, CCSType.class, RTType.class,
              FormulaType.class, SmilesStructureType.class, InChIStructureType.class,
              InChIKeyStructureType.class, MethodType.class, UsiType.class)
          .map(c -> DataTypes.get((Class) c)).toList();

      // Create a header string by joining the unique IDs of the DataTypes with commas
      var header = columns.stream().map(DataType::getUniqueID).collect(Collectors.joining(","));

      // write header to file
      writer.append(header).append("\n");

      var methodCounter = new Object2IntOpenHashMap<String>(4);
      // loop through all rows in the feature list
      for (FeatureListRow row : featureList.getRows()) {
        methodCounter.clear();
        List<Object> featureAnnotations = row.getAllFeatureAnnotations();
        for (Object object : featureAnnotations) {
          if (object instanceof FeatureAnnotation annotation) {
            String method = annotation.getAnnotationMethodUniqueId();
            // count exported for method
            int alreadyExported = methodCounter.computeIfAbsent(method, m -> 0);
            if (alreadyExported >= topNMatches) {
              continue;
            }
            // Export fields from the FeatureAnnotation object
            Integer rowId = row.getID();
            String compoundName = annotation.getCompoundName();
            IonType adductType = annotation.getAdductType();
            String scoreType = annotation.getScoreString();
            Double precursorMZ = annotation.getPrecursorMZ();
            Float mobility = annotation.getMobility();
            Float getCCS = annotation.getCCS();
            Float getRT = annotation.getRT();
            String smiles = annotation.getSmiles();
            String inchi = annotation.getInChI();
            String inchikey = annotation.getInChIKey();
            String formula = annotation.getFormula();
            String usi = null;
            if (annotation instanceof SpectralDBAnnotation spec) {
              usi = spec.getEntry().getAsString(DBEntryField.USI).orElse("");
            }

            String result = Stream.of(rowId, compoundName, adductType, scoreType, precursorMZ,
                    mobility, getCCS, getRT, formula, smiles, inchi, inchikey, method, usi)
                .map(o -> (o == null) ? "" : CSVUtils.escape(o.toString(), fieldSeparator))
                .collect(Collectors.joining(","));

            // Export the fields as needed
            writer.append(result).append("\n");
            processedRows++;
            methodCounter.put(method, alreadyExported + 1);
          }
        }
      }
      System.out.println("Export successful!");
    } catch (IOException e) {
      System.out.println("Error exporting feature annotations: " + e.getMessage());
    }
  }
}
