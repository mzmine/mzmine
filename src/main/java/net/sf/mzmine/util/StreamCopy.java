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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class StreamCopy {

    private long copiedLength, totalLength;
    private boolean canceled = false, finished = false;

    /**
     * Copy the data from inputStream to outputStream using nio channels
     * 
     * @param input
     *            InputStream
     * @param output
     *            OutputStream
     */
    public void copy(InputStream input, OutputStream output)
            throws IOException {
        this.copy(input, output, 0);
    }

    /**
     * Copy the data from inputStream to outputStream using nio channels
     * 
     * @param input
     *            InputStream
     * @param output
     *            OutputStream
     */
    public void copy(InputStream input, OutputStream output, long totalLength)
            throws IOException {

        this.totalLength = totalLength;

        ReadableByteChannel in = Channels.newChannel(input);
        WritableByteChannel out = Channels.newChannel(output);

        // Allocate 1MB buffer
        ByteBuffer bbuffer = ByteBuffer.allocate(1 << 20);

        int len = 0;

        while ((len = in.read(bbuffer)) != -1) {

            if (canceled)
                return;

            bbuffer.flip();
            out.write(bbuffer);
            bbuffer.clear();
            copiedLength += len;
        }

        finished = true;

    }

    /**
     * 
     * @return the progress of the "copy()" function copying the data from one
     *         stream to another
     */
    public double getProgress() {
        if (finished)
            return 1.0;
        if (totalLength == 0)
            return 0;
        if (copiedLength >= totalLength)
            return 1.0;
        return (double) (double)copiedLength / (double)totalLength;
    }

    /**
     * Cancel the copying
     */
    public void cancel() {
        canceled = true;
    }

    /**
     * Checks if copying is finished
     */
    public boolean finished() {
        return finished;
    }
}
