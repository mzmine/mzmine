/*
 * Copyright 2006-2021 The MZmine Development Team
 * This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package io.github.mzmine.modules.io.import_waters;

import MassLynxSDK.MassLynxIonMode;
import com.google.common.collect.Range;

public class IntermediateScan  {

  private boolean iscontinuum;
  private int mslevel;
  private MassLynxIonMode ionmode;
  private Range<Double> MZRange;
  private int function_number;
  private float retentionTime;

  public IntermediateScan(boolean iscontinuum,int mslevel, MassLynxIonMode ionmode,
      Range<Double> MZRange, int function_number,float retentionTime)
  {
    this.function_number=function_number;
    this.retentionTime=retentionTime;
    this.MZRange=MZRange;
    this.ionmode=ionmode;
    this.iscontinuum=iscontinuum;
    this.mslevel=mslevel;
  }

  public boolean isIscontinuum() {
    return iscontinuum;
  }

  public int getMslevel() {
    return mslevel;
  }

  public MassLynxIonMode getIonmode() {
    return ionmode;
  }

  public Range<Double> getMZRange() {
    return MZRange;
  }

  public int getFunction_number() {
    return function_number;
  }

  public float getRetentionTime() {
    return retentionTime;
  }
}

