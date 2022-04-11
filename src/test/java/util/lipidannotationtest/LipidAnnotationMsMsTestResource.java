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
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package util.lipidannotationtest;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;

public class LipidAnnotationMsMsTestResource {

  private double[] mzFragments;
  private IonizationType ionizationType;
  private ILipidAnnotation testLipid;

  public LipidAnnotationMsMsTestResource(double[] mzFragments, IonizationType ionizationType,
      ILipidAnnotation testLipid) {
    this.mzFragments = mzFragments;
    this.ionizationType = ionizationType;
    this.testLipid = testLipid;
  }

  public double[] getMzFragments() {
    return mzFragments;
  }

  public void setMzFragments(double[] mzFragments) {
    this.mzFragments = mzFragments;
  }

  public IonizationType getIonizationType() {
    return ionizationType;
  }

  public void setIonizationType(IonizationType ionizationType) {
    this.ionizationType = ionizationType;
  }

  public ILipidAnnotation getTestLipid() {
    return testLipid;
  }

  public void setTestLipid(ILipidAnnotation testLipid) {
    this.testLipid = testLipid;
  }

}
