/*
 * (C) Copyright 2015-2017 by MSDK Development Team
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

package io.github.msdk.datamodel;

import java.net.URL;

import javax.annotation.Nullable;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Simple IonAnnotation implementation;
 */
public class SimpleIonAnnotation implements IonAnnotation {

  private @Nullable IAtomContainer chemicalStructure;
  private @Nullable IMolecularFormula formula;
  private @Nullable IonType ionType;
  private @Nullable Double expectedMz;
  private @Nullable String description;
  private @Nullable String identificationMethod;
  private @Nullable String annotationId;
  private @Nullable URL accessionURL;
  private @Nullable Float expectedRT;
  private @Nullable String inchiKey;
  private @Nullable String database;
  private @Nullable String spectraRef;
  private @Nullable Integer reliability;

  /** {@inheritDoc} */
  @Override
  @Nullable
  public IAtomContainer getChemicalStructure() {
    return chemicalStructure;
  }

  /**
   * {@inheritDoc}
   *
   * @param chemicalStructure a {@link IAtomContainer} object.
   */
  public void setChemicalStructure(@Nullable IAtomContainer chemicalStructure) {
    this.chemicalStructure = chemicalStructure;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public IMolecularFormula getFormula() {
    return formula;
  }

  /**
   * {@inheritDoc}
   *
   * @param formula a {@link IMolecularFormula} object.
   */
  public void setFormula(@Nullable IMolecularFormula formula) {
    this.formula = formula;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public IonType getIonType() {
    return ionType;
  }

  /**
   * {@inheritDoc}
   *
   * @param ionType a {@link IonType} object.
   */
  public void setIonType(@Nullable IonType ionType) {
    this.ionType = ionType;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Double getExpectedMz() {
    return expectedMz;
  }

  /**
   * {@inheritDoc}
   *
   * @param expectedMz a {@link Double} object.
   */
  public void setExpectedMz(@Nullable Double expectedMz) {
    this.expectedMz = expectedMz;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   *
   * @param description a {@link String} object.
   */
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getIdentificationMethod() {
    return identificationMethod;
  }

  /**
   * {@inheritDoc}
   *
   * @param identificationMethod a {@link String} object.
   */
  public void setIdentificationMethod(@Nullable String identificationMethod) {
    this.identificationMethod = identificationMethod;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getAnnotationId() {
    return annotationId;
  }

  /**
   * {@inheritDoc}
   *
   * @param annotationId a {@link String} object.
   */
  public void setAnnotationId(@Nullable String annotationId) {
    this.annotationId = annotationId;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public URL getAccessionURL() {
    return accessionURL;
  }

  /**
   * {@inheritDoc}
   *
   * @param accessionURL a {@link URL} object.
   */
  public void setAccessionURL(@Nullable URL accessionURL) {
    this.accessionURL = accessionURL;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Float getExpectedRetentionTime() {
    return expectedRT;
  }

  /**
   * {@inheritDoc}
   *
   * @param expectedRT a {@link Float} object.
   */
  public void setExpectedRetentionTime(@Nullable Float expectedRT) {
    this.expectedRT = expectedRT;
  }


  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getInchiKey() {
    return inchiKey;
  }

  /**
   * {@inheritDoc}
   *
   * @param inchiKey a {@link String} object.
   */
  public void setInchiKey(@Nullable String inchiKey) {
    this.inchiKey = inchiKey;
  }




  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getDatabase() {
    return database;
  }

  /**
   * {@inheritDoc}
   *
   * @param database a {@link String} object.
   */
  public void setDatabase(@Nullable String database) {
    this.database = database;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getSpectraRef() {
    return spectraRef;
  }

  /**
   * {@inheritDoc}
   *
   * @param spectraRef a {@link String} object.
   */
  public void setSpectraRef(@Nullable String spectraRef) {
    this.spectraRef = spectraRef;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Integer getReliability() {
    return reliability;
  }

  /**
   * {@inheritDoc}
   *
   * @param reliability a {@link Integer} object.
   */
  public void setReliability(@Nullable Integer reliability) {
    this.reliability = reliability;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return annotationId + " @ " + expectedMz;
  }

}
