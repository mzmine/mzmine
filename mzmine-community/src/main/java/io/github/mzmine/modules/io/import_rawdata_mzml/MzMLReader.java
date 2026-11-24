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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.header.MzMLHeaderMetadata;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLTags;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forward-only reader for ordinary and indexed mzML. Each instance owns one document.
 */
final class MzMLReader {

  private final XMLStreamReader xml;
  private final BooleanSupplier canceled;
  private final MzMLRecordReader records;
  private final MzMLHeaderMetadata header = new MzMLHeaderMetadata();
  private final List<MzMLChromatogram> chromatograms = new ArrayList<>();
  private @Nullable String timestamp;
  private final List<BuildingMobilityScanStorage> frames = new ArrayList<>();
  private final List<BuildingMzMLMsScan> otherSpectra = new ArrayList<>();
  private final MzMLFrameAssembler assembler;
  private final MzMLScanAssembler spectra;
  private final ScanImportProcessorConfig config;
  private final @Nullable MemoryMapStorage storage;
  private int declaredSpectra;
  private int parsedSpectra;

  MzMLReader(@NotNull final XMLStreamReader xml, @NotNull final File source,
      @NotNull final ScanImportProcessorConfig config, @Nullable final MemoryMapStorage storage,
      @NotNull final BooleanSupplier canceled) {
    this.xml = xml;
    this.config = config;
    this.storage = storage;
    this.canceled = canceled;
    spectra = new MzMLScanAssembler(source, storage);
    assembler = new MzMLFrameAssembler(frames, config, storage);
    records = new MzMLRecordReader(xml, header);
  }

  /**
   * @return imported records after consuming the entire XML document, including its footer
   */
  @NotNull MzMLImportData readDocument() throws XMLStreamException {
    xml.nextTag();
    if (xml.getLocalName().equals(MzMLTags.TAG_INDEXED_MZML)) {
      boolean found = false;
      while (nextChild()) {
        if (xml.getLocalName().equals(MzMLTags.TAG_MZML) && !found) {
          readMzML();
          found = true;
        } else {
          MzMLXml.skipElement(xml);
        }
      }
      if (!found) {
        throw new XMLStreamException("Missing mzML inside indexedmzML", xml.getLocation());
      }
    } else {
      xml.require(XMLStreamConstants.START_ELEMENT, null, MzMLTags.TAG_MZML);
      readMzML();
    }
    while (xml.hasNext()) {
      xml.next();
    }
    return new MzMLImportData(header, timestamp, spectra, frames, otherSpectra, chromatograms,
        assembler.warning());
  }

  private void readMzML() throws XMLStreamException {
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_RUN)) {
        readRun();
      } else {
        header.readSection(xml);
      }
    }
  }

  private void readRun() throws XMLStreamException {
    final String instrument = MzMLXml.required(xml,
        MzMLTags.ATTR_DEFAULT_INSTRUMENT_CONFIGURATION_REF);
    header.setRunReferences(instrument,
        xml.getAttributeValue(null, MzMLTags.ATTR_DEFAULT_SOURCE_FILE_REF),
        xml.getAttributeValue(null, MzMLTags.ATTR_SAMPLE_REF));
    timestamp = xml.getAttributeValue(null, MzMLTags.ATTR_START_TIME_STAMP);
    while (nextChild()) {
      switch (xml.getLocalName()) {
        case MzMLTags.TAG_SPECTRUM_LIST -> readSpectrumList();
        case MzMLTags.TAG_CHROMATOGRAM_LIST -> readChromatogramList();
        default -> MzMLXml.skipElement(xml);
      }
    }
  }

  private void readSpectrumList() throws XMLStreamException {
    declaredSpectra = Integer.parseInt(MzMLXml.required(xml, MzMLTags.ATTR_COUNT));
    MzMLXml.required(xml, MzMLTags.ATTR_DEFAULT_DATA_PROCESSING_REF);
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_SPECTRUM)) {
        final String id = xml.getAttributeValue(null, MzMLTags.ATTR_ID);
        try {
          acceptSpectrum(records.readSpectrum());
        } catch (RuntimeException | XMLStreamException e) {
          throw new XMLStreamException("Cannot read spectrum " + id, xml.getLocation(), e);
        }
        parsedSpectra++;
      } else {
        MzMLXml.skipElement(xml);
      }
    }
    assembler.finishFrame();
  }

  private void readChromatogramList() throws XMLStreamException {
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_CHROMATOGRAM)) {
        final String id = xml.getAttributeValue(null, MzMLTags.ATTR_ID);
        try {
          final var trace = records.readChromatogram();
          if (trace.hasBinaryArrays()) {
            chromatograms.add(trace);
          }
        } catch (RuntimeException | XMLStreamException e) {
          throw new XMLStreamException("Cannot read chromatogram " + id, xml.getLocation(), e);
        }
      } else {
        MzMLXml.skipElement(xml);
      }
    }
  }

  private void acceptSpectrum(@NotNull final BuildingMzMLMsScan scan) {
    if (scan.isUVSpectrum()) {
      assembler.finishFrame();
      if (scan.loadProcessMemMapUvData(storage)) {
        otherSpectra.add(scan);
      }
      return;
    }
    if (scan.getMzBinaryDataInfo() == null) {
      return;
    }
    if (scan.getIntensityBinaryDataInfo() == null
        || scan.getMzBinaryDataInfo().getArrayLength() != scan.getIntensityBinaryDataInfo()
        .getArrayLength()) {
      throw new IllegalArgumentException(
          "Missing or mismatched intensity array in " + scan.getId());
    }
    if (scan.isMergedMobilitySpectrum() || scan.getMobility() != null) {
      assembler.accept(scan);
    } else {
      assembler.finishFrame();
      if (config.scanFilter().matches(scan) && scan.loadProcessMemMapMzData(storage, config)) {
        spectra.add(scan, config.isMassDetectActive(scan.getMSLevel()));
      }
    }
  }

  private boolean nextChild() throws XMLStreamException {
    if (canceled.getAsBoolean()) {
      throw new CancellationException();
    }
    return xml.nextTag() == XMLStreamConstants.START_ELEMENT;
  }

  int declaredSpectra() {
    return declaredSpectra;
  }

  int parsedSpectra() {
    return parsedSpectra;
  }
}
