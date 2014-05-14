package org.openbravo.b2bintegration.base.emailengine;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

class ClientAuthenticator extends Authenticator {
  private String _username;
  private String _password;

  public ClientAuthenticator(String username, String password) {
    _username = username;
    _password = password;
  }

  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(_username, _password);
  }
}
