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
 * Enum of supported raw data file formats
 */
public enum FileType {

  /**
   * mzML format. See Martens L, Chambers M, Sturm M, Kessner D, Levander F, Shofstahl J, Tang WH,
   * Römpp A, Neumann S, Pizarro AD, Montecchi-Palazzi L, Tasman N, Coleman M, Reisinger F, Souda P,
   * Hermjakob H, Binz PA, Deutsch EW (2011) mzML-a community standard for mass spectrometry data,
   * Mol Cell Proteomics 10(1):R110.000133. doi:10.1074/mcp.R110.000133
   */
  MZML,

  /**
   * mz5 format, based on HDF5. See Wilhelm M, Kirchner M, Steen JA, Steen H (2012) mz5: space- and
   * time-efficient storage of mass spectrometry data sets, Mol Cell Proteomics 11(1):O111.011379.
   * doi:10.1074/mcp.O111.011379
   */
  MZ5,

  /**
   * mzDB format. See 1. Bouyssié, D. et al. mzDB: a file format using multiple indexing strategies
   * for the efficient analysis of large LC-MS/MS and SWATH-MS data sets. Mol. Cell Proteomics 14,
   * 771–781 (2015). doi:10.1074/mcp.O114.039115
   */
  MZDB,

  /**
   * mzXML format, now deprecated in favor of mzML. See Pedrioli PG, Eng JK, Hubley R, Vogelzang M,
   * Deutsch EW, Raught B, Pratt B, Nilsson E, Angeletti RH, Apweiler R, Cheung K, Costello CE,
   * Hermjakob H, Huang S, Julian RK, Kapp E, McComb ME, Oliver SG, Omenn G, Paton NW, Simpson R,
   * Smith R, Taylor CF, Zhu W, Aebersold R (2004) A common open representation of mass spectrometry
   * data and its application to proteomics research, Nat. Biotechnol. 22(11):1459–66.
   * doi:10.1038/nbt1031
   */
  MZXML,

  /**
   * mzData format, now deprecated in favor of mzML. See Orchard S, Montechi-Palazzi L, Deutsch EW,
   * Binz PA, Jones AR, Paton N, Pizarro A, Creasy DM, Wojcik J, Hermjakob H (2007) Five years of
   * progress in the Standardization of Proteomics Data 4(th) Annual Spring Workshop of the
   * HUPO-Proteomics Standards Initiative April 23–25, 2007 Ecole Nationale Supérieure (ENS), Lyon,
   * France, Proteomics 7(19):3436–40. doi:10.1002/pmic.200700658
   */
  MZDATA,

  /**
   * NetCDF (ANDI-MS) format, commonly used for GC-MS data. See the following standards
   *
   * ASTM E1947, Standard Specification for Analytical Data Interchange Protocol for Chromatographic
   * Data. doi:10.1520/E1947-98R14
   *
   * ASTM E1948, Standard Guide for Analytical Data Interchange Protocol for Chromatographic Data.
   * doi:10.1520/E1948-98R14
   */
  NETCDF,

  /**
   * Native RAW format of Thermo Fisher Scientific MS instruments.
   */
  THERMO_RAW,

  /**
   * Native RAW format of Waters MS instruments.
   */
  WATERS_RAW,

  /**
   * mzTab format. Standard format for sharing feature tables.
   */
  MZTAB,

  /**
   * Peak table and MS export format used by LECO's ChromaTOF software for GC, GC-MS and GCxGC-MS
   * instruments.
   */
  CSV_CHROMATOF,

  /**
   * Unknown or unsupported format.
   */
  UNKNOWN

}
