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

package net.sf.mzmine.modules.visualization.vankrevelendiagram;

import java.awt.Color;
import java.awt.Paint;
/**
 * Kendrick mass plot paint scale
 */
public class VanKrevelenDiagramPaintScales {
    public static Paint[] getFullRainBowScale(){
        int ncolor = 360;
        Color[] readRainbow = new Color[ncolor];
        Color[] rainbow = new Color[ncolor];

        float x = (float) (1./(ncolor + 160));
        for (int i=0; i < rainbow.length; i++)
        {       
            readRainbow[i] = new Color(Color.HSBtoRGB((i)*x,1.0F,1.0F));
        }
        for(int i = 0; i < rainbow.length; i++){
            rainbow[i] = readRainbow[readRainbow.length-i-1];
        }
        return rainbow;
    }
}
