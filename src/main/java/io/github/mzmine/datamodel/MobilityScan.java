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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mass spectrum acquired during an ion mobility experiment. Note that this class does not extend
 * {@link Scan} but just {@link MassSpectrum}.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MobilityScan extends MassSpectrum {

  public static final double DEFAULT_MOBILITY = -1.0d;

  /**
   * @return The mobility of this sub-spectrum. The unit will depend on the respective mass
   * spectrometer and can be checked via {@link MobilityScan#getMobilityType()}.
   */
  public double getMobility();

  /**
   * See {@link MobilityType}
   *
   * @return The type of mobility acquired in this mass spectrum.
   */
  public MobilityType getMobilityType();

  /**
   * @return THe frame this spectrum belongs to.
   */
  public Frame getFrame();

  /**
   * @return The retention time of the frame when this spectrum was acquired.
   */
  public float getRetentionTime();

  /**
   * @return The index of this mobility subscan.
   */
  public int getMobilityScamNumber();

  @Nullable
  public ImsMsMsInfo getMsMsInfo();

  public void addMassList(final @Nonnull MassList massList);

  public void removeMassList(final @Nonnull MassList massList);

  @Nonnull
  public Set<MassList> getMassLists();

  public MassList getMassList(@Nonnull String name);
}
