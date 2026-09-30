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

package io.github.mzmine.modules.io.projectsave;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.rawfiletypes.RawFileMetadataType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Saves and restores the file metadata of all raw data files
 * ({@link RawDataFile#getFileMetadata()}) in an mzmine project archive. Only data types tagged with
 * {@link RawFileMetadataType} are saved. Their XML methods receive a dummy feature list and row.
 */
public final class RawFileMetadataProjectIO {

  public static final String RAW_FILE_METADATA_FILENAME = "raw_file_metadata.xml";

  private static final Logger logger = Logger.getLogger(RawFileMetadataProjectIO.class.getName());

  private static final String XML_ROOT_ELEMENT = "rawfilemetadata";
  private static final String XML_VERSION_ATTR = "version";
  private static final String XML_VERSION = "1";

  private RawFileMetadataProjectIO() {
  }

  /**
   * @return true if metadata was written, false if no raw file has metadata
   */
  public static boolean saveToZip(@NotNull final ZipOutputStream zipStream,
      @NotNull final MZmineProject project) throws IOException {
    final List<RawDataFile> files = project.getCurrentRawDataFiles().stream()
        .filter(file -> !file.getFileMetadata().isEmpty()).toList();
    if (files.isEmpty()) {
      return false;
    }

    final ModularFeatureList dummyFlist = FeatureList.createDummy();
    final ModularFeatureListRow dummyRow = new ModularFeatureListRow(dummyFlist, 0);

    zipStream.putNextEntry(new ZipEntry(RAW_FILE_METADATA_FILENAME));
    try {
      final XMLStreamWriter writer = new IndentingXMLStreamWriter(
          XMLOutputFactory.newInstance().createXMLStreamWriter(zipStream, "UTF-8"));
      writer.writeStartDocument("UTF-8", "1.0");
      writer.writeStartElement(XML_ROOT_ELEMENT);
      writer.writeAttribute(XML_VERSION_ATTR, XML_VERSION);

      for (final RawDataFile file : files) {
        writer.writeStartElement(CONST.XML_RAW_FILE_ELEMENT);
        writer.writeAttribute(CONST.XML_RAW_FILE_NAME_ELEMENT, file.getName());
        writeMetadata(writer, file, dummyFlist, dummyRow);
        writer.writeEndElement();
      }

      writer.writeEndElement();
      writer.writeEndDocument();
      writer.flush();
      // does not close the underlying zip stream
      writer.close();
    } catch (XMLStreamException e) {
      throw new IOException("Cannot save raw file metadata: " + e.getMessage(), e);
    }
    zipStream.closeEntry();
    return true;
  }

  private static void writeMetadata(@NotNull final XMLStreamWriter writer,
      @NotNull final RawDataFile file, @NotNull final ModularFeatureList dummyFlist,
      @NotNull final ModularFeatureListRow dummyRow) throws XMLStreamException {
    final ModularDataModel metadata = file.getFileMetadata();
    for (final Entry<DataType, Object> entry : metadata.stream().toList()) {
      final DataType<?> type = entry.getKey();
      final Object value = entry.getValue();
      if (value == null) {
        continue;
      }
      if (!(type instanceof RawFileMetadataType)) {
        // decision: only tagged types guarantee that they do not rely on flist and row
        logger.fine(() -> "Raw file metadata type %s is not saved, it is not a %s".formatted(
            type.getClass().getName(), RawFileMetadataType.class.getSimpleName()));
        continue;
      }
      writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
      writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, type.getUniqueID());
      try {
        type.saveToXML(writer, value, dummyFlist, dummyRow, null, file);
      } catch (XMLStreamException | RuntimeException e) {
        logger.log(Level.WARNING,
            "Cannot save raw file metadata %s of file %s: %s".formatted(type.getUniqueID(),
                file.getName(), e.getMessage()), e);
      }
      writer.writeEndElement();
    }
  }

  /**
   * Loads the metadata into the raw data files of the project, matched by file name. Loaded values
   * replace values that were set during import.
   *
   * @return true if the archive contained raw file metadata
   */
  public static boolean loadFromZip(@NotNull final ZipFile zipFile,
      @NotNull final MZmineProject project) throws IOException {
    final ZipEntry entry = zipFile.getEntry(RAW_FILE_METADATA_FILENAME);
    if (entry == null) {
      return false;
    }

    final ModularFeatureList dummyFlist = FeatureList.createDummy();
    final ModularFeatureListRow dummyRow = new ModularFeatureListRow(dummyFlist, 0);

    try (InputStream is = zipFile.getInputStream(entry)) {
      final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
      try {
        RawDataFile currentFile = null;
        while (reader.hasNext()) {
          if (reader.next() != XMLStreamConstants.START_ELEMENT) {
            continue;
          }
          final String element = reader.getLocalName();
          if (CONST.XML_RAW_FILE_ELEMENT.equals(element)) {
            final String name = reader.getAttributeValue(null, CONST.XML_RAW_FILE_NAME_ELEMENT);
            currentFile = project.getDataFileByName(name);
            if (currentFile == null) {
              logger.warning(
                  "Cannot load raw file metadata for %s. File does not exist in project.".formatted(
                      name));
            }
          } else if (CONST.XML_DATA_TYPE_ELEMENT.equals(element)) {
            readDataType(reader, currentFile, project, dummyFlist, dummyRow);
          }
        }
      } finally {
        reader.close();
      }
    } catch (XMLStreamException e) {
      throw new IOException("Cannot load raw file metadata: " + e.getMessage(), e);
    }
    return true;
  }

  private static void readDataType(@NotNull final XMLStreamReader reader,
      @Nullable final RawDataFile file, @NotNull final MZmineProject project,
      @NotNull final ModularFeatureList dummyFlist, @NotNull final ModularFeatureListRow dummyRow)
      throws XMLStreamException {
    final String typeId = reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR);
    final DataType type = DataTypes.getTypeForId(typeId);
    if (file == null || !(type instanceof RawFileMetadataType)) {
      if (file != null) {
        logger.info(() -> "Unknown raw file metadata type " + typeId + ", skipping");
      }
      skipElement(reader);
      return;
    }
    try {
      final Object value = type.loadFromXML(reader, project, dummyFlist, dummyRow, null, file);
      if (value != null) {
        file.getFileMetadata().set(type, value);
      }
    } catch (XMLStreamException | RuntimeException e) {
      logger.log(Level.WARNING,
          "Cannot load raw file metadata %s of file %s: %s".formatted(typeId, file.getName(),
              e.getMessage()), e);
    }
  }

  /**
   * Skips the current element including all children. Reader must be at the start element.
   */
  private static void skipElement(@NotNull final XMLStreamReader reader) throws XMLStreamException {
    int depth = 1;
    while (depth > 0 && reader.hasNext()) {
      final int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        depth--;
      }
    }
  }
}
