/*
 * (C) Copyright 2015-2018 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.dataprocessing.id_sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.DBLink;
import io.github.msdk.datamodel.SimpleIonAnnotation;

/**
 * <p>Class SiriusIonAnnotation</p>
 * This class extends SimpleIonAnnotation and adds several fields
 * FTree - the result tree provided by Sirius experiment processed
 * SMILES - the result string provided by FingerIdWebMethod, after computation of FingerprintCandidates
 * DBLinks - map of DB name->id, shows where corresponding result element can be found (ex.: Pubchem: 3123)
 * Score fields - score value from Sirius result or FingerId result objects
 */
public class SiriusIonAnnotation extends SimpleIonAnnotation {
  private FTree ftree;
  private String smilesString;
  private DBLink[] dblinks;
  private Double siriusScore;
  private Double fingerIdScore;

  public SiriusIonAnnotation(SiriusIonAnnotation master) {
    super();
    copyInternal(this, master);
  }

  public SiriusIonAnnotation() {
    super();
  }

  public SiriusIonAnnotation copy() {
    SiriusIonAnnotation target = new SiriusIonAnnotation();
    copyInternal(target, this);
    return target;
  }

  private void copyInternal(SiriusIonAnnotation target,SiriusIonAnnotation master) {
    target.setAccessionURL(master.getAccessionURL());
    target.setAnnotationId(master.getAnnotationId());
    target.setChemicalStructure(master.getChemicalStructure());
    target.setDatabase(master.getDatabase());
    target.setDescription(master.getDescription());
    target.setExpectedMz(master.getExpectedMz());
    target.setExpectedRetentionTime(master.getExpectedRetentionTime());
    target.setSpectraRef(master.getSpectraRef());
    target.setReliability(master.getReliability());
    target.setIonType(master.getIonType());
    target.setInchiKey(master.getInchiKey());
    target.setIdentificationMethod(master.getIdentificationMethod());
    target.setFormula(master.getFormula());
    target.setFTree(master.getFTree());
    target.setDBLinks(master.getDBLinks());
    target.setSMILES(master.getSMILES());
    target.setFingerIdScore(master.getFingerIdScore());
    target.setSiriusScore(master.getSiriusScore());
  }

  public void setFTree(FTree ftree) {
    this.ftree = ftree;
  }

  public FTree getFTree() {
    return ftree;
  }

  //TODO: check SMILES string somehow!
  public void setSMILES(String SMILES) {
    smilesString = SMILES;
  }

  public String getSMILES() {
    return smilesString;
  }

  public void setDBLinks(DBLink[] links) {
    this.dblinks = links;
  }

  public DBLink[] getDBLinks() {
    return dblinks;
  }

  public void setSiriusScore(Double score) {
    siriusScore = score;
  }

  public Double getSiriusScore() {
    return siriusScore;
  }

  public void setFingerIdScore(Double score) {
    fingerIdScore = score;
  }

  public Double getFingerIdScore() {
    return fingerIdScore;
  }
}
