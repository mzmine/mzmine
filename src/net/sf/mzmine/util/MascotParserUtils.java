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


package net.sf.mzmine.util;

import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.mzmine.data.proteomics.SerieIonType;

public class MascotParserUtils {
	

	/* 
	 * FRAGMENT ION/RULE KEYS: 
	 * 1 singly charged 
	 * 2 doubly charged if precursor 2+ or higher
	 * 		(not internal or immonium) 
	 * 3 doubly charged if precursor 3+ or higher
	 * 		(not internal or immonium) 
	 * 4 immonium
	 * 5   a series
	 * 6   a - NH3 if a significant and fragment includes RKNQ
	 * 7   a - H2O if a significant and fragment includes STED
	 * 8   b series
	 * 9   b - NH3 if b significant and fragment includes RKNQ
	 * 10  b - H2O if b significant and fragment includes STED
	 * 11  c series
	 * 12  x series
	 * 13  y series
	 * 14  y - NH3 if y significant and fragment includes RKNQ
	 * 15  y - H2O if y significant and fragment includes STED
	 * 16  z series
	 * 17  internal yb < 700 Da
	 * 18  internal ya < 700 Da
	 * 19  y or y++ must be significant
	 * 20  y or y++ must be highest scoring series
	 * 21  z+1 series
	 * 22  d and d' series
	 * 23  v series
	 * 24  w and w' series
	 * 25  z+2 series
	 * 
	 * INSTRUMENT RULES:
	 * Default = 1,2,8,9,10,13,14,15 
	 * ESI-QUAD-TOF = 1,2,8,9,10,13,14,15
	 * MALDI-TOF-PSD = 1,4,5,6,7,8,9,10,13
	 * ESI-TRAP = 1,2,8,9,10,13,14,15
	 * ESI-QUAD = 1,2,8,9,10,13,14,15
	 * ESI-FTICR =  1,2,8,9,10,13,14,15
	 * MALDI-TOF-TOF = 1,4,5,6,7,8,9,10,13,14,15,17,18,22,23,24
	 * ESI-4SECTOR = 1,2,4,5,8,9,10,13,16,17,18
	 * FTMS-ECD = 1,2,11,13,21,25
	 * ETD-TRAP = 1,2,11,13,21,25
	 * MALDI-QUAD-TOF = 1,2,4,89,10,13,14,15,17,18
	 * MALDI-QIT-TOF =  1,4,5,6,78,9,10,13,14,15,17,18
	 *  
    */
    public static SerieIonType[] parseFragmentationRules(String sRules) {
        Vector<SerieIonType> ionSeries = new Vector<SerieIonType>();
        StringTokenizer st = new StringTokenizer(sRules,",");
        int ionRule = 0;
        while (st.hasMoreTokens()) {
        	ionRule = Integer.parseInt(st.nextToken());
            switch (ionRule) {
                case 5: // a-ion
                	ionSeries.add(SerieIonType.A_SERIES);
                    break;

                case 8: // b-ion
                	ionSeries.add(SerieIonType.B_SERIES);
                    break;

                case 11: // c-ion
                	ionSeries.add(SerieIonType.C_SERIES);
                    break;

                case 12: // x-ion
                	ionSeries.add(SerieIonType.X_SERIES);
                    break;

                case 13: // y-ion
                	ionSeries.add(SerieIonType.Y_SERIES);
                    break;

                case 16: // z-ion
                	ionSeries.add(SerieIonType.Z_SERIES);
                    break;

                case 21: // zH-ion
                	ionSeries.add(SerieIonType.ZH_SERIES);
                    break;

                case 25: // zHH-ion
                	ionSeries.add(SerieIonType.ZHH_SERIES);
                    break;

                default:
                    break;
            }
        }

        return ionSeries.toArray(new SerieIonType[0]);
    }


}
