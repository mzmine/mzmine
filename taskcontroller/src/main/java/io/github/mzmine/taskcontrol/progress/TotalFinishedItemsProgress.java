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

package io.github.mzmine.taskcontrol.progress;

import java.util.concurrent.atomic.AtomicLong;

public final class TotalFinishedItemsProgress implements ProgressProvider {

  private final AtomicLong total = new AtomicLong(0);
  private final AtomicLong finished = new AtomicLong(0);

  public TotalFinishedItemsProgress() {
  }

  public TotalFinishedItemsProgress(long totalItems) {
    total.set(totalItems);
  }

  @Override
  public double progress() {
    return total.get() == 0 ? 0 : finished.get() / (double) total.get();
  }

  public AtomicLong getTotal() {
    return total;
  }

  public void setTotal(final long total) {
    this.total.set(total);
  }

  public AtomicLong getFinished() {
    return finished;
  }

  public void setFinished(final long finished) {
    this.finished.set(finished);
  }

  public double addFinished(long add) {
    return finished.addAndGet(add);
  }

  public long getAndIncrement() {
    return finished.getAndIncrement();
  }
}
