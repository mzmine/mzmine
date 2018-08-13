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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

<<<<<<< tomasmaster_mz_histogram
public abstract class DelayedDocumentListener implements DocumentListener, Runnable {
=======
/**
 * Use a consumer or override documentCHanged method
 *
 */
public class DelayedDocumentListener
    implements DocumentListener, Runnable, EventListener, Serializable {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());
>>>>>>> 8cceeb3 License 2 for mz histogram module

  private long lastAutoUpdateTime = -1;
  private boolean isAutoUpdateStarted = false;
  private long dalay = 1500;
  private boolean isActive = true;
  private DocumentEvent lastEvent = null;
  private boolean isStopped = false;

  public DelayedDocumentListener() {
    super();
  }

  public DelayedDocumentListener(long dalay) {
    super();
    this.dalay = dalay;
  }

  /**
   * Starts the auto update function. Waits for more events to happen for delay ms. New events reset
   * the timer. documentChanged method is called if the timer runs out.
   */
  public void startAutoUpdater(DocumentEvent e) {
    lastAutoUpdateTime = System.currentTimeMillis();
    lastEvent = e;
    isStopped = false;
    if (!isAutoUpdateStarted) {
      isAutoUpdateStarted = true;
      Thread t = new Thread(this);
      t.start();
    }
  }

  @Override
  public void run() {
    while (!isStopped) {
      if (lastAutoUpdateTime + dalay <= System.currentTimeMillis()) {
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
    isStopped = false;
    isAutoUpdateStarted = false;
  }

  /**
   * The document was changed
   * 
   * @param e last document event (only)
   */
  public abstract void documentChanged(DocumentEvent e);

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
    return dalay;
  }

  public void setDalay(long dalay) {
    this.dalay = dalay;
  }

  public boolean isActive() {
    return isActive;
  }

  /**
   * Set the active state. Stops an active thread if false.
   * 
   * @param isActive
   */
  public void setActive(boolean isActive) {
    this.isActive = isActive;
    if (!isActive)
      stop();
  }

  public void stop() {
    isStopped = true;
  }

}
