package kendrickMassPlots;

import java.awt.Color;
import java.awt.Paint;

public class KendrickMassPlotPaintScales {
	
	 public static Paint[] getRedScale(){
		   int ncolor = 190;
		   Color[] redScale = new Color[ncolor];
		   for (int i=0; i < redScale.length; i++)
		      {
		        
			   	redScale[i] = new Color(255, redScale.length-i-1, redScale.length-i-1);
		        
		      }
		   
		   return redScale;
	   }
	 
	 public static Paint[] getGreenScale(){
		   int ncolor = 190;
		   Color[] greenScale = new Color[ncolor];
		   for (int i=0; i < greenScale.length; i++)
		      {
		        
			   	greenScale[i] = new Color(greenScale.length-i-1, 255, greenScale.length-i-1);
		        
		      }
		   
		   return greenScale;
	   }
	 
	 public static Paint[] getBlueScale(){
	      int ncolor = 190;
	      Color[] blueScale = new Color[ncolor];
	      for (int i=0; i < blueScale.length; i++)
	         {
	           
	         blueScale[i] = new Color(blueScale.length-i-1, blueScale.length-i-1, 255);
	           
	         }
	      
	      return blueScale;
	    }
	 
	 public static Paint[] getYellowScale(){
	      int ncolor = 190;
	      Color[] yellowScale = new Color[ncolor];
	      for (int i=0; i < yellowScale.length; i++)
	         {
	           
	         yellowScale[i] = new Color(255, 255, yellowScale.length-i-1);
	           
	         }
	      
	      return yellowScale;
	    }
	 
	 public static Paint[] getCyanScale(){
	      int ncolor = 190;
	      Color[] cyanScale = new Color[ncolor];
	      for (int i=0; i < cyanScale.length; i++)
	         {
	           
	         cyanScale[i] = new Color(cyanScale.length-i-1, 255, 255);
	           
	         }
	      
	      return cyanScale;
	    }
	
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
