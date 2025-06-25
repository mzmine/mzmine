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

package io.github.mzmine.util.scans.similarity;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The result of a {@link SpectralSimilarityFunction}.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class SpectralSimilarity {

  public static final String XML_ELEMENT = "spectralsimilarity";
  private static final String XML_FUNCTION_ELEMENT = "similairtyfunction";
  private static final String XML_OVERLAPPING_PEAKS_ELEMENT = "overlappingpeaks";
  private static final String XML_SCORE_ELEMENT = "score";
  private static final String XML_EXPLAINED_LIB_INTENSITY_ELEMENT = "explainedLibraryIntensity";
  private static final String XML_LIBRARY_SEPCTRUM_ELEMENT = "libraryspectrum";
  private static final String XML_QUERY_SEPCTRUM_ELEMENT = "queryspectrum";
  private static final String XML_ALIGNED_SEPCTRUM_LIST_ELEMENT = "alignedspectrumlist";
  private static final String XML_ALIGNED_SEPCTRUM_ELEMENT = "alignedspectrum";

  private final double explainedLibraryIntensity;
  // similarity score (depends on similarity function)
  private final double score;
  // aligned signals in library and query spectrum
  private final int overlap;
  // similarity function name
  private final String funcitonName;

  // spectral data can be nullable to save memory
  // library and query spectrum (may be filtered)
  private @Nullable DataPoint[] library;
  private @Nullable DataPoint[] query;
  // aligned data points (found in both the library[0] and the query[1]
  // sepctrum)
  // alinged[library, query][data points]
  private @Nullable DataPoint[][] aligned;

  /**
   * The result of a {@link SpectralSimilarityFunction}.
   *
   * @param funcitonName              Similarity function name
   * @param score                     similarity score
   * @param overlap                   count of aligned data points in library and query spectrum
   * @param explainedLibraryIntensity ratio of explained lib intensity
   */
  public SpectralSimilarity(String funcitonName, double score, int overlap,
      double explainedLibraryIntensity) {
    this.funcitonName = funcitonName;
    this.score = score;
    this.overlap = overlap;
    this.explainedLibraryIntensity = explainedLibraryIntensity;
  }

  /**
   * The result of a {@link SpectralSimilarityFunction}.
   *
   * @param funcitonName Similarity function name
   * @param score        similarity score
   * @param overlap      count of aligned data points in library and query spectrum
   * @param librarySpec  library spectrum (or other) which was matched to querySpec (may be
   *                     filtered)
   * @param querySpec    query spectrum which was matched to librarySpec (may be filtered)
   * @param alignedDP    aligned data points (alignedDP.get(data point index)[library/query
   *                     spectrum])
   */
  public SpectralSimilarity(String funcitonName, double score, int overlap,
      @Nullable DataPoint[] librarySpec, @Nullable DataPoint[] querySpec,
      @Nullable List<DataPoint[]> alignedDP) {
    DataPointSorter sorter = new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending);
    this.funcitonName = funcitonName;
    this.score = score;
    this.overlap = overlap;
    this.library = librarySpec;
    this.query = querySpec;
    if (this.library != null) {
      Arrays.sort(this.library, sorter);
    }
    if (this.query != null) {
      Arrays.sort(this.query, sorter);
    }
    if (alignedDP != null) {
      // filter unaligned
      List<DataPoint[]> filtered = ScanAlignment.removeUnaligned(alignedDP);
      aligned = ScanAlignment.convertBackToMassLists(filtered);

      for (DataPoint[] dp : aligned) {
        Arrays.sort(dp, sorter);
      }
      double sumAligned = Arrays.stream(Objects.requireNonNull(getAlignedLibrarySignals()))
          .mapToDouble(DataPoint::getIntensity).sum();
      double sumAll = Arrays.stream(library).filter(Objects::nonNull)
          .mapToDouble(DataPoint::getIntensity).sum();
      explainedLibraryIntensity = sumAligned / sumAll;
    } else {
      explainedLibraryIntensity = 0;
    }
  }

  /**
   * For now a workaround to show spectral library entries as matches against itself.
   */
  public static SpectralSimilarity ofMatchIdentity(final @NotNull SpectralLibraryEntry entry) {
    var dps = entry.getDataPoints();
    var aligned = Arrays.stream(dps).map(dp -> new DataPoint[]{dp, dp}).toList();
    return new SpectralSimilarity("library_identity", 1d, dps.length, dps, dps, aligned);
  }

  public static SpectralSimilarity loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Cannot read spectral similarity. Wrong element.");
    }

    String function = null;
    int overlap = 0;
    double score = 0;
    double explainedLibraryIntensity = 0;

    DataPoint[] query = null;
    DataPoint[] library = null;
    DataPoint[][] aligned = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case XML_FUNCTION_ELEMENT -> function = reader.getElementText();
        case XML_SCORE_ELEMENT -> score = Double.parseDouble(reader.getElementText());
        case XML_EXPLAINED_LIB_INTENSITY_ELEMENT ->
            explainedLibraryIntensity = Double.parseDouble(reader.getElementText());
        case XML_OVERLAPPING_PEAKS_ELEMENT -> overlap = Integer.parseInt(reader.getElementText());
        case XML_LIBRARY_SEPCTRUM_ELEMENT -> library = parseDatapointArray(reader);
        case XML_QUERY_SEPCTRUM_ELEMENT -> query = parseDatapointArray(reader);
        case XML_ALIGNED_SEPCTRUM_LIST_ELEMENT -> aligned = parseAlignedSpetra(reader);
      }
    }

    final SpectralSimilarity similarity = new SpectralSimilarity(function, score, overlap,
        explainedLibraryIntensity);
    similarity.library = library;
    similarity.query = query;
    similarity.aligned = aligned;

    return similarity;
  }

  private static DataPoint[][] parseAlignedSpetra(XMLStreamReader reader)
      throws XMLStreamException {
    final int numSpectra = Integer.parseInt(
        reader.getAttributeValue(null, CONST.XML_NUM_VALUES_ATTR));

    DataPoint[][] aligned = new DataPoint[numSpectra][];
    int index = 0;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ALIGNED_SEPCTRUM_LIST_ELEMENT)) && index < numSpectra) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(XML_ALIGNED_SEPCTRUM_ELEMENT)) {
        aligned[index] = parseDatapointArray(reader);
        index++;
      }
    }

    return aligned;
  }

  /**
   * Reads a data point array from the current element. The current element must be a start element
   * and enclose the mz and intensity array. This method aborts reading as soon as mzs and
   * intensities elements have been found, even without reaching the end element.
   */
  private static DataPoint[] parseDatapointArray(XMLStreamReader reader) throws XMLStreamException {
    if (!reader.isStartElement()) {
      return null;
    }
    final String elementName = reader.getLocalName();

    double[] mzs = null;
    double[] intensities = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(elementName))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case CONST.XML_MZ_VALUES_ELEMENT ->
            mzs = ParsingUtils.stringToDoubleArray(reader.getElementText());
        case CONST.XML_INTENSITY_VALUES_ELEMENT ->
            intensities = ParsingUtils.stringToDoubleArray(reader.getElementText());
      }

      if (mzs != null && intensities != null) {
        break;
      }
    }

    assert mzs != null && intensities != null;
    assert mzs.length == intensities.length;

    DataPoint[] dps = new DataPoint[mzs.length];
    for (int i = 0; i < dps.length; i++) {
      dps[i] = new SimpleDataPoint(mzs[i], intensities[i]);
    }

    return dps;
  }

  private DataPoint[] getAlignedLibrarySignals() {
    return aligned != null && aligned.length > 0 ? aligned[0] : null;
  }

  private DataPoint[] getAlignedQuerySignals() {
    return aligned != null && aligned.length > 1 ? aligned[1] : null;
  }

  /**
   * Explained library intensity (intensity of all matched signals / intensity of all signals)
   *
   * @return ratio of explained library signal intensity
   */
  public double getExplainedLibraryIntensity() {
    return explainedLibraryIntensity;
  }

  /**
   * Number of overlapping signals in both spectra
   *
   * @return number of matched signals
   */
  public int getOverlap() {
    return overlap;
  }

  /**
   * Cosine similarity
   *
   * @return cosine similarity (0-1)
   */
  public double getScore() {
    return score;
  }

  /**
   * SPectralSimilarityFunction name
   *
   * @return function name
   */
  public String getFunctionName() {
    return funcitonName;
  }

  /**
   * Library spectrum (usually filtered)
   *
   * @return library signals
   */
  public DataPoint[] getLibrary() {
    return library;
  }

  /**
   * Query spectrum (usually filtered)
   *
   * @return query signals
   */
  public DataPoint[] getQuery() {
    return query;
  }

  /**
   * All aligned data points of library(0) and query(1) spectrum
   *
   * @return DataPoint[library, query][datapoints]
   */
  public DataPoint[][] getAlignedDataPoints() {
    return aligned;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);

    writer.writeStartElement(XML_FUNCTION_ELEMENT);
    writer.writeCharacters(funcitonName);
    writer.writeEndElement();

    writer.writeStartElement(XML_OVERLAPPING_PEAKS_ELEMENT);
    writer.writeCharacters(String.valueOf(overlap));
    writer.writeEndElement();

    writer.writeStartElement(XML_SCORE_ELEMENT);
    writer.writeCharacters(String.valueOf(score));
    writer.writeEndElement();

    writer.writeStartElement(XML_EXPLAINED_LIB_INTENSITY_ELEMENT);
    writer.writeCharacters(String.valueOf(explainedLibraryIntensity));
    writer.writeEndElement();

    if (library != null) {
      writer.writeStartElement(XML_LIBRARY_SEPCTRUM_ELEMENT);

      writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
      writer.writeCharacters(ParsingUtils.doubleArrayToString(
          Arrays.stream(library).filter(Objects::nonNull).mapToDouble(DataPoint::getMZ).toArray()));
      writer.writeEndElement();

      writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
      writer.writeCharacters(ParsingUtils.doubleArrayToString(
          Arrays.stream(library).filter(Objects::nonNull).mapToDouble(DataPoint::getIntensity)
              .toArray()));
      writer.writeEndElement();

      writer.writeEndElement(); // lib spectrum
    }

    if (query != null) {
      writer.writeStartElement(XML_QUERY_SEPCTRUM_ELEMENT);

      writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
      writer.writeCharacters(ParsingUtils.doubleArrayToString(
          Arrays.stream(query).filter(Objects::nonNull).mapToDouble(DataPoint::getMZ).toArray()));
      writer.writeEndElement();

      writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
      writer.writeCharacters(ParsingUtils.doubleArrayToString(
          Arrays.stream(query).filter(Objects::nonNull).mapToDouble(DataPoint::getIntensity)
              .toArray()));
      writer.writeEndElement();

      writer.writeEndElement(); // query spectrum
    }

    if (aligned != null) {
      writer.writeStartElement(XML_ALIGNED_SEPCTRUM_LIST_ELEMENT);
      writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(aligned.length));

      for (DataPoint[] dataPoints : aligned) {

        writer.writeStartElement(XML_ALIGNED_SEPCTRUM_ELEMENT);

        writer.writeStartElement(CONST.XML_MZ_VALUES_ELEMENT);
        writer.writeCharacters(ParsingUtils.doubleArrayToString(
            Arrays.stream(dataPoints).mapToDouble(DataPoint::getMZ).toArray()));
        writer.writeEndElement();

        writer.writeStartElement(CONST.XML_INTENSITY_VALUES_ELEMENT);
        writer.writeCharacters(ParsingUtils.doubleArrayToString(
            Arrays.stream(dataPoints).mapToDouble(DataPoint::getIntensity).toArray()));
        writer.writeEndElement();

        writer.writeEndElement(); // alinged spectrum
      }

      writer.writeEndElement(); // aligned spectrum list
    }

    writer.writeEndElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpectralSimilarity that = (SpectralSimilarity) o;
    return Double.compare(that.getScore(), getScore()) == 0 && getOverlap() == that.getOverlap()
           && Objects.equals(funcitonName, that.funcitonName) && Arrays.equals(getLibrary(),
        that.getLibrary()) && Arrays.equals(getQuery(), that.getQuery()) && Arrays.deepEquals(
        aligned, that.aligned);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(getScore(), getOverlap(), funcitonName);
    result = 31 * result + Arrays.hashCode(getLibrary());
    result = 31 * result + Arrays.hashCode(getQuery());
    result = 31 * result + Arrays.deepHashCode(aligned);
    return result;
  }
}
