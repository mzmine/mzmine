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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

public class LipidClassDescription {

  private String id;
  private String lipidCoreClass;
  private String lipidMainClass;
  private String lipidClass;
  private String molecularFormula;
  private String abbreviation;
  private String ionization;
  private String exactMass;
  private String info;
  private String status;
  private String msmsFragmentsPos;

  public LipidClassDescription(String id, String lipidCoreClass, String lipidMainClass, String lipidClass,
      String molecularFormula, String abbreviation, String ionization, String exactMass,
			String info, String status, String msmsFragmentsPos) {

    this.id = id;
    this.lipidCoreClass = lipidCoreClass;
    this.lipidMainClass = lipidMainClass;
    this.lipidClass = lipidClass;
    this.molecularFormula = molecularFormula;
    this.abbreviation = abbreviation;
    this.ionization = ionization;
    this.exactMass = exactMass;
    this.info = info;
    this.status = status;
    this.msmsFragmentsPos = msmsFragmentsPos;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLipidCoreClass() {
    return lipidCoreClass;
  }

  public void setLipidCoreClass(String lipidCoreClass) {
    this.lipidCoreClass = lipidCoreClass;
  }

  public String getLipidMainClass() {
    return lipidMainClass;
  }

  public void setLipidMainClass(String lipidMainClass) {
    this.lipidMainClass = lipidMainClass;
  }

  public String getLipidClass() {
    return lipidClass;
  }

  public void setLipidClass(String lipidClass) {
    this.lipidClass = lipidClass;
  }

  public String getMolecularFormula() {
    return molecularFormula;
  }

  public void setMolecularFormula(String molecularFormula) {
    this.molecularFormula = molecularFormula;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  public void setAbbreviation(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  public String getIonization() {
    return ionization;
  }

  public void setIonization(String ionization) {
    this.ionization = ionization;
  }

  public String getExactMass() {
    return exactMass;
  }

  public void setExactMass(String exactMass) {
    this.exactMass = exactMass;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMsmsFragmentsPos() {
    return msmsFragmentsPos;
  }

  public void setMsmsFragmentsPos(String msmsFragmentsPos) {
    this.msmsFragmentsPos = msmsFragmentsPos;
  }


}
