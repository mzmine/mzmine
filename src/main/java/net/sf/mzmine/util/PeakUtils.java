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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.text.Format;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;

/**
 * Utilities for peaks and peak lists
 * 
 */
public class PeakUtils {

    /**
     * Common utility method to be used as Peak.toString() method in various
     * Peak implementations
     * 
     * @param peak
     *            Peak to be converted to String
     * @return String representation of the peak
     */
    public static String peakToString(Feature peak) {
	StringBuffer buf = new StringBuffer();
	Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
	Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
	buf.append(mzFormat.format(peak.getMZ()));
	buf.append(" m/z @");
	buf.append(timeFormat.format(peak.getRT()));
	buf.append(" [" + peak.getDataFile().getName() + "]");
	return buf.toString();
    }

    /**
     * Compares identities of two peak list rows. 1) if preferred identities are
     * available, they must be same 2) if no identities are available on both
     * rows, return true 3) otherwise all identities on both rows must be same
     * 
     * @return True if identities match between rows
     * 
     */
    public static boolean compareIdentities(PeakListRow row1, PeakListRow row2) {

	if ((row1 == null) || (row2 == null))
	    return false;

	// If both have preferred identity available, then compare only those
	PeakIdentity row1PreferredIdentity = row1.getPreferredPeakIdentity();
	PeakIdentity row2PreferredIdentity = row2.getPreferredPeakIdentity();
	if ((row1PreferredIdentity != null) && (row2PreferredIdentity != null)) {
	    if (row1PreferredIdentity.getName().equals(
		    row2PreferredIdentity.getName()))
		return true;
	    else
		return false;
	}

	// If no identities at all for both rows, then return true
	PeakIdentity[] row1Identities = row1.getPeakIdentities();
	PeakIdentity[] row2Identities = row2.getPeakIdentities();
	if ((row1Identities.length == 0) && (row2Identities.length == 0))
	    return true;

	// Otherwise compare all against all and require that each identity has
	// a matching identity on the other row
	if (row1Identities.length != row2Identities.length)
	    return false;
	boolean sameID = false;
	for (PeakIdentity row1Identity : row1Identities) {
	    sameID = false;
	    for (PeakIdentity row2Identity : row2Identities) {
		if (row1Identity.getName().equals(row2Identity.getName())) {
		    sameID = true;
		    break;
		}
	    }
	    if (!sameID)
		break;
	}

	return sameID;
    }

    /**
     * Compare charge state of the best MS/MS precursor masses
     * 
     * @param row1
     *            PeaklistRow 1
     * @param row2
     *            PeakListRow 2
     * 
     * @return true, same charge state
     */
    public static boolean compareChargeState(PeakListRow row1, PeakListRow row2) {

	assert ((row1 != null) && (row2 != null));

	int firstCharge = row1.getBestPeak().getCharge();
	int secondCharge = row2.getBestPeak().getCharge();

	return (firstCharge == 0) || (secondCharge == 0)
		|| (firstCharge == secondCharge);

    }

    /**
     * Returns true if peak list row contains a compound identity matching to id
     * 
     */
    public static boolean containsIdentity(PeakListRow row, PeakIdentity id) {

	for (PeakIdentity identity : row.getPeakIdentities()) {
	    if (identity.getName().equals(id.getName()))
		return true;
	}

	return false;
    }

    /**
     * Copies properties such as identification results and comments from the
     * source row to the target row.
     */
    public static void copyPeakListRowProperties(PeakListRow source,
	    PeakListRow target) {

	// Combine the comments
	String targetComment = target.getComment();
	if ((targetComment == null) || (targetComment.trim().length() == 0)) {
	    targetComment = source.getComment();
	} else {
	    if ((source.getComment() != null)
		    && (source.getComment().trim().length() > 0))
		targetComment += "; " + source.getComment();
	}
	target.setComment(targetComment);

	// Copy all peak identities, if these are not already present
	for (PeakIdentity identity : source.getPeakIdentities()) {
	    if (!containsIdentity(target, identity))
		target.addPeakIdentity(identity, false);
	}

	// Set the preferred identity
	target.setPreferredPeakIdentity(source.getPreferredPeakIdentity());

    }

    /**
     * Copies properties such as isotope pattern and charge from the source peak
     * to the target peak
     */
    public static void copyPeakProperties(Feature source, Feature target) {

	// Copy isotope pattern
	IsotopePattern originalPattern = source.getIsotopePattern();
	if (originalPattern != null)
	    target.setIsotopePattern(originalPattern);

	// Copy charge
	int charge = source.getCharge();
	target.setCharge(charge);

    }

    /**
     * Finds a combined m/z range that covers all given peaks
     */
    public static Range<Double> findMZRange(Feature peaks[]) {

	Range<Double> mzRange = null;

	for (Feature p : peaks) {
	    if (mzRange == null) {
		mzRange = p.getRawDataPointsMZRange();
	    } else {
		mzRange = mzRange.span(p.getRawDataPointsMZRange());
	    }
	}

	return mzRange;

    }

}
