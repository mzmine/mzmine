/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @description Wrap a Logger as a PrintStream. Useful mainly for porting code that previously
 *              logged to a System PrintStream or to a proxy. Calling LoggerStream.println() will
 *              call Logger.info() for Level.INFO. A call to flush() or to a println() method will
 *              flush previously written text, and complete a call to Logger. You may be surprised
 *              by extra newlines, if you call print("\n") and flush() instead of println();
 *
 */
public class LoggerStream extends PrintStream {
  private Level _level = null;
  private Logger _logger = null;
  private ByteArrayOutputStream _baos = null;

  /**
   * Wrap a Logger as a PrintStream .
   * 
   * @param logger Everything written to this PrintStream will be passed to the appropriate method
   *        of the Logger
   * @param level This indicates which method of the Logger should be called.
   */
  public LoggerStream(Logger logger, Level level) {
    super(new ByteArrayOutputStream(), true);
    _baos = (ByteArrayOutputStream) (this.out);
    _logger = logger;
    _level = level;
  }

  // from PrintStream
  @Override
  public synchronized void flush() {
    super.flush();
    if (_baos.size() == 0)
      return;
    String out1 = _baos.toString();

    _logger.log(_level, out1);

    _baos.reset();
  }

  // from PrintStream
  @Override
  public synchronized void println() {
    flush();
  }

  // from PrintStream
  @Override
  public synchronized void println(Object x) {
    print(x); // flush already adds a newline
    flush();
  }

  // from PrintStream
  @Override
  public synchronized void println(String x) {
    print(x); // flush already adds a newline
    flush();
  }

  // from PrintStream
  @Override
  public synchronized void close() {
    flush();
    super.close();
  }

  // from PrintStream
  @Override
  public synchronized boolean checkError() {
    flush();
    return super.checkError();
  }

  /**
   * test code
   * 
   * @param args command line
   */
  public static void main(String[] args) {
    Logger logger = Logger.getLogger("com.lgc.wsh.util");
    PrintStream psInfo = new LoggerStream(logger, Level.INFO);
    // PrintStream psWarning = new LoggerStream(logger, Level.WARNING);
    psInfo.print(3.);
    psInfo.println("*3.=9.");
    // if (false) {
    // psWarning.print(3.);
    // psWarning.println("*3.=9.");
    // }
    psInfo.print(3.);
    psInfo.flush();
    psInfo.println("*3.=9.");
    psInfo.println();
    psInfo.print("x");
    psInfo.close();
  }
}
