/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;

/**
 * A bounded capture of one process output stream, kept only to put into an error message or a log
 * line.
 * <p>
 * MSPepSearch is run with {@code /PROGRESS}, which prints several lines per submitted spectrum on
 * stderr, so a whole feature list produces hundreds of thousands of lines of which nothing but the
 * ends is ever read. This keeps the first {@link #HEAD_LINES} and the last {@link #TAIL_LINES}
 * lines and drops everything between them.
 * <p>
 * Both ends are kept on purpose: MSPepSearch reports the failures that matter - a library that
 * cannot be opened, an option combination it rejects - right after startup, which a tail only
 * buffer would push out on a run that produced output before failing.
 * <p>
 * Thread safe, because the stream is drained on its own thread while the task waits for the
 * process.
 */
final class ProcessOutputBuffer {

  /**
   * Lines kept from the start of the stream, where MSPepSearch reports startup failures.
   */
  private static final int HEAD_LINES = 20;

  /**
   * Lines kept from the end of the stream.
   */
  private static final int TAIL_LINES = 80;

  /**
   * A single line is truncated to this many characters, so that the whole buffer stays bounded even
   * if the process emits one very long line.
   */
  private static final int MAX_LINE_LENGTH = 500;

  private static final String ELLIPSIS = "...";

  private final List<String> head = new ArrayList<>(HEAD_LINES);
  private final Deque<String> tail = new ArrayDeque<>(TAIL_LINES);

  /**
   * Lines dropped between head and tail, reported in {@link #toString()} so that the gap is
   * visible.
   */
  private int dropped = 0;

  /**
   * Appends one line of process output.
   */
  synchronized void add(@NotNull final String line) {

    final String truncated =
        line.length() <= MAX_LINE_LENGTH ? line : line.substring(0, MAX_LINE_LENGTH) + ELLIPSIS;

    if (head.size() < HEAD_LINES) {
      head.add(truncated);
      return;
    }

    tail.addLast(truncated);
    if (tail.size() > TAIL_LINES) {
      tail.pollFirst();
      dropped++;
    }
  }

  /**
   * @return the captured lines, with a marker in place of the dropped ones. Empty if the stream
   * carried nothing.
   */
  @Override
  public synchronized @NotNull String toString() {

    final StringJoiner joiner = new StringJoiner(System.lineSeparator());
    head.forEach(joiner::add);
    if (dropped > 0) {
      joiner.add(ELLIPSIS + " " + dropped + " lines omitted " + ELLIPSIS);
    }
    tail.forEach(joiner::add);

    return joiner.toString().strip();
  }
}
