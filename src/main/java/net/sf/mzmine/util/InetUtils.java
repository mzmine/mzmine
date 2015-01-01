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

package net.sf.mzmine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Internet related utilities
 */
public class InetUtils {

    /**
     * Opens a connection to the given URL (typically HTTP) and retrieves the
     * data from server. Data is assumed to be in UTF-8 encoding.
     */
    public static String retrieveData(URL url) throws IOException {

	URLConnection connection = url.openConnection();
	connection.setRequestProperty("User-agent", "MZmine 2");
	InputStream is = connection.getInputStream();

	if (is == null) {
	    throw new IOException("Could not establish a connection to " + url);
	}

	StringBuffer buffer = new StringBuffer();

	try {
	    InputStreamReader reader = new InputStreamReader(is, "UTF-8");

	    char[] cb = new char[1024];

	    int amtRead = reader.read(cb);
	    while (amtRead > 0) {
		buffer.append(cb, 0, amtRead);
		amtRead = reader.read(cb);
	    }

	} catch (UnsupportedEncodingException e) {
	    // This should never happen, because UTF-8 is supported
	    e.printStackTrace();
	}

	is.close();

	return buffer.toString();

    }

}
