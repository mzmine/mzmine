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

import java.util.Iterator;

import net.sf.mzmine.data.PeakIdentity;
import be.proteomics.mascotdatfile.util.mascot.PeptideHit;
import be.proteomics.mascotdatfile.util.mascot.ProteinHit;

public class MascotPeakIdentity implements PeakIdentity {
	
	private PeptideHit peptide;
	private String pepName;
	
	/**
	 * This class implements PeakIdentity and wrap the information of the peptide assigned to the chromatographic peak.
	 * 
	 * @param peptide
	 */
	public MascotPeakIdentity(PeptideHit peptide){
		this.peptide = peptide;
		StringBuffer name = new StringBuffer();
		for (Iterator i = peptide.getProteinHits().iterator(); i.hasNext();){
			ProteinHit p = (ProteinHit) i.next();
			name.append(p.getAccession()+" ");
		}
		this.pepName = name.toString();
	}

	public String getDescription() {
		return peptide.getModifiedSequence();
	}

	public String getIdentificationMethod() {
		return "Mascot";
	}

	public String getName() {
		return pepName;
	}
	
	public PeptideHit getPeptide(){
		return peptide;
	}
	
	public String toString(){
		return this.getName();
	}

}
