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

package io.github.mzmine.modules.io.export_library_analysis_csv;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;

/**
 * this package private record holds precomputed filtered spectral and neutral loss data for a
 * spectral library entry
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
record FilteredSpec(SpectralDBEntry entry, DataPoint[] dps, DataPoint[] neutralLosses,
                    double precursorMZ) {

}
