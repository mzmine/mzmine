/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.spectraldb.entry;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDBAnnotation implements FeatureAnnotation, Comparable<SpectralDBAnnotation> {

  public static final String XML_ATTR = "spectral_library_annotation";
  private static final String XML_CCS_ERROR_ELEMENT = "ccserror";
  private static final String XML_TESTED_RT_ELEMENT = "testedrt";
  private static final String XML_TESTED_MZ_ELEMENT = "testedmz";

  private static final Logger logger = Logger.getLogger(SpectralDBAnnotation.class.getName());

  private final SpectralLibraryEntry entry;
  private final SpectralSimilarity similarity;
  @Nullable
  private final Float ccsError;
  @Nullable
  private final Double testedPrecursorMz;
  @Nullable
  private final Float testedRt;
  @Nullable
  private final Scan queryScan;

  public SpectralDBAnnotation(SpectralLibraryEntry entry, SpectralSimilarity similarity,
      Scan queryScan, @Nullable Float ccsError, @Nullable Double testedPrecursorMz,
      @Nullable Float testedRt) {
    this.queryScan = queryScan;
    this.entry = entry;
    this.similarity = similarity;
    this.ccsError = ccsError;
    this.testedPrecursorMz = testedPrecursorMz;
    this.testedRt = testedRt;
  }

  public SpectralDBAnnotation(SpectralDBFeatureIdentity id) {
    this(id.getEntry(), id.getSimilarity(), id.getQueryScan(), id.getCCSError(), null, null);
  }

  public static FeatureAnnotation loadFromXML(XMLStreamReader reader, MZmineProject project,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))
        || !reader.getAttributeValue(null, XML_TYPE_ATTR).equals(XML_ATTR)) {
      throw new IllegalStateException("Current element is not a feature annotation element");
    }

    SpectralLibraryEntry entry = null;
    SpectralSimilarity similarity = null;
    Scan scan = null;
    Float ccsError = null;
    Float testedRt = null;
    Double testedPrecursorMz = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(FeatureAnnotation.XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      switch (reader.getLocalName()) {
        case SpectralLibraryEntry.XML_ELEMENT_ENTRY ->
            entry = SpectralLibraryEntry.loadFromXML(reader, project);
        case SpectralSimilarity.XML_ELEMENT -> similarity = SpectralSimilarity.loadFromXML(reader);
        case CONST.XML_RAW_FILE_SCAN_ELEMENT -> scan = Scan.loadScanFromXML(reader, possibleFiles);
        case XML_CCS_ERROR_ELEMENT ->
            ccsError = ParsingUtils.stringToFloat(reader.getElementText());
        case XML_TESTED_RT_ELEMENT ->
            testedRt = ParsingUtils.stringToFloat(reader.getElementText());
        case XML_TESTED_MZ_ELEMENT ->
            testedPrecursorMz = ParsingUtils.stringToDouble(reader.getElementText());
      }
    }

    assert entry != null && similarity != null;

    return new SpectralDBAnnotation(entry, similarity, scan, ccsError, testedPrecursorMz, testedRt);
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

    writer.writeStartElement(XML_TESTED_MZ_ELEMENT);
    writer.writeCharacters(ParsingUtils.parseNullableString(
        this.testedPrecursorMz != null ? String.valueOf(this.testedPrecursorMz) : null));
    writer.writeEndElement();

    writer.writeStartElement(XML_TESTED_RT_ELEMENT);
    writer.writeCharacters(ParsingUtils.parseNullableString(
        this.testedRt != null ? String.valueOf(this.testedRt) : null));
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

  @NotNull
  public DataPoint[] getLibraryDataPoints(DataPointsTag tag) {
    return switch (tag) {
      case ORIGINAL -> requireNonNullElse(entry.getDataPoints(), new DataPoint[0]);
      case FILTERED -> requireNonNullElse(similarity.getLibrary(), new DataPoint[0]);
      case ALIGNED -> requireNonNullElse(similarity.getAlignedDataPoints()[0], new DataPoint[0]);
      case MERGED, ALIGNED_MODIFIED -> new DataPoint[0];
      case UNALIGNED -> {
        var input = getLibraryDataPoints(DataPointsTag.FILTERED);
        var aligned = Set.of(getLibraryDataPoints(DataPointsTag.ALIGNED));
        yield Arrays.stream(input).filter(dp -> !aligned.contains(dp)).toArray(DataPoint[]::new);
      }
    };
  }

  @NotNull
  public DataPoint[] getQueryDataPoints(DataPointsTag tag) {
    return switch (tag) {
      case ORIGINAL -> {
        DataPoint[] dp = getQueryDataPoints();
        if (dp == null) {
          yield new DataPoint[0];
        }
        Arrays.sort(dp, new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));
        yield dp;
      }
      case FILTERED -> requireNonNullElse(similarity.getQuery(), new DataPoint[0]);
      case ALIGNED -> requireNonNullElse(similarity.getAlignedDataPoints()[1], new DataPoint[0]);
      case MERGED, ALIGNED_MODIFIED -> new DataPoint[0];
      case UNALIGNED -> {
        var input = getQueryDataPoints(DataPointsTag.FILTERED);
        var aligned = Set.of(getQueryDataPoints(DataPointsTag.ALIGNED));
        yield Arrays.stream(input).filter(dp -> !aligned.contains(dp)).toArray(DataPoint[]::new);
      }
    };
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
  public @Nullable String getInChI() {
    return entry.getOrElse(DBEntryField.INCHI, null);
  }

  @Override
  public @Nullable String getInChIKey() {
    return entry.getOrElse(DBEntryField.INCHIKEY, null);
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
    final String adduct = entry.getOrElse(DBEntryField.ION_TYPE, null);
    return IonTypeParser.parse(adduct);
  }

  @Override
  public @Nullable String getIupacName() {
    return entry.getOrElse(DBEntryField.IUPAC_NAME, null);
  }

  @Override
  public @Nullable String getCAS() {
    return entry.getOrElse(DBEntryField.CAS, null);
  }

  @Override
  public @Nullable String getInternalId() {
    return entry.getOrElse(DBEntryField.INTERNAL_ID, null);
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

  public SpectralLibraryEntry getEntry() {
    return entry;
  }

  public SpectralSimilarity getSimilarity() {
    return similarity;
  }

  @Nullable
  public Float getCCSError() {
    return ccsError;
  }

  @Nullable
  public Double getTestedPrecursorMz() {
    return testedPrecursorMz;
  }

  @Override
  @Nullable
  public MolecularStructure getStructure() {
    return entry.getStructure();
  }


  @Nullable
  public Double getMzPpmError() {
    Double libMz = getPrecursorMZ();
    if (libMz == null || testedPrecursorMz == null) {
      return null;
    }
    return MathUtils.getPpmDiff(libMz, testedPrecursorMz);
  }

  @Nullable
  public Double getMzAbsoluteError() {
    Double libMz = getPrecursorMZ();
    if (libMz == null || testedPrecursorMz == null) {
      return null;
    }
    return testedPrecursorMz - libMz;
  }

  @Nullable
  public Float getRtAbsoluteError() {
    Float libRt = getRT();
    if (libRt == null || testedRt == null) {
      return null;
    }
    return testedRt - libRt;
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
    return String.format("%s (%.3f)", getCompoundName(), requireNonNullElse(getScore(), 0f));
  }

  @Override
  public @NotNull String getXmlAttributeKey() {
    return XML_ATTR;
  }

  @Override
  public int compareTo(@NotNull final SpectralDBAnnotation o) {
    if (o.getScore() == null && getScore() == null) {
      return 0;
    }
    if (o.getScore() == null) {
      return -1;
    }
    if (getScore() == null) {
      return 1;
    }

    return Float.compare(this.getScore(), o.getScore());
  }
}
