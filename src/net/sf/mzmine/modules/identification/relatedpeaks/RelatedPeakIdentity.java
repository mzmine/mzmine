/*
 * Copyright 2006-2008 The MZmine Development Team
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
package net.sf.mzmine.modules.identification.relatedpeaks;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;

public class RelatedPeakIdentity implements PeakIdentity, Comparable {

    private String compoundID,  compoundName,  compoundFormula;
    private String identificationMethod;
    private PeakListRow originalPeakListRow;
    private PeakListRow relatedPeakListRow;
    private CommonAdducts adduct;

    /**
     * @param compoundID
     * @param compoundName  
     * @param compoundFormula    
     * @param identificationMethod 
     */
    public RelatedPeakIdentity(String compoundID, String compoundName,
            String compoundFormula, String identificationMethod,
            PeakListRow originalPeakListRow, PeakListRow relatedPeakListRow,
            CommonAdducts adduct) {
        this.compoundID = compoundID;
        this.compoundName = compoundName;
        this.compoundFormula = compoundFormula;
        if (compoundName == null) {
            this.compoundName = compoundFormula;
        } else if (compoundName.equals("")) {
            this.compoundName = compoundFormula;
        }
        this.identificationMethod = identificationMethod;
        this.originalPeakListRow = originalPeakListRow;
        this.relatedPeakListRow = relatedPeakListRow;
        this.adduct = adduct;
    }

    /**
     * @return Returns the identificationMethod
     */
    public String getIdentificationMethod() {
        return this.identificationMethod;
    }

    /**
     * @return Returns the ID
     */
    public String getID() {
        return this.compoundID;
    }

    /**
     * @return Returns the Name
     */
    public String getName() {
        return this.compoundName;
    }

    /**
     * @return Returns the compoundFormula.
     */
    public String getCompoundFormula() {
        return this.compoundFormula;
    }

    /**
     * @return Returns the originalPeakListRow
     */
    public PeakListRow getOriginalPeakListRow() {
        return this.originalPeakListRow;
    }

    /**
     * @return Returns the relatedPeakListRow
     */
    public PeakListRow getRelatedPeakListRow() {
        return this.relatedPeakListRow;
    }

    /**
     * @return Returns the type of adduct
     */
    public String getTypeOfAdduct() {
        return adduct.getName();
    }

    /**
     * @return Returns the mass difference
     */
    public double getMassDifference() {
        return adduct.getMassDifference();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String ret;
        ret = compoundName + " (" + adduct.getMassDifference() + ") identification method: " + identificationMethod;
        return ret;
    }

    public int compareTo(Object value) {
        if (value == UNKNOWN_IDENTITY) {
            return 1;
        }

        PeakIdentity identityValue = (PeakIdentity) value;
        String valueName = identityValue.getName();
        if (valueName == null) {
            return 1;
        }
        return valueName.compareTo(compoundName);
    }
}
