/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a merged spectrum from scans of the same raw data file. If a merged spectrum across
 * multiple raw data files is needed, implementations have to check for compatibility.
 * {@link SimpleMergedMsMsSpectrum#getScanNumber()} will return -1 to represent the artificial state
 * of this spectrum.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleMergedMsMsSpectrum extends SimpleMergedMassSpectrum implements
    MergedMsMsSpectrum {

  public static final String XML_SCAN_TYPE = "simplemergedmsmsspectrum";

  protected MsMsInfo msMsInfo;

  public SimpleMergedMsMsSpectrum(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, MsMsInfo info, int msLevel,
      @NotNull List<? extends MassSpectrum> sourceSpectra,
      @NotNull SpectraMerging.IntensityMergingType intensityMergingType,
      @NotNull CenterFunction centerFunction, MergingType mergeType) {
    super(storage, mzValues, intensityValues, msLevel, sourceSpectra, intensityMergingType,
        centerFunction, mergeType);

    msMsInfo = info;
    this.scanDefinition = ScanUtils.scanToString(this, true);
  }

  /**
   * @param reader        The xml reader.
   * @param file          The file of the MS1 feature in LC-IMS-Ms analysis. In maldi analysis, this
   *                      is the file of the MS2 spectrum. IF the ms2 spectrum is merged from
   *                      multiple files, this is a random file the spectrum was merged from.
   * @param possibleFiles All files currently loaded in the project, to extract the original spectra
   *                      from.
   * @return The loaded spectrum.
   */
  public static SimpleMergedMsMsSpectrum loadFromXML(XMLStreamReader reader, RawDataFile file,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!reader.isStartElement() || !reader.getLocalName().equals(Scan.XML_SCAN_ELEMENT)
        || !reader.getAttributeValue(null, Scan.XML_SCAN_TYPE_ATTR).equals(XML_SCAN_TYPE)) {
      throw new IllegalStateException("Wrong scan type.");
    }

    final int mslevel = Integer.parseInt(reader.getAttributeValue(null, CONST.XML_MSLEVEL_ATTR));
    final IntensityMergingType type = IntensityMergingType.parseOrElse(
        reader.getAttributeValue(null, CONST.XML_INTENSITY_MERGE_TYPE_ATTR), null);
    String mergingType = reader.getAttributeValue(null, CONST.XML_MERGE_TYPE_ATTR);
    final MergingType mergeSpecType =
        mergingType == null ? null : MergingType.parseOrElse(mergingType, MergingType.UNKNOWN);
    assert file.getName().equals(reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT));

    double[] mzs = null;
    double[] intensties = null;
    List<Scan> scans = new ArrayList<>();
    MsMsInfo info = null;
    MassList ml = null; // only saved if its not a ScanPointerMassList

    while (reader.hasNext()) {
      int next = reader.next();
      if (next == XMLEvent.END_ELEMENT && reader.getLocalName().equals(Scan.XML_SCAN_ELEMENT)) {
        break;
      }
      if (next != XMLEvent.START_ELEMENT) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensties = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_SCAN_LIST_ELEMENT -> {
          final String fileName = reader.getAttributeValue(null, CONST.XML_RAW_FILE_ELEMENT);
          // for old projects assume that the MS2 file is the default file and fall back to that.
          // For new projects it's a random file of the merged MS2 spectra.
          final String finalFileName = fileName != null ? fileName : file.getName();

          final RawDataFile specificFile = possibleFiles.stream()
              .filter(f -> f.getName().equals(finalFileName)).findFirst().orElse(null);
          if (specificFile == null) {
            throw new IllegalArgumentException(
                "Raw file with name '%s' not present. Cannot load merged MS2 spectrum.".formatted(
                    fileName));
          }
          final List<Scan> tempScans = ParsingUtils.stringToScanList(reader.getElementText(),
              specificFile);
          if (tempScans == null) {
            throw new IllegalStateException(
                "Could not load MS2 scans in MergedMsMsSpectrum, did not find specified scans.");
          }
          scans.addAll(tempScans);
        }
        // the file has already been determined before
        case MsMsInfo.XML_ELEMENT -> info = MsMsInfo.loadFromXML(reader, file, List.of(file));
        case SimpleMassList.XML_ELEMENT ->
            SimpleMassList.loadFromXML(reader, file.getMemoryMapStorage());
      }
    }

    assert mzs != null && intensties != null && scans != null;
    final SimpleMergedMsMsSpectrum scan = new SimpleMergedMsMsSpectrum(file.getMemoryMapStorage(),
        mzs, intensties, info, mslevel, scans, type, SpectraMerging.DEFAULT_CENTER_FUNCTION,
        mergeSpecType);

    if (ml != null) {
      scan.addMassList(ml);
    }

    return scan;
  }

  @Override
  public float getCollisionEnergy() {
    return msMsInfo != null ? Objects.requireNonNullElse(msMsInfo.getActivationEnergy(), 0f) : 0f;
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    return msMsInfo;
  }

  @Override
  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    // in case we have a PseudoSpectrum in our source spectra, we save as a PseudoSpectrum, because
    // we cannot point to the actual spectra in the raw file (PseudoSpectra do not exist in the raw,
    // but are, e.g., generated by peak shape correlation
    if (getSourceSpectra().stream().anyMatch(PseudoSpectrum.class::isInstance)) {
      final PseudoSpectrum pseudo = getSourceSpectra().stream()
          .filter(PseudoSpectrum.class::isInstance).map(PseudoSpectrum.class::cast).findFirst()
          .get();

      final SimplePseudoSpectrum spectrum = new SimplePseudoSpectrum(getDataFile(), getMSLevel(),
          getRetentionTime(), null, DataPointUtils.getDoubleBufferAsArray(getMzValues()),
          DataPointUtils.getDoubleBufferAsArray(getIntensityValues()), getPolarity(),
          getScanDefinition(), pseudo.getPseudoSpectrumType());
      spectrum.saveToXML(writer);
      return;
    }

    writer.writeStartElement(Scan.XML_SCAN_ELEMENT);
    writer.writeAttribute(Scan.XML_SCAN_TYPE_ATTR, SimpleMergedMsMsSpectrum.XML_SCAN_TYPE);

    writer.writeAttribute(CONST.XML_MSLEVEL_ATTR, String.valueOf(getMSLevel()));
    writer.writeAttribute(CONST.XML_MERGE_TYPE_ATTR, getMergingType().getUniqueID());
    writer.writeAttribute(CONST.XML_CE_ATTR, String.valueOf(getCollisionEnergy()));
    writer.writeAttribute(CONST.XML_INTENSITY_MERGE_TYPE_ATTR,
        getIntensityMergingType().getUniqueID());
    writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, getDataFile().getName());

    if (msMsInfo != null) {
      msMsInfo.writeToXML(writer);
    }

    writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleBufferToString(getMzValues()));
    writer.writeEndElement();

    writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
    writer.writeCharacters(ParsingUtils.doubleBufferToString(getIntensityValues()));
    writer.writeEndElement();

    // in theory, it is possible that a lc-ms file was aligned to a lc-ims-ms file, which were then
    // merged to get a spectral library match. Grouping scans by file should solve this
    // --
    // mobility scans from multiple files may be merged into a single merged msms spectrum, in case
    // of MALDI (SIMSEF) acquisitions. Account for that here by grouping scans per file and saving
    // all scans individually. Introduced as of version 3.6.
    final Map<RawDataFile, String> fileScansMap = ParsingUtils.scanListToStrings(
        getSourceSpectra().stream().filter(s -> s instanceof Scan).map(s -> (Scan) s).toList());
    for (Entry<RawDataFile, String> fileScanEntry : fileScansMap.entrySet()) {
      writer.writeStartElement(CONST.XML_SCAN_LIST_ELEMENT);
      writer.writeAttribute(CONST.XML_RAW_FILE_ELEMENT, fileScanEntry.getKey().getName());
      writer.writeCharacters(fileScanEntry.getValue());
      writer.writeEndElement();
    }

    if (massList instanceof SimpleMassList) {
      ((SimpleMassList) massList).saveToXML(writer);
    }

    writer.writeEndElement();
  }
}
