package net.sf.mzmine.parameters.parametertypes;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.util.StringCrypter;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

public class EncryptionKeyParameter implements Parameter<StringCrypter> {
    private StringCrypter value;

    @Override
    public String getName() {
        return "AES Encryption Key";
    }

    @Override
    public StringCrypter getValue() {
        return value;
    }

    @Override
    public void setValue(StringCrypter newValue) {
        value = newValue;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        try {
            final String nuVal = xmlElement.getTextContent();
            if (nuVal == null || nuVal.isEmpty())
                return;
            value = new StringCrypter(nuVal);
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).warning("Could not load Encryption key! " +
                    "Encrypted parameters in the config file might not be decryptable.");
        }
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        xmlElement.setTextContent(value.toString());
    }

    @Override
    public Parameter<StringCrypter> cloneParameter() {
        EncryptionKeyParameter newP = new EncryptionKeyParameter();
        newP.setValue(new StringCrypter(value.toBytes()));
        return newP;
    }

    @Override
    public boolean isSensitive() {
        return true;
    }
}
