package org.openbravo.b2bintegration.base.filterexpressions;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;

public class UserBusinessPartnerId implements FilterExpression {
  private Logger log = Logger.getLogger(UserBusinessPartnerId.class);
  private Map<String, String> requestMap;
  private HttpSession httpSession;
  private String windowId;

  @Override
  public String getExpression(Map<String, String> _requestMap) {

    // Will wait for bug to be solved
    // BusinessPartner bp = OBContext.getOBContext().getUser().getBusinessPartner();
    String bpid = OBContext.getOBContext().getUser().getB2bBpartnerid();
    if (bpid != null) {
      return bpid;
    } else
      return "";
  }

}
