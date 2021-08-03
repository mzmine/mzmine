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
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.msdk;

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
