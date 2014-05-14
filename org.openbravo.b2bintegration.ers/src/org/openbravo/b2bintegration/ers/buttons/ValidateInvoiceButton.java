package org.openbravo.b2bintegration.ers.buttons;

import org.openbravo.b2bintegration.ers.actions.ErsUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * 
 */
public class ValidateInvoiceButton extends DalBaseProcess {

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String strCInvoiceId = (String) bundle.getParams().get("C_Invoice_ID");
    final String action = (String) bundle.getParams().get("action");
    final Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);

    try {

      ErsUtils.changeInvoiceStatus(invoice, action);

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
