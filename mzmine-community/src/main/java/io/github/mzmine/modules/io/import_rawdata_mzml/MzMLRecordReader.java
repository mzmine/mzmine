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

import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLArrayType;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLBinaryDataInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLBitLength;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLCompressionType;
import io.github.mzmine.modules.io.import_rawdata_mzml.header.MzMLHeaderMetadata;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCVGroup;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLIsolationWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorActivation;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorSelectedIon;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorSelectedIonList;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLProduct;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScanList;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScanWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScanWindowList;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLTags;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLUserParam;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads one spectrum or chromatogram from the run. Each read method enters on the element's start
 * tag and leaves the cursor on its matching end tag, including for empty elements.
 */
final class MzMLRecordReader {

  private static final Pattern SCAN = Pattern.compile("scan=([0-9]+)");
  private static final Pattern AGILENT_SCAN = Pattern.compile("scan[iI]d=([0-9]+)");
  private final XMLStreamReader xml;
  private final MzMLHeaderMetadata header;

  MzMLRecordReader(@NotNull final XMLStreamReader xml, @NotNull final MzMLHeaderMetadata header) {
    this.xml = xml;
    this.header = header;
  }

  /**
   * @return the spectrum, including encoded payloads for classification and processing
   */
  @NotNull BuildingMzMLMsScan readSpectrum() throws XMLStreamException {
    final String id = MzMLXml.required(xml, MzMLTags.ATTR_ID);
    final int index = Integer.parseInt(MzMLXml.required(xml, MzMLTags.ATTR_INDEX));
    final int length = Integer.parseInt(MzMLXml.required(xml, MzMLTags.ATTR_DEFAULT_ARRAY_LENGTH));
    final var spectrum = new BuildingMzMLMsScan(id, scanNumber(id, index));
    while (nextChild()) {
      switch (xml.getLocalName()) {
        case MzMLTags.TAG_SCAN_LIST -> readScanList(spectrum.getScanList());
        case MzMLTags.TAG_PRECURSOR_LIST -> {
          while (nextChild()) {
            if (xml.getLocalName().equals(MzMLTags.TAG_PRECURSOR)) {
              spectrum.getPrecursorList().addPrecursor(readPrecursor());
            } else {
              MzMLXml.skipElement(xml);
            }
          }
        }
        case MzMLTags.TAG_PRODUCT_LIST -> {
          while (nextChild()) {
            if (xml.getLocalName().equals(MzMLTags.TAG_PRODUCT)) {
              spectrum.getProductList().addProduct(readProduct());
            } else {
              MzMLXml.skipElement(xml);
            }
          }
        }
        case MzMLTags.TAG_BINARY_DATA_ARRAY_LIST -> readArrays(length, array -> {
          switch (array.getArrayType()) {
            case MZ -> spectrum.setMzBinaryDataInfo(array);
            case INTENSITY -> spectrum.setIntensityBinaryDataInfo(array);
            case ION_MOBILITY -> spectrum.setMobilityBinaryDataInfo(array);
            case WAVELENGTH -> spectrum.setWavelengthBinaryDataInfo(array);
            case TIME -> {
            }
          }
        });
        default -> readParam(spectrum.getCVParams());
      }
    }
    return spectrum;
  }

  /**
   * @return a chromatogram with decoded time and intensity arrays
   */
  @NotNull MzMLChromatogram readChromatogram() throws XMLStreamException {
    final String id = MzMLXml.required(xml, MzMLTags.ATTR_ID);
    Integer.parseInt(MzMLXml.required(xml, MzMLTags.ATTR_INDEX));
    final int length = Integer.parseInt(MzMLXml.required(xml, MzMLTags.ATTR_DEFAULT_ARRAY_LENGTH));
    final var trace = new MzMLChromatogram(id, length);
    while (nextChild()) {
      switch (xml.getLocalName()) {
        case MzMLTags.TAG_PRECURSOR -> trace.setPrecursor(readPrecursor());
        case MzMLTags.TAG_PRODUCT -> trace.setProduct(readProduct());
        case MzMLTags.TAG_BINARY_DATA_ARRAY_LIST -> readArrays(length, trace::readBinary);
        default -> readParam(trace);
      }
    }
    return trace;
  }

  private void readScanList(@NotNull final MzMLScanList scans) throws XMLStreamException {
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_SCAN)) {
        scans.addScan(readScan());
      } else {
        readParam(scans);
      }
    }
  }

  private @NotNull MzMLScan readScan() throws XMLStreamException {
    final var scan = new MzMLScan();
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_SCAN_WINDOW_LIST)) {
        final var windows = new MzMLScanWindowList();
        while (nextChild()) {
          if (xml.getLocalName().equals(MzMLTags.TAG_SCAN_WINDOW)) {
            windows.addScanWindow(readParams(new MzMLScanWindow()));
          } else {
            MzMLXml.skipElement(xml);
          }
        }
        scan.setScanWindowList(windows);
      } else {
        readParam(scan);
      }
    }
    return scan;
  }

  private @NotNull MzMLPrecursorElement readPrecursor() throws XMLStreamException {
    final var precursor = new MzMLPrecursorElement(attribute(MzMLTags.ATTR_SPECTRUM_REF));
    while (nextChild()) {
      switch (xml.getLocalName()) {
        case MzMLTags.TAG_ISOLATION_WINDOW ->
            precursor.setIsolationWindow(readParams(new MzMLIsolationWindow()));
        case MzMLTags.TAG_ACTIVATION ->
            precursor.setActivation(readParams(new MzMLPrecursorActivation()));
        case MzMLTags.TAG_SELECTED_ION_LIST -> {
          final var ions = new MzMLPrecursorSelectedIonList();
          while (nextChild()) {
            if (xml.getLocalName().equals(MzMLTags.TAG_SELECTED_ION)) {
              ions.addSelectedIon(readParams(new MzMLPrecursorSelectedIon()));
            } else {
              MzMLXml.skipElement(xml);
            }
          }
          precursor.setSelectedIonList(ions);
        }
        default -> MzMLXml.skipElement(xml);
      }
    }
    return precursor;
  }

  private @NotNull MzMLProduct readProduct() throws XMLStreamException {
    final var product = new MzMLProduct();
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_ISOLATION_WINDOW)) {
        product.setIsolationWindow(readParams(new MzMLIsolationWindow()));
      } else {
        MzMLXml.skipElement(xml);
      }
    }
    return product;
  }

  private <T extends MzMLCVGroup> @NotNull T readParams(@NotNull final T target)
      throws XMLStreamException {
    while (nextChild()) {
      readParam(target);
    }
    return target;
  }

  private void readParam(@NotNull final MzMLCVGroup target) throws XMLStreamException {
    switch (xml.getLocalName()) {
      case MzMLTags.TAG_CV_PARAM -> target.addCVParam(MzMLCVParam.read(xml));
      case MzMLTags.TAG_REF_PARAM_GROUP_REF -> {
        final var group = header.getParamGroup(MzMLXml.required(xml, MzMLTags.ATTR_REF));
        if (group != null) {
          target.getCVParamsList().addAll(group.getCvParams());
          if (target instanceof MzMLIsolationWindow window) {
            for (final var param : group.getUserParams()) {
              applyWindowParam(window, param.name(), param.value());
            }
          }
        }
      }
      case MzMLTags.TAG_USER_PARAM -> {
        if (target instanceof MzMLIsolationWindow window) {
          applyWindowParam(window, attribute(MzMLTags.ATTR_NAME), attribute(MzMLTags.ATTR_VALUE));
        }
      }
    }
    MzMLXml.skipElement(xml);
  }

  private static void applyWindowParam(@NotNull final MzMLIsolationWindow window,
      @Nullable final String name, @Nullable final String value) {
    if (MzMLUserParam.MS_LEVEL_IN_PRECURSOR_LIST.equals(name) && value != null) {
      window.setMSLevel(value);
    }
  }

  private void readArrays(final int defaultLength,
      @NotNull final Consumer<MzMLBinaryDataInfo> target) throws XMLStreamException {
    while (nextChild()) {
      if (xml.getLocalName().equals(MzMLTags.TAG_BINARY_DATA_ARRAY)) {
        final MzMLBinaryDataInfo array = readArray(defaultLength);
        if (array != null) {
          target.accept(array);
        }
      } else {
        MzMLXml.skipElement(xml);
      }
    }
  }

  private @Nullable MzMLBinaryDataInfo readArray(final int defaultLength)
      throws XMLStreamException {
    final String length = attribute(MzMLTags.ATTR_ARRAY_LENGTH);
    final var array = new MzMLBinaryDataInfo(
        Integer.parseInt(MzMLXml.required(xml, MzMLTags.ATTR_ENCODED_LENGTH)),
        length == null ? defaultLength : Integer.parseInt(length));
    boolean supported = true;
    while (nextChild()) {
      switch (xml.getLocalName()) {
        case MzMLTags.TAG_CV_PARAM -> {
          supported &= applyArrayParam(array, MzMLXml.required(xml, MzMLTags.ATTR_ACCESSION),
              attribute(MzMLTags.ATTR_UNIT_ACCESSION));
          MzMLXml.skipElement(xml);
        }
        case MzMLTags.TAG_REF_PARAM_GROUP_REF -> {
          final var group = header.getParamGroup(MzMLXml.required(xml, MzMLTags.ATTR_REF));
          if (group != null) {
            for (final var cv : group.getCvParams()) {
              supported &= applyArrayParam(array, cv.getAccession(),
                  cv.getUnitAccession().orElse(null));
            }
          }
          MzMLXml.skipElement(xml);
        }
        case MzMLTags.TAG_BINARY -> {
          if (supported && array.getArrayType() != null) {
            array.setTextContent(xml.getElementText());
          } else {
            MzMLXml.skipElement(xml);
          }
        }
        default -> MzMLXml.skipElement(xml);
      }
    }
    return array.getXmlBinaryContent() == null ? null : array;
  }

  private static boolean applyArrayParam(@NotNull final MzMLBinaryDataInfo array,
      @NotNull final String accession, @Nullable final String unit) {
    final MzMLBitLength precision = MzMLBitLength.of(accession);
    if (precision != null) {
      array.setBitLength(precision);
    } else if (MzMLCompressionType.isCompressionTypeAccession(accession)) {
      array.setCompressionType(
          array.getCompressionType().combine(MzMLCompressionType.ofAccession(accession)));
    } else if (MzMLArrayType.isArrayTypeAccession(accession)) {
      array.setArrayType(MzMLArrayType.ofAccession(accession));
      array.setUnitAccession(unit);
    } else if (accession.equals("MS:1003006")) {
      throw new IllegalArgumentException("Importing timsTOF files from mzML with the "
          + "--combineIonMobilitySpectra option is not supported. Please use the native .d data instead.");
    } else {
      return false;
    }
    return true;
  }

  private boolean nextChild() throws XMLStreamException {
    return xml.nextTag() == XMLStreamConstants.START_ELEMENT;
  }

  private @Nullable String attribute(@NotNull final String name) {
    return xml.getAttributeValue(null, name);
  }

  private static int scanNumber(@NotNull final String id, final int index) {
    final Matcher scan = SCAN.matcher(id);
    if (scan.find()) {
      return Integer.parseInt(scan.group(1));
    }
    final Matcher agilent = AGILENT_SCAN.matcher(id);
    return agilent.find() ? Integer.parseInt(agilent.group(1)) : index + 1;
  }
}
