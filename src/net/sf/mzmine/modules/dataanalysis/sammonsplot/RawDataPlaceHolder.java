
package net.sf.mzmine.modules.dataanalysis.sammonsplot;

class RawDataPlaceHolder {

    private String niceName;
    private int rawDataID;

    public RawDataPlaceHolder(String _niceName, int _rawDataID) {
        niceName = _niceName;
        rawDataID = _rawDataID;
    }

    public String toString() {
        return niceName;
    }

    public int getRawDataID() {
        return rawDataID;
    }
}