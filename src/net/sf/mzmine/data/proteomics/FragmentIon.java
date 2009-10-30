/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.data.proteomics;

import net.sf.mzmine.util.ProteomeUtils;

public class FragmentIon {
	
	private double mass;
	private FragmentIonType ionType;
	private int position;
	private double intensity = 0;
	
	public FragmentIon(double mass, FragmentIonType fragmentIonType, int position){
		this.mass = mass;
		this.ionType = fragmentIonType;
		this.position = position;
	}
	
	public double getMass(){
		return mass;
	}

    public double getIntensity(){
    	return intensity;
    }

    public void setIntensity(double intensity){
    	this.intensity = intensity;
    }

    public FragmentIonType getType(){
    	return ionType;
    }

    public int getPosition(){
    	return position;
    }
    
    public String toString(){
    	return ProteomeUtils.fragmentIonToString(this); 
    }

    //Include in parser or peptide verification.
    /**
     * This method checks if the mass of this fragmention was found in the original spectrum with a given MassError. This method does not take any intensity parameters in count!<br>
     * If the mass was matched, <b>the instance boolean iMatch will be set to true</b>, if there is no match, the boolean will stay false.
     * This boolean will be returned in the end of this method!
     *
     * @param aPeaks     Peak[] containing all the peak masses from the mass spectrum that was used by the query that delivered this peptidehit.
     * @param aMassError This is the mass error to check if this theoretical fragment ion was found in the spectrum.
     * @return boolean      This boolean says if this theoretical FragmentIon wass found with a mass error iMassError in the spectrum.
     */
    //public boolean isMatch(Peak[] aPeaks, double aMassError);

    /**
     * This method checks if the mass of this fragmention was found in the original spectrum with a given MassError.<br>
     * The Peak that is matched must have an intensity above a threshold that is based on the highest intensity of the spectrum.<br>
     * If the mass was matched, <b>the instance boolean iMatch will be set to true</b>, if there is no match, the boolean will stay false.
     * This boolean will be returned in the end of this method!
     *
     * @param aPeaks               Peak[] containing all the peak masses of the Query wherefrom this PeptideHit was created.
     * @param aMaxIntensity        double with the max intensity of the spectrum.
     * @param aIntensityPercentage This double is a percent (ex: 0.10) , The relative intensityThreshold will then be (aMaxIntensity*aIntensityPercentage),
     *                             only matches that are above this threshold will be added to the Vector.
     * @param aMassError           This is the mass error to check if this theoretical fragment ion was found in the spectrum.
     * @return boolean      This boolean says if this theoretical FragmentIon wass found with a mass error iMassError in the spectrum.
     */
    //public boolean isMatchAboveIntensityThreshold(Peak[] aPeaks, double aMaxIntensity, double aIntensityPercentage, double aMassError);

    /**
     * Returns this double holds the mass error between the theoretical fragmention and the matched peakmass.
     * <b>The fragmention must be a match in the mass spectrum before this method can be used!</b>
     *
     * @return this double holds the mass error between the theoretical fragmention and the matched peakmass.
     */
    //public double getTheoreticalExperimentalMassError();

    //boolean hasBeenMatched();
}
