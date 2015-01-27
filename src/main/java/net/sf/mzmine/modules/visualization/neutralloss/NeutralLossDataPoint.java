/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.neutralloss;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
class NeutralLossDataPoint {

    private double mzValue;
    private int scanNumber;
    private double precursorMZ;
    private int precursorCharge;
    private double retentionTime;
    private double neutralLoss;
    private double precursorMass;
    private String label;
    private static int defaultPrecursorCharge = 2;

    /**
     * @param scanNumber
     * @param precursorScanNumber
     * @param precursorMZ
     * @param precursorCharge
     * @param retentionTime
     */
    NeutralLossDataPoint(double mzValue, int scanNumber,
	    double precursorMZ, int precursorCharge,
	    double retentionTime) {

	NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

	this.mzValue = mzValue;
	this.scanNumber = scanNumber;
	this.precursorMZ = precursorMZ;
	this.precursorCharge = precursorCharge;
	this.retentionTime = retentionTime;

	precursorMass = precursorMZ;
	if (precursorCharge > 0)
	    precursorMass *= precursorCharge;

	if ((precursorCharge == 0) && (precursorMass < mzValue))
	    precursorMass *= defaultPrecursorCharge;

	neutralLoss = precursorMass - mzValue;

	StringBuffer sb = new StringBuffer();
	sb.append("loss: ");
	sb.append(mzFormat.format(neutralLoss));
	sb.append(", m/z ");
	sb.append(mzFormat.format(mzValue));
	sb.append(", scan #" + scanNumber + ", RT ");
	sb.append(rtFormat.format(retentionTime));
	sb.append(", m/z ");
	sb.append(mzFormat.format(precursorMZ));
	if (precursorCharge > 0)
	    sb.append(" (charge " + precursorCharge + ")");
	label = sb.toString();

    }

    /**
     * @return Returns the mzValue.
     */
    double getMzValue() {
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
    double getPrecursorMZ() {
	return precursorMZ;
    }

    /**
     * @return Returns the precursor mass, or m/z if charge is unknown.
     */
    double getPrecursorMass() {
	return precursorMass;
    }

    /**
     * @return Returns the retentionTime.
     */
    double getRetentionTime() {
	return retentionTime;
    }

    /**
     * @return Returns the scanNumber.
     */
    int getScanNumber() {
	return scanNumber;
    }

    double getNeutralLoss() {
	return neutralLoss;
    }

    public String getName() {
	return label;

    }

}
