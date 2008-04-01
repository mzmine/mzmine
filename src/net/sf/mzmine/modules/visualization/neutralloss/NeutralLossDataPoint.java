/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.neutralloss;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
class NeutralLossDataPoint {

    private float mzValue;
    private int scanNumber, precursorScanNumber;
    private float precursorMZ;
    private int precursorCharge;
    private float retentionTime;
    private float neutralLoss;
    private float precursorMass;
    private String label;

    /**
     * @param scanNumber
     * @param precursorScanNumber
     * @param precursorMZ
     * @param precursorCharge
     * @param retentionTime
     */
    NeutralLossDataPoint(float mzValue, int scanNumber,
            int precursorScanNumber, float precursorMZ, int precursorCharge,
            float retentionTime) {

        NumberFormat rtFormat = MZmineCore.getRTFormat();
        NumberFormat mzFormat = MZmineCore.getMZFormat();

        this.mzValue = mzValue;
        this.scanNumber = scanNumber;
        this.precursorScanNumber = precursorScanNumber;
        this.precursorMZ = precursorMZ;
        this.precursorCharge = precursorCharge;
        this.retentionTime = retentionTime;

        precursorMass = precursorMZ;
        if (precursorCharge > 0)
            precursorMass *= precursorCharge;
        neutralLoss = precursorMass - mzValue;

        StringBuffer sb = new StringBuffer();
        sb.append("loss: ");
        sb.append(mzFormat.format(neutralLoss));
        sb.append(", m/z ");
        sb.append(mzFormat.format(mzValue));
        sb.append(", scan #" + scanNumber + ", RT ");
        sb.append(rtFormat.format(retentionTime));
        sb.append(", precursor scan #" + precursorScanNumber);
        sb.append(", m/z ");
        sb.append(mzFormat.format(precursorMZ));
        if (precursorCharge > 0)
            sb.append(" (chrg " + precursorCharge + ")");
        label = sb.toString();

    }

    /**
     * @return Returns the mzValue.
     */
    float getMzValue() {
        return mzValue;
    }

    /**
     * @return Returns the precursorCharge.
     */
    int getPrecursorCharge() {
        return precursorCharge;
    }

    /**
     * @return Returns the precursorMZ.
     */
    float getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @return Returns the precursor mass, or m/z if charge is unknown.
     */
    float getPrecursorMass() {
        return precursorMass;
    }

    /**
     * @return Returns the precursorScanNumber.
     */
    int getPrecursorScanNumber() {
        return precursorScanNumber;
    }

    /**
     * @return Returns the retentionTime.
     */
    float getRetentionTime() {
        return retentionTime;
    }

    /**
     * @return Returns the scanNumber.
     */
    int getScanNumber() {
        return scanNumber;
    }

    float getNeutralLoss() {
        return neutralLoss;
    }

    public String toString() {
        return label;

    }

}
