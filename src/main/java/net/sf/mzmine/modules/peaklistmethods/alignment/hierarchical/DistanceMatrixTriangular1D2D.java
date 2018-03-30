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

import org.gnf.clustering.DistanceMatrix;

public class DistanceMatrixTriangular1D2D implements DistanceMatrix {

    private int dimension;

    private final LargeArrayFloat list;

    public DistanceMatrixTriangular1D2D(int nRowCount) {

        list = new LargeArrayFloat(sumFormula(nRowCount));
        dimension = nRowCount;
    }

    public DistanceMatrixTriangular1D2D(DistanceMatrix distanceMatrix2) {

        this.dimension = distanceMatrix2.getRowCount();
        this.list = new LargeArrayFloat(sumFormula(this.dimension));

        for (int i = 0; i < this.dimension; ++i) {
            for (int j = i; j < this.dimension; ++j) {
                this.setValue(i, j, distanceMatrix2.getValue(i, j));
            }
        }
    }

    static public long getListIndex(int row, int column) { // Symmetrical

        if (row > column)
            return sumFormula(row) + (long) column;
        else
            return sumFormula(column) + (long) row;
    }

    static public long sumFormula(long i) {
        return (i * i + i) / 2;
    }

    @Override
    public int getRowCount() {
        return dimension;
    }

    @Override
    public int getColCount() {
        return dimension;
    }

    @Override
    public float getValue(int nRow, int nCol) {

        return list.get(getListIndex(nRow, nCol));
    }

    @Override
    public void setValue(int nRow, int nCol, float fVal) {

        list.set(getListIndex(nRow, nCol), fVal);
    }

    // ---------------------------------------

    public void printVector() {
        // System.out.println(Arrays.toString(this.getVector()));
        System.out.println(list.toString());
    }

    // -
    public void print() {

        for (int i = 0; i < this.dimension; i++) {

            System.out.println("\n");

            for (int j = 0; j < this.dimension; j++) {

                System.out.println(" " + this.getValue(i, j));
            }

        }
    }

    // -
    public double[][] toTwoDimArray() {

        double[][] arr = new double[this.dimension][this.dimension];

        for (int i = 0; i < this.dimension; i++) {

            for (int j = 0; j < this.dimension; j++) {

                arr[i][j] = this.getValue(i, j);
            }
        }

        return arr;
    }

}
