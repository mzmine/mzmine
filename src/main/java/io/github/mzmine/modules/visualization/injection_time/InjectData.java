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

package io.github.mzmine.modules.visualization.injection_time;

import io.github.mzmine.datamodel.Scan;

/**
 * Local data structure to represent injection time to intensity relationship for ms levels
 *
 * @param scan            the selected scan
 * @param injectTime      the injection time of the scan
 * @param lowestIntensity lowest intensity in mass list
 * @param mz              the mz of the data point with lowest intensity
 * @param msLevel         the ms level of the scan
 * @param mobility        ion mobility if present
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
record InjectData(Scan scan, double injectTime, double lowestIntensity, double mz, int msLevel,
                  double mobility) {

}
