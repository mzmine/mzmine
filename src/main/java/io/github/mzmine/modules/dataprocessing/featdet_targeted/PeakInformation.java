/*
 *  Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;

/**
 *
 * @author scsandra
 */
public class PeakInformation {
  private double mz;
  private double rt;
  private String name;

  public PeakInformation(double mz, double rt, String name) {
    this.mz = mz;
    this.rt = rt;
    this.name = name;
  }

  public double getMZ() {
    return mz;
  }

  public double getRT() {
    return rt;
  }

  public String getName() {
    return name;
  }

  public CompoundDBAnnotation toCompountDBAnnotation() {
    final SimpleCompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    annotation.put(PrecursorMZType.class, mz);
    annotation.put(RTType.class, (float)rt);
    annotation.put(CompoundNameType.class, name);
    return annotation;
  }
}
