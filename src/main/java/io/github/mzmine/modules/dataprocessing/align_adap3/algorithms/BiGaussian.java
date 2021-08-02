	/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */
package io.github.mzmine.modules.dataprocessing.align_adap3.algorithms;


import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.SliceSparseMatrix.Triplet;
import java.lang.Math;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * <p>
 * BiGaussian Class is used for fitting BiGaussian on EIC. BiGaussian is composed of 2 halves of
 * Gaussian with different standard deviations. It depends on 4 parameters (height, mu, sigmaLeft,
 * sigmaRight) and computed by the formula
 * </p>
 * <p>
 * <p>
 * f(x) = height * exp(-(x-mu)^2 / (2 * sigmaRight^2)) if x > mu
 * </p>
 * <p>
 * f(x) = height * exp(-(x-mu)^2 / (2 * sigmaLeft^2)) if x < mu
 * </p>
 */
public class BiGaussian {


    enum Direction {
        RIGHT, LEFT
    }


    // This is used for storing BiGaussian parameters inside constructor.
    public final double maxHeight;
    public final int mu;
    public final double sigmaLeft;
    public final double sigmaRight;

    /**
     * <p>
     * Inside BiGaussian Constructor we're determining 4 BiGaussian ADAP3DFeatureDetectionParameters. MaxHeight, Mu,
     * SigmaLeft and SigmaRight.
     * </p>
     *
     * @param horizontalSlice a {@link List} object. This
     *                        is horizontal slice from the sparse matrix.
     * @param roundedmz       a {@link Double} object. It's rounded m/z value. Original m/z value
     *                        multiplied by 10000.
     * @param leftBound       a {@link Integer} object. This is minimum scan number.
     * @param rightBound      a {@link Integer} object. This is maximum scan number.
     */
    BiGaussian(List<SliceSparseMatrix.Triplet> horizontalSlice, int roundedmz, int leftBound, int rightBound) {

        // This is max height for BiGaussian fit. It's in terms of intensities.
        maxHeight = horizontalSlice.stream().map(x -> x != null ? x.intensity : 0.0)
                .max(Double::compareTo).orElse(0.0);

        // Below logic is for finding BiGaussian parameters.
        mu = getScanNumber(horizontalSlice, maxHeight);
        double halfHeight = (double) maxHeight / 2;


        double interpolationLeftSideX = InterpolationX(horizontalSlice, mu, halfHeight, leftBound,
                rightBound, roundedmz, Direction.LEFT);
        // This is sigma left for BiGaussian.
        sigmaLeft = (mu - interpolationLeftSideX) / Math.sqrt(2 * Math.log(2));


        double interpolationRightSideX = InterpolationX(horizontalSlice, mu, halfHeight, leftBound,
                rightBound, roundedmz, Direction.RIGHT);
        // This is sigma right for BiGaussian.
        sigmaRight = ((interpolationRightSideX - mu)) / Math.sqrt(2 * Math.log(2));
    }


    /**
     * <p>
     * InterpolationX is used to calculate X value of Halfwidth-halfheight point of either left or
     * right half of BiGaussian.
     * </p>
     *
     * @param mu         a {@link Integer} object. This is X value for maximum height in terms of
     *                   scan number.
     * @param halfHeight a {@link Double} object. This is m/z value from the raw file.
     * @param leftBound  a {@link Integer} object. This is minimum scan number.
     * @param rightBound a {@link Integer} object. This is maximum scan number.
     * @param roundedmz  a {@link Integer} object. This is rounded m/z value.
     * @param direction  a {@link Enum} object. This decides for which half we want to calculate X
     *                   value.
     */
    private double InterpolationX(List<SliceSparseMatrix.Triplet> horizontalSlice, int mu, double halfHeight,
                                  int leftBound, int rightBound, int roundedmz, Direction direction) {

        int step = direction == Direction.RIGHT ? 1 : -1;

        Comparator<SliceSparseMatrix.Triplet> compareMzScan = (t1, t2) -> (t1.mz != t2.mz)
                ? Integer.compare(t1.mz, t2.mz)
                : Integer.compare(t1.scanListIndex, t2.scanListIndex);

        //Sort horizontalSlice according to mz values and SLI.
        Collections.sort(horizontalSlice, compareMzScan);

        // Find triplet next to the apex
        SliceSparseMatrix.Triplet searchTriplet1 = new SliceSparseMatrix.Triplet();
        searchTriplet1.mz = roundedmz;
        searchTriplet1.scanListIndex = mu + step;
        int index1 = Collections.binarySearch(horizontalSlice, searchTriplet1, compareMzScan);

        if (index1 < 0) {
        	throw new IllegalArgumentException("Cannot find peak apex.");
        }

        double Y1 = Double.NaN;
        double Y2 = Double.NaN;
        for ( ; index1 >= 0 && index1 < horizontalSlice.size() ; index1 += step) {
        	
	        SliceSparseMatrix.Triplet triplet1 = horizontalSlice.get(index1);
            if (triplet1.mz != roundedmz
                    || triplet1.scanListIndex < leftBound
                    || triplet1.scanListIndex > rightBound) {
                break;
            }
            if (triplet1.intensity != 0 && triplet1.intensity < halfHeight) {
                SliceSparseMatrix.Triplet triplet2 = horizontalSlice.get(index1 - step);
                if (triplet2.mz != roundedmz
                        || triplet1.scanListIndex < leftBound
                        || triplet1.scanListIndex > rightBound) {
                    //triplet2.mz is not equal to roundedmz. The set of mz values, equal to roundedmz is over.
                    break;
                }
                Y1 = triplet1.intensity;
                if (triplet2.intensity != 0) {
                    Y2 = triplet2.intensity;
                    break;
                }
            }
        }

        if (Double.isNaN(Y1) || Double.isNaN(Y2))
            throw new IllegalArgumentException("Cannot find BiGaussian.");
        /*
         * I've used the formula of line passing through points (x1,y1) and (x2,y2) in interpolationX.
         * Those are the points which are exactly above and below of halfHeight(halfMaxIntensity). I've
         * to find exact point between those two points. I've y-value for that point as halfHeight but I
         * don't have x-value. X-value is scan number and Y-value is intensity.
         */

        return ((halfHeight - Y2)
                * (horizontalSlice.get(index1 - step).scanListIndex - horizontalSlice.get(index1).scanListIndex))
                / (Y2 - Y1)
                + horizontalSlice.get(index1 - step).scanListIndex;
    }

    /**
     * <p>
     * This method is used for getting scan number for given intensity value.
     * </p>
     *
     * @param height a {@link Double} object. This is intensity value from the horizontal
     *               slice from sparse matrix.
     */
    private int getScanNumber(List<SliceSparseMatrix.Triplet> horizontalSlice, double height) {
        int mu = 0;
        Iterator<SliceSparseMatrix.Triplet> iterator = horizontalSlice.iterator();

        while (iterator.hasNext()) {
            Triplet triplet = iterator.next();
            if (triplet.intensity == height) {
                mu = triplet.scanListIndex;
                break;
            }
        }
        return mu;
    }

    /**
     * <p>
     * This method is used calculating BiGaussian values for EIC.
     * </p>
     *
     * @param x a {@link Integer} object. This is scan number.
     * @return a double.
     */
    public double getValue(int x) {

        double sigma = x >= mu ? sigmaRight : sigmaLeft;
        double exponentialTerm = Math.exp(-1 * Math.pow(x - mu, 2) / (2 * Math.pow(sigma, 2)));
        return maxHeight * exponentialTerm;

    }
}
