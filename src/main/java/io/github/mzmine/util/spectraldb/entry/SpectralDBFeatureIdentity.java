/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.Nullable;

/**
 * To be replaced by {@link SpectralDBAnnotation}.
 */
@Deprecated
@ScheduledForRemoval
public class SpectralDBFeatureIdentity extends SimpleFeatureIdentity {

  public static final String XML_IDENTITY_TYPE = "spectraldatabasefeatureidentity";

  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");
  private static final String XML_CCS_ERROR_ELEMENT = "ccserror";

  private final SpectralDBEntry entry;
  private final SpectralSimilarity similarity;

  @Nullable
  private final Float ccsError;

  private Scan queryScan;

  public SpectralDBFeatureIdentity(Scan queryScan, SpectralDBEntry entry,
      SpectralSimilarity similarity, String method, @Nullable Float ccsError) {
    super(MessageFormat.format("{0} as {3} ({1}) {2} cos={4}",
            entry.getField(DBEntryField.NAME).orElse("NONAME"),
            // Name
            entry.getField(DBEntryField.MZ).orElse(""), // precursor m/z
            entry.getField(DBEntryField.FORMULA).orElse(""), // molecular
            // formula
            entry.getField(DBEntryField.ION_TYPE).orElse(""), // Ion type
            COS_FORM.format(similarity.getScore())), // cosine similarity
        entry.getField(DBEntryField.FORMULA).orElse("").toString(), method, "", "");
    this.entry = entry;
    this.similarity = similarity;
    this.queryScan = queryScan;
    this.ccsError = ccsError;
  }

  public SpectralDBEntry getEntry() {
    return entry;
  }

  public SpectralSimilarity getSimilarity() {
    return similarity;
  }

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

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_GENERAL_IDENTITY_ELEMENT);
    writer.writeAttribute(FeatureIdentity.XML_IDENTITY_TYPE_ATTR,
        SpectralDBFeatureIdentity.XML_IDENTITY_TYPE);

    savePropertyMap(writer);
    entry.saveToXML(writer);
    similarity.saveToXML(writer);

    writer.writeStartElement(XML_CCS_ERROR_ELEMENT);
    writer.writeCharacters(
        ParsingUtils.parseNullableString(ccsError != null ? String.valueOf(ccsError) : null));
    writer.writeEndElement();

    if (queryScan != null) {
      Scan.saveScanToXML(writer, getQueryScan());
    }

    writer.writeEndElement();
  }

  public static SpectralDBFeatureIdentity loadFromXML(XMLStreamReader reader,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName()
        .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT) && reader.getAttributeValue(null,
            FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
        .equals(SpectralDBFeatureIdentity.XML_IDENTITY_TYPE))) {
      throw new IllegalStateException(
          "Current element is not a SpectralDBFeatureIdentity element.");
    }

    SpectralDBEntry entry = null;
    SpectralSimilarity similarity = null;
    Scan scan = null;
    Map<String, String> map = null;
    Float ccsError = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case SpectralDBEntry.XML_ELEMENT -> entry = SpectralDBEntry.loadFromXML(reader);
        case SpectralSimilarity.XML_ELEMENT -> similarity = SpectralSimilarity.loadFromXML(reader);
        case SimpleFeatureIdentity.XML_PROPERTIES_ELEMENT -> map = SimpleFeatureIdentity.readPropertyValues(
            reader);
        case CONST.XML_RAW_FILE_SCAN_ELEMENT -> scan = Scan.loadScanFromXML(reader, possibleFiles);
        case XML_CCS_ERROR_ELEMENT -> {
          final String content = ParsingUtils.readNullableString(reader.getElementText());
          ccsError = content != null ? Float.valueOf(content) : null;
        }
      }
    }

    assert entry != null && similarity != null && map != null;

    SpectralDBFeatureIdentity id = new SpectralDBFeatureIdentity(scan, entry, similarity,
        map.get(FeatureIdentity.PROPERTY_METHOD), ccsError);
    map.forEach(id::setPropertyValue);
    return id;
  }

  public double getScore() {
    return getSimilarity().getScore();
  }

  @Nullable
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
    SpectralDBFeatureIdentity that = (SpectralDBFeatureIdentity) o;
    return Objects.equals(getEntry(), that.getEntry()) && Objects.equals(getSimilarity(),
        that.getSimilarity()) && Objects.equals(ccsError, that.ccsError) && Objects
        .equals(getQueryScan().getScanNumber(), that.getQueryScan().getScanNumber())
        && getQueryScan().getDataFile().equals(that.getQueryScan().getDataFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEntry(), getSimilarity(), ccsError, getQueryScan());
  }
}
