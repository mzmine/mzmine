/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
package net.sf.mzmine.project.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveFileUtils {

	private double progress;

	/**
	 * Copy the data from inputStream to outputStream using nio channels
	 * @param input InputStream
	 * @param output OutputStream
	 * @param mode: decides which stream has to be closed
	 */
	public void saveFile(InputStream input, OutputStream output, long fileLenght, SaveFileUtilsMode mode) {
		try {
			progress = 0.0;

			ReadableByteChannel in = Channels.newChannel(input);
			WritableByteChannel out = Channels.newChannel(output);
			ByteBuffer bbuffer = ByteBuffer.allocate(16 * 1024);
			int len = 0;
			int lenTotal = 0;

			while ((len = in.read(bbuffer)) != -1) {
				bbuffer.flip();
				out.write(bbuffer);
				bbuffer.clear();

				lenTotal += len;
				progress = (double) lenTotal / fileLenght;
			}

			// closing streams
			switch (mode) {
				case CLOSE_ALL:
					in.close();
					out.close();
					break;
				case CLOSE_IN:
					in.close();
					break;
				case CLOSE_OUT:
					out.close();
					break;
			}

		} catch (IOException ex) {
			Logger.getLogger(SaveFileUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 
	 * @return the progress of the "saveFile()" function copying the data from one stream to another
	 */
	public double getProgress(){
		return progress;
	}
}
