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

package io.github.mzmine.modules.io.projectsave;

enum RawDataElementName {

  RAWDATA("rawdata"), //
  NAME("name"), //
  QUANTITY_SCAN("num_scans"), //
  ID("id"), //
  SCAN("scan"), //
  SCAN_ID("id"), //
  MS_LEVEL("mslevel"), //
  // QUANTITY_FRAGMENT_SCAN("fragmentscans"),
  // FRAGMENT_SCAN("fragmentscan"), //
  QUANTITY("quantity"), //
  PARENT_SCAN("parent"), //
  PRECURSOR_MZ("precursor_mz"), //
  PRECURSOR_CHARGE("precursor_charge"), //
  RETENTION_TIME("rt"), //
  CENTROIDED("centroid"), //
  QUANTITY_DATAPOINTS("num_dp"), //
  MASS_LIST("mass_list"), //
  STORED_DATAPOINTS("stored_datapoints"), //
  STORED_DATA("stored_data"), //
  STORAGE_ID("storage_id"), //
  POLARITY("polarity"), //
  SCAN_DESCRIPTION("scan_description"), //
  SCAN_MZ_RANGE("scan_mz_range"), //
  COLOR("color"), //

  // imaging related
  COORDINATES("coordinates"),

  // ims related
  FRAME("frame"), //
  MOBILITY("mobility"), //
  MOBILITY_TYPE("mobility_type"), //
  FRAME_ID("frame_id"), //
  QUANTITY_FRAMES("num_frames"), //
  MOBILITY_SCANNUM("mobility_scannum"), //
  QUANTITY_MOBILITY_SCANS("num_mobility_scans"), //
  MOBILITY_SCAN("mobility_scan"), //
  LOWER_MOBILITY_RANGE("lower_mobility_range"), //
  UPPER_MOBILITY_RANGE("upper_mobility_range");

  private String elementName;

  private RawDataElementName(String itemName) {
    this.elementName = itemName;
  }

  public String getElementName() {
    return elementName;
  }

}
