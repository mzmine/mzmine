/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores combinations of intensity and mz values.
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public interface IonSpectrumSeries<T extends MassSpectrum> extends IonSeries {

  /**
   * @param writer   A writer to append the scans element to.
   * @param series   The series containing scans to save.
   * @param allScans All scans belonging to the current collection. <b>NOTE</b> As a general
   *                 contract: No preselected list shall be given here, by default, the original
   *                 selection shall be used. For example, when saving an EIC from a feature list,
   *                 this should be passed all scans obtained from the {@link
   *                 RawDataFile#getScans()} method, not the preselected scans obtained by {@link
   *                 io.github.mzmine.datamodel.features.ModularFeatureList#getSeletedScans(RawDataFile)}.
   */
  public static <T extends MassSpectrum> void saveSpectraIndicesToXML(XMLStreamWriter writer,
      IonSpectrumSeries<T> series, List<T> allScans) throws XMLStreamException {
    writer.writeStartElement(CONST.XML_SCAN_LIST_ELEMENT);
    writer.writeAttribute(CONST.XML_NUM_VALUES_ATTR, String.valueOf(series.getNumberOfValues()));
    final int[] indices = ParsingUtils.getIndicesOfSubListElements(series.getSpectra(), allScans);
    writer.writeCharacters(ParsingUtils.intArrayToString(indices, indices.length));
    writer.writeEndElement();
  }

  List<T> getSpectra();

  default T getSpectrum(int index) {
    return getSpectra().get(index);
  }

  /**
   * @param spectrum
   * @return The intensity value for the given spectrum or 0 if the no intensity was measured at
   * that spectrum.
   */
  double getIntensityForSpectrum(T spectrum);

  /**
   * @param spectrum
   * @return The mz for the given spectrum or 0 if no intensity was measured at that spectrum.
   */
  default double getMzForSpectrum(T spectrum) {
    int index = getSpectra().indexOf(spectrum);
    if (index != -1) {
      return getMZ(index);
    }
    return 0;
  }

  /**
   * Creates a sub series of this series with the data corresponding to the given list of spectra.
   * Used during deconvolution, when a feature is cut into several individual features.
   *
   * @param storage The new storage, may be null if data shall be stored in ram.
   * @param subset  The subset of spectra. sorted by their scan number (retention time or mobility)
   * @return The subset series.
   */
  IonSpectrumSeries<T> subSeries(@Nullable MemoryMapStorage storage, @NotNull List<T> subset);

  /**
   * Creates a copy of this series using the same list of scans but possibly new mz/intensity
   * values.
   *
   * @param storage            May be null if the new series shall be stored in ram.
   * @param newMzValues
   * @param newIntensityValues
   * @return
   */
  IonSpectrumSeries<T> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues);

}
