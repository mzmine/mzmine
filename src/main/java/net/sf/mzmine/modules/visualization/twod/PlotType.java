package net.sf.mzmine.modules.visualization.twod;

/**
 * Created by owen myers on 4/5/17.
 */
public enum PlotType {

    FAST2D("Resampled data"),
    POINT2D("Use raw data points");

    private String type;

    PlotType(String type){
        this.type=type;
    }
    public String toString(){
       return type;
    }
}
