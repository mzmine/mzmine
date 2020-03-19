package io.github.mzmine.util.interpolatinglookuppaintscale;


import java.awt.*;

public class InterpolatingLookupPaintScaleRow {


    private Double key;
    private Color value;


    public InterpolatingLookupPaintScaleRow(Double key, Color value) {

        this.key = key;
        this.value = value;

    }


    public Double getKey(){return key;}

    public Color getValue(){return value;}

}
