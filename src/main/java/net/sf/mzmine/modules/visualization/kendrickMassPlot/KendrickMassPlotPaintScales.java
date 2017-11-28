package net.sf.mzmine.modules.visualization.kendrickMassPlot;

import java.awt.Color;
import java.awt.Paint;

public class KendrickMassPlotPaintScales {
	 public static Paint[] getFullRainBowScale()
	   {
	      int ncolor = 360;
	      Color[] readRainbow = new Color[ncolor];
	      Color[] rainbow = new Color[ncolor];
	     
	      float x = (float) (1./(ncolor + 160));
	      for (int i=0; i < rainbow.length; i++)
	      {
	         readRainbow[i] = new Color( Color.HSBtoRGB((i)*x,1.0F,1.0F));
	         
	      }
	      for(int i = 0; i < rainbow.length; i++){
	    	  rainbow[i] = readRainbow[readRainbow.length-i-1];
	      }
	      return rainbow;
	   }
}
