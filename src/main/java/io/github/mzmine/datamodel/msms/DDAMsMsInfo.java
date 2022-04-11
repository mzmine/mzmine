/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.msms;

import io.github.mzmine.datamodel.Scan;
import org.jetbrains.annotations.Nullable;

public interface DDAMsMsInfo extends MsMsInfo {

  String XML_PRECURSOR_MZ_ATTR = "precursormz";
  String XML_PRECURSOR_CHARGE_ATTR = "charge";
  String XML_FRAGMENT_SCAN_ATTR = "fragmentscan";
  String XML_PARENT_SCAN_ATTR = "parentscan";
  String XML_ACTIVATION_ENERGY_ATTR = "energy";
  String XML_ACTIVATION_TYPE_ATTR = "activationtype";
  String XML_MSLEVEL_ATTR = "mslevel";
  String XML_ISOLATION_WINDOW_ATTR = "isolationwindow";

  @Nullable Float getActivationEnergy();

  double getIsolationMz();

  @Nullable Integer getPrecursorCharge();

  @Nullable Scan getParentScan();

  @Nullable Scan getMsMsScan();

}
