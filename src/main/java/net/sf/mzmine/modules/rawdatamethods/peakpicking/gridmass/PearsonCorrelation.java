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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

class PearsonCorrelation {

    private int count; // Number of numbers that have been entered.
    private double sumX = 0;
    private double sumY = 0;
    private double sumXX = 0;
    private double sumYY = 0;
    private double sumXY = 0;

    void enter(double x, double y) {
	// Add the number to the dataset.
	count++;
	sumX += x;
	sumY += y;
	sumXX += x * x;
	sumYY += y * y;
	sumXY += x * y;
    }

    int getCount() {
	// Return number of items that have been entered.
	return count;
    }

    double meanX() {
	return sumX / count;
    }

    double meanY() {
	return sumY / count;
    }

    double stdevX() {
	if (count < 2)
	    return meanX();
	return Math.sqrt((sumXX - sumX * sumX / count) / (count - 1));
    }

    double stdevY() {
	if (count < 2)
	    return meanY();
	return Math.sqrt((sumYY - sumY * sumY / count) / (count - 1));
    }

    double correlation() {
	double numerator = count * sumXY - sumX * sumY;
	int n = (count > 50 ? count - 1 : count);
	double denominator = Math.sqrt(n * sumXX - sumX * sumX)
		* Math.sqrt(n * sumYY - sumY * sumY);
	double c = (count < 3 ? 0 : numerator / denominator);
	return c;
    }
}