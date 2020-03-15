package io.github.mzmine.util.dialogs;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

import java.util.EventObject;

public class DialogController {
    @FXML
    public BorderPane borderLayout;

    @FXML
    public WebView fontWebview_up = new WebView();

    @FXML
    public WebView fontWebview_center = new WebView();

    private String result = "Unknown";
    private int atomicNumber = 0;
    private String elementSymbol = new String();





    private void elementNameTranslator(int rowIndex, int colIndex) {
        if (rowIndex == 1) {
            switch (colIndex) {
                case 1:
                    atomicNumber = 1;
                    result = "Hydrogen";
                    Hfunc();
                    break;
                case 18:
                    atomicNumber = 2;
                    result = "Helium";
                    Hefunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 2) {
            switch (colIndex) {
                case 1:
                    atomicNumber = 3;
                    result = "Lithium";
                    Lifunc();
                    break;
                case 2:
                    atomicNumber = 4;
                    result = "Beryllium";
                    Befunc();
                    break;
                case 13:
                    atomicNumber = 5;
                    result = "Boron";
                    Bfunc();
                    break;
                case 14:
                    atomicNumber = 6;
                    result = "Carbon";
                    Cfunc();
                    break;
                case 15:
                    atomicNumber = 7;
                    result = "Nitrogen";
                    Nfunc();
                    break;
                case 16:
                    atomicNumber = 8;
                    result = "Oxygen";
                    Ofunc();
                    break;
                case 17:
                    atomicNumber = 9;
                    result = "Fluorine";
                    Ffunc();
                    break;
                case 18:
                    atomicNumber = 10;
                    result = "Neon";
                    Nefunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 3) {
            switch (colIndex) {
                case 1:
                    atomicNumber = 11;
                    result = "Sodium";
                    Nafunc();
                    break;
                case 2:
                    atomicNumber = 12;
                    result = "Magnesium";
                    Mgfunc();
                    break;
                case 13:
                    atomicNumber = 13;
                    result = "Aluminum";
                    Alfunc();
                    break;
                case 14:
                    atomicNumber = 14;
                    result = "Silicon";
                    Sifunc();
                    break;
                case 15:
                    atomicNumber = 15;
                    result = "Phosphorus";
                    Pfunc();
                    break;
                case 16:
                    atomicNumber = 16;
                    result = "Sulfur";
                    Sfunc();
                    break;
                case 17:
                    atomicNumber = 17;
                    result = "Chlorine";
                    Clfunc();
                    break;
                case 18:
                    atomicNumber = 18;
                    result = "Argon";
                    Arfunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 4) {
            switch (colIndex) {
                case 1:
                    atomicNumber = 19;
                    result = "Potassium";
                    Kfunc();
                    break;
                case 2:
                    atomicNumber = 20;
                    result = "Calcium";
                    Cafunc();
                    break;
                case 3:
                    atomicNumber = 21;
                    result = "Scandium";
                    Scfunc();
                    break;
                case 4:
                    atomicNumber = 22;
                    result = "Titanium";
                    Tifunc();
                    break;
                case 5:
                    atomicNumber = 23;
                    result = "Vanadium";
                    Vfunc();
                    break;
                case 6:
                    atomicNumber = 24;
                    result = "Chromium";
                    Crfunc();
                    break;
                case 7:
                    atomicNumber = 25;
                    result = "Manganese";
                    Mnfunc();
                    break;
                case 8:
                    atomicNumber = 26;
                    result = "Iron";
                    Fefunc();
                    break;
                case 9:
                    atomicNumber = 27;
                    result = "Cobalt";
                    Cofunc();
                    break;
                case 10:
                    atomicNumber = 28;
                    result = "Nickel";
                    Nifunc();
                    break;
                case 11:
                    atomicNumber = 29;
                    result = "Copper";
                    Cufunc();
                    break;
                case 12:
                    atomicNumber = 30;
                    result = "Zinc";
                    Znfunc();
                    break;
                case 13:
                    atomicNumber = 31;
                    result = "Gallium";
                    Gafunc();
                    break;
                case 14:
                    atomicNumber = 32;
                    result = "Germanium";
                    Gefunc();
                    break;
                case 15:
                    atomicNumber = 33;
                    result = "Arsenic";
                    Asfunc();
                    break;
                case 16:
                    atomicNumber = 34;
                    result = "Selenium";
                    Sefunc();
                    break;
                case 17:
                    atomicNumber = 35;
                    result = "Bromine";
                    Brfunc();
                    break;
                case 18:
                    atomicNumber = 36;
                    result = "Krypton";
                    Krfunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 5) {
            switch (colIndex) {
                case 1:
                    atomicNumber = 37;
                    result = "Rubidium";
                    Rbfunc();
                    break;
                case 2:
                    atomicNumber = 38;
                    result = "Strontium";
                    Srfunc();
                    break;
                case 3:
                    atomicNumber = 39;
                    result = "Yttrium";
                    Yfunc();
                    break;
                case 4:
                    atomicNumber = 40;
                    result = "Zirconium";
                    Zrfunc();
                    break;
                case 5:
                    atomicNumber = 41;
                    result = "Niobium";
                    Nbfunc();
                    break;
                case 6:
                    atomicNumber = 42;
                    result = "Molybdenum";
                    Mofunc();
                    break;
                case 7:
                    atomicNumber = 43;
                    result = "Technetium";
                    Tcfunc();
                    break;
                case 8:
                    atomicNumber = 44;
                    result = "Ruthenium";
                    Rufunc();
                    break;
                case 9:
                    atomicNumber = 45;
                    result = "Rhodium";
                    Rhfunc();
                    break;
                case 10:
                    atomicNumber = 46;
                    result = "Palladium";
                    Pdfunc();
                    break;
                case 11:
                    atomicNumber = 47;
                    result = "Silver";
                    Agfunc();
                    break;
                case 12:
                    atomicNumber = 48;
                    result = "Cadmium";
                    Cdfunc();
                    break;
                case 13:
                    atomicNumber = 49;
                    result = "Indium";
                    Infunc();
                    break;
                case 14:
                    atomicNumber = 50;
                    result = "Tin";
                    Snfunc();
                    break;
                case 15:
                    atomicNumber = 51;
                    result = "Antimony";
                    Sbfunc();
                    break;
                case 16:
                    atomicNumber = 52;
                    result = "Tellurium";
                    Tefunc();
                    break;
                case 17:
                    atomicNumber = 53;
                    result = "Iodine";
                    Ifunc();
                    break;
                case 18:
                    atomicNumber = 54;
                    result = "Xenon";
                    Xefunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 6) {
            switch (colIndex) {
                case 1:
                    atomicNumber = 55;
                    result = "Cesium";
                    Csfunc();
                    break;
                case 2:
                    atomicNumber = 56;
                    result = "Barium";
                    Bafunc();
                    break;
                case 3:
                    atomicNumber = 57;
                    result = "Lanthanum";
                    Lafunc();
                    break;
                case 4:
                    atomicNumber = 72;
                    result = "Hafnium";
                    Hffunc();
                    break;
                case 5:
                    atomicNumber = 73;
                    result = "Tantalum";
                    Tafunc();
                    break;
                case 6:
                    atomicNumber = 74;
                    result = "Tungsten";
                    Wfunc();
                    break;
                case 7:
                    atomicNumber = 75;
                    result = "Rhenium";
                    Refunc();
                    break;
                case 8:
                    atomicNumber = 76;
                    result = "Osmium";
                    Osfunc();
                    break;
                case 9:
                    atomicNumber = 77;
                    result = "Iridium";
                    Irfunc();
                    break;
                case 10:
                    atomicNumber = 78;
                    result = "Platinum";
                    Ptfunc();
                    break;
                case 11:
                    atomicNumber = 79;
                    result = "Gold";
                    Aufunc();
                    break;
                case 12:
                    atomicNumber = 80;
                    result = "Mercury";
                    Hgfunc();
                    break;
                case 13:
                    atomicNumber = 81;
                    result = "Thallium";
                    Ylfunc();
                    break;
                case 14:
                    atomicNumber = 82;
                    result = "Lead";
                    Pbfunc();
                    break;
                case 15:
                    atomicNumber = 83;
                    result = "Bismuth";
                    Bifunc();
                    break;
                case 16:
                    atomicNumber = 84;
                    result = "Polonium";
                    Pofunc();
                    break;
                case 17:
                    atomicNumber = 85;
                    result = "Astatine";
                    Atfunc();
                    break;
                case 18:
                    atomicNumber = 86;
                    result = "Radon";
                    Rnfunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 7) {
            switch (colIndex){
                case 1:
                    atomicNumber = 87;
                    result = "Francium";
                    Frfunc();
                    break;
                case 2:
                    atomicNumber = 88;
                    result = "Radium";
                    Rafunc();
                    break;
                case 3:
                    atomicNumber = 89;
                    result = "Actinium";
                    Acfunc();
                    break;
                case 4:
                    atomicNumber = 104;
                    result = "Rutherfordium";
                    Rffunc();
                    break;
                case 5:
                    atomicNumber = 105;
                    result = "Dubnium";
                    Dbfunc();
                    break;
                case 6:
                    atomicNumber = 106;
                    result = "Seaborgium";
                    Sgfunc();
                    break;
                case 7:
                    atomicNumber = 107;
                    result = "Bohrium";
                    Bhfunc();
                    break;
                case 8:
                    atomicNumber = 108;
                    result = "Hassium";
                    Hsfunc();
                    break;
                case 9:
                    atomicNumber = 109;
                    result = "Meitnerium";
                    Mtfunc();
                    break;
                case 10:
                    atomicNumber = 110;
                    result = "Darmstadtium";
                    Dsfunc();
                    break;
                case 11:
                    atomicNumber = 111;
                    result = "Roentgenium";
                    Rgfunc();
                    break;
                case 12:
                    atomicNumber = 112;
                    result = "Ununbium";
                    break;
                case 13:
                    atomicNumber = 113;
                    result = "Ununtrium";
                    break;
                case 14:
                    atomicNumber = 114;
                    result = "Ununquadium";
                    break;
                case 15:
                    atomicNumber = 115;
                    result = "Ununpentium";
                    break;
                case 16:
                    atomicNumber = 116;
                    result = "Ununhexium";
                    break;
                case 17:
                    atomicNumber = 117;
                    result = "Ununseptium";
                    break;
                case 18:
                    atomicNumber = 118;
                    result = "Ununoctium";
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 8) {
            switch (colIndex) {
                case 3:
                    atomicNumber = 58;
                    result = "Cerium";
                    Cefunc();
                    break;
                case 4:
                    atomicNumber = 59;
                    result = "Praseodymium";
                    Prfunc();
                    break;
                case 5:
                    atomicNumber = 60;
                    result = "Neodymium";
                    Ndfunc();
                    break;
                case 6:
                    atomicNumber = 61;
                    result = "Promethium";
                    Pmfunc();
                    break;
                case 7:
                    atomicNumber = 62;
                    result = "Samarium";
                    Smfunc();
                    break;
                case 8:
                    atomicNumber = 63;
                    result = "Europium";
                    Eufunc();
                    break;
                case 9:
                    atomicNumber = 64;
                    result = "Gadolinium";
                    Gdfunc();
                    break;
                case 10:
                    atomicNumber = 65;
                    result = "Terbium";
                    Tbfunc();
                    break;
                case 11:
                    atomicNumber = 66;
                    result = "Dysprosium";
                    Dyfunc();
                    break;
                case 12:
                    atomicNumber = 67;
                    result = "Holmium";
                    Hofunc();
                    break;
                case 13:
                    atomicNumber = 68;
                    result = "Erbium";
                    Erfunc();
                    break;
                case 14:
                    atomicNumber = 69;
                    result = "Thulium";
                    Tmfunc();
                    break;
                case 15:
                    atomicNumber = 70;
                    result = "Ytterbium";
                    Ybfunc();
                    break;
                case 16:
                    atomicNumber = 71;
                    result = "Lutetium";
                    Lufunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        } else if (rowIndex == 9) {
            switch (colIndex) {
                case 3:
                    atomicNumber = 90;
                    result = "Thorium";
                    Thfunc();
                    break;
                case 4:
                    atomicNumber = 91;
                    result = "Protactinium";
                    Pafunc();
                    break;
                case 5:
                    atomicNumber = 92;
                    result = "Uranium";
                    Ufunc();
                    break;
                case 6:
                    atomicNumber = 93;
                    result = "Neptunium";
                    Npfunc();
                    break;
                case 7:
                    atomicNumber = 94;
                    result = "Plutonium";
                    Pufunc();
                    break;
                case 8:
                    atomicNumber = 95;
                    result = "Americium";
                    Amfunc();
                    break;
                case 9:
                    atomicNumber = 96;
                    result = "Curium";
                    Cmfunc();
                    break;
                case 10:
                    atomicNumber = 97;
                    result = "Berkelium";
                    Bkfunc();
                    break;
                case 11:
                    atomicNumber = 98;
                    result = "Californium";
                    Cffunc();
                    break;
                case 12:
                    atomicNumber = 99;
                    result = "Einsteinium";
                    Esfunc();
                    break;
                case 13:
                    atomicNumber = 100;
                    result = "Fermium";
                    Fmfunc();
                    break;
                case 14:
                    atomicNumber = 101;
                    result = "Mendelevium";
                    Mdfunc();
                    break;
                case 15:
                    atomicNumber = 102;
                    result = "Nobelium";
                    Nofunc();
                    break;
                case 16:
                    atomicNumber = 103;
                    result = "Lawrencium";
                    Lrfunc();
                    break;
                default:
                    atomicNumber = 0;
                    result = "Unknown";
                    break;
            }
        }
    }



    public String serieTranslator(String serie) {
        if (serie.equals("Noble Gasses"))
            return DialogController.GT.get("Noble Gases");
        else if (serie.equals("Halogens"))
            return DialogController.GT.get("Halogens");
        else if (serie.equals("Nonmetals"))
            return DialogController.GT.get("Nonmetals");
        else if (serie.equals("Metalloids"))
            return DialogController.GT.get("Metalloids");
        else if (serie.equals("Metals"))
            return DialogController.GT.get("Metals");
        else if (serie.equals("Alkali Earth Metals"))
            return DialogController.GT.get("Alkali Earth Metals");
        else if (serie.equals("Alkali Metals"))
            return DialogController.GT.get("Alkali Metals");
        else if (serie.equals("Transition metals"))
            return DialogController.GT.get("Transition metals");
        else if (serie.equals("Lanthanides"))
            return DialogController.GT.get("Lanthanides");
        else if (serie.equals("Actinides"))
            return DialogController.GT.get("Actinides");
        else
            return DialogController.GT.get("Unknown");
    }

    String phaseTranslator(String serie) {
        if (serie.equals("Gas"))
            return GT.get("Gas");
        else if (serie.equals("Liquid"))
            return GT.get("Liquid");
        else if (serie.equals("Solid"))
            return GT.get("Solid");
        else
            return GT.get("Unknown");
    }

private static class GT {
    private static String get(String s) {
        return s;
    }
}


    public String getElementSymbol() {
        return elementSymbol;
    }

    @FXML
    public void MouseArrive(MouseEvent event) {
        Node source = (Node)event.getSource() ;
        Integer colIndex = GridPane.getColumnIndex(source);
        Integer rowIndex = GridPane.getRowIndex(source);
        Button b = (Button)source;
        elementSymbol = b.getText();
        elementNameTranslator(rowIndex, colIndex);
        StringBuilder sb_up = new StringBuilder("<html><FONT SIZE=+2>" + result
                + " (" + elementSymbol + ")</FONT><br> " + DialogController.GT.get("Atomic number") + " "
                + atomicNumber
                + (", " + DialogController.GT.get("Group") + " " + colIndex) + ", "
                + DialogController.GT.get("Period") + " " +rowIndex + "</html>");

        fontWebview_up.getEngine().loadContent(sb_up.toString());


        StringBuilder sb_Center = new StringBuilder(
                "<html><FONT> " + GT.get("CAS RN:") + " "
                        + PeriodicTable.getCASId(elementSymbol) + "<br> " + DialogController.GT.get("Element Category:") + " "
                        + serieTranslator(PeriodicTable.getChemicalSeries(elementSymbol)) + "<br> "
                        + DialogController.GT.get("State:") + " " + phaseTranslator(PeriodicTable.getPhase(elementSymbol))
                        + "<br> " + DialogController.GT.get("Electronegativity:") + " "
                        + (PeriodicTable.getPaulingElectronegativity(elementSymbol) == null ? DialogController.GT.get("undefined")
                        : PeriodicTable.getPaulingElectronegativity(elementSymbol))
                        + "<br>" + "</FONT></html>"
        );
        fontWebview_center.getEngine().loadContent(sb_Center.toString());
    }

    @FXML
    public void Hfunc() {

    }

    @FXML
    public void Hefunc() {

    }

    @FXML
    public void Lifunc() {

    }

    @FXML
    public void Befunc() {

    }

    @FXML
    public void Bfunc() {

    }

    @FXML
    public void Cfunc() {

    }

    @FXML
    public void Nfunc() {

    }

    @FXML
    public void Ofunc() {

    }

    @FXML
    public void Ffunc() {

    }

    @FXML
    public void Nefunc() {

    }

    @FXML
    public void Nafunc() {

    }

    @FXML
    public void Mgfunc() {

    }

    @FXML
    public void Alfunc() {

    }

    @FXML
    public void Sifunc() {

    }

    @FXML
    public void Pfunc() {

    }

    @FXML
    public void Sfunc() {

    }

    @FXML
    public void Clfunc() {

    }

    @FXML
    public void Arfunc() {

    }

    @FXML
    public void Kfunc() {

    }

    @FXML
    public void Cafunc() {

    }

    @FXML
    public void Scfunc() {

    }

    @FXML
    public void Tifunc() {

    }

    @FXML
    public void Vfunc() {

    }

    @FXML
    public void Crfunc() {

    }

    @FXML
    public void Mnfunc() {

    }

    @FXML
    public void Fefunc() {

    }

    @FXML
    public void Cofunc() {

    }

    @FXML
    public void Nifunc() {

    }

    @FXML
    public void Cufunc() {

    }

    @FXML
    public void Znfunc() {

    }

    @FXML
    public void Gafunc() {

    }

    @FXML
    public void Gefunc() {

    }

    @FXML
    public void Asfunc() {

    }

    @FXML
    public void Sefunc() {

    }

    @FXML
    public void Brfunc() {

    }

    @FXML
    public void Krfunc() {

    }

    @FXML
    public void Rbfunc() {

    }

    @FXML
    public void Srfunc() {

    }

    @FXML
    public void Yfunc() {

    }

    @FXML
    public void Zrfunc() {

    }

    @FXML
    public void Nbfunc() {

    }

    @FXML
    public void Mofunc() {

    }

    @FXML
    public void Tcfunc() {

    }

    @FXML
    public void Rufunc() {

    }

    @FXML
    public void Rhfunc() {

    }

    @FXML
    public void Pdfunc() {

    }

    @FXML
    public void Agfunc() {

    }

    @FXML
    public void Cdfunc() {

    }

    @FXML
    public void Infunc() {

    }

    @FXML
    public void Snfunc() {

    }

    @FXML
    public void Sbfunc() {

    }

    @FXML
    public void Tefunc() {

    }

    @FXML
    public void Ifunc() {

    }

    @FXML
    public void Xefunc() {

    }

    @FXML
    public void Csfunc() {

    }

    @FXML
    public void Bafunc() {

    }

    @FXML
    public void Lafunc() {

    }

    @FXML
    public void Hffunc() {

    }

    @FXML
    public void Tafunc() {

    }

    @FXML
    public void Wfunc() {

    }

    @FXML
    public void Refunc() {

    }

    @FXML
    public void Osfunc() {

    }

    @FXML
    public void Irfunc() {

    }

    @FXML
    public void Ptfunc() {

    }

    @FXML
    public void Aufunc() {

    }

    @FXML
    public void Hgfunc() {

    }

    @FXML
    public void Ylfunc() {

    }

    @FXML
    public void Pbfunc() {

    }

    @FXML
    public void Bifunc() {

    }

    @FXML
    public void Pofunc() {

    }

    @FXML
    public void Atfunc() {

    }

    @FXML
    public void Rnfunc() {

    }

    @FXML
    public void Frfunc() {

    }

    @FXML
    public void Rafunc() {

    }

    @FXML
    public void Acfunc() {

    }

    @FXML
    public void Rffunc() {

    }

    @FXML
    public void Dbfunc() {

    }

    @FXML
    public void Sgfunc() {

    }

    @FXML
    public void Bhfunc() {

    }

    @FXML
    public void Hsfunc() {

    }

    @FXML
    public void Mtfunc() {

    }

    @FXML
    public void Dsfunc() {

    }

    @FXML
    public void Rgfunc() {

    }

    @FXML
    public void Cefunc() {

    }

    @FXML
    public void Prfunc() {

    }

    @FXML
    public void Ndfunc() {

    }

    @FXML
    public void Pmfunc() {

    }

    @FXML
    public void Smfunc() {

    }

    @FXML
    public void Eufunc() {

    }

    @FXML
    public void Gdfunc() {

    }

    @FXML
    public void Tbfunc() {

    }

    @FXML
    public void Dyfunc() {

    }

    @FXML
    public void Hofunc() {

    }

    @FXML
    public void Erfunc() {

    }

    @FXML
    public void Tmfunc() {

    }

    @FXML
    public void Ybfunc() {

    }

    @FXML
    public void Lufunc() {

    }

    @FXML
    public void Thfunc() {

    }

    @FXML
    public void Pafunc() {

    }

    @FXML
    public void Ufunc() {

    }

    @FXML
    public void Npfunc() {

    }

    @FXML
    public void Pufunc() {

    }

    @FXML
    public void Amfunc() {

    }

    @FXML
    public void Cmfunc() {

    }

    @FXML
    public void Bkfunc() {

    }

    @FXML
    public void Cffunc() {

    }

    @FXML
    public void Esfunc() {

    }

    @FXML
    public void Fmfunc() {

    }

    @FXML
    public void Mdfunc() {

    }

    @FXML
    public void Nofunc() {

    }

    @FXML
    public void Lrfunc() {

    }

}