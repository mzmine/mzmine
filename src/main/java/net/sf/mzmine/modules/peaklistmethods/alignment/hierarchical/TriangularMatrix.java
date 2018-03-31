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

package net.sf.mzmine.modules.peaklistmethods.alignment.hierarchical;

import java.util.logging.Logger;

public abstract class TriangularMatrix {
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int dimension;

    public abstract double set(int row, int column, double value);

    public abstract double get(int row, int column);

    // public abstract int getSize();
    public int getDimension() {
        return this.dimension;
    }

    protected void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public void validateArguments(int row, int column) {
        if (row > column) {
            throw new IllegalArgumentException("Row (" + row
                    + " given) has to be smaller or equal than column ("
                    + column + " given)!");
        }
    }

    public long getListIndex(int row, int column) { // Symmetrical

        if (row > column)
            return sumFormula(row) + (long) column;
        else
            return sumFormula(column) + (long) row;
    }

    public long sumFormula(long i) {
        return (i * i + i) / 2;
    }

    public void print() {

        for (int i = 0; i < getDimension(); i++) {

            logger.info("\n");

            for (int j = 0; j < getDimension(); j++) {

                logger.info(" " + this.get(i, j));
            }

        }
    }

    public abstract void printVector();

    public double[][] toTwoDimArray() {

        double[][] arr = new double[this.dimension][this.dimension];

        for (int i = 0; i < getDimension(); i++) {

            for (int j = 0; j < getDimension(); j++) {

                arr[i][j] = this.get(i, j);
            }
        }

        return arr;
    }

}
