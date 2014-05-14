package org.openbravo.b2bintegration.pomgmt;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.openbravo.b2bintegration.B2bOrgParameters;
import org.openbravo.b2bintegration.B2bVendorParameters;
import org.openbravo.b2bintegration.base.activitylog.ActivityLogUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * 
 */
public class CreateASN extends DalBaseProcess {
  BusinessPartner b2bvendor;
  B2bVendorParameters b2bvendorparams;
  B2bOrgParameters b2borgparams;
  DocumentType AsnDocType;
  private Date deliverydate;
  private Date dispatchdate;
  private String description;
  private Order purchaseorder;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String strdispatchdate = (String) bundle.getParams().get("dispatchdate");
    dispatchdate = getDate(strdispatchdate);
    final String strCOrderid = (String) bundle.getParams().get("C_Order_ID");
    purchaseorder = OBDal.getInstance().get(Order.class, strCOrderid);

    try {

      checkparams();
      ShipmentInOut AsnHeader = createShipmentHeader();
      addLines(AsnHeader);

      // If successful,
      ActivityLogUtils.log(OBContext.getOBContext().getUser(),
          "b2bpo_activitylog_ASNfromOrderCreated", AsnHeader);

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

  private void addLines(ShipmentInOut asnHeader) {
    for (OrderLine orderline : purchaseorder.getOrderLineList()) {
      AddSingleLine(orderline, asnHeader);
    }

  }

  private void AddSingleLine(OrderLine orderline, ShipmentInOut asnHeader) {
    final ShipmentInOutLine shipline = OBProvider.getInstance().get(ShipmentInOutLine.class);
    shipline.setClient(asnHeader.getClient());
    shipline.setOrganization(asnHeader.getOrganization());
    shipline.setShipmentReceipt(asnHeader);
    shipline.setCreatedBy(OBContext.getOBContext().getUser());
    shipline.setUpdatedBy(OBContext.getOBContext().getUser());

    shipline.setOrderQuantity(null);
    shipline.setSalesOrderLine(orderline);

    shipline.setLineNo(orderline.getLineNo());
    shipline.setProduct(orderline.getProduct());
    shipline.setUOM(orderline.getUOM());
    shipline.setMovementQuantity(orderline.getOrderQuantity());
    shipline.setStorageBin(b2bvendorparams.getStorageBin());
    shipline.setDescription(orderline.getDescription());

    OBDal.getInstance().save(shipline);
    OBDal.getInstance().flush();

  }

  private ShipmentInOut createShipmentHeader() {

    final ShipmentInOut ship = OBProvider.getInstance().get(ShipmentInOut.class);
    ship.setClient(purchaseorder.getClient());
    ship.setOrganization(purchaseorder.getOrganization());
    ship.setCreatedBy(OBContext.getOBContext().getUser());
    ship.setUpdatedBy(OBContext.getOBContext().getUser());

    ship.setBusinessPartner(purchaseorder.getBusinessPartner());
    ship.setPartnerAddress(purchaseorder.getPartnerAddress());
    ship.setDeliveryLocation(purchaseorder.getDeliveryLocation());

    ship.setMovementDate(purchaseorder.getScheduledDeliveryDate());
    ship.setAccountingDate(purchaseorder.getOrderDate());
    ship.setB2bvmiShippingdate(dispatchdate);

    ship.setSalesOrder(purchaseorder);
    ship.setOrderDate(purchaseorder.getOrderDate());

    ship.setOrderReference(purchaseorder.getOrderReference());
    ship.setDescription(purchaseorder.getDescription());

    ship.setWarehouse(b2bvendorparams.getStorageBin().getWarehouse());

    setDocTypesAndDocNo(ship);

    // Some defaults for goods receipt
    ship.setSalesTransaction(false);
    ship.setPosted("N");
    ship.setProcessed(false);
    ship.setProcessNow(false);
    ship.setMovementType("V+");
    ship.setDeliveryTerms("A");
    ship.setDeliveryMethod("P");
    ship.setFreightCostRule("I");
    ship.setPriority("5");

    ship.setB2bvmiShippingstatus("DR");

    OBContext.setAdminMode(true);
    OBDal.getInstance().save(ship);
    OBDal.getInstance().flush();
    OBContext.restorePreviousMode();

    return ship;
  }

  private void setDocTypesAndDocNo(ShipmentInOut ship) {

    OBContext.setAdminMode(true);
    Sequence seq = AsnDocType.getDocumentSequence();
    String prefix = seq.getPrefix() != null ? seq.getPrefix() : "";
    String suffix = seq.getSuffix() != null ? seq.getSuffix() : "";
    String DocumentNo = prefix + seq.getNextAssignedNumber().toString() + suffix;
    seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
    OBContext.restorePreviousMode();

    ship.setDocumentType(AsnDocType);
    ship.setDocumentNo(DocumentNo);

    ship.setDocumentStatus("DR");
    ship.setDocumentAction("CO");
  }

  private void checkparams() throws Exception {
    // Cheack that there is Bpartner and this bpartner is a B2B guy
    b2bvendor = OBDal.getInstance().get(BusinessPartner.class,
        OBContext.getOBContext().getUser().getB2bBpartnerid());

    if (b2bvendor == null) {
      throw new Exception("@b2bvmi_createasn_noBPfound@");
    }

    OBCriteria<B2bVendorParameters> obc = OBDal.getInstance().createCriteria(
        B2bVendorParameters.class);
    obc.add(Restrictions.eq(B2bVendorParameters.PROPERTY_BUSINESSPARTNER, b2bvendor));

    if (obc.list().isEmpty()) {
      throw new Exception("@b2bvmi_createasn_BPisnotB2B@");
    }

    b2bvendorparams = obc.list().get(0);

    OBCriteria<B2bOrgParameters> obc2 = OBDal.getInstance().createCriteria(B2bOrgParameters.class);
    obc.add(Restrictions.eq(B2bOrgParameters.PROPERTY_ORGANIZATION, b2bvendor.getOrganization()));

    if (obc2.list().isEmpty()) {
      throw new Exception("@b2bvmi_createasn_OrgNotConfigured@");
    }

    b2borgparams = obc2.list().get(0);

    // Check that a default Storage Bin exists
    if (b2bvendorparams.getStorageBin() == null) {
      throw new Exception("@b2bvmi_createasn_BPhasnoStorageBin@");
    }

    // Get default ASN document from vendorparams
    AsnDocType = b2bvendorparams.getDefaultAsnDoctype();
    if (AsnDocType == null) {
      AsnDocType = b2borgparams.getDefaultAsnDoctype();
      if ((AsnDocType == null) || (AsnDocType.getDocumentSequence() == null)) {
        throw new Exception("@b2bvmi_createasn_doctypenotconfigured@");
      }
    }

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
}
