/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.colorpalette;

import java.util.Vector;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.chart.ChartTheme;
import javafx.scene.paint.Color;

public class SimpleColorPalette extends Vector<Color> {

  private static final Color defclr = Color.BLACK;
  private static final Logger logger = Logger.getLogger(SimpleColorPalette.class.getName());

  private int next;
  private static final int MAIN_COLOR = 0;
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  public SimpleColorPalette() {
    super();
    next = 0;
  }
  
  public SimpleColorPalette(Color[] clrs) {
    for(Color clr : clrs) {
      add(clr);
    }
  }
  
  public void applyToChartTheme(ChartTheme theme) {
    
  }
  
  /**
   * 
   * @return The next color in the color palette.
   */
  public Color getNextColor() {
    if (this.isEmpty())
      return defclr;
    
    if (next >= this.size() - 1)
      next = 0;
    next++;
    
    return get(next - 1);
  }
  
  public @Nonnull Color getMainColor() {
    if(isValidPalette())
      return get(MAIN_COLOR);
    return Color.BLACK;
  }
  
  public boolean isValidPalette() {
    if(this.isEmpty())
      return false;
    if(this.size() < 3)
      return false;
    for(Color clr : this)
      if (clr != null) {
        if(indexOf(clr) > 2)
          this.remove(indexOf(clr));
        else
          return false;
       }
    return true;
  }
}
