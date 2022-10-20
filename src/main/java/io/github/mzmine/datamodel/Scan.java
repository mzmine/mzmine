/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.impl.SimpleMergedMsMsSpectrum;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.projectload.CachedIMSFrame;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represent one spectrum of a raw data file.
 */
public interface Scan extends MassSpectrum, Comparable<Scan> {

  public static final String XML_SCAN_ELEMENT = CONST.XML_RAW_FILE_SCAN_ELEMENT;
  public static final String XML_SCAN_TYPE_ATTR = "scantype";

  /**
   * Appends a scan to the current xml element.
   */
  public static void saveScanToXML(@NotNull final XMLStreamWriter writer, @NotNull final Scan scan)
      throws XMLStreamException {
    if (scan instanceof PseudoSpectrum pseudo) { // first, because instanceof's for frame trigger, too.
      pseudo.saveToXML(writer);
    } else if (scan instanceof SimpleScan || scan instanceof Frame) {
      writer.writeStartElement(CONST.XML_RAW_FILE_SCAN_ELEMENT);

      writer.writeAttribute(Scan.XML_SCAN_TYPE_ATTR, SimpleScan.XML_SCAN_TYPE);
      String name = scan.getDataFile().getName();
      writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, name);
      writer.writeAttribute(CONST.XML_RAW_FILE_SCAN_INDEX_ATTR,
          String.valueOf(scan.getDataFile().getScans().indexOf(scan)));

      writer.writeEndElement();
    } else if (scan instanceof MergedMsMsSpectrum merged) {
      merged.saveToXML(writer);
    } else {
      throw new UnsupportedOperationException(
          "Saving of scan type " + scan.getClass().getName() + " not implemented.");
    }
  }

  /**
   * Loads a scan from a xml file. The current element must have {@link Scan#XML_SCAN_ELEMENT} as
   * name.
   *
   * @param possibleFiles A list of possible raw data files.
   * @return The loaded scan.
   * @throws XMLStreamException
   * @throws IllegalStateException    If the current element is of the wrong type.
   * @throws IllegalArgumentException If the raw data file of the scan is not part of the
   *                                  collection.
   */
  public static Scan loadScanFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName()
        .equals(CONST.XML_RAW_FILE_SCAN_ELEMENT))) {
      throw new IllegalStateException("Current element is not a scan element.");
    }

    final String name = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
    final RawDataFile file = possibleFiles.stream().filter(f -> f.getName().equals(name))
        .findFirst().orElse(null);
    if (file == null) {
      throw new IllegalArgumentException("Raw data file not found");
    }

    switch (reader.getAttributeValue(null, Scan.XML_SCAN_TYPE_ATTR)) {
      case SimpleScan.XML_SCAN_TYPE -> {
        final int index = Integer.parseInt(
            reader.getAttributeValue(null, CONST.XML_RAW_FILE_SCAN_INDEX_ATTR));
        final Scan scan = file.getScan(index);
        return scan instanceof CachedIMSFrame cached ? cached.getOriginalFrame() : scan;
      }
      case SimpleMergedMsMsSpectrum.XML_SCAN_TYPE -> {
        return SimpleMergedMsMsSpectrum.loadFromXML(reader, (IMSRawDataFile) file);
      }
      case SimplePseudoSpectrum.XML_SCAN_TYPE -> {
        return SimplePseudoSpectrum.loadFromXML(reader, file);
      }
      default -> {
        throw new IllegalArgumentException("Cannot load scan from xml. Scan type not recognized.");
      }
    }
  }

  /**
   * @return RawDataFile containing this Scan
   */
  @NotNull RawDataFile getDataFile();

  /**
   * @return Scan number
   */
  int getScanNumber();

  /**
   * @return Instrument-specific scan definition as String
   */
  @NotNull String getScanDefinition();

  /**
   * @return MS level
   */
  int getMSLevel();

  /**
   * @return Retention time of this scan in minutes
   */
  float getRetentionTime();

  /**
   * @return The injection time of this scan or null.
   */
  @Nullable
  public Float getInjectionTime();

  /**
   * @return The actual scanning range of the instrument
   */
  @NotNull Range<Double> getScanningMZRange();

  /**
   *
   * @return The {@link MsMsInfo}. If null, this is not an MSn scan.
   */
  @Nullable MsMsInfo getMsMsInfo();

  /**
   * @return The charge or null. Works for subclasses of {@link DDAMsMsInfo}.
   */
  default Integer getPrecursorCharge() {
    return getMsMsInfo() instanceof DDAMsMsInfo info ? info.getPrecursorCharge() : null;
  }

  /**
   * @return The precursor mz or null. Works for subclasses of {@link DDAMsMsInfo}.
   */
  default Double getPrecursorMz() {
    return getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : null;
  }

  @NotNull PolarityType getPolarity();

  @Nullable MassList getMassList();

  void addMassList(@NotNull MassList massList);

  /**
   * Standard method to sort scans based on scan number (or if not available retention time)
   *
   * @param s other scan
   * @return
   */
  @Override
  default int compareTo(@NotNull Scan s) {
    assert s != null;
    int result = Integer.compare(this.getScanNumber(), s.getScanNumber());
    if (result != 0) {
      return result;
    } else {
      return Float.compare(this.getRetentionTime(), s.getRetentionTime());
    }
  }

  /**
   *
   * Method to check if the scan m/z range is not empty
   *
   * @return boolean
   */
  default boolean isEmptyScan() {
    return this.getNumberOfDataPoints() == 0;
  }

}

