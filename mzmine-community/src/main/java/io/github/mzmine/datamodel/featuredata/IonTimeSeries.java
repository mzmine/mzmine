/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.ModifiableSpectra;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to store a consecutive number of data points (mz and intensity values).
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public interface IonTimeSeries<T extends Scan> extends IonSpectrumSeries<T>, IntensityTimeSeries,
    ModifiableSpectra<T> {

  SimpleIonTimeSeries EMPTY = new SimpleIonTimeSeries(null, new double[0], new double[0],
      List.of());

  @Override
  IonTimeSeries<T> emptySeries();

  /**
   * @param scan
   * @return The intensity value for the given scan or 0 if the no intensity was measured at that
   * scan.
   */
  @Override
  default double getIntensityForSpectrum(Scan scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getIntensity(index);
    }
    return 0;
  }

  /**
   * @param scan
   * @return The mz for the given scan or 0 if no intensity was measured at that scan.
   */
  @Override
  default double getMzForSpectrum(Scan scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getMZ(index);
    }
    return 0;
  }

  @Override
  default IonTimeSeries<T> subSeries(MemoryMapStorage storage, float start, float end) {
    final IndexRange indexRange = BinarySearch.indexRange(Range.closed(start, end), getSpectra(),
        Scan::getRetentionTime);
    if (indexRange.isEmpty()) {
      return emptySeries();
    }
    return subSeries(storage, indexRange.min(), indexRange.maxExclusive());
  }

  @Override
  default IonTimeSeries<T> subSeries(MemoryMapStorage storage, int startIndexInclusive,
      int endIndexExclusive) {
    if (endIndexExclusive - startIndexInclusive <= 0) {
      return emptySeries();
    }

    // sublist:
    // PRO: the original list is kept alive either way (e.g. the Scan list in FeatureList) - saves memory
    // CONTRA: the original list is not referenced and and will be kept alive by sublist

    // chromatograms are all from a different list of scans
    // if resolver runs on {@link FeatureFullDataAccess} it is one list for all features of that file
    // in that case it makes sense to keep the original list or a sublist
    // in this case the feature list keeps the MS1 scans list alive (if its based on

    // this general implementation should not use sublist as this mayb be used in other cases as well.
    // TODO think about using this fact to just having one list of scans with continuos sublists
    // like having one MemorySegment for chromatogram and then just save the index ranges of scans for resolved features.
    List<T> scans = new ArrayList<>(getSpectra().subList(startIndexInclusive, endIndexExclusive));
    return subSeries(storage, scans);
  }

  @Override
  IonTimeSeries<T> subSeries(@Nullable MemoryMapStorage storage, @NotNull List<T> subset);

  @Override
  IonTimeSeries<T> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newIntensityValues);

  @Override
  IonTimeSeries<T> copyAndReplace(@Nullable MemoryMapStorage storage, @NotNull double[] newMzValues,
      @NotNull double[] newIntensityValues);

  /**
   * Saves this time series to xml. The implementing class is responsible for creating the xml
   * element and closing the xml element.
   *
   * @param writer   The writer.
   * @param allScans All scans of the given raw data file (those are used during import). NOT the
   *                 preselected scans obtained from
   *                 {@link
   *                 io.github.mzmine.datamodel.features.ModularFeatureList#getSeletedScans(RawDataFile)}.
   */
  void saveValueToXML(XMLStreamWriter writer, List<T> allScans) throws XMLStreamException;

  void saveValueToXML(XMLStreamWriter writer, List<T> allScans, boolean includeRt) throws XMLStreamException;

  @Override
  default @Nullable MemoryMapStorage getStorage() {
    if (getSpectra().isEmpty() || !(getSpectrum(0) instanceof Scan scan)) {
      return null;
    }

    return scan.getDataFile().getMemoryMapStorage();
  }
}
