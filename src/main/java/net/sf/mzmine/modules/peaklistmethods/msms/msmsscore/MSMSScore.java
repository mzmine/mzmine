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

package net.sf.mzmine.modules.peaklistmethods.msms.msmsscore;

import java.util.Map;

import net.sf.mzmine.datamodel.DataPoint;

/**
 * 
 * Wrapper class for a score of MS/MS evaluation, with a mapping from MS/MS data
 * points to interpreted formulas
 * 
 */
public class MSMSScore {

    private double score;
    private Map<DataPoint, String> annotation;

    public MSMSScore(double score, Map<DataPoint, String> annotation) {
	this.score = score;
	this.annotation = annotation;
    }

    public double getScore() {
	return score;
    }

    public Map<DataPoint, String> getAnnotation() {
	return annotation;
    }

}
