/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel;

import javax.annotation.Nullable;


/**
 * Represent one MS/MS spectrum in a raw data file.
 */
public interface MsMsScan extends MsScan {

    /**
     * 
     * @return parent scan or null if there is no parent scan
     */
    @Nullable MsScan getParentScan();

    void setParentScan(@Nullable MsScan parentScan);

    /**
     * 
     * @return Precursor m/z
     */
    double getPrecursorMz();

    double getActivationEnergy();

    void setPrecursorMZ(double precursorMZ);

    void setActivationEnergy(double activationEnergy);

    /**
     * 
     * @return Precursor charge or 0 if charge is unknown
     */
    int getPrecursorCharge();

    void setPrecursorCharge(int charge);

}
