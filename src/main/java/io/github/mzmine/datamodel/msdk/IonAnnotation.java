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

package io.github.mzmine.datamodel.msdk;

import java.net.URL;

import javax.annotation.Nullable;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Annotation of a detected feature with a chemical structure, formula, or textual description.
 */
public interface IonAnnotation {

  /**
   * <p>
   * getChemicalStructure.
   * </p>
   *
   * @return Chemical structure of this annotation.
   */
  @Nullable
  IAtomContainer getChemicalStructure();

  /**
   * <p>
   * Chemical formula annotation should include charge, e.g. C6H13O6+ or [C34H58N5O35P3]2-.
   * </p>
   *
   * @return Chemical formula of this annotation.
   */
  @Nullable
  IMolecularFormula getFormula();

  /**
   * <p>
   * getIonType.
   * </p>
   *
   * @return Ionization type for this annotation.
   */
  @Nullable
  IonType getIonType();

  /**
   * <p>
   * getExpectedMz.
   * </p>
   *
   * @return Expected m/z value of this annotation.
   */
  @Nullable
  Double getExpectedMz();

  /**
   * <p>
   * getRetentionTime.
   * </p>
   *
   * @return RT
   * @since 0.0.8
   */
  @Nullable
  Float getExpectedRetentionTime();

  /**
   * <p>getAnnotationId.</p>
   *
   * @return a {@link String} object.
   */
  String getAnnotationId();

  /**
   * <p>
   * getDescription.
   * </p>
   *
   * @return Textual description of this annotation.
   */
  @Nullable
  String getDescription();

  /**
   * <p>
   * getIdentificationMethod.
   * </p>
   *
   * @return Identification method (e.g. database name) of this annotation.
   */
  @Nullable
  String getIdentificationMethod();

  /**
   * <p>
   * getAccessionURL.
   * </p>
   *
   * @return Accession URL for a database, if this annotation comes from a database.
   */
  @Nullable
  URL getAccessionURL();

  /**
   * <p>
   * getInchiKey
   * </p>
   *
   * @return InChI key of this annotation.
   */
  @Nullable
  String getInchiKey();

  /**
   * <p>
   * getDatabase
   * </p>
   *
   * @return Textual database description of this annotation.
   */
  @Nullable
  String getDatabase();

  /**
   * <p>
   * getSpectraRef
   * </p>
   *
   * @return Textual reference to a spectrum in a spectrum file for this annotation.
   */
  @Nullable
  String getSpectraRef();

  /**
   * Returns the reliability of the identification. This must be reported as an integer between 1-4:
   * 1: Identified metabolites 2: Putatively annotated compounds 3: Putatively characterized
   * compound classes 4: Unknown compounds
   *
   * @return The reliability of the ion annotation identification.
   */
  @Nullable
  Integer getReliability();

}
