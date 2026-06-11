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

package io.github.mzmine.modules.io.projectload.version_3_0;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundFeature;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_sortannotations.PreferredAnnotationRankingModule;
import io.github.mzmine.modules.dataprocessing.filter_sortannotations.PreferredAnnotationRankingParameters;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
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
  public static final Pattern fileNamePattern = Pattern.compile(
      "([^\\n]+)(" + FeatureListSaveTask.DATA_FILE_SUFFIX + ")");

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

  /**
   * @param reader  The xml reader.
   * @param type    the data type to be read.
   * @param project The current project.
   * @param flist   The current feature list.
   * @param row     The row.
   * @param feature The current feature. Can be null.
   * @param file    The data file of the current feature. null if the feature is null.
   * @return
   */
  public static Object parseDataType(XMLStreamReader reader, DataType<?> type,
      MZmineProject project, ModularFeatureList flist, ModularFeatureListRow row,
      ModularFeature feature, RawDataFile file) {
    if (type != null) {
      try {
        return type.loadFromXML(reader, project, flist, row, feature, file);
      } catch (Exception e) {
        logger.log(Level.WARNING, e,
            () -> "Error loading data type " + type.getHeaderString() + " in row (id=" + row.getID()
                + ") feature " + (file != null ? file.getName() : "") + " from XML.");
      }
    } else {
      logger.info(() -> "No data type for id " + reader.getAttributeValue(null,
          CONST.XML_DATA_TYPE_ID_ATTR));
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

    List<FeatureList> loadedFeatureLists = new ArrayList<>();
    try {
      Path tempDirectory = FileAndPathUtil.createTempDirectory(TEMP_FLIST_DATA_FOLDER);

      logger.info(() -> "Unzipping feature lists of project to " + tempDirectory.toString());
      ZipUtils.unzipDirectory(FeatureListSaveTask.FLIST_FOLDER, zip, tempDirectory.toFile());
      logger.info(() -> "Unzipping feature lists done.");

      File[] files = new File(tempDirectory.toFile(), FeatureListSaveTask.FLIST_FOLDER).listFiles(
          (dir, name) -> fileNamePattern.matcher(name).matches());
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
        parseFeatureList(storage, project, flist, flistFile);

        // TODO maybe remove so that ModularFeatureList.getFeatureList can be unmodifiable
        // disable buffering after the import (replace references to CachedIMSRawDataFiles with IMSRawDataFiles
        flist.replaceCachedFilesAndScans();

        project.addFeatureList(flist);
        loadedFeatureLists.add(flist);
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

    //  group flists by date created, only use the latest set of feature lists in next batch step
    final Set<FeatureList> mostRecentStepFeatureLists = Set.copyOf(loadedFeatureLists.stream()
        .collect(
            Collectors.groupingBy(flist -> flist.getAppliedMethods().getLast().getModuleCallDate()))
        .entrySet().stream().max(Entry.comparingByKey()).map(Entry::getValue).orElse(List.of()));
    loadedFeatureLists.forEach(
        flist -> flist.setExcludedFromBatchLast(!mostRecentStepFeatureLists.contains(flist)));

    setStatus(TaskStatus.FINISHED);
  }

  private void parseFeatureList(MemoryMapStorage storage, MZmineProject project,
      ModularFeatureList flist, File flistFile) {
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
                  "The name of the loaded feature list does not match the expected name. %s != %s Does a feature list with this name already exist?".formatted(
                      flist.getName(), reader.getAttributeValue(null, CONST.XML_FLIST_NAME_ATTR)));
            }
          } else if (CONST.XML_ROW_ELEMENT.equals(localName)) {
            parseRow(reader, storage, project, flist);
            processedRows++;
          } else if (CONST.XML_COMPOUND_LIST_ELEMENT.equals(localName)) {
            parseCompoundList(reader, project, flist);
          }
        }
      }

    } catch (IOException | XMLStreamException e) {
      logger.log(Level.WARNING, "Error opening file " + flistFile.getAbsolutePath(), e);
    }
  }

  /**
   * Parse a {@code <compoundlist>} block in two passes within a single forward StAX scan:
   * <ul>
   * <li>Pass A: read the leading {@code <ids>} element (every compound id at every level of the
   * tree) and register a {@link ModularCompoundRow} stub for each id in the
   * {@link CompoundList}'s id index. The separate {@code <toplevel_ids>} element lists only the
   * ids that should appear in {@link CompoundList#getRows()}; this list is remembered for
   * pass C. After pass A, {@link CompoundList#findRowByCompoundId(int)} resolves any forward
   * reference, including references to nested-only compound rows that never appear in the
   * top-level list.</li>
   * <li>Pass B: for each {@code <compoundrow id="X">} (top-level or nested), look up the stub and
   * populate its data types (compound row schema) and compound features (compound features
   * schema) by iterating the XML directly — all DataType resolution goes through
   * {@link DataTypes#getTypeForId}, so no schema-type enumeration is hardcoded here.</li>
   * <li>Pass C ({@link CompoundList#finalizeLoaded(List)}): resolve the remembered top-level ids
   * to populated rows in saved order, set them as the top-level rows, rebuild the member index
   * recursively, and wire listeners.</li>
   * </ul>
   * Finally calls {@link ModularFeatureList#setCompoundList(CompoundList)}.
   */
  public static void parseCompoundList(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist)
      throws XMLStreamException {
    final String numRowsStr = reader.getAttributeValue(null, CONST.XML_NUM_ROWS_ATTR);
    final int numRows = numRowsStr != null ? Integer.parseInt(numRowsStr) : 0;

    final CompoundList cl = new CompoundList(flist, flist.getMemoryMapStorage(), numRows);

    // Ordered list of compound ids that should appear in CompoundList.getRows() after load.
    // Populated when the loader reads <toplevel_ids>; resolved to stubs at finalizeLoaded time.
    final List<Integer> topLevelIds = new ArrayList<>();

    while (reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLEvent.END_ELEMENT && CONST.XML_COMPOUND_LIST_ELEMENT.equals(
          reader.getLocalName())) {
        break;
      }
      if (event != XMLEvent.START_ELEMENT) {
        continue;
      }
      final String localName = reader.getLocalName();
      if (CONST.XML_COMPOUND_IDS_ELEMENT.equals(localName)) {
        // Pass A: stub registration for every compound id at every level
        final String text = reader.getElementText().trim();
        if (!text.isEmpty()) {
          for (final String idStr : text.split("\\s+")) {
            final int id = Integer.parseInt(idStr);
            cl.registerCompoundRowStub(new ModularCompoundRow(cl, id));
          }
        }
      } else if (CONST.XML_COMPOUND_TOP_LEVEL_IDS_ELEMENT.equals(localName)) {
        final String text = reader.getElementText().trim();
        if (!text.isEmpty()) {
          for (final String idStr : text.split("\\s+")) {
            topLevelIds.add(Integer.parseInt(idStr));
          }
        }
      } else if (CONST.XML_COMPOUND_ROW_ELEMENT.equals(localName)) {
        // Pass B: populate content of an existing stub (top-level or nested)
        final int compoundId = Integer.parseInt(
            reader.getAttributeValue(null, CONST.XML_COMPOUND_ID_ATTR));
        final ModularCompoundRow row = cl.findRowByCompoundId(compoundId);
        if (row == null) {
          logger.log(Level.WARNING,
              () -> "Skipping <compoundrow id=" + compoundId
                  + "> because no stub was created for it (missing <ids>?)");
          // skip the element
          skipElement(reader, CONST.XML_COMPOUND_ROW_ELEMENT);
          continue;
        }
        parseCompoundRow(reader, project, flist, cl, row);
      }
    }

    // Resolve top-level ids to populated stubs. Missing ids degrade gracefully — log + skip.
    final List<ModularCompoundRow> topLevelRows = new ArrayList<>(topLevelIds.size());
    for (final int id : topLevelIds) {
      final ModularCompoundRow row = cl.findRowByCompoundId(id);
      if (row == null) {
        logger.log(Level.WARNING,
            () -> "Top-level compound id " + id + " has no stub — skipping in top-level list");
        continue;
      }
      topLevelRows.add(row);
    }
    cl.finalizeLoaded(topLevelRows);
    flist.setCompoundList(cl);
  }

  private static void parseCompoundRow(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final CompoundList cl, @NotNull final ModularCompoundRow row)
      throws XMLStreamException {
    while (reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLEvent.END_ELEMENT && CONST.XML_COMPOUND_ROW_ELEMENT.equals(
          reader.getLocalName())) {
        return;
      }
      if (event != XMLEvent.START_ELEMENT) {
        continue;
      }
      final String localName = reader.getLocalName();
      if (CONST.XML_DATA_TYPE_ELEMENT.equals(localName)) {
        final DataType type = DataTypes.getTypeForId(
            reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
        final Object value = parseDataType(reader, type, project, flist, row, null, null);
        if (type != null && value != null) {
          try {
            row.set(type, value);
          } catch (RuntimeException e) {
            logger.log(Level.WARNING, () -> String.format(
                "DataType %s and value %s were not set to compound row. Maybe incompatible during loading?",
                type, value));
          }
        }
      } else if (CONST.XML_FEATURE_ELEMENT.equals(localName)) {
        final String fileName = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
        final RawDataFile rf = project.getCurrentRawDataFiles().stream()
            .filter(f -> f.getName().equals(fileName)).findFirst().orElse(null);
        if (rf == null) {
          logger.warning(
              () -> "Cannot load compound feature for compound row id " + row.getCompoundId()
                  + " for file " + fileName + ". File does not exist in project.");
          skipElement(reader, CONST.XML_FEATURE_ELEMENT);
          continue;
        }
        parseCompoundFeature(reader, project, flist, cl, row, rf);
      }
    }
  }

  private static void parseCompoundFeature(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final CompoundList cl, @NotNull final ModularCompoundRow row,
      @NotNull final RawDataFile rf) throws XMLStreamException {
    final ModularCompoundFeature cf = new ModularCompoundFeature(cl, row, rf);
    while (reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLEvent.END_ELEMENT && CONST.XML_FEATURE_ELEMENT.equals(
          reader.getLocalName())) {
        break;
      }
      if (event != XMLEvent.START_ELEMENT) {
        continue;
      }
      if (!CONST.XML_DATA_TYPE_ELEMENT.equals(reader.getLocalName())) {
        continue;
      }
      final DataType type = DataTypes.getTypeForId(
          reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
      final Object value = parseDataType(reader, type, project, flist, row, cf, rf);
      if (type != null && value != null) {
        try {
          cf.set(type, value);
        } catch (RuntimeException e) {
          logger.log(Level.WARNING, () -> String.format(
              "DataType %s and value %s were not set to compound feature. Maybe incompatible during loading?",
              type, value));
        }
      }
    }
    row.addFeature(rf, cf, false);
  }

  private static void skipElement(@NotNull final XMLStreamReader reader,
      @NotNull final String localName) throws XMLStreamException {
    int depth = 1;
    while (reader.hasNext() && depth > 0) {
      final int event = reader.next();
      if (event == XMLEvent.START_ELEMENT && localName.equals(reader.getLocalName())) {
        depth++;
      } else if (event == XMLEvent.END_ELEMENT && localName.equals(reader.getLocalName())) {
        depth--;
      }
    }
  }

  /**
   * Creates the modular feature list from the metadata file using
   * {@link this#readMetadataCreateFeatureList(File, MemoryMapStorage)}.
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
      final Document configuration = XMLUtils.load(file);

      final XPathFactory factory = XPathFactory.newInstance();
      final XPath xpath = factory.newXPath();

      XPathExpression metadataExpr = xpath.compile(
          "//" + CONST.XML_ROOT_ELEMENT + "/" + CONST.XML_FLIST_METADATA_ELEMENT);
      final Element metadataElement = (Element) (((NodeList) metadataExpr.evaluate(configuration,
          XPathConstants.NODESET)).item(0));

      XPathExpression expr = xpath.compile(
          "//" + CONST.XML_ROOT_ELEMENT + "/" + CONST.XML_FLIST_APPLIED_METHODS_LIST_ELEMENT);
      NodeList nodelist = (NodeList) expr.evaluate(configuration, XPathConstants.NODESET);
      if (nodelist.getLength() != 1) {
        throw new IllegalArgumentException(
            "XML file " + file + " does not have an applied methods element.");
      }
      // set applied methods
      Element appliedMethodsList = (Element) nodelist.item(0);
      NodeList methodElements = appliedMethodsList.getElementsByTagName(
          CONST.XML_FLIST_APPLIED_METHOD_ELEMENT);

      List<FeatureListAppliedMethod> appliedMethods = new ArrayList<>();
      for (int i = 0; i < methodElements.getLength(); i++) {
        FeatureListAppliedMethod method = SimpleFeatureListAppliedMethod.loadValueFromXML(
            (Element) methodElements.item(i));
        appliedMethods.add(method);
      }

      XPathExpression rawFilesListExpr = xpath.compile(
          "//" + CONST.XML_ROOT_ELEMENT + "/" + CONST.XML_RAW_FILES_LIST_ELEMENT);
      nodelist = (NodeList) rawFilesListExpr.evaluate(configuration, XPathConstants.NODESET);
      NodeList filesList = ((Element) nodelist.item(0)).getElementsByTagName(
          CONST.XML_RAW_FILE_ELEMENT);

      // order of raw files is not important. Will be sorted in feature list by name
      Map<RawDataFile, List<Scan>> selectedScansMap = new HashMap<>();
      for (int i = 0; i < filesList.getLength(); i++) {
        NodeList nameList = ((Element) filesList.item(i)).getElementsByTagName(
            CONST.XML_RAW_FILE_NAME_ELEMENT);
        NodeList pathList = ((Element) filesList.item(i)).getElementsByTagName(
            CONST.XML_RAW_FILE_PATH_ELEMENT);
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

        final Element selectedScansElement = (Element) ((Element) filesList.item(
            i)).getElementsByTagName(CONST.XML_FLIST_SELECTED_SCANS_ELEMENT).item(0);
        final int[] selectedScanIndices = ParsingUtils.stringToIntArray(
            selectedScansElement.getTextContent());
        final List<Scan> selectedScans = ParsingUtils.getSublistFromIndices(f.get().getScans(),
            selectedScanIndices);
        selectedScansMap.put(f.get(), selectedScans);
      }

      var dataFiles = new ArrayList<>(selectedScansMap.keySet());
      var flistName = metadataElement.getElementsByTagName(CONST.XML_FLIST_NAME_ELEMENT).item(0)
          .getTextContent();
      // need data files in constructor
      // make sure the non-cached ims files are used to create the columns in the data model.
      // the caching is only needed in the selected scans map
      final ModularFeatureList flist = new ModularFeatureList(flistName, storage,
          dataFiles.stream().map(f -> f instanceof CachedIMSRawDataFile c ? c.getOriginalFile() : f)
              .toList());
      flist.setDateCreated(
          metadataElement.getElementsByTagName(CONST.XML_FLIST_DATE_CREATED_ELEMENT).item(0)
              .getTextContent());
      flist.getAppliedMethods().addAll(appliedMethods);
      selectedScansMap.forEach(flist::setSelectedScans);

      final FeatureListAppliedMethod preferredAnnoationSorting = ParameterUtils.getLatestModuleCall(
          appliedMethods, PreferredAnnotationRankingModule.class);
      if (preferredAnnoationSorting != null) {
        PreferredAnnotationRankingParameters param = (PreferredAnnotationRankingParameters) preferredAnnoationSorting.getParameters();
        flist.setAnnotationSortConfig(param.toConfig());
      }
      return flist;
    } catch (XPathExpressionException | ParserConfigurationException | SAXException |
             IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }

  private void parseRow(XMLStreamReader reader, MemoryMapStorage storage, MZmineProject project,
      ModularFeatureList flist) throws XMLStreamException {
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
          final RawDataFile file = project.getCurrentRawDataFiles().stream()
              .filter(f -> f.getName().equals(fileName)).findFirst().orElse(null);
          if (file == null) {
            logger.warning(() -> "Cannot load feature for row id " + id + " for file " + fileName
                + ". File does not exist in project.");
            continue;
          }
          parseFeature(reader, storage, project, flist, row, file);
        } else if (reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
          DataType type = DataTypes.getTypeForId(
              reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
          Object value = parseDataType(reader, type, project, flist, row, null, null);
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
      MZmineProject project, @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
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
        DataType type = DataTypes.getTypeForId(
            reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
        Object value = parseDataType(reader, type, project, flist, row, feature, file);
        if (type != null && value != null) {
          try {
            feature.set(type, value);
          } catch (RuntimeException e) {
            // TODO - maybe log?
            logger.log(Level.WARNING, () -> String.format(
                "DataType %s and value %s were not set to feature. Maybe incompatible during loading?",
                type, value));
            // cannot set bound values. can go silent.
          }
        }
      }
    }

    DataTypeUtils.applyFeatureSpecificGraphicalTypes(feature);
    row.addFeature(originalFile, feature);
  }
}
