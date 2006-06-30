/**
 * 
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
    public static int[] toArray(Collection<Integer> collection) {
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
    

}
