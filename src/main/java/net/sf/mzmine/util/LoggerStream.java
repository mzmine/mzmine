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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * @description Wrap a Logger as a PrintStream.
 *  Useful mainly for porting code that previously
 *  logged to a System PrintStream or to a proxy.
 *  Calling LoggerStream.println()
 *  will call Logger.info() for Level.INFO.
 *  A call to flush() or to a println() method
 *  will flush previously written text, and
 *  complete a call to Logger.  You may be surprised
 *  by extra newlines, if you call print("\n")
 *  and flush() instead of println();
 *
 */
public class LoggerStream extends PrintStream {
	private Level _level = null;
	private Logger _logger = null;
	private ByteArrayOutputStream _baos = null;

	/** 
	 * Wrap a Logger as a PrintStream .
	 * @param logger Everything written to this PrintStream
	 *        will be passed to the appropriate method of the Logger
	 * @param level This indicates which method of the
	 *        Logger should be called.
	 */
	public LoggerStream(Logger logger, Level level) {
		super (new ByteArrayOutputStream(), true);
		_baos = (ByteArrayOutputStream) (this.out);
		_logger = logger;
		_level = level;
	}

	// from PrintStream
	@Override public synchronized void flush() {
		super.flush();
		if (_baos.size() ==0) return;
		String out1 = _baos.toString();

		_logger.log(_level,out1);

		_baos.reset();
	}

	// from PrintStream
	@Override public synchronized void println() {
		flush();
	}

	// from PrintStream
	@Override public synchronized void println(Object x) {
		print(x); // flush already adds a newline
		flush();
	}

	// from PrintStream
	@Override public synchronized void println(String x) {
		print(x); // flush already adds a newline
		flush();
	}

	// from PrintStream
	@Override public synchronized void close() {
		flush();
		super.close();
	}

	// from PrintStream
	@Override public synchronized boolean checkError() {
		flush();
		return super.checkError();
	}

	/** 
	 * test code
	 * @param args command line
	 */
	public static void main(String[] args) {
		Logger logger = Logger.getLogger("com.lgc.wsh.util");
		PrintStream psInfo = new LoggerStream(logger, Level.INFO);
		//PrintStream psWarning = new LoggerStream(logger, Level.WARNING);
		psInfo.print(3.);
		psInfo.println("*3.=9.");
		//if (false) {
		//  psWarning.print(3.);
		//  psWarning.println("*3.=9.");
		//}
		psInfo.print(3.);
		psInfo.flush();
		psInfo.println("*3.=9.");
		psInfo.println();
		psInfo.print("x");
		psInfo.close();
	}
}
