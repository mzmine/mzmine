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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;

public class MolecularSpeciesLevelAnnotation implements ILipidAnnotation {

  private ILipidClass lipidClass;
  private String annotation;
  private static final LipidAnnotationLevel lipidAnnotationLevel =
      LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL;
  private IMolecularFormula molecularFormula;
  private List<ILipidChain> lipidChains;

  public MolecularSpeciesLevelAnnotation(ILipidClass lipidClass, String annotation,
      IMolecularFormula molecularFormula, List<ILipidChain> lipidChains) {
    this.lipidClass = lipidClass;
    this.annotation = annotation;
    this.molecularFormula = molecularFormula;
    this.lipidChains = lipidChains;
  }

  @Override
  public ILipidClass getLipidClass() {
    return lipidClass;
  }

  @Override
  public void setLipidClass(ILipidClass lipidClass) {
    this.lipidClass = lipidClass;
  }

  @Override
  public String getAnnotation() {
    return annotation;
  }

  @Override
  public void setAnnotation(String annotation) {
    this.annotation = annotation;
  }

  @Override
  public LipidAnnotationLevel getLipidAnnotationLevel() {
    return lipidAnnotationLevel;
  }

  @Override
  public IMolecularFormula getMolecularFormula() {
    return molecularFormula;
  }

  public List<ILipidChain> getLipidChains() {
    return lipidChains;
  }

  public void setLipidChains(List<ILipidChain> lipidChains) {
    this.lipidChains = lipidChains;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
    result = prime * result + ((lipidChains == null) ? 0 : lipidChains.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MolecularSpeciesLevelAnnotation other = (MolecularSpeciesLevelAnnotation) obj;
    if (annotation == null) {
      if (other.annotation != null)
        return false;
    } else if (!annotation.equals(other.annotation))
      return false;
    if (lipidChains == null) {
      if (other.lipidChains != null)
        return false;
    } else if (!lipidChains.equals(other.lipidChains))
      return false;
    return true;
  }

}
