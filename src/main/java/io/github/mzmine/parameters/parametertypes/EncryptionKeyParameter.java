/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.parameters.parametertypes;

import org.w3c.dom.Element;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.util.StringCrypter;

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
            Logger.getLogger(this.getClass().getName())
                    .warning("Could not load Encryption key! "
                            + "Encrypted parameters in the config file might not be decryptable.");
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
