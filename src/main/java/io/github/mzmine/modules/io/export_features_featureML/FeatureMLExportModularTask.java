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

package io.github.mzmine.modules.io.export_features_featureML;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.ProcessedItemsCounter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Export results to featureML format for visualization in TOPPView
// schema available at
// https://github.com/OpenMS/OpenMS/blob/7a2e4a41d4c9f511306afcb8bb4f1b773ace9b9a/share/OpenMS/SCHEMAS/FeatureXML_1_9.xsd


public class FeatureMLExportModularTask extends AbstractTask implements ProcessedItemsCounter {

  public static final String DATAFILE_PREFIX = "datafile";
  private static final Logger logger = Logger.getLogger(FeatureMLExportModularTask.class.getName());
  private final ModularFeatureList[] featureLists;
  // parameter values
  private final File fileName;
  private final String headerSeparator = ":";
  private final FeatureListRowsFilter rowFilter;
  // track number of exported items
  private final AtomicInteger exportedRows = new AtomicInteger(0);
  private int processedTypes = 0, totalTypes = 0;

  public FeatureMLExportModularTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(FeatureMLExportModularParameters.featureLists)
        .getValue().getMatchingFeatureLists();
    fileName = parameters.getParameter(FeatureMLExportModularParameters.filename).getValue();
    this.rowFilter = parameters.getParameter(FeatureMLExportModularParameters.filter).getValue();
  }

  /**
   * @param featureLists feature lists to export
   * @param fileName export file name
   */
  public FeatureMLExportModularTask(ModularFeatureList[] featureLists, File fileName,
      FeatureListRowsFilter rowFilter, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = featureLists;
    this.fileName = fileName;
    this.rowFilter = rowFilter;
  }

  @Override
  public int getProcessedItems() {
    return exportedRows.get();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalTypes == 0) {
      return 0;
    }
    return (double) processedTypes / (double) totalTypes;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to featureML file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    String plNamePattern = "{}";
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (ModularFeatureList featureList : featureLists) {
      totalTypes += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (ModularFeatureList featureList : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }
      // check concurrent modification during export
      final int numRows = featureList.getNumberOfRows();
      final long numFeatures = featureList.streamFeatures().count();
      final long numMS2 = featureList.stream().filter(row -> row.hasMs2Fragmentation()).count();

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, "featureML");

      // Open file

      try (BufferedWriter writer =
          Files.newBufferedWriter(curFile.toPath(), StandardCharsets.UTF_8)) {
        exportFeatureList(featureList, writer);

      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.WARNING,
            String.format(
                "Error writing featureML format to file: %s for feature list: %s. Message: %s",
                curFile.getAbsolutePath(), featureList.getName(), e.getMessage()),
            e);
        return;
      }

      checkConcurrentModification(featureList, numRows, numFeatures, numMS2);
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

  @SuppressWarnings("rawtypes")
  private void exportFeatureList(ModularFeatureList flist, BufferedWriter writer)
      throws IOException {
    final List<FeatureListRow> rows = flist.getRows().stream().filter(rowFilter::accept)
        .sorted(FeatureListRowSorter.DEFAULT_ID).toList();
    List<RawDataFile> rawDataFiles = flist.getRawDataFiles();

    List<DataType> rowTypes =
        flist.getRowTypes().values().stream().filter(this::filterType).collect(Collectors.toList());

    // write featureML header
    writer.write(String.format("<?xml version='1.0' encoding='ISO-8859-1'?>"));
    writer.newLine();
    writer.write(String.format(
        "  <featureMap version='1.4' id='fm_16311276685788915066' xsi:noNamespaceSchemaLocation='http://open-ms.sourceforge.net/schemas/FeatureXML_1_4.xsd' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>"));
    writer.newLine();
    writer.write(String.format("    <dataProcessing completion_time='%s'>",
        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date())));
    writer.newLine();
    writer.write(
        String.format("      <software name='MzMine3' version='%s' />", "TODO add version here"));
    writer.newLine();
    writer.write(String.format("    </dataProcessing>"));
    writer.newLine();
    writer.write(String.format("    <featureList count='%d'>", rowTypes.size()));
    writer.newLine();

    double minRT = 100000.;
    double maxRT = 0.;
    double meanRT = 0.;
    double minMZ = 0.;
    double maxMZ = 0.;
    double meanMZ = 0.;

    for (FeatureListRow row : rows) {
      // get convex hull (min/max RT and MZ Range) for the feature in the experiment
      minRT = row.get(RTRangeType.class).lowerEndpoint();
      maxRT = row.get(RTRangeType.class).upperEndpoint();
      meanRT = row.getAverageRT();
      minMZ = row.getMZRange().lowerEndpoint();
      maxMZ = row.getMZRange().upperEndpoint();
      meanMZ = row.getAverageMZ();

      // convert RTs to minutes
      minRT = minRT * 60;
      maxRT = maxRT * 60;
      meanRT = meanRT * 60;

      writer.write(String.format("      <feature id='%d'>", row.getID()));
      writer.newLine();
      writer.write(String.format("        <position dim='0'>%f</position>", meanRT));
      writer.newLine();
      writer.write(String.format("        <position dim='1'>%f</position>", meanMZ));
      writer.newLine();
      writer.write(String.format("        <intensity>1</intensity>"));
      writer.newLine();
      writer.write(String.format("        <quality dim='0'>0</quality>"));
      writer.newLine();
      writer.write(String.format("        <quality dim='1'>0</quality>"));
      writer.newLine();
      writer.write(String.format("        <overallquality>%d</overallquality>", row.getNumberOfFeatures()));
      writer.newLine();
      writer.write(String.format("        <charge>1</charge>"));
      writer.newLine();
      writer.write(String.format("        <convexhull nr='0'>"));
      writer.newLine();
      writer.write(String.format("          <pt x='%f' y='%f' />", minRT, minMZ));
      writer.newLine();
      writer.write(String.format("          <pt x='%f' y='%f' />", minRT, maxMZ));
      writer.newLine();
      writer.write(String.format("          <pt x='%f' y='%f' />", maxRT, maxMZ));
      writer.newLine();
      writer.write(String.format("          <pt x='%f' y='%f' />", maxRT, minMZ));
      writer.newLine();
      writer.write(String.format("        </convexhull>"));
      writer.newLine();

      // get convex hulls of individual features from the different samples
      int numberOfConvexHulls = 1;
      for (RawDataFile rawFile : rawDataFiles) {

        if (row.getFeature(rawFile) != null) {
          minMZ = row.getFeature(rawFile).getRawDataPointsMZRange().lowerEndpoint();
          maxMZ = row.getFeature(rawFile).getRawDataPointsMZRange().upperEndpoint();
          minRT = row.getFeature(rawFile).getRawDataPointsRTRange().lowerEndpoint() * 60;      // convert RTs to minutes
          maxRT = row.getFeature(rawFile).getRawDataPointsRTRange().upperEndpoint() * 60;

          writer.write(String.format("        <convexhull nr='%d'>", numberOfConvexHulls));
          writer.newLine();
          writer.write(String.format("          <pt x='%f' y='%f' />", minRT, minMZ));
          writer.newLine();
          writer.write(String.format("          <pt x='%f' y='%f' />", minRT, maxMZ));
          writer.newLine();
          writer.write(String.format("          <pt x='%f' y='%f' />", maxRT, maxMZ));
          writer.newLine();
          writer.write(String.format("          <pt x='%f' y='%f' />", maxRT, minMZ));
          writer.newLine();
          writer.write(String.format("        </convexhull>"));
          writer.newLine();

          numberOfConvexHulls += 1;
        }
      }

      writer.write(String.format("      </feature>"));
      writer.newLine();
    }

    // write featureML footer
    writer.append("    </featureList>");
    writer.newLine();
    writer.append("  </featureMap>");
    writer.newLine();

    exportedRows.incrementAndGet();
    processedTypes++;
  }


  /**
   * @return true if type should be exported
   */
  public boolean filterType(DataType type) {
    return !(type instanceof NoTextColumn || type instanceof NullColumnType
        || type instanceof LinkedGraphicalType);
  }


  private void checkConcurrentModification(FeatureList featureList, int numRows, long numFeatures,
      long numMS2) {
    final int numRowsEnd = featureList.getNumberOfRows();
    final long numFeaturesEnd = featureList.streamFeatures().count();
    final long numMS2End = featureList.stream().filter(row -> row.hasMs2Fragmentation()).count();

    if (numRows != numRowsEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) CSV export old=%d new=%d",
          featureList.getName(), numRows, numRowsEnd));
    }
    if (numFeatures != numFeaturesEnd) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS during featurelist (%s) CSV export old=%d new=%d",
          featureList.getName(), numFeatures, numFeaturesEnd));
    }
    if (numMS2 != numMS2End) {
      throw new ConcurrentModificationException(String.format(
          "Detected modification to number of ROWS WITH MS2 during featurelist (%s) CSV export old=%d new=%d",
          featureList.getName(), numMS2, numMS2End));
    }
  }
}
