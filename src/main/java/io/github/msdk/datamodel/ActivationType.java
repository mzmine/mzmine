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

/**
 * Represents the various types of MS/MS experiments.
 *
 * See also K. K. Murray, R. K. Boyd, M. N. Eberlin, G. J. Langley, L. Li and Y. Naito, Definitions
 * of Terms Relating to Mass Spectrometry (IUPAC Recommendations 2013) Pure Appl. Chem. 2013, 85,
 * 1515-1609.
 */
public enum ActivationType {

  /**
   * Collision-induced dissociation.
   */
  CID,

  /**
   * Higher-energy C-trap dissociation.
   */
  HCD,

  /**
   * Electron-capture dissociation.
   */
  ECD,

  /**
   * Electron-transfer dissociation.
   */
  ETD,

  /**
   * Electron-transfer and higher-energy collision dissociation.
   */
  ETHCD,

  /**
   * Ultraviolet photodissociation.
   */
  UVPD,

  /**
   * Unknown MS/MS experiment type.
   */
  UNKNOWN;

}
