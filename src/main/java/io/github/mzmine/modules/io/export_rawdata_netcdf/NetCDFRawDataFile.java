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

package io.github.mzmine.modules.io.export_rawdata_netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import io.github.mzmine.datamodel.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.msdk.SimpleRawDataFile;
import ucar.nc2.NetcdfFile;

/**
 * <p>NetCDFRawDataFile class.</p>
 *
 */
public class NetCDFRawDataFile extends SimpleRawDataFile {

  private NetcdfFile inputFile;

  /**
   * <p>Constructor for NetCDFRawDataFile.</p>
   *
   * @param rawDataFileName a {@link String} object.
   * @param originalRawDataFile a {@link Optional} object.
   * @param inputFile a {@link NetcdfFile} object.
   */
  public NetCDFRawDataFile(String rawDataFileName, Optional<File> originalRawDataFile,
      NetcdfFile inputFile) {
    super(rawDataFileName, originalRawDataFile);
    this.inputFile = inputFile;
  }

  /** {@inheritDoc} */
  @Override
  public void dispose() {
    try {
      inputFile.close();
    } catch (IOException e) {
      new MSDKRuntimeException(e);
    }
    super.dispose();
  }

}
