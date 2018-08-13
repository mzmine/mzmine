/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.framework.listener;

import java.io.Serializable;
import java.util.EventListener;
import java.util.function.Consumer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use a consumer or override documentCHanged method
 *
 */
public class DelayedDocumentListener
    implements DocumentListener, Runnable, EventListener, Serializable {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

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
      logger.debug("Auto update started");
      isAutoUpdateStarted = true;
      Thread t = new Thread(this);
      t.start();
    } else
      logger.debug("no auto update this time");
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
        logger.error("", e);
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
