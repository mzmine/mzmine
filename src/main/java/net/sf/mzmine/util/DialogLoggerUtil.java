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

package net.sf.mzmine.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class DialogLoggerUtil {

  /*
   * Dialogs
   */
  public static void showErrorDialog(Component parent, String message, Exception e) {
    JOptionPane.showMessageDialog(parent, message + " \n" + e.getMessage(), "ERROR",
        JOptionPane.ERROR_MESSAGE);
  }

  public static void showErrorDialog(Component parent, String title, String message) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
  }

  public static void showMessageDialog(Component parent, String title, String message) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  public static boolean showDialogYesNo(Component parent, String title, String text) {
    Object[] options = {"Yes", "No"};
    int n = JOptionPane.showOptionDialog(parent, text, title, JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    return n == 0;
  }

  /**
   * shows a message dialog just for a few given milliseconds
   * 
   * @param parent
   * @param title
   * @param message
   * @param time
   */
  public static void showMessageDialogForTime(JFrame parent, String title, String message,
      long time) {
    TimeDialog dialog = new TimeDialog(parent, time);
    dialog.setLayout(new FlowLayout(FlowLayout.LEFT));
    dialog.add(new JLabel(message));
    dialog.setTitle(title);
    dialog.pack();
    centerOnScreen(dialog, true);
    dialog.startDialog();
  }

  /**
   * Center on screen ( abslute true/false (exact center or 25% upper left) )o
   * 
   * @param c
   * @param absolute
   */
  public static void centerOnScreen(final Component c, final boolean absolute) {
    final int width = c.getWidth();
    final int height = c.getHeight();
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (screenSize.width / 2) - (width / 2);
    int y = (screenSize.height / 2) - (height / 2);
    if (!absolute) {
      x /= 2;
      y /= 2;
    }
    c.setLocation(x, y);
  }

  /**
   * Center on parent ( absolute true/false (exact center or 25% upper left) )
   * 
   * @param child
   * @param absolute
   */
  public static void centerOnParent(final Window child, final boolean absolute) {
    child.pack();
    boolean useChildsOwner = child.getOwner() != null
        ? ((child.getOwner() instanceof JFrame) || (child.getOwner() instanceof JDialog)) : false;
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final Dimension parentSize = useChildsOwner ? child.getOwner().getSize() : screenSize;
    final Point parentLocationOnScreen =
        useChildsOwner ? child.getOwner().getLocationOnScreen() : new Point(0, 0);
    final Dimension childSize = child.getSize();
    childSize.width = Math.min(childSize.width, screenSize.width);
    childSize.height = Math.min(childSize.height, screenSize.height);
    child.setSize(childSize);
    int x;
    int y;
    if ((child.getOwner() != null) && child.getOwner().isShowing()) {
      x = (parentSize.width - childSize.width) / 2;
      y = (parentSize.height - childSize.height) / 2;
      x += parentLocationOnScreen.x;
      y += parentLocationOnScreen.y;
    } else {
      x = (screenSize.width - childSize.width) / 2;
      y = (screenSize.height - childSize.height) / 2;
    }
    if (!absolute) {
      x /= 2;
      y /= 2;
    }
    child.setLocation(x, y);
  }

  // ################################################################################################################
  // internal dialog classes
  private static class TimeDialog extends JDialog implements Runnable {
    long time;

    public TimeDialog(JFrame parent, long time) {
      super(parent);
      this.time = time;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        this.setVisible(false);
        this.dispose();
      }
    }

    public void startDialog() {
      setVisible(true);
      new Thread(this).start();
    }
  }
}
