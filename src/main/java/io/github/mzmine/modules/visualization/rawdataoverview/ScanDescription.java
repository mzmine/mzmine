/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.Scan;

/*
 * Raw data overview raw data table model class class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public record ScanDescription(Scan scan, String scanNumber, String retentionTime, String msLevel,
                              String precursorMz, String mzRange, String scanType, String polarity,
                              String definition, String basePeak, String basePeakIntensity,

                              // ion injection time e.g., in traps
                              String injectionTime) {

}
