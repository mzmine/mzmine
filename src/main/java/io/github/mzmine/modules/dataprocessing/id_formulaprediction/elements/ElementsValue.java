package io.github.mzmine.modules.dataprocessing.id_formulaprediction.elements;

import org.openscience.cdk.interfaces.IIsotope;
public class ElementsValue {

    IIsotope chosenIsotope;
    int Max = 100;
    int Min = 0;

    ElementsValue(IIsotope iisotope, int max, int min)
    {

        this.chosenIsotope = iisotope;
        this.Max = max;
        this.Min = min;
    }

    int getMax(){return this.Max;}
    int getMin(){return this.Min;}
    IIsotope getIsotope(){return this.chosenIsotope;}

}
