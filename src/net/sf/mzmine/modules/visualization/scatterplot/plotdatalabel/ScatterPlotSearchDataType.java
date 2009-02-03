package net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel;

public enum ScatterPlotSearchDataType {

	NAME ("name"),
	
	MASS ("m/z"),
	
	RT ("rt");
	
	private String text;
	
	ScatterPlotSearchDataType (String text){
		this.text = text;
	}
	
	public String getText(){
		return text;
	}
	
	public String toString(){
		return text;
	}

}
