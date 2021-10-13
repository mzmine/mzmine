/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
