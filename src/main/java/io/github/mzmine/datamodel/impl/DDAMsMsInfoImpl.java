/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

public class DDAMsMsInfoImpl implements DDAMsMsInfo {

  @NotNull
  private final double isolationMz;
  @Nullable
  private final Integer charge;
  @Nullable
  private final Float activationEnergy;
  @Nullable
  private final Scan msMsScan;
  @Nullable
  private final Scan parentScan;
  @Nullable
  private final Integer msLevel;
  @NotNull
  private final ActivationMethod method;

  public DDAMsMsInfoImpl(double isolationMz, @Nullable Integer charge,
      @Nullable Float activationEnergy, @Nullable Scan msMsScan, @Nullable Scan parentScan,
      @Nullable Integer msLevel, @NotNull ActivationMethod method) {
    this.isolationMz = isolationMz;
    this.charge = charge;
    this.activationEnergy = activationEnergy;
    this.msMsScan = msMsScan;
    this.parentScan = parentScan;
    this.msLevel = msLevel;
    this.method = method;
  }

  @Override
  public @Nullable Float getActivationEnergy() {
    return activationEnergy;
  }

  @Override
  public double getIsolationMz() {
    return isolationMz;
  }

  @Override
  public @Nullable Integer getPrecursorCharge() {
    return charge;
  }

  @Override
  public @Nullable Scan getParentScan() {
    return parentScan;
  }

  @Override
  public Scan getMsMsScan() {
    return msMsScan;
  }

  @Override
  public @Nullable Integer getMsLevel() {
    return msLevel;
  }

  @Override
  public @NotNull ActivationMethod getActivationMethod() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DDAMsMsInfoImpl)) {
      return false;
    }
    DDAMsMsInfoImpl that = (DDAMsMsInfoImpl) o;
    return Double.compare(that.getIsolationMz(), getIsolationMz()) == 0 && Objects.equals(charge,
        that.charge) && Objects.equals(getActivationEnergy(), that.getActivationEnergy())
        && Objects.equals(getMsMsScan(), that.getMsMsScan()) && Objects.equals(getParentScan(),
        that.getParentScan()) && Objects.equals(getMsLevel(), that.getMsLevel())
        && method == that.method;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIsolationMz(), charge, getActivationEnergy(), getMsMsScan(),
        getParentScan(), getMsLevel(), method);
  }
}
