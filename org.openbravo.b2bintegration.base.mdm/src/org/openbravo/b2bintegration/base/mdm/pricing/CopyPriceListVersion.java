package org.openbravo.b2bintegration.base.mdm.pricing;

import org.openbravo.b2bintegration.B2bGlobalParameters;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * 
 */
public class CopyPriceListVersion extends DalBaseProcess {
  private static B2bGlobalParameters b2bglobalparams;
  private static BusinessPartner SupplyChainManager;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String strMPricelistVersionId = (String) bundle.getParams().get("M_PriceList_Version_ID");
    final String newPriceListVersionName = (String) bundle.getParams().get(
        "newpricelistversionname");
    final PriceListVersion sourcePriceListVersion = OBDal.getInstance().get(PriceListVersion.class,
        strMPricelistVersionId);

    try {

      final PriceListVersion targetPriceListVersion = (PriceListVersion) DalUtil.copy(
          sourcePriceListVersion, true, true);

      targetPriceListVersion.setName(newPriceListVersionName);

      // Save all new created object
      OBDal.getInstance().save(targetPriceListVersion);

      // If successful,

      OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      String messagestring = Utility.messageBD(bundle.getConnection(),
          "b2bmdm_pricelistcreate_success", bundle.getContext().getLanguage());

      messagestring = messagestring.replace("@pricelistversionname@", newPriceListVersionName);

      msg.setMessage(messagestring);
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
