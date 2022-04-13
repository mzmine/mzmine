/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDBAnnotation implements FeatureAnnotation {

  public static final String XML_ATTR = "spectral_library_annotation";
  private static final String XML_CCS_ERROR_ELEMENT = "ccserror";

  private static final Logger logger = Logger.getLogger(SpectralDBAnnotation.class.getName());

  private final SpectralDBEntry entry;
  private final SpectralSimilarity similarity;
  private final Float ccsError;
  @Nullable
  private final Scan queryScan;

  public SpectralDBAnnotation(SpectralDBEntry entry, SpectralSimilarity similarity, Scan queryScan,
      @Nullable Float ccsError) {
    this.queryScan = queryScan;
    this.entry = entry;
    this.similarity = similarity;
    this.ccsError = ccsError;
  }

  public SpectralDBAnnotation(SpectralDBFeatureIdentity id) {
    this(id.getEntry(), id.getSimilarity(), id.getQueryScan(), id.getCCSError());
  }

  public static FeatureAnnotation loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))
        || !reader.getAttributeValue(null, XML_TYPE_ATTR).equals(XML_ATTR)) {
      throw new IllegalStateException("Current element is not a feature annotation element");
    }

    SpectralDBEntry entry = null;
    SpectralSimilarity similarity = null;
    Scan scan = null;
    Float ccsError = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(FeatureAnnotation.XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case SpectralDBEntry.XML_ELEMENT -> entry = SpectralDBEntry.loadFromXML(reader);
        case SpectralSimilarity.XML_ELEMENT -> similarity = SpectralSimilarity.loadFromXML(reader);
        case CONST.XML_RAW_FILE_SCAN_ELEMENT -> scan = Scan.loadScanFromXML(reader, possibleFiles);
        case XML_CCS_ERROR_ELEMENT -> {
          final String content = ParsingUtils.readNullableString(reader.getElementText());
          ccsError = content != null ? Float.valueOf(content) : null;
        }
      }
    }

    assert entry != null && similarity != null;

    return new SpectralDBAnnotation(entry, similarity, scan, ccsError);
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException {
    writeOpeningTag(writer);

    entry.saveToXML(writer);
    similarity.saveToXML(writer);

    writer.writeStartElement(XML_CCS_ERROR_ELEMENT);
    writer.writeCharacters(ParsingUtils.parseNullableString(
        this.ccsError != null ? String.valueOf(this.ccsError) : null));
    writer.writeEndElement();

    if (queryScan != null) {
      Scan.saveScanToXML(writer, getQueryScan());
    }

    writeClosingTag(writer);
  }

  @Nullable
  public Scan getQueryScan() {
    return queryScan;
  }

  public DataPoint[] getQueryDataPoints() {
    if (queryScan == null || queryScan.getMassList() == null) {
      return null;
    }
    return queryScan.getMassList().getDataPoints();
  }

  public DataPoint[] getLibraryDataPoints(DataPointsTag tag) {
    switch (tag) {
      case ORIGINAL:
        return entry.getDataPoints();
      case FILTERED:
        return similarity.getLibrary();
      case ALIGNED:
        return similarity.getAlignedDataPoints()[0];
      case MERGED:
        return new DataPoint[0];
    }
    return new DataPoint[0];
  }

  public DataPoint[] getQueryDataPoints(DataPointsTag tag) {
    switch (tag) {
      case ORIGINAL:
        DataPoint[] dp = getQueryDataPoints();
        if (dp == null) {
          return new DataPoint[0];
        }
        Arrays.sort(dp, new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));
        return dp;
      case FILTERED:
        return similarity.getQuery();
      case ALIGNED:
        return similarity.getAlignedDataPoints()[1];
      case MERGED:
        return new DataPoint[0];
    }
    return new DataPoint[0];
  }

  @Override
  public @Nullable Double getPrecursorMZ() {
    return entry.getPrecursorMZ();
  }

  @Override
  public @Nullable String getSmiles() {
    return entry.getOrElse(DBEntryField.SMILES, null);
  }

  @Override
  public @Nullable String getCompoundName() {
    return entry.getOrElse(DBEntryField.NAME, null);
  }

  @Override
  public @Nullable String getFormula() {
    return entry.getOrElse(DBEntryField.FORMULA, null);
  }

  @Override
  public @Nullable IonType getAdductType() {
    final String adduct = entry.getOrElse(DBEntryField.SMILES, null);
    return IonType.parseFromString(adduct);
  }

  @Override
  public @Nullable Float getMobility() {
    return null;
  }

  @Override
  public @Nullable Float getCCS() {
    return entry.getOrElse(DBEntryField.CCS, null);
  }

  @Override
  public @Nullable Float getRT() {
    return entry.getOrElse(DBEntryField.RT, null);
  }

  @Override
  public @Nullable String getDatabase() {
    return null;
  }

  @Override
  public @Nullable Float getScore() {
    return (float) similarity.getScore();
  }

  public SpectralDBEntry getEntry() {
    return entry;
  }

  public SpectralSimilarity getSimilarity() {
    return similarity;
  }

  public Float getCCSError() {
    return ccsError;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpectralDBAnnotation that = (SpectralDBAnnotation) o;
    return Objects.equals(getEntry(), that.getEntry()) && Objects.equals(getSimilarity(),
        that.getSimilarity()) && Objects.equals(ccsError, that.ccsError) && Objects.equals(
        getQueryScan().getScanNumber(), that.getQueryScan().getScanNumber())
        && getQueryScan().getDataFile().equals(that.getQueryScan().getDataFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEntry(), getSimilarity(), ccsError, getQueryScan());
  }

  @Override
  public String toString() {
    return String.format("%s (%.3f)", getCompoundName(),
        Objects.requireNonNullElse(getScore(), 0f));
  }

  @Override
  public @NotNull String getXmlAttributeKey() {
    return XML_ATTR;
  }
}
