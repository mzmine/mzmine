/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;

public class MSPEntryGenerator {

  /**
   * Creates a simple MSP nist format DB entry
   * 
   * @param param
   * @param dps
   * @return
   */
  public static String createMSPEntry(LibrarySubmitIonParameters param, DataPoint[] dps) {

    LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();

    boolean exportRT = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getValue();
    String ionMode =
        meta.getParameter(LibraryMetaDataParameters.IONMODE).getValue().equals(Polarity.Positive)
            ? "P"
            : "N";

    String def = ": ";
    String br = "\n";
    StringBuilder s = new StringBuilder();
    // tag spectrum from mzmine2
    // ion specific
    s.append(DBEntryField.NAME.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.COMPOUND_NAME).getValue() + br);
    s.append(DBEntryField.INCHIKEY.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.INCHI_AUX).getValue() + br);
    s.append(DBEntryField.MS_LEVEL.getNistMspID() + def + "MS2" + br);
    s.append(DBEntryField.INSTRUMENT_TYPE.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.INSTRUMENT).getValue() + br);
    s.append(DBEntryField.INSTRUMENT.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.INSTRUMENT_NAME).getValue() + br);
    s.append(DBEntryField.ION_MODE.getNistMspID() + def + ionMode + br);
    s.append(DBEntryField.COLLISION_ENERGY.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.FRAGMENTATION_METHOD).getValue() + br);
    s.append(DBEntryField.FORMULA.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.FORMULA).getValue() + br);
    s.append(DBEntryField.EXACT_MASS.getNistMspID() + def
        + meta.getParameter(LibraryMetaDataParameters.EXACT_MASS).getValue() + br);

    s.append(DBEntryField.MZ.getNistMspID() + def
        + param.getParameter(LibrarySubmitIonParameters.MZ).getValue() + br);
    s.append(DBEntryField.IONTYPE.getNistMspID() + def
        + param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue() + br);

    if (exportRT) {
      Double rt =
          meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getEmbeddedParameter().getValue();
      if (rt != null)
        s.append(DBEntryField.RT.getNistMspID() + def + rt + br);
    }

    // num peaks and data
    s.append(DBEntryField.NUM_PEAKS.getNistMspID() + def + dps.length + br);

    NumberFormat mzForm = new DecimalFormat("0.######");
    for (DataPoint dp : dps) {
      s.append(mzForm.format(dp.getMZ()) + " " + dp.getIntensity() + br);
    }
    s.append(br);
    return s.toString();
  }
}
