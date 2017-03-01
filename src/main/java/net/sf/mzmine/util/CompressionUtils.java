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

package net.sf.mzmine.util;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Compression related utilities
 */
public class CompressionUtils {

    /**
     * Decompress the zlib-compressed bytes and return an array of decompressed
     * bytes
     * 
     */
    public static byte[] decompress(byte compressedBytes[])
	    throws DataFormatException {

	Inflater decompresser = new Inflater();

	decompresser.setInput(compressedBytes);

	byte[] resultBuffer = new byte[compressedBytes.length * 2];
	byte[] resultTotal = new byte[0];

	int resultLength = decompresser.inflate(resultBuffer);

	while (resultLength > 0) {
	    byte previousResult[] = resultTotal;
	    resultTotal = new byte[resultTotal.length + resultLength];
	    System.arraycopy(previousResult, 0, resultTotal, 0,
		    previousResult.length);
	    System.arraycopy(resultBuffer, 0, resultTotal,
		    previousResult.length, resultLength);
	    resultLength = decompresser.inflate(resultBuffer);
	}

	decompresser.end();

	return resultTotal;
    }

}
