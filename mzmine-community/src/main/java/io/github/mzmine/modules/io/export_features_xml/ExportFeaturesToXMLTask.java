package io.github.mzmine.modules.io.export_features_xml;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportFeaturesToXMLTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(ExportFeaturesToXMLTask.class.getName());

  private final FeatureList flist;
  private File file;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected ExportFeaturesToXMLTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, FeatureList flist) {
    super(storage, moduleCallDate, parameters, moduleClass);

    this.flist = flist;
    file = parameters.getValue(ExportFeaturesToXMLParameters.file);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    file = SiriusExportTask.getFileForFeatureList(flist, file, "{}", ".xml");

    try (OutputStream os = Files.newOutputStream(file.toPath(),
        WriterOptions.REPLACE.toOpenOption())) {
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      final XMLStreamWriter writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(os));
      writer.writeStartDocument("UTF-8", "1.0");

      writer.writeStartElement("featurelist"); // feature list
      writer.writeAttribute(CONST.XML_FLIST_NAME_ATTR, flist.getName());
      writer.writeAttribute(CONST.XML_NUM_ROWS_ATTR, String.valueOf(flist.getNumberOfRows()));
      writer.writeAttribute(CONST.XML_DATE_CREATED_ATTR, flist.getDateCreated());

      for (final var row : flist.getRowsCopy()) {
        writer.writeStartElement(CONST.XML_ROW_ELEMENT); // row
        writer.writeAttribute(new IDType().getUniqueID(), String.valueOf(row.getID()));

        for (final var feature : row.getFeatures()) {
          final RawDataFile file = feature.getRawDataFile();
          if (file == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
            continue;
          }

          writer.writeStartElement(CONST.XML_FEATURE_ELEMENT); // feature
          writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, file.getName());

          final FeatureDataType type = new FeatureDataType();
          final IonTimeSeries<? extends Scan> series = feature.getFeatureData();

          writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT); // data type element
          writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, type.getUniqueID());

          final List<? extends Scan> selectedScans = flist.getSeletedScans(file);
          if (selectedScans == null) {
            // sanity check during saving.
            throw new IllegalArgumentException("Cannot find selected scans.");
          }

          { // feature data
            writer.writeStartElement(type.getUniqueID());
            series.saveValueToXML(writer, (List) file.getScans(),
                true); // use ALL scans of the given raw data file.
            writer.writeEndElement();
          }
          writer.writeEndElement(); // data type element

          writer.writeEndElement(); // feature
        }

        writer.writeEndElement(); // row
      }

      writer.writeEndElement(); // feature list
      writer.writeEndDocument();
    } catch (XMLStreamException | IOException e) {
      logger.log(Level.SEVERE, "Error while expoting feature list " + flist.getName() + " to xml.",
          e);
    }
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list " + flist.getName();
  }
}
