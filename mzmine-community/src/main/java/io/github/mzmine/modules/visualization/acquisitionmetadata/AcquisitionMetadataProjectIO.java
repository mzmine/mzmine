/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.visualization.acquisitionmetadata;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Term;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.projectsave.RawDataFileSaveHandler;
import io.github.mzmine.project.ProjectService;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Saves acquisition declarations and native table edits alongside the raw-file import descriptor.
 *
 * <p>Records are joined only by the raw-import source path and mzmine project file name. A project
 * loader supplies the resolved path map for embedded files, so the temporary extraction directory
 * never becomes an identity. Ambiguous or malformed records fail loading instead of being attached
 * to another file.
 */
public final class AcquisitionMetadataProjectIO {

  public static final String ACQUISITION_METADATA_FILENAME = "acquisition_metadata.xml";
  private static final String FORMAT_VERSION = "1";
  private static final int MAX_FILES = 10_000;
  private static final int MAX_FIELDS_PER_FILE = 256;
  private static final int MAX_VALUE_BYTES = 65_536;
  private static final int MAX_ARCHIVE_BYTES = 64 * 1024 * 1024;

  private AcquisitionMetadataProjectIO() {
  }

  public static boolean saveToZip(final @NotNull ZipOutputStream zipStream) throws IOException {
    return saveToZip(zipStream, false);
  }

  /**
   * @param saveFilesInProject whether raw-data paths are rewritten to embedded-project paths
   */
  public static boolean saveToZip(final @NotNull ZipOutputStream zipStream,
      final boolean saveFilesInProject) throws IOException {
    final List<RawDataFile> files = ProjectService.getProject().getCurrentRawDataFiles();
    final BoundedOutputStream output = new BoundedOutputStream(MAX_ARCHIVE_BYTES);
    try {
      final XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output,
          StandardCharsets.UTF_8.name());
      xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      xml.writeStartElement("acquisition-metadata");
      xml.writeAttribute("version", FORMAT_VERSION);
      int saved = 0;
      final Map<RawFileIdentity, RawDataFile> sources = new LinkedHashMap<>();
      for (final RawDataFile file : files) {
        final AcquisitionMetadata metadata = file.getAcquisitionMetadata();
        if (metadata.equals(AcquisitionMetadata.EMPTY)) {
          continue;
        }
        final String source = savedSource(file, saveFilesInProject);
        if (source == null) {
          continue;
        }
        final RawFileIdentity identity = new RawFileIdentity(source, file.getName());
        if (sources.putIfAbsent(identity, file) != null) {
          throw new IOException("Ambiguous raw-data import identity in acquisition metadata.");
        }
        if (++saved > MAX_FILES) {
          throw new IOException("Too many acquisition metadata records.");
        }
        if (metadata.terms().size() + metadata.localFields().size() + metadata.editedValues().size()
            > MAX_FIELDS_PER_FILE) {
          throw new IOException("Too many acquisition metadata fields.");
        }
        xml.writeStartElement("raw-file");
        xml.writeAttribute("source", encode(source));
        xml.writeAttribute("name", encode(file.getName()));
        for (final Term term : metadata.terms()) {
          xml.writeEmptyElement("controlled-term");
          xml.writeAttribute("field", term.field().name());
          xml.writeAttribute("accession", encode(term.accession()));
          xml.writeAttribute("label", encode(term.label()));
        }
        for (final var field : metadata.localFields().entrySet()) {
          xml.writeEmptyElement("header-field");
          xml.writeAttribute("name", encode(field.getKey()));
          xml.writeAttribute("value", encode(field.getValue()));
        }
        for (final var field : metadata.editedValues().entrySet()) {
          xml.writeEmptyElement("edited-field");
          xml.writeAttribute("name", encode(field.getKey()));
          xml.writeAttribute("value", encode(field.getValue()));
        }
        xml.writeEndElement();
      }
      xml.writeEndElement();
      xml.writeEndDocument();
      xml.close();
      if (saved == 0) {
        return false;
      }
    } catch (XMLStreamException exception) {
      throw new IOException("Could not write acquisition metadata.", exception);
    }

    zipStream.putNextEntry(new ZipEntry(ACQUISITION_METADATA_FILENAME));
    zipStream.write(output.toByteArray());
    zipStream.closeEntry();
    return true;
  }

  public static boolean loadFromZip(final @NotNull ZipFile zipFile) throws IOException {
    return loadFromZip(zipFile, Map.of());
  }

  /**
   * @param resolvedRawSources map from saved import paths to paths resolved during project opening
   */
  public static boolean loadFromZip(final @NotNull ZipFile zipFile,
      final @NotNull Map<String, String> resolvedRawSources) throws IOException {
    final ZipEntry entry = zipFile.getEntry(ACQUISITION_METADATA_FILENAME);
    if (entry == null) {
      return false;
    }
    final Map<RawFileIdentity, RawDataFile> files = filesByAbsolutePath();
    if (entry.getSize() > MAX_ARCHIVE_BYTES) {
      throw new IOException("Acquisition metadata archive entry is too large.");
    }
    final byte[] bytes;
    try (var input = zipFile.getInputStream(entry)) {
      bytes = input.readNBytes(MAX_ARCHIVE_BYTES + 1);
    }
    if (bytes.length > MAX_ARCHIVE_BYTES) {
      throw new IOException("Acquisition metadata archive entry is too large.");
    }
    try {
      final XMLInputFactory factory = XMLInputFactory.newFactory();
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
      final XMLStreamReader xml = factory.createXMLStreamReader(new ByteArrayInputStream(bytes),
          StandardCharsets.UTF_8.name());
      int records = 0;
      boolean rootSeen = false;
      final Map<RawFileIdentity, RawFileRecord> parsed = new LinkedHashMap<>();
      while (xml.hasNext()) {
        final int event = xml.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          if ("acquisition-metadata".equals(xml.getLocalName())) {
            rootSeen = true;
            require(FORMAT_VERSION.equals(xml.getAttributeValue(null, "version")),
                "Unsupported acquisition metadata format.");
          } else if ("raw-file".equals(xml.getLocalName())) {
            if (++records > MAX_FILES) {
              throw new IOException("Too many acquisition metadata records.");
            }
            final RawFileRecord record = readRawFile(xml);
            if (parsed.putIfAbsent(record.identity(), record) != null) {
              throw new IOException("Duplicate acquisition metadata source record.");
            }
          }
        }
      }
      xml.close();
      require(rootSeen, "Missing acquisition metadata root element.");
      // Do not mutate any loaded file until every record has passed validation.
      for (final RawFileRecord record : parsed.values()) {
        final String resolvedSource = resolvedRawSources.getOrDefault(record.source(), record.source());
        final RawDataFile file = files.get(new RawFileIdentity(resolvedSource, record.name()));
        if (file != null) {
          file.setAcquisitionMetadata(new AcquisitionMetadata(record.terms(), record.fields(), record.editedValues()));
        }
      }
      return true;
    } catch (XMLStreamException | IllegalArgumentException exception) {
      throw new IOException("Could not read acquisition metadata.", exception);
    }
  }

  private static @NotNull RawFileRecord readRawFile(final @NotNull XMLStreamReader xml)
      throws XMLStreamException, IOException {
    final String savedSource = decode(requiredAttribute(xml, "source"));
    final String name = decode(requiredAttribute(xml, "name"));
    final List<Term> terms = new java.util.ArrayList<>();
    final Map<String, String> fields = new LinkedHashMap<>();
    final Map<String, String> editedValues = new LinkedHashMap<>();
    int fieldsRead = 0;
    while (xml.hasNext()) {
      final int event = xml.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        switch (xml.getLocalName()) {
          case "controlled-term" -> {
            if (++fieldsRead > MAX_FIELDS_PER_FILE) {
              throw new IOException("Too many acquisition metadata fields.");
            }
            terms.add(new Term(AcquisitionMetadata.Field.valueOf(requiredAttribute(xml, "field")),
                decode(requiredAttribute(xml, "accession")), decode(requiredAttribute(xml, "label"))));
          }
          case "header-field" -> {
            if (++fieldsRead > MAX_FIELDS_PER_FILE) {
              throw new IOException("Too many acquisition metadata fields.");
            }
            final String fieldName = decode(requiredAttribute(xml, "name"));
            if (fields.putIfAbsent(fieldName, decode(requiredAttribute(xml, "value"))) != null) {
              throw new IOException("Duplicate acquisition metadata header field.");
            }
          }
          case "edited-field" -> {
            if (++fieldsRead > MAX_FIELDS_PER_FILE) {
              throw new IOException("Too many acquisition metadata fields.");
            }
            final String fieldName = decode(requiredAttribute(xml, "name"));
            if (editedValues.putIfAbsent(fieldName, decode(requiredAttribute(xml, "value"))) != null) {
              throw new IOException("Duplicate edited acquisition metadata field.");
            }
          }
          default -> throw new IOException("Unexpected acquisition metadata element.");
        }
      } else if (event == XMLStreamConstants.END_ELEMENT && "raw-file".equals(xml.getLocalName())) {
        break;
      }
    }
    return new RawFileRecord(savedSource, name, terms, fields, editedValues);
  }

  private static @NotNull Map<RawFileIdentity, RawDataFile> filesByAbsolutePath() throws IOException {
    final Map<RawFileIdentity, RawDataFile> files = new LinkedHashMap<>();
    for (final RawDataFile file : ProjectService.getProject().getCurrentRawDataFiles()) {
      final String source = file.getAbsolutePath();
      if (source == null) {
        continue;
      }
      if (files.putIfAbsent(new RawFileIdentity(source, file.getName()), file) != null) {
        throw new IOException("Ambiguous loaded raw-data identity.");
      }
    }
    return files;
  }

  private static String savedSource(final @NotNull RawDataFile file,
      final boolean saveFilesInProject) {
    if (file.getAbsolutePath() == null) {
      return null;
    }
    return saveFilesInProject ? RawDataFileSaveHandler.getZipPath(file,
        RawDataFileSaveHandler.DATA_FILES_PREFIX, RawDataFileSaveHandler.DATA_FILES_SUFFIX)
        : file.getAbsolutePath();
  }

  private static @NotNull String requiredAttribute(final @NotNull XMLStreamReader xml,
      final @NotNull String name) throws IOException {
    final String value = xml.getAttributeValue(null, name);
    if (value == null) {
      throw new IOException("Missing acquisition metadata attribute " + name + ".");
    }
    return value;
  }

  private static void require(final boolean condition, final String message) throws IOException {
    if (!condition) {
      throw new IOException(message);
    }
  }

  private static @NotNull String encode(final @NotNull String value) throws IOException {
    final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_VALUE_BYTES) {
      throw new IOException("Acquisition metadata value is too large.");
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static @NotNull String decode(final @NotNull String value) throws IOException {
    try {
      final byte[] bytes = Base64.getUrlDecoder().decode(value);
      if (bytes.length > MAX_VALUE_BYTES) {
        throw new IOException("Acquisition metadata value is too large.");
      }
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      throw new IOException("Invalid acquisition metadata value.", exception);
    }
  }

  private record RawFileRecord(@NotNull String source, @NotNull String name, @NotNull List<Term> terms,
                               @NotNull Map<String, String> fields,
                               @NotNull Map<String, String> editedValues) {
    private @NotNull RawFileIdentity identity() {
      return new RawFileIdentity(source, name);
    }
  }

  private record RawFileIdentity(@NotNull String source, @NotNull String name) {
  }

  /** Bounded in-memory XML output so saving cannot create an unreadable archive entry. */
  static final class BoundedOutputStream extends OutputStream {

    private final int limit;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    BoundedOutputStream(final int limit) {
      this.limit = limit;
    }

    @Override
    public void write(final int value) throws IOException {
      ensureCapacity(1);
      output.write(value);
    }

    @Override
    public void write(final @NotNull byte[] bytes, final int offset, final int length)
        throws IOException {
      ensureCapacity(length);
      output.write(bytes, offset, length);
    }

    @NotNull byte[] toByteArray() {
      return output.toByteArray();
    }

    private void ensureCapacity(final int added) throws IOException {
      if (added < 0 || output.size() > limit - added) {
        throw new IOException("Acquisition metadata archive entry is too large.");
      }
    }
  }
}
