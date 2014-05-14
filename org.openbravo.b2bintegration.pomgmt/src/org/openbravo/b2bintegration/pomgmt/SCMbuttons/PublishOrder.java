package org.openbravo.b2bintegration.pomgmt.SCMbuttons;

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
public class PublishOrder extends DalBaseProcess {
  BusinessPartner b2bvendor;
  B2bVendorParameters b2bvendorparams;
  Order purchaseorder;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String strCOrderid = (String) bundle.getParams().get("C_Order_ID");

    purchaseorder = OBDal.getInstance().get(Order.class, strCOrderid);

    try {

      checkparams();
      // set document status
      purchaseorder.setB2BPOPublishDate(new Date());
      purchaseorder.setB2bpoOrderstatus("DR");

      StringBuilder newReqChanges = new StringBuilder();
      newReqChanges.append(getDateTimeString(new Date()));
      newReqChanges.append(" - ");
      newReqChanges.append("Purchase Order Published");
      newReqChanges.append("\n");
      if (purchaseorder.getB2bpoScmreqchanges() != null) {
        newReqChanges.append(purchaseorder.getB2bpoScmreqchanges());
      }
      purchaseorder.setB2bpoScmreqchanges(newReqChanges.toString());

      OBDal.getInstance().flush();
      ActivityLogUtils.log(OBContext.getOBContext().getUser(), "b2bpo_activitylog_orderpublished",
          purchaseorder);

      if (b2bvendorparams.isAutoackorder()) {
        purchaseorder.setB2BPOACKDate(new Date());
        purchaseorder.setB2bpoOrderstatus("ACK");

        StringBuilder newReqChanges2 = new StringBuilder();
        newReqChanges2.append(getDateTimeString(new Date()));
        newReqChanges2.append(" - ");
        newReqChanges2.append("(Auto) - Purchase Order ACKed");
        newReqChanges2.append("\n");
        if (purchaseorder.getB2bpoVendorreqchanges() != null) {
          newReqChanges2.append(purchaseorder.getB2bpoVendorreqchanges());
        }
        purchaseorder.setB2bpoVendorreqchanges(newReqChanges2.toString());

        OBDal.getInstance().flush();
        ActivityLogUtils.log(purchaseorder.getBusinessPartner().getADUserList().get(0),
            "b2bpo_activitylog_orderautoACKed", purchaseorder);
        if (b2bvendorparams.isAutoacceptorder()) {
          purchaseorder.setB2bpoAcceptancedate(new Date());
          purchaseorder.setB2bpoOrderstatus("OK");

          StringBuilder newReqChanges3 = new StringBuilder();
          newReqChanges3.append(getDateTimeString(new Date()));
          newReqChanges3.append(" - ");
          newReqChanges3.append("(Auto) - Purchase Order Accepted");
          newReqChanges3.append("\n");
          if (purchaseorder.getB2bpoVendorreqchanges() != null) {
            newReqChanges3.append(purchaseorder.getB2bpoVendorreqchanges());
          }
          purchaseorder.setB2bpoVendorreqchanges(newReqChanges3.toString());

          OBDal.getInstance().flush();
          ActivityLogUtils.log(purchaseorder.getBusinessPartner().getADUserList().get(0),
              "b2bpo_activitylog_orderautoaccepted", purchaseorder);

        }
      }

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

  private static Date getDate(String strDate) {
    if (strDate.equals(""))
      return null;
    try {
      String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
          "dateFormat.java");
      SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
      return (outputFormat.parse(strDate));
    } catch (Exception e) {

      return null;
    }
  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

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
}
