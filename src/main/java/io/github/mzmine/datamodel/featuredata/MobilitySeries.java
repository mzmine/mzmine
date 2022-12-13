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

/**
 * Stores mobility values.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MobilitySeries extends SeriesValueCount {

  // no FloatBuffer getMobilityValues(), because this usually occurs with scans and
  // thus mobility is stored in the scan/rawdatafile object and present in ram

  /**
   * Note that the index does not correspond to scan numbers. Usually, {@link
   * io.github.mzmine.datamodel.Scan} are associated with series, making {@link
   * io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries#getSpectra()} or {@link
   * io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries#getSpectrum(int)} more
   * convenient.
   *
   * @param index
   * @return The mobility value at the index position.
   * @see io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries#getSpectrum(int)
   * @see io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries#getSpectra()
   */
  double getMobility(int index);

}
