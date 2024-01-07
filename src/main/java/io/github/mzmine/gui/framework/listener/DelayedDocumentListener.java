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

package io.github.mzmine.gui.framework.listener;

import java.io.Serializable;
import java.util.EventListener;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Use a consumer or override documentCHanged method
 *
 */
public class DelayedDocumentListener
    implements DocumentListener, Runnable, EventListener, Serializable {
  private static final long serialVersionUID = 1L;

  private final Logger logger = Logger.getLogger(getClass().getName());

  private long lastAutoUpdateTime = -1;
  private boolean isAutoUpdateStarted = false;
  private long delay = 1500;
  private boolean isActive = true;
  private DocumentEvent lastEvent = null;
  private boolean isStopped = false;
  private Consumer<DocumentEvent> consumer = null;

  public DelayedDocumentListener() {
    this(null);
  }

  public DelayedDocumentListener(Consumer<DocumentEvent> consumer) {
    super();
    this.consumer = consumer;
  }

  public DelayedDocumentListener(long delay) {
    this(delay, null);
  }

  public DelayedDocumentListener(long delay, Consumer<DocumentEvent> consumer) {
    super();
    this.delay = delay;
    this.consumer = consumer;
  }

  /**
   * starts the auto update function
   */
  public void startAutoUpdater(DocumentEvent e) {
    lastAutoUpdateTime = System.currentTimeMillis();
    lastEvent = e;
    isStopped = false;
    if (!isAutoUpdateStarted) {
      logger.finest("Auto update started");
      isAutoUpdateStarted = true;
      Thread t = new Thread(this);
      t.start();
    } else
      logger.finest("no auto update this time");
  }

  @Override
  public void run() {
    while (!isStopped) {
      if (lastAutoUpdateTime + delay <= System.currentTimeMillis()) {
        documentChanged(lastEvent);
        lastAutoUpdateTime = -1;
        isAutoUpdateStarted = false;
        break;
      }
      try {
        Thread.currentThread().sleep(80);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    isAutoUpdateStarted = false;
    isStopped = false;
  }

  /**
   * the document was changed
   *
   * @param e last document event (only)
   */
  public void documentChanged(DocumentEvent e) {
    if (consumer != null)
      consumer.accept(e);
  }

  @Override
  public void removeUpdate(DocumentEvent arg0) {
    if (isActive)
      startAutoUpdater(arg0);
  }

  @Override
  public void insertUpdate(DocumentEvent arg0) {
    if (isActive)
      startAutoUpdater(arg0);
  }

  @Override
  public void changedUpdate(DocumentEvent arg0) {
    if (isActive)
      startAutoUpdater(arg0);
  }

  public long getDalay() {
    return delay;
  }

  public void setDalay(long dalay) {
    this.delay = dalay;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
    if (!isActive)
      stop();
  }

  public void stop() {
    isStopped = true;
  }

}
