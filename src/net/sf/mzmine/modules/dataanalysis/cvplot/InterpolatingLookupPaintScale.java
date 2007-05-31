package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.Color;
import java.awt.Paint;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.jfree.chart.renderer.PaintScale;
import org.jfree.util.PublicCloneable;

public class InterpolatingLookupPaintScale implements PaintScale, PublicCloneable, Serializable {

	private class CompatibleTreeMap extends TreeMap<Double, int[]> {
		
		public Entry<Double, int[]> floorEntry(double value) {
			
			Double previousKey = null;
			for (Double currentKey : this.keySet()) {
				if (currentKey > value)
					break;
				previousKey = currentKey;
			}		
			if (previousKey==null) return null;
			return new AbstractMap.SimpleEntry<Double, int[]>(previousKey, this.get(previousKey));
			
		}
		
		public Entry<Double, int[]> ceilingEntry(double value) {
			for (Double currentKey : this.keySet()) {
				if (currentKey > value)
					return new AbstractMap.SimpleEntry<Double, int[]>(currentKey, this.get(currentKey));
			}
			return null;
		}
		
	}
	
	private CompatibleTreeMap lookupTable;
	
	public InterpolatingLookupPaintScale() {
		lookupTable = new CompatibleTreeMap();
	}
	
	public void addLookupValue(double value, int[] rgb) {
		lookupTable.put(value, rgb);
	}
	
	public Paint getPaint(double value) {
		Entry<Double, int[]> floor = lookupTable.floorEntry(value);
		Entry<Double, int[]> ceil = lookupTable.ceilingEntry(value);

		// Special cases, no floor, ceil or both available for given value
		if ( (floor==null) && (ceil==null) ) return new Color(0,0,0);
		if (floor==null) {
			int[] rgb = ceil.getValue();
			return new Color(rgb[0], rgb[1], rgb[2]);
		}
		if (ceil==null) {
			int[] rgb = floor.getValue();
			return new Color(rgb[0], rgb[1], rgb[2]);
		}

		// Normal case, interpolate between floor and ceil 
		double floorValue = floor.getKey();
		int[] floorRGB = floor.getValue();
		double ceilValue = ceil.getKey();
		int[] ceilRGB = ceil.getValue();
		
		int[] rgb = new int[3];
		for (int i=0; i<3; i++)
			rgb[i] = floorRGB[i] + (int)java.lang.Math.round( (double)(ceilRGB[i]-floorRGB[i]) * (double)(value-floorValue)/(double)(ceilValue-floorValue) );
		
		return new Color(rgb[0], rgb[1], rgb[2]);

	}
	
	public double getLowerBound() {
		return lookupTable.firstKey();
	}
	
	public double getUpperBound() {
		return lookupTable.lastKey();
	}

	
	public boolean equals(Object obj) {
		if (obj==this) return true;
		
		if (!(obj instanceof InterpolatingLookupPaintScale)) return false;
		
		InterpolatingLookupPaintScale that = (InterpolatingLookupPaintScale)obj;
		
		if (that.lookupTable.equals(lookupTable)) return true; else return false; 
		
	}
	
    public Object clone() throws CloneNotSupportedException {
    	return super.clone();
    }
	

}