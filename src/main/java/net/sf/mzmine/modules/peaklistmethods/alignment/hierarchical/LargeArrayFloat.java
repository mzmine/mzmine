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

public class LargeArrayFloat {

    private final long CHUNK_SIZE = 1024 * 1024 * 1024; // 1GiB

    long size;
    float[][] data;

    public LargeArrayFloat(long size) {

        this.size = size;
        if (size == 0) {
            data = null;
        } else {
            int chunks = (int) (size / CHUNK_SIZE);
            int remainder = (int) (size - ((long) chunks) * CHUNK_SIZE);

            System.out.println(this.getClass().getSimpleName()
                    + " > Created with " + chunks + " chunks (size: "
                    + CHUNK_SIZE + " each) + a remainder of " + remainder
                    + " => TOTAL: " + size);

            data = new float[chunks + (remainder == 0 ? 0 : 1)][];
            for (int idx = chunks; --idx >= 0;) {
                data[idx] = new float[(int) CHUNK_SIZE];
            }
            if (remainder != 0) {
                data[chunks] = new float[remainder];
            }

            // System.out.println(this.getClass().getSimpleName()
            // + " > Created with " + chunks + " chunks (size: " + CHUNK_SIZE +
            // " each) + a remainder of " + remainder
            // + " => TOTAL: " + size);
        }
    }

    public float get(long index) {

        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Error attempting to access data element " + index
                            + ".  Array is " + size + " elements long.");
        }
        int chunk = (int) (index / CHUNK_SIZE);
        int offset = (int) (index - (((long) chunk) * CHUNK_SIZE));
        return data[chunk][offset];
    }

    public void set(long index, float f) {

        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Error attempting to access data element " + index
                            + ".  Array is " + size + " elements long.");
        }
        int chunk = (int) (index / CHUNK_SIZE);
        int offset = (int) (index - (((long) chunk) * CHUNK_SIZE));
        data[chunk][offset] = f;
    }

    public void writeToFile() { // toString won't make sense for large array!

        // String str = "";
        //
        // for (int i = 0; i < size; ++i) {
        // str +=
        // }
        //
        // return str;

        // ...
    }

}
