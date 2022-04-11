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
 * Stores retention time values.
 */
public interface TimeSeries extends SeriesValueCount {

  // no FloatBuffer getRetentionTimeValues(), because this usually occurs with scans and
  // thus rt is stored in the scan object and present in ram

  /**
   * Note that the index does not correspond to scan numbers. Usually, {@link
   * io.github.mzmine.datamodel.Scan} are associated with series, making {@link
   * IonSpectrumSeries#getSpectra()} or {@link IonSpectrumSeries#getSpectrum(int)} more convenient.
   *
   * @param index
   * @return The rt value at the index position.
   */
  float getRetentionTime(int index);

}
