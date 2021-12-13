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

package io.github.mzmine.modules.io.projectload.version_3_0;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.ZipUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FeatureListLoadTask extends AbstractTask {

  public static final String TEMP_FLIST_DATA_FOLDER = "mzmine_featurelists_temp";
  public static final Pattern fileNamePattern = Pattern
      .compile("([^\\n]+)(" + FeatureListSaveTask.DATA_FILE_SUFFIX + ")");

  private static final Logger logger = Logger.getLogger(FeatureListLoadTask.class.getName());
  final String idTypeUniqueID = new IDType().getUniqueID();
  private final ZipFile zip;
  private final MZmineProject project;
  private final AtomicInteger rowCounter = new AtomicInteger(0);
  private int totalRows = 1;
  private int processedRows = 0;
  private String currentFlist = "";
  private int numFlists = 1;
  private int processedFlists;

  public FeatureListLoadTask(@Nullable MemoryMapStorage storage, @NotNull MZmineProject project,
      ZipFile zip) {
    super(storage, Instant.now());
    this.project = project;
    this.zip = zip;
  }

  public static Object parseDataType(XMLStreamReader reader, DataType<?> type,
      ModularFeatureList flist, ModularFeatureListRow row, ModularFeature feature,
      RawDataFile file) {
    if (type != null) {
      try {
        return type.loadFromXML(reader, flist, row, feature, file);
      } catch (Exception e) {
        logger.log(Level.WARNING, e,
            () -> "Error loading data type " + type.getHeaderString() + " in row (id=" + row.getID()
                  + ") feature " + (file != null ? file.getName() : "") + " from XML.");
      }
    } else {
      logger.info(() -> "No data type for id " + reader
          .getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
    }
    return null;
  }

  @Override
  public String getTaskDescription() {
    return "Importing feature list " + currentFlist + (processedFlists + 1) + "/" + numFlists
           + ". Parsing row " + processedRows + "/" + totalRows;
  }

  @Override
  public double getFinishedPercentage() {
    return (double) processedFlists / numFlists // overall progress finished flists
           + (double) processedRows / totalRows / numFlists; // current flist progress
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {
      Path tempDirectory = Files.createTempDirectory(TEMP_FLIST_DATA_FOLDER);

      logger.info(() -> "Unzipping feature lists of project to " + tempDirectory.toString());
      ZipUtils.unzipDirectory(FeatureListSaveTask.FLIST_FOLDER, zip, tempDirectory.toFile());
      logger.info(() -> "Unzipping feature lists done.");

      File[] files = new File(tempDirectory.toFile(), FeatureListSaveTask.FLIST_FOLDER)
          .listFiles((dir, name) -> fileNamePattern.matcher(name).matches());
      if (files == null) {
        logger.info("Did not find feature lists to load.");
        setStatus(TaskStatus.FINISHED);
        return;
      }

      numFlists = files.length;

      final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

      // enable caching of mobility scans during project import.
      project.setProjectLoadImsImportCaching(true);

      for (File flistFile : files) {
        if (isCanceled()) {
          return;
        }
        rowCounter.set(0);

        final File metadataFile = new File(flistFile.toString()
            .replace(FeatureListSaveTask.DATA_FILE_SUFFIX,
                FeatureListSaveTask.METADATA_FILE_SUFFIX));
        ModularFeatureList flist = createRows(storage, flistFile, metadataFile);

        if (flist == null) {
          logger.severe(
              () -> "Cannot load feature list from files " + flistFile.getAbsolutePath() + " and "
                    + metadataFile.getAbsolutePath());
          continue;
        }
        parseFeatureList(storage, flist, flistFile);

        // disable buffering after the import (replace references to CachedIMSRawDataFiles with IMSRawDataFiles
        flist.replaceCachedFilesAndScans();

        project.addFeatureList(flist);
        processedFlists++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      project.setProjectLoadImsImportCaching(false);
      MZmineCore.getDesktop().displayErrorMessage(e.getMessage());
      return;
    }

    // disable caching on project level
    project.setProjectLoadImsImportCaching(false);
    setStatus(TaskStatus.FINISHED);
  }

  private void parseFeatureList(MemoryMapStorage storage, ModularFeatureList flist,
      File flistFile) {
    currentFlist = flist.getName();
    processedRows = 0;
    totalRows = flist.getNumberOfRows();

    try (InputStream fis = new FileInputStream(flistFile)) {
      final XMLInputFactory xif = XMLInputFactory.newInstance();
      final XMLStreamReader reader = xif.createXMLStreamReader(fis);

      while (reader.hasNext()) {
        if (isCanceled()) {
          return;
        }

        int type = reader.next();
        if (type == XMLEvent.START_ELEMENT) {
          final String localName = reader.getLocalName();
          if (CONST.XML_FEATURE_LIST_ELEMENT.equals(localName)) {
            if (!flist.getName().equals(reader.getAttributeValue(null, CONST.XML_FLIST_NAME_ATTR))
                || !flist.getDateCreated()
                .equals(reader.getAttributeValue(null, CONST.XML_DATE_CREATED_ATTR))) {
              throw new IllegalArgumentException(
                  "Feature list names do not match. " + flist.getName() + " != " + reader
                      .getAttributeValue(null, CONST.XML_FLIST_NAME_ATTR));
            }
          } else if (CONST.XML_ROW_ELEMENT.equals(localName)) {
            parseRow(reader, storage, flist);
            processedRows++;
          }
        }
      }

    } catch (IOException | XMLStreamException e) {
      logger.log(Level.WARNING, "Error opening file " + flistFile.getAbsolutePath(), e);
    }
  }

  /**
   * Creates the modular feature list from the metadata file using {@link
   * this#readMetadataCreateFeatureList(File, MemoryMapStorage)}.
   * <p></p>
   * Then passes the feature list data file once and creates the rows with the associated ids. No
   * other data will be put into the rows. This is done so rows can reference each other by their id
   * while being loaded to the feature list.
   *
   * @param storage      The storage for the feature list.
   * @param dataFile     The file containing the feature list data.
   * @param metadataFile The file containign the metadata associated with the feature list.
   * @return The created feature list with empty rows (row ids are set)
   */
  private ModularFeatureList createRows(MemoryMapStorage storage, File dataFile,
      File metadataFile) {

    ModularFeatureList flist = readMetadataCreateFeatureList(metadataFile, storage);
    if (flist == null) {
      throw new IllegalStateException("Cannot create feature list.");
    }

    try (InputStream fis = new FileInputStream(dataFile)) {
      final XMLInputFactory xif = XMLInputFactory.newInstance();
      final XMLStreamReader reader = xif.createXMLStreamReader(fis);

      logger.finest(
          () -> "Creating " + ModularFeatureListRow.class.getSimpleName() + "s for feature list "
                + flist.getName() + ".");
      while (reader.hasNext()) {
        final int type = reader.next();
        if (type == XMLEvent.START_ELEMENT && reader.getLocalName().equals(CONST.XML_ROW_ELEMENT)) {
          int id = Integer.parseInt(reader.getAttributeValue(null, idTypeUniqueID));
          flist.addRow(new ModularFeatureListRow(flist, id));
        }
      }
    } catch (IOException | XMLStreamException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return null;
    }

    logger.finest(
        () -> "Created " + flist.getNumberOfRows() + " rows in feature list " + flist.getName());
    return flist;
  }

  /**
   * Creates a feature list from the metadata xml file. Adds the selected raw data files (must be in
   * the loaded project) and sets the selected scans.
   *
   * @param file    The Metadata file.
   * @param storage The storage to use for the feature list.
   * @return The created feature list.
   */
  private ModularFeatureList readMetadataCreateFeatureList(File file, MemoryMapStorage storage) {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document configuration = dBuilder.parse(file);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      XPathExpression metadataExpr = xpath
          .compile("//" + CONST.XML_ROOT_ELEMENT + "/" + CONST.XML_FLIST_METADATA_ELEMENT);
      final Element metadataElement = (Element) (((NodeList) metadataExpr
          .evaluate(configuration, XPathConstants.NODESET)).item(0));

      final ModularFeatureList flist = new ModularFeatureList(
          metadataElement.getElementsByTagName(CONST.XML_FLIST_NAME_ELEMENT).item(0)
              .getTextContent(), storage, new ArrayList<>());

      flist.setDateCreated(
          metadataElement.getElementsByTagName(CONST.XML_FLIST_DATE_CREATED_ELEMENT).item(0)
              .getTextContent());

      XPathExpression expr = xpath.compile(
          "//" + CONST.XML_ROOT_ELEMENT + "/" + CONST.XML_FLIST_APPLIED_METHODS_LIST_ELEMENT);
      NodeList nodelist = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      if (nodelist.getLength() != 1) {
        throw new IllegalArgumentException(
            "XML file " + file + " does not have an applied methods element.");
      }
      // set applied methods
      Element appliedMethodsList = (Element) nodelist.item(0);
      NodeList methodElements = appliedMethodsList
          .getElementsByTagName(CONST.XML_FLIST_APPLIED_METHOD_ELEMENT);
      for (int i = 0; i < methodElements.getLength(); i++) {
        FeatureListAppliedMethod method = SimpleFeatureListAppliedMethod
            .loadValueFromXML((Element) methodElements.item(i));
        flist.getAppliedMethods().add(method);
      }

      XPathExpression rawFilesListExpr = xpath
          .compile("//" + CONST.XML_ROOT_ELEMENT + "/" + CONST.XML_RAW_FILES_LIST_ELEMENT);
      nodelist = (NodeList) rawFilesListExpr.evaluate(configuration, XPathConstants.NODESET);
      NodeList filesList = ((Element) nodelist.item(0))
          .getElementsByTagName(CONST.XML_RAW_FILE_ELEMENT);

      // set selected scans
      for (int i = 0; i < filesList.getLength(); i++) {
        NodeList nameList = ((Element) filesList.item(i))
            .getElementsByTagName(CONST.XML_RAW_FILE_NAME_ELEMENT);
        NodeList pathList = ((Element) filesList.item(i))
            .getElementsByTagName(CONST.XML_RAW_FILE_PATH_ELEMENT);
        String name = nameList.item(0).getTextContent();
        String path = pathList.item(0).getTextContent();

        Optional<RawDataFile> f = Arrays.stream(project.getDataFiles())
            .filter(r -> r.getName().equals(name) /*&& (
                path.isEmpty() && path.equals("null") && r.getAbsolutePath() != null ? true
                    : r.getAbsolutePath().equals(path))*/).findFirst();
        if (f.isEmpty()) {
          throw new IllegalStateException("Raw data file with name " + name + " and path " + path
                                          + " not imported to project.");
        }
        flist.getRawDataFiles().add(f.get());

        final Element selectedScansElement = (Element) ((Element) filesList.item(i))
            .getElementsByTagName(CONST.XML_FLIST_SELECTED_SCANS_ELEMENT).item(0);
        final int[] selectedScanIndices = ParsingUtils
            .stringToIntArray(selectedScansElement.getTextContent());
        final List<Scan> selectedScans = ParsingUtils
            .getSublistFromIndices(f.get().getScans(), selectedScanIndices);
        flist.setSelectedScans(f.get(), selectedScans);

      }
      return flist;
    } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }

  private void parseRow(XMLStreamReader reader, MemoryMapStorage storage, ModularFeatureList flist)
      throws XMLStreamException {
    if (!reader.getLocalName().equals(CONST.XML_ROW_ELEMENT)) {
      throw new IllegalStateException("Cannot parse row if current element is not a row element");
    }

    int id = Integer.parseInt(reader.getAttributeValue(null, idTypeUniqueID));
    final ModularFeatureListRow row = (ModularFeatureListRow) flist.getRow(rowCounter.get());
    if (id != row.getID()) {
      throw new IllegalStateException("Row ids do not match.");
    }

    while (!(reader.getEventType() == XMLEvent.END_ELEMENT && reader.getLocalName()
        .equals(CONST.XML_ROW_ELEMENT)) && reader.hasNext()) {
      if (reader.next() == XMLEvent.START_ELEMENT) {
        if (reader.getLocalName().equals(CONST.XML_FEATURE_ELEMENT)) {
          final String fileName = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
          final RawDataFile file = project.getRawDataFiles().stream()
              .filter(f -> f.getName().equals(fileName)).findFirst().orElse(null);
          if (file == null) {
            logger.warning(() -> "Cannot load feature for row id " + id + " for file " + fileName
                                 + ". File does not exist in project.");
            continue;
          }
          parseFeature(reader, storage, flist, row, file);
        } else if (reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
          DataType type = DataTypes
              .getTypeForId(reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
          Object value = parseDataType(reader, type, flist, row, null, null);
          if (type != null && value != null) {
            try {
              row.set(type, value);
            } catch (RuntimeException e) {
              // TODO - maybe log?
              logger.log(Level.WARNING, () -> String.format(
                  "DataType %s and value %s were not set to row. Maybe incompatible during loading?",
                  type, value));
              // cannot set bound values. can go silent.
            }
          }
        }
      }
    }
    rowCounter.getAndIncrement();
  }

  private void parseFeature(@NotNull XMLStreamReader reader, @Nullable MemoryMapStorage storage,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @NotNull RawDataFile file) throws XMLStreamException {

    // create feature with original file, but use buffered file for data type loading.
    final RawDataFile originalFile =
        file instanceof CachedIMSRawDataFile c ? c.getOriginalFile() : file;
    final ModularFeature feature = new ModularFeature(flist, originalFile, null, null);

    while (!(reader.getEventType() == XMLEvent.END_ELEMENT && reader.getLocalName()
        .equals(CONST.XML_FEATURE_ELEMENT)) && reader.hasNext()) {
      if (reader.next() != XMLEvent.START_ELEMENT) {
        continue;
      }

      if (reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        // the data types are responsible for loading their values
        DataType type = DataTypes
            .getTypeForId(reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
        Object value = parseDataType(reader, type, flist, row, feature, file);
        if (type != null && value != null) {
          try {
            feature.set(type, value);
          } catch (RuntimeException e) {
            // TODO - maybe log?
            logger.log(Level.WARNING, () -> String.format(
                "DataType %s and value %s were not set to row. Maybe incompatible during loading?",
                type, value));
            // cannot set bound values. can go silent.
          }
        }
      }
    }

    row.addFeature(originalFile, feature);
  }
}
