package org.openbravo.b2bintegration.ers.actions;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

public class ErsUtils {
  public static void changeInvoiceStatus(Invoice invoice, String newstatus) {
    invoice.setB2bersValidationstatus(newstatus);
    OBDal.getInstance().flush();
  }

  public static void ackPayment(FIN_Payment payment, Boolean auto) {
    if (auto) {
      payment.setB2bersAckstatus("AUTO");
    } else
      payment.setB2bersAckstatus("OK");
    OBDal.getInstance().flush();
  }
}