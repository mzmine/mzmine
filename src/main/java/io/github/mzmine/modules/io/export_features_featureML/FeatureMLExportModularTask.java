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

import com.google.common.collect.Range;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * Export results to featureML format for visualization in TOPPView schema available at <a
 * href="https://github.com/OpenMS/OpenMS/blob/7a2e4a41d4c9f511306afcb8bb4f1b773ace9b9a/share/OpenMS/SCHEMAS/FeatureXML_1_9.xsd"></a>
 */

public class FeatureMLExportModularTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeatureMLExportModularTask.class.getName());
  private final ModularFeatureList[] featureLists;
  // parameter values
  private final File fileName;
  private final FeatureListRowsFilter rowFilter;
  private int processedRows = 0;
  private int totalRows = 0;

  public FeatureMLExportModularTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(FeatureMLExportModularParameters.featureLists)
        .getValue().getMatchingFeatureLists();
    fileName = parameters.getParameter(FeatureMLExportModularParameters.filename).getValue();
    this.rowFilter = parameters.getParameter(FeatureMLExportModularParameters.filter).getValue();
  }

  /**
   * @param featureLists feature lists to export
   * @param fileName     export file name
   */
  public FeatureMLExportModularTask(ModularFeatureList[] featureLists, File fileName,
      FeatureListRowsFilter rowFilter, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = featureLists;
    this.fileName = fileName;
    this.rowFilter = rowFilter;
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : processedRows / (double) totalRows;
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

    if (!substitute && featureLists.length > 1) {
      setErrorMessage("""
          Cannot export multiple feature lists to the same featureML file. Please use "{}" pattern in filename.\
          This will be replaced with the feature list name to generate one file per feature list.
          """);
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Total number of rows
    for (ModularFeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (ModularFeatureList featureList : featureLists) {
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
        curFile = new File(newFilename);
      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, "featureML");

      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {

        // Get XMLOutputFactory instance.
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        // Create XMLStreamWriter object from xmlOutputFactory.
        XMLStreamWriter xmlStreamWriter = new IndentingXMLStreamWriter(
            xmlOutputFactory.createXMLStreamWriter(writer));

        exportFeatureList(featureList, xmlStreamWriter);

        xmlStreamWriter.flush();
        xmlStreamWriter.close();

        logger.log(Level.INFO,
            String.format("Written feature list to file '%s'", curFile.toPath()));

      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.WARNING, String.format(
            "Error writing featureML format to file: %s for feature list: %s. Message: %s",
            curFile.getAbsolutePath(), featureList.getName(), e.getMessage()), e);
        return;
      } catch (XMLStreamException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not write XML file " + curFile);
        logger.log(Level.WARNING, String.format(
            "Error writing featureML format to file: %s for feature list: %s. Message: %s",
            curFile.getAbsolutePath(), featureList.getName(), e.getMessage()), e);
        return;
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute) {
        break;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void exportFeatureList(ModularFeatureList flist, XMLStreamWriter xmlWriter)
      throws IOException, XMLStreamException {
    final List<FeatureListRow> rows = flist.getRows().stream().filter(rowFilter::accept)
        .sorted(FeatureListRowSorter.DEFAULT_ID).toList();
    List<RawDataFile> rawDataFiles = flist.getRawDataFiles();

    // write featureML header
    xmlWriter.writeStartDocument();

    // featureMap
    this.generateElement(xmlWriter, "featureMap",
        new String[]{"version", "1.4", "id", "fm_16311276685788915066",
            "xsi:noNamespaceSchemaLocation",
            "http://open-ms.sourceforge.net/schemas/FeatureXML_1_4.xsd", "xmlns:xsi",
            "http://www.w3.org/2001/XMLSchema-instance"});

    // dataProcessing
    this.generateElement(xmlWriter, "dataProcessing", new String[]{"completion_time",
        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date())});

    // software
    this.generateEmptyElement(xmlWriter, "software",
        new String[]{"name", "MZmine", "version", MZmineCore.getMZmineVersion().toString()});

    // end dataProcessing
    xmlWriter.writeEndElement();

    // featureList
    this.generateElement(xmlWriter, "featureList",
        new String[]{"count", String.format("%d", rows.size())});

    int featureNum = 1;
    for (FeatureListRow row : rows) {

      // get convex hull (min/max RT and MZ Range) for the feature in the experiment
      double minRT = row.get(RTRangeType.class).lowerEndpoint();
      double maxRT = row.get(RTRangeType.class).upperEndpoint();
      double meanRT = row.getAverageRT();
      double minMZ = row.getMZRange().lowerEndpoint();
      double maxMZ = row.getMZRange().upperEndpoint();
      double meanMZ = row.getAverageMZ();

      // convert RTs to minutes
      minRT = minRT * 60;
      maxRT = maxRT * 60;
      meanRT = meanRT * 60;

      // feature
      this.generateElement(xmlWriter, "feature",
          new String[]{"id", String.format("%d", featureNum)});

      this.generateSimpleFilledElement(xmlWriter, "position", String.format("%f", meanRT),
          new String[]{"dim", "0"});
      this.generateSimpleFilledElement(xmlWriter, "position", String.format("%f", meanMZ),
          new String[]{"dim", "1"});

      this.generateSimpleFilledElement(xmlWriter, "intensity", "1", new String[]{});

      this.generateSimpleFilledElement(xmlWriter, "quality", "0", new String[]{"dim", "0"});
      this.generateSimpleFilledElement(xmlWriter, "quality", "0", new String[]{"dim", "1"});
      this.generateSimpleFilledElement(xmlWriter, "overallquality",
          String.format("%d", row.getNumberOfFeatures()), new String[]{});

      this.generateSimpleFilledElement(xmlWriter, "charge", String.format("%d", row.getRowCharge()),
          new String[]{});

      this.generateConvexHullForXML(xmlWriter, 0, minRT, maxRT, minMZ, maxMZ);

      // get convex hulls of individual features from the different samples
      int numberOfConvexHulls = 1;
      for (RawDataFile rawFile : rawDataFiles) {

        Feature feature = row.getFeature(rawFile);
        if (feature != null) {
          Range<Double> mzRange = feature.getRawDataPointsMZRange();
          minMZ = mzRange.lowerEndpoint();
          maxMZ = mzRange.upperEndpoint();
          // convert RTs to minutes
          Range<Float> rtRange = feature.getRawDataPointsRTRange();
          minRT = rtRange.lowerEndpoint() * 60;
          maxRT = rtRange.upperEndpoint() * 60;

          this.generateConvexHullForXML(xmlWriter, numberOfConvexHulls, minRT, maxRT, minMZ, maxMZ);
          numberOfConvexHulls += 1;
        }
      }

      // end feature
      xmlWriter.writeEndElement();

      featureNum++;
      processedRows++;
    }

    // end featureList
    xmlWriter.writeEndElement();

    // end featureMap
    xmlWriter.writeEndElement();
    xmlWriter.writeEndDocument();

    xmlWriter.flush();
  }


  private void generateEmptyElement(XMLStreamWriter xmlWriter, String elementName,
      String[] attributeNamesAndValues) throws XMLStreamException {
    this.generateElement(xmlWriter, elementName, attributeNamesAndValues, true);
  }

  private void generateElement(XMLStreamWriter xmlWriter, String elementName,
      String[] attributeNamesAndValues) throws XMLStreamException {
    this.generateElement(xmlWriter, elementName, attributeNamesAndValues, false);
  }

  private void generateElement(XMLStreamWriter xmlWriter, String elementName,
      String[] attributeNamesAndValues, boolean selfContained) throws XMLStreamException {

    if (attributeNamesAndValues.length % 2 != 0) {
      throw (new IllegalArgumentException(
          "Number of elements in parameter attributeNamesAndValues must be a multiple of 2 (attribute1Name, attribute1Value, attribute2Name, attribute2Value, ..."));
    }

    if (selfContained) {
      xmlWriter.writeEmptyElement(elementName);
    } else {
      xmlWriter.writeStartElement(elementName);
    }

    for (int i = 0; i < attributeNamesAndValues.length; i += 2) {
      xmlWriter.writeAttribute(attributeNamesAndValues[i], attributeNamesAndValues[i + 1]);
    }
  }

  private void generateSimpleFilledElement(XMLStreamWriter xmlWriter, String elementName,
      String elementValue, String[] attributeNamesAndValues) throws XMLStreamException {
    this.generateElement(xmlWriter, elementName, attributeNamesAndValues);
    xmlWriter.writeCharacters(elementValue);
    xmlWriter.writeEndElement();
  }

  private void generateConvexHullForXML(XMLStreamWriter xmlWriter, int convexHullNumber,
      double minRt, double maxRt, double minMZ, double maxMZ) throws XMLStreamException {

    // convexhull
    this.generateElement(xmlWriter, "convexhull",
        new String[]{"nr", String.format("%d", convexHullNumber)});

    this.generateEmptyElement(xmlWriter, "pt",
        new String[]{"x", String.format("%f", minRt), "y", String.format("%f", minMZ)});
    this.generateEmptyElement(xmlWriter, "pt",
        new String[]{"x", String.format("%f", minRt), "y", String.format("%f", maxMZ)});
    this.generateEmptyElement(xmlWriter, "pt",
        new String[]{"x", String.format("%f", maxRt), "y", String.format("%f", maxMZ)});
    this.generateEmptyElement(xmlWriter, "pt",
        new String[]{"x", String.format("%f", maxRt), "y", String.format("%f", minMZ)});

    // end convexhull
    xmlWriter.writeEndElement();
  }
}
