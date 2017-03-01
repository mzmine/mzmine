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

package net.sf.mzmine.util;

import java.text.Format;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.FragmentIon;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.ModificationPeptide;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.Peptide;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.Protein;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.data.ProteinSection;

public class ProteomeUtils {

    /**
     * Common utility method to be used as Peptide.toString() method
     * 
     * @param Peptide
     *            Peptide to be converted to String
     * @return String representation of the peptide
     */
    public static String peptideToString(Peptide peptide) {
	StringBuffer buf = new StringBuffer();
	Format mzFormat = MZmineCore.getConfiguration().getMZFormat();

	Protein protein = peptide.getProtein();
	ProteinSection section;
	section = protein.getSection(peptide);
	buf.append(protein.getSysname() + ":" + section.getName() + "; "
		+ protein.getHits() + " hits");

	buf.append("\n" + peptide.getSequence());
	buf.append("\nPeptideMz ");
	buf.append(mzFormat.format(peptide.getMass()));
	buf.append(" ;CalculatedMz ");
	buf.append(mzFormat.format(peptide.getMassExpected()));
	buf.append(" ;Score ");
	buf.append(peptide.getIonScore());
	buf.append(" \nCharge ");
	buf.append(peptide.getPrecursorCharge());
	buf.append(" ;FragmentScan ");
	buf.append(peptide.getScan().getScanNumber());

	return buf.toString();
    }

    /**
     * Common utility method to be used as Protein.toString() method
     * 
     * @param Protein
     *            Protein to be converted to String
     * @return String representation of the protein
     */
    public static String proteinToString(Protein protein) {
	StringBuffer buf = new StringBuffer();
	buf.append(protein.getSysname());
	buf.append(" ;Peptides ");
	buf.append(protein.getPeptidesNumber());
	buf.append(" ;Hits ");
	buf.append(protein.getHits());

	return buf.toString();
    }

    /**
     * Common utility method to be used as ModificationPeptide.toString() method
     * 
     * @param ModificationPeptide
     *            Modification to be converted to String
     * @return String representation of the modification
     */
    public static String modificationToString(ModificationPeptide modification) {
	StringBuffer buf = new StringBuffer();
	Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
	if (modification.isFixed())
	    buf.append("Fixed mod. ");
	else
	    buf.append("Variable mod. ");
	buf.append(modification.getName());
	buf.append(" (" + mzFormat.format(modification.getMass()) + ")");

	return buf.toString();
    }

    /**
     * Common utility method to be used as FragmentIon.toString() method
     * 
     * @param FragmentIon
     *            Fragment ion to be converted to String
     * @return String representation of the ion
     */
    public static String fragmentIonToString(FragmentIon fragmentIon) {
	StringBuffer buf = new StringBuffer();
	Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
	buf.append(fragmentIon.getType().getName());
	buf.append(" #");
	buf.append(fragmentIon.getPosition());
	buf.append(" ; ");
	buf.append(mzFormat.format(fragmentIon.getMass()));
	buf.append(" MZ");
	return buf.toString();
    }

}
