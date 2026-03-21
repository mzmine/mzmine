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

package io.github.mzmine.util.spectraldb.entry;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularDataModelMap;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.AnnotationMethodType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.RIDiffType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.ExplainedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SimilarityType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularTask;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDBAnnotation extends ModularDataModelMap implements FeatureAnnotation,
    Comparable<SpectralDBAnnotation> {

  public static final String XML_ATTR = "spectral_library_annotation";
  private static final String XML_CCS_ERROR_ELEMENT = "ccserror";
  private static final String XML_TESTED_RT_ELEMENT = "testedrt";
  private static final String XML_TESTED_MZ_ELEMENT = "testedmz";
  private static final String XML_RI_DIFF_ELEMENT = "retention_index_diff";

  private static final Logger logger = Logger.getLogger(SpectralDBAnnotation.class.getName());

  private final SpectralLibraryEntry entry;
  private final SpectralSimilarity similarity;
  @Nullable
  private final Scan queryScan;

  private final Map<DataType, Object> map = new HashMap<>();

  public SpectralDBAnnotation(SpectralLibraryEntry entry, SpectralSimilarity similarity,
      Scan queryScan, @Nullable Float ccsRelativeError, @Nullable Double testedPrecursorMz,
      @Nullable Float testedRt, @Nullable Float riDiff) {
    this.queryScan = queryScan;
    this.entry = entry;
    this.similarity = similarity;

    if (ccsRelativeError != null) {
      set(CCSRelativeErrorType.class, ccsRelativeError);
    }
    if (testedPrecursorMz != null && entry.getPrecursorMZ() != null) {
      set(MzAbsoluteDifferenceType.class, testedPrecursorMz - entry.getPrecursorMZ());
    }
    if (testedRt != null && entry.getOrElse(DBEntryField.RT, null) != null) {
      set(RtAbsoluteDifferenceType.class,
          testedRt - (Float) entry.getOrElse(DBEntryField.RT, null));
    }
    if (riDiff != null) {
      set(RIDiffType.class, riDiff);
    }
  }

  /**
   *
   * @param map A map of {@link DataType} -> Object mappings. These types are added to this
   *            {@link SpectralDBAnnotation}, not the {@link SpectralLibraryEntry}.
   *            <br>
   *            This annotation should only contain mappings that reference between the static types
   *            of the spectral library entry and the
   *            {@link io.github.mzmine.datamodel.features.FeatureListRow}. E.g. Errors such as
   *            {@link RIDiffType} or {@link RtAbsoluteDifferenceType}.
   */
  public SpectralDBAnnotation(@NotNull SpectralLibraryEntry entry,
      @NotNull SpectralSimilarity similarity, @Nullable Scan queryScan,
      @Nullable Map<DataType, Object> map) {
    this.queryScan = queryScan;
    this.entry = entry;
    this.similarity = similarity;

    if (map != null) {
      for (Entry<DataType, Object> e : map.entrySet()) {
        set(e.getKey(), e.getValue());
      }
    }
  }

  public SpectralDBAnnotation(SpectralDBFeatureIdentity id) {
    this(id.getEntry(), id.getSimilarity(), id.getQueryScan(), id.getCCSError(), null, null, null);
  }

  public static FeatureAnnotation loadFromXML(XMLStreamReader reader, MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))
        || !reader.getAttributeValue(null, XML_TYPE_ATTR).equals(XML_ATTR)) {
      throw new IllegalStateException("Current element is not a feature annotation element");
    }

    final int version = ParsingUtils.readAttributeValueOrDefault(reader, CONST.XML_VERSION_ATTR, 1,
        Integer::valueOf);

    if (version == 1) {
      return loadFromXmlVersion1(reader, project, possibleFiles);
    }

    SpectralLibraryEntry entry = null;
    SpectralSimilarity similarity = null;
    Scan scan = null;
    final Map<DataType, Object> map = new HashMap<>();
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
        case CONST.XML_DATA_TYPE_ELEMENT -> {
          String uniqueId = reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR);
          DataType<?> dt = DataTypes.getTypeForId(uniqueId);
          if (dt == null) {
            break;
          }

          Object value = dt.loadFromXML(reader, project, flist, row, null, null);
          if (value != null) {
            map.put(dt, value);
          }
        }
        default -> {
        }
      }
    }

    return new SpectralDBAnnotation(entry, similarity, scan, map);
  }

  private static @NotNull SpectralDBAnnotation loadFromXmlVersion1(XMLStreamReader reader,
      MZmineProject project, Collection<RawDataFile> possibleFiles) throws XMLStreamException {
    SpectralLibraryEntry entry = null;
    SpectralSimilarity similarity = null;
    Scan scan = null;
    Float ccsError = null;
    Float testedRt = null;
    Double testedPrecursorMz = null;
    Float riDiff = null;

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
        case XML_RI_DIFF_ELEMENT -> riDiff = ParsingUtils.stringToFloat(reader.getElementText());
        default -> {
        }
      }
    }

    assert entry != null && similarity != null;

    return new SpectralDBAnnotation(entry, similarity, scan, ccsError, testedPrecursorMz, testedRt,
        riDiff);
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException {
    writeOpeningTag(writer);
    writer.writeAttribute(CONST.XML_VERSION_ATTR, "2");

    entry.saveToXML(writer);
    similarity.saveToXML(writer);

    for (DataType type : getTypes()) {
      FeatureListSaveTask.writeDataType(writer, type, get(type), flist, row, null, null);
    }

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
      case ALIGNED -> requireNonNullElse(similarity.getAlignedDataPoints(),
          new DataPoint[][]{new DataPoint[0], new DataPoint[0]})[0];
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
      case ALIGNED -> requireNonNullElse(similarity.getAlignedDataPoints(),
          new DataPoint[][]{new DataPoint[0], new DataPoint[0]})[1];
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
    return entry.getLibraryName();
  }

  @Override
  public @NotNull Class<? extends DataType> getDataType() {
    return SpectralLibraryMatchesType.class;
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
    return get(CCSRelativeErrorType.class);
  }

  @Override
  @Nullable
  public MolecularStructure getStructure() {
    return entry.getStructure();
  }


  @Nullable
  public Double getMzPpmError() {
    Double libMz = getPrecursorMZ();
    if (libMz == null || getMzAbsoluteError() == null) {
      return null;
    }
    return getMzAbsoluteError() / libMz * 1E6;
  }

  @Nullable
  public Double getMzAbsoluteError() {
    return get(MzAbsoluteDifferenceType.class);
  }

  @Nullable
  public Float getRtAbsoluteError() {
    return get(RtAbsoluteDifferenceType.class);
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
        that.getSimilarity()) && Objects.equals(getRiDiff(), that.getRiDiff()) && Objects.equals(
        getCCSError(), that.getCCSError()) && Objects.equals(getQueryScan().getScanNumber(),
        that.getQueryScan().getScanNumber()) && getQueryScan().getDataFile()
        .equals(that.getQueryScan().getDataFile());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEntry(), getSimilarity(), getCCSError(), getRiDiff(), getQueryScan());
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
  public @Nullable String getComment() {
    return entry.getOrElse(DBEntryField.COMMENT, null);
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

  public @Nullable Float getRiDiff() {
    return get(RIDiffType.class);
  }

  /**
   * An additional json string that may contain additional fields that are otherwise not captured.
   *
   */
  public @Nullable String getAdditionalJson() {
    return entry.getOrElse(DBEntryField.JSON_STRING, null);
  }

  @Override
  public Map<DataType, Object> getMap() {
    return map;
  }

  /**
   * This method first gets values from the annotation and then from the entry. This is needed in
   * the {@link CSVExportModularTask}.
   * <p>
   * This behavior is similar to {@link CompoundDBAnnotation}, where match and entry information is
   * mixed and a single get function retrieves all.
   *
   * @return the value of this annotation or of the entry
   */
  @Override
  public <T> @Nullable T get(DataType<T> type) {
    T value = super.get(type);
    if (value != null) {
      return value;
    }
    value = entry.get(type);
    if (value != null) {
      return value;
    }

    // moved from SpectralLibraryMatchesType that had some special mapping columns
    // those columns are neither saved in the match nor in the entry but are computed
    // some values from entry might have failed return due to mismatching types
    // therefore some are still mapped here to the corresponding correct type
    try {
      return (T) switch (type) {
        case SimilarityType _ -> (float) this.getSimilarity().getScore(); // type requires float
        case ExplainedIntensityPercentType __ ->
            (float) this.getSimilarity().getExplainedLibraryIntensity();
        case MatchingSignalsType _ -> this.getSimilarity().getOverlap();
        case MzPpmDifferenceType _ -> this.getMzPpmError(); // not added as type so needs mapping
        case AnnotationMethodType _ -> getAnnotationMethodName(); // not in map so need method call
        default -> null; // just return null here as type is just unknown to this match
      };
    } catch (Exception e) {
      // may have mismatching data types because source of values in library entry is diverse and uncontrolled
      return null;
    }
  }

  /**
   * This annotation should only contain mappings that reference between the static types of the
   * spectral library entry and the {@link io.github.mzmine.datamodel.features.FeatureListRow}. E.g.
   * Errors such as {@link RIDiffType} or {@link RtAbsoluteDifferenceType}.
   *
   * @see ModularDataModelMap#set(DataType, Object)
   */
  @Override
  public <T> boolean set(DataType<T> type, T value) {
    return super.set(type, value);
  }

  @Override
  public @Nullable IsotopePattern getIsotopePattern() {
    // entry caches the isotope pattern
    return entry.getIsotopePattern();
  }
}
