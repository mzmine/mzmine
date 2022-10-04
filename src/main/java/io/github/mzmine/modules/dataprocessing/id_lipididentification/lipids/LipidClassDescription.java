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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

public class LipidClassDescription {

  private String id;
  private String lipidClass;
  private String molecularFormula;
  private String abbreviation;
  private String exactMass;
  private String info;
  private String status;
  private String msmsFragmentsPos;

  public LipidClassDescription(String id, String lipidClass, String molecularFormula,
      String abbreviation, String exactMass, String info, String status, String msmsFragmentsPos) {

    this.id = id;
    this.lipidClass = lipidClass;
    this.molecularFormula = molecularFormula;
    this.abbreviation = abbreviation;
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
