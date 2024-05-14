/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_ccsbase;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.MolecularClassType;
import io.github.mzmine.modules.io.export_ccsbase.CcsBaseEntryMap.CcsBaseEntry;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CcsBaseExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(CcsBaseExportTask.class.getName());

  private final String CCS_BASE_HEADER = "name,adduct,mass,z,mz,ccs,smi,chem_class_label,ccs_type,ccs_method";

  private final FeatureList[] flists;
  private final String calibrationString;
  private final String fallbackClassLabel;

  private final File file;
  private final int totalRows;
  private int finishedRows = 0;

  public CcsBaseExportTask(ParameterSet parameters, Instant moduleCallDate) {
    super(null, moduleCallDate);
    file = FileAndPathUtil.getRealFilePath(parameters.getValue(CcsBaseExportParameters.file),
        ".csv");
    flists = parameters.getValue(CcsBaseExportParameters.flists).getMatchingFeatureLists();
    calibrationString = parameters.getValue(CcsBaseExportParameters.calibrationMethod);
    fallbackClassLabel = parameters.getValue(CcsBaseExportParameters.fallbackMoleculeInfo);
    totalRows = Arrays.stream(flists).mapToInt(FeatureList::getNumberOfRows).sum();
  }

  @Nullable
  public static CcsBaseEntryMap.CcsBaseEntry annotationToCcsBaseCsvEntry(FeatureListRow row,
      FeatureAnnotation annotation, @NotNull String fallbackClassLabel,
      @NotNull String calibrationString) {

    try {

      final String classLabel =
          annotation instanceof ModularDataModel m && m.get(MolecularClassType.class) != null
              ? m.get(MolecularClassType.class) : fallbackClassLabel;

      final var entry = new CcsBaseEntry(annotation.getCompoundName(),
          annotation.getAdductType().getName(),
          annotation.getPrecursorMZ() * annotation.getAdductType().getCharge(),
          annotation.getAdductType().getCharge(), annotation.getPrecursorMZ(),
          row.getAverageCCS().doubleValue(), annotation.getSmiles(), classLabel,
          row.getBestFeature().getMobilityUnit().getCcsBaseEntryString(), calibrationString,
          row.getBestFeature().getHeight());

      return entry;
    } catch (NullPointerException | ClassCastException e) {
      logger.log(Level.INFO, "Cannot export CCSBase entry for row " + row.toString(), e);
      return null;
    }
  }

  @Override
  public String getTaskDescription() {
    return "Exporting annotations to CCSBase entry file.";
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows != 0 ? finishedRows / (double) totalRows : 0d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final CcsBaseEntryMap map = new CcsBaseEntryMap();

    for (final FeatureList flist : flists) {
      for (FeatureListRow row : flist.getRows()) {
        if (!row.isIdentified()
            || !(row.getPreferredAnnotation() instanceof FeatureAnnotation annotation)) {
          continue;
        }

        final var entry = annotationToCcsBaseCsvEntry(row, annotation, fallbackClassLabel,
            calibrationString);

        map.addEntry(entry);
        finishedRows++;
      }
    }

    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE)) {
      writer.write(CCS_BASE_HEADER);
      writer.newLine();
      for (Entry<CcsBaseEntry, Double> entry : map.entrySet()) {
        final String csvLine = entry.getKey().toCsvLine();
        if (csvLine != null) {
          writer.write(csvLine);
          writer.newLine();
        }
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setErrorMessage("Error while exporting CCSBase file.");
      setStatus(TaskStatus.ERROR);
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

}
