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
    

}
