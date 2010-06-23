/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import java.util.HashMap;
import java.util.Iterator;

import net.sf.mzmine.data.impl.SimplePeakIdentity;
import be.proteomics.mascotdatfile.util.interfaces.Modification;
import be.proteomics.mascotdatfile.util.mascot.PeptideHit;
import be.proteomics.mascotdatfile.util.mascot.ProteinHit;

public class MascotPeakIdentity extends SimplePeakIdentity {

	public static final String PROPERTY_PEPTIDE = "Peptide sequence";
	public static final String PROPERTY_MASS = "Mass (Mr)";
	public static final String PROPERTY_DELTA = "Delta";
	public static final String PROPERTY_SCORE = "Score";
	public static final String PROPERTY_MISSES = "Misses";
	public static final String PROPERTY_MODIFICATIONS = "Modifications";

	private PeptideHit peptide;
	
	public MascotPeakIdentity() {
		super();
	}

	/**
	 * This class implements PeakIdentity and wrap the information of the
	 * peptide assigned to the chromatographic peak.
	 *
	 * @param peptide
	 */
	public MascotPeakIdentity(PeptideHit peptide) {

		this.peptide = peptide;

		StringBuffer name = new StringBuffer();
		Iterator<ProteinHit> it = peptide.getProteinHits().iterator();
		while (it.hasNext()) {
			ProteinHit p = it.next();
			if (name.length() > 0)
				name.append(" ");
			name.append(p.getAccession());
		}

		setPropertyValue(PROPERTY_NAME, name.toString());

		Modification[] mods = peptide.getModifications();
		HashMap<Integer, String> varMods = new HashMap<Integer, String>();
		for (int i = 0; i < mods.length; i++) {
			if (mods[i] != null) {
				String modString = mods[i].getType() + " (" + mods[i].getLocation()
					+ ")";
				varMods.put(mods[i].getModificationID(), modString);
			}
		}

		int[] modSequences = peptide.getVariableModificationsArray();
		String modSeqString = "";
		for (int i = 0; i < modSequences.length; i++) {
			if (varMods.containsKey(modSequences[i])) {
				modSeqString += varMods.get(modSequences[i]) + " [" + i + "], ";
			}
		}

		setPropertyValue(PROPERTY_METHOD, "MASCOT search");
		setPropertyValue(PROPERTY_PEPTIDE, peptide.getSequence());
		setPropertyValue(PROPERTY_FORMULA, peptide.getSequence());
		setPropertyValue(PROPERTY_MASS, String.valueOf(peptide.getPeptideMr()));
		setPropertyValue(PROPERTY_DELTA, String.valueOf(peptide.getDeltaMass()));
		setPropertyValue(PROPERTY_SCORE, String.valueOf(peptide.getIonsScore()));
		setPropertyValue(PROPERTY_MISSES, String.valueOf(peptide
				.getMissedCleavages()));
		setPropertyValue(PROPERTY_MODIFICATIONS, modSeqString);

	}

	public PeptideHit getPeptide() {
		return peptide;
	}

}
