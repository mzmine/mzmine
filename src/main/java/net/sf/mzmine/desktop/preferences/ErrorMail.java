/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.desktop.preferences;

import java.io.IOException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import net.sf.mzmine.util.EMailUtil;

/**
 * Class to send an error messsage only smtp support
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ErrorMail {

  public void sendErrorEmail(String emailTo, String emailFrom, String smtpServer, String subject,
      String msg, String password, Integer port) throws IOException {

    Properties props = new Properties();
    props.put("mail.smtp.host", smtpServer); // SMTP Host
    props.put("mail.smtp.socketFactory.port", port); // SSL Port
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // SSL Factory
                                                                                  // Class
    props.put("mail.smtp.auth", "true"); // Enabling SMTP Authentication
    props.put("mail.smtp.port", port); // SMTP Port, gmail 465

    Authenticator auth = new Authenticator() {

      // override the getPasswordAuthentication method
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(emailFrom, password);
      }
    };
    Session session = Session.getDefaultInstance(props, auth);
    EMailUtil.sendEmail(session, emailTo, subject, msg);

  }
}
