package org.openbravo.b2bintegration.ers.buttons;

import org.openbravo.b2bintegration.ers.actions.ErsUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * 
 */
public class AckPaymentButton extends DalBaseProcess {

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String strFinPaymentId = (String) bundle.getParams().get("Fin_Payment_ID");
    final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strFinPaymentId);
    bundle.getParamsDefalated();

    try {

      ErsUtils.ackPayment(payment, false);

      // If successful,

      OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      msg.setMessage("@Success@");
      bundle.setResult(msg);
    } catch (Exception e) {
      OBError msg = new OBError();
      msg.setType("Error");
      msg.setTitle("@Error@");
      msg.setMessage(e.getMessage());
      bundle.setResult(msg);
    } finally {

    }
    // If not successful, show DAL error

  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

  }
}
