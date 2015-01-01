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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot.data;

import java.util.HashMap;

public class PeptideIonSerie {

    private HashMap<SerieIonType, IonSignificance> fragmentSeries;

    /**
     * This class represents the ion series for one peptide, containing type of
     * series and significance for score calculation
     */
    public PeptideIonSerie() {
	this.fragmentSeries = new HashMap<SerieIonType, IonSignificance>();
    }

    /**
     * This class represents the ion series for one peptide, containing type of
     * series and significance for score calculation
     * 
     * @param serie
     * @param significance
     */
    public PeptideIonSerie(SerieIonType serie, IonSignificance significance) {
	this.fragmentSeries = new HashMap<SerieIonType, IonSignificance>();
	fragmentSeries.put(serie, significance);
    }

    /**
     * Add an ion serie to this peptideIonSerie
     * 
     * @param serie
     * @param significance
     */
    public void addSerie(SerieIonType serie, IonSignificance significance) {
	if (!fragmentSeries.containsKey(serie))
	    fragmentSeries.put(serie, significance);
    }

    /**
     * Returns all the fragment series and their significance
     * 
     * @return fragmentSeries
     */
    public HashMap<SerieIonType, IonSignificance> getFragmentSeries() {
	return fragmentSeries;
    }

}
