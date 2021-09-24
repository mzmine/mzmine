package io.github.mzmine.modules.io.projectsave;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.StreamCopy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.beans.property.Property;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FeatureListSaveTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeatureListSaveTask.class.getName());


  private static final String METADATA_FILE_SUFFIX = "_metadata.xml";
  private static final String DATA_FILE_SUFFIX = "_data.xml";
  private static final String FLIST_FOLDER = "featurelists/";

  private final ModularFeatureList flist;
  private final ZipOutputStream zos;
  private final int rows;
  private final StreamCopy copy;
  private int processedRows = 0;

  public FeatureListSaveTask(ModularFeatureList flist, ZipOutputStream zos) {
    super(null, new Date());
    this.flist = flist;
    this.zos = zos;
    rows = flist.getNumberOfRows();
    copy = new StreamCopy();
  }

  @Override
  public String getTaskDescription() {
    return "Saving feature list " + flist.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return (((double) processedRows / rows) + copy.getProgress()) / 2;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (!saveFeatureData()) {
      return;
    }

    saveAppliedMethods();

    setStatus(TaskStatus.FINISHED);
  }

  private boolean saveAppliedMethods() {
    logger.finest(() -> "Creating temporary file for feature list " + flist.getName() + ".");
    File tempFile;
    try {
      tempFile = File.createTempFile("mzmine_featurelist_applied_methods", ".tmp");
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot create temporary file.", e);
      setStatus(TaskStatus.ERROR);
      return false;
    }

    try {
      final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document document = dBuilder.newDocument();
      final Element root = document.createElement(CONST.XML_ROOT_ELEMENT);
      document.appendChild(root);
      final Element appliedMethodsList = document
          .createElement(CONST.XML_APPLIED_METHODS_LIST_ELEMENT);
      root.appendChild(appliedMethodsList);
      appliedMethodsList.setAttribute(CONST.XML_FLIST_NAME_ATTR, flist.getName());

      for (FeatureListAppliedMethod appliedMethod : flist.getAppliedMethods()) {
        Element methodElement = document.createElement(CONST.XML_APPLIED_METHOD_ELEMENT);
        appliedMethod.saveValueToXML(methodElement);
        appliedMethodsList.appendChild(methodElement);
      }

      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer transformer = transfac.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      try (var stream = new FileOutputStream(tempFile)) {
        StreamResult result = new StreamResult(stream);
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
      } catch (IOException e) {
        e.printStackTrace();
      }

      zos.putNextEntry(new ZipEntry(getMetadataFileName(flist)));

      try (InputStream is = new FileInputStream(tempFile)) {
        copy.copy(is, zos);
      }

      tempFile.delete();
    } catch (ParserConfigurationException | TransformerException | IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return false;
    }

    return true;
  }

  private boolean saveFeatureData() {
    logger.finest(() -> "Creating temporary file for feature list " + flist.getName() + ".");
    File tempFile;
    try {
      tempFile = File.createTempFile("mzmine_featurelist_data", ".tmp");
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot create temporary file.", e);
      setStatus(TaskStatus.ERROR);
      return false;
    }

    try (OutputStream os = new FileOutputStream(tempFile)) {
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      final XMLStreamWriter writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(os));
      writer.writeStartDocument("UTF-8", "1.0");

      writer.writeStartElement("featurelist");
      writer.writeAttribute(CONST.XML_FLIST_NAME_ATTR, flist.getName());
      writer.writeAttribute(CONST.XML_NUM_ROWS_ATTR, String.valueOf(flist.getNumberOfRows()));
      writer.writeAttribute(CONST.XML_DATE_CREATED_ATTR, flist.getDateCreated());

      for (FeatureListRow r : flist.getRows()) {
        if (isCanceled()) {
          break;
        }

        ModularFeatureListRow row = (ModularFeatureListRow) r;
        writeRow(writer, row);

        processedRows++;
      }
      writer.writeEndElement();
    } catch (IOException | XMLStreamException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (isCanceled()) {
      tempFile.delete();
      return false;
    }

    try (FileInputStream is = new FileInputStream(tempFile)) {
      zos.putNextEntry(new ZipEntry(getDataFileName(flist)));
      copy.copy(is, zos);
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return false;
    }

    tempFile.delete();
    return true;
  }

  private void writeRow(XMLStreamWriter writer, ModularFeatureListRow row)
      throws XMLStreamException {

    writer.writeStartElement(CONST.XML_ROW_ELEMENT);

    for (Entry<DataType, Property<?>> entry : row.getMap().entrySet()) {
      DataType<?> dataType = entry.getKey();
      Property<?> valueProperty = entry.getValue();
      if (dataType instanceof FeaturesType) {
        continue;
      }
      writeDataType(writer, dataType, valueProperty.getValue(), flist, row, null, null);
    }

    for (ModularFeature feature : row.getFeatures()) {
      writeFeature(writer, row, feature);
    }

    writer.writeEndElement();
  }

  private void writeDataType(XMLStreamWriter writer, DataType<?> dataType,
      @Nullable final Object value, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {

    writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
    writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, dataType.getUniqueID());

    try { // catch here, so we can easily debug and don't destroy the flist while saving in case an unexpected exception happens
      dataType.saveToXML(writer, value, flist, row, feature, file);
    } catch (XMLStreamException e) {
      logger.warning(() -> "Error while writing data type " + dataType.getClass().getSimpleName()
          + " with value " + String.valueOf(value) + " to xml.");
      e.printStackTrace();
    }
    writer.writeEndElement();
  }

  private void writeFeature(XMLStreamWriter writer, ModularFeatureListRow row,
      ModularFeature feature) throws XMLStreamException {
    final RawDataFile rawDataFile = feature.getRawDataFile();

    writer.writeStartElement(CONST.XML_FEATURE_ELEMENT);
    writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, rawDataFile.getName());

    for (Entry<DataType, Property<?>> entry : feature.getMap().entrySet()) {
      writeDataType(writer, entry.getKey(), entry.getValue().getValue(), flist, row, feature,
          rawDataFile);
    }

    writer.writeEndElement();
  }

  private String getDataFileName(ModularFeatureList flist) {
    return FLIST_FOLDER + CONST.XML_FEATURE_LIST_ELEMENT + "_" + flist.getName() + DATA_FILE_SUFFIX;
  }

  private String getMetadataFileName(ModularFeatureList flist) {
    return FLIST_FOLDER + CONST.XML_FEATURE_LIST_ELEMENT + "_" + flist.getName()
        + METADATA_FILE_SUFFIX;
  }
}
