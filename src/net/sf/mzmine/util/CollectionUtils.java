/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.util.Collection;
import java.util.Iterator;


/**
 * Collection API related utilities
 */
public class CollectionUtils {
    
    
    /**
     * Returns an array of ints consisting of the elements of the specified collection.
     * @param collection Collection of Integers
     * @return Array of ints
     */
    public static int[] toIntArray(Collection<Integer> collection) {
        int array[] = new int[collection.size()];
        int index = 0;
        Iterator<Integer> it = collection.iterator();
        while (it.hasNext()) {
            array[index++] = it.next();
        }
        return array;
    }
    
    /**
     * Converts an array of ints to an array of Integers
     * @param array Array of ints
     * @return Array of Integers
     */
    public static Integer[] toIntegerArray(int array[]) {
        Integer newArray[] = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
            newArray[i] = new Integer(array[i]);
        return newArray;
    }
    
    /**
     * Returns an array of doubles consisting of the elements of the specified collection.
     * @param collection Collection of Doubles
     * @return Array of doubles
     */
    public static double[] toDoubleArray(Collection<Double> collection) {
        double array[] = new double[collection.size()];
        int index = 0;
        Iterator<Double> it = collection.iterator();
        while (it.hasNext()) {
            array[index++] = it.next();
        }
        return array;
    }    
    

    /**
     * Checks if the haystack array contains all elements of needles array
     * @param haystack array of ints
     * @param needles array of ints
     * @return true if haystack contains all elements of needles
     */
    public static boolean isSubset(int haystack[], int needles[]) {
        needleTraversal: for (int i = 0; i < needles.length; i++) {
            for (int j = 0; j < haystack.length; j++) {
                if (needles[i] == haystack[j]) continue needleTraversal;
            }
            return false;
        }
        return true;
    }

}
