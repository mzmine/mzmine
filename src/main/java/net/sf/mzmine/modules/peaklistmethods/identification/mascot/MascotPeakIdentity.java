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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class MascotPeakIdentity extends SimplePeakIdentity {

    /*
     * private static final String PROPERTY_PEPTIDE = "Peptide sequence";
     * private static final String PROPERTY_MASS = "Mass (Mr)"; private static
     * final String PROPERTY_DELTA = "Delta"; private static final String
     * PROPERTY_SCORE = "Score"; private static final String PROPERTY_MISSES =
     * "Misses"; private static final String PROPERTY_MODIFICATIONS =
     * "Modifications";
     * 
     * /** This class implements PeakIdentity and wrap the information of the
     * peptide assigned to the chromatographic peak.
     * 
     * @param peptide the peptide hit.
     * 
     * @SuppressWarnings("unchecked") public MascotPeakIdentity(final PeptideHit
     * peptide) {
     * 
     * 
     * final StringBuilder name = new StringBuilder(); for (final ProteinHit p :
     * (Iterable<ProteinHit>) peptide.getProteinHits()) { if (name.length() > 0)
     * { name.append(' '); } name.append(p.getAccession()); }
     * 
     * setPropertyValue(PROPERTY_NAME, name.toString());
     * 
     * final HashMap<Integer, String> varMods = new HashMap<Integer, String>();
     * for (final Modification mod : peptide.getModifications()) { if (mod !=
     * null) { varMods.put(mod.getModificationID(), mod.getType() + " (" +
     * mod.getLocation() + ')'); } }
     * 
     * final int[] modSequences = peptide.getVariableModificationsArray();
     * String modSeqString = ""; for (int i = 0; i < modSequences.length; i++) {
     * if (varMods.containsKey(modSequences[i])) { modSeqString +=
     * varMods.get(modSequences[i]) + " [" + i + "], "; } }
     * 
     * setPropertyValue(PROPERTY_METHOD, "MASCOT search");
     * setPropertyValue(PROPERTY_PEPTIDE, peptide.getSequence());
     * setPropertyValue(PROPERTY_FORMULA, peptide.getSequence());
     * setPropertyValue(PROPERTY_MASS, String.valueOf(peptide.getPeptideMr()));
     * setPropertyValue(PROPERTY_DELTA, String.valueOf(peptide.getDeltaMass()));
     * setPropertyValue(PROPERTY_SCORE, String.valueOf(peptide.getIonsScore()));
     * setPropertyValue(PROPERTY_MISSES,
     * String.valueOf(peptide.getMissedCleavages()));
     * setPropertyValue(PROPERTY_MODIFICATIONS, modSeqString); }
     */

}
