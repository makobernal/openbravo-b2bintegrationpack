package org.openbravo.b2bintegration.pomgmt.VendorButtons;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openbravo.b2bintegration.B2bVendorParameters;
import org.openbravo.b2bintegration.base.activitylog.ActivityLogUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * 
 */
public class RequestChange extends DalBaseProcess {
  BusinessPartner b2bvendor;
  B2bVendorParameters b2bvendorparams;
  Order purchaseorder;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String reply = (String) bundle.getParams().get("reply");
    final String strCOrderid = (String) bundle.getParams().get("C_Order_ID");

    purchaseorder = OBDal.getInstance().get(Order.class, strCOrderid);

    try {

      checkparams();
      // set document status
      StringBuilder newReqChanges = new StringBuilder();

      newReqChanges.append(getDateTimeString(new Date()));
      newReqChanges.append(" - ");
      newReqChanges.append(reply);
      newReqChanges.append("\n");
      if (purchaseorder.getB2bpoVendorreqchanges() != null) {
        newReqChanges.append(purchaseorder.getB2bpoVendorreqchanges());
      }
      purchaseorder.setB2bpoVendorreqchanges(newReqChanges.toString());

      purchaseorder.setB2bpoScmreqchanges(newReqChanges.toString());
      purchaseorder.setB2bpoOrderstatus("NSCM");
      OBDal.getInstance().flush();
      ActivityLogUtils.log(OBContext.getOBContext().getUser(),
          "b2bpo_activitylog_vendorrequestedchange", purchaseorder);

      // send email to vendor

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

  private void checkparams() throws Exception {
    // Check that bpartner is a B2B guy

    if (purchaseorder.getBusinessPartner().getB2BVendorParametersList().isEmpty()) {
      throw new Exception("@b2bpo_publishorder_BPisnotB2B@");
    }
    ;

    b2bvendorparams = purchaseorder.getBusinessPartner().getB2BVendorParametersList().get(0);

  }

  private static String getDateTimeString(Date date) {
    try {
      String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
          "dateTimeFormat.java");
      SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
      return (outputFormat.format(date));
    } catch (Exception e) {

      return null;
    }
  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

  }
}
