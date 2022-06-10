package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

public record MaldiSpotInfo(int frameNum, int chip, String spotName, int regionNumber,
                            int xIndexPos, int yIndexPos, double motorX, double motorY,
                            double motorZ) {

}
