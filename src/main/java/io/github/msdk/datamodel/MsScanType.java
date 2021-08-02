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
 * Represents the type of MS scan.
 *
 * See also K. K. Murray, R. K. Boyd, M. N. Eberlin, G. J. Langley, L. Li and Y. Naito, Definitions
 * of Terms Relating to Mass Spectrometry (IUPAC Recommendations 2013) Pure Appl. Chem. 2013, 85,
 * 1515-1609.
 */
public enum MsScanType {

  /**
   * Full MS scan.
   */
  FULLMS,

  /**
   * Fragmentation (tandem MS, or MS/MS) scan.
   */
  MSMS,

  /**
   * Single ion monitoring (SIM) scan. This scan isolates a narrow range of m/z values before
   * scanning, thus increasing the sensitivity in this range.
   */
  SIM,

  /**
   * Multiple Reaction Monitoring (MRM) or Selected Reaction Monitoring (SRM) scan.
   */
  MRM_SRM,

  /**
   * Data Independent Acquisition (DIA) scan.
   */
  DIA,

  /**
   * Unknown MS scan type.
   */
  UNKNOWN;
}
