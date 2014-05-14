package org.openbravo.b2bintegration.base.mdm.product;

import org.hibernate.criterion.Restrictions;
import org.openbravo.b2bintegration.B2bGlobalParameters;
import org.openbravo.b2bintegration.B2bOrgParameters;
import org.openbravo.b2bintegration.B2bVendorParameters;
import org.openbravo.b2bintegration.base.activitylog.ActivityLogUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * 
 */
public class AddVendorProduct extends DalBaseProcess {
  private static B2bVendorParameters b2bvendorparams;
  private static B2bOrgParameters b2borgparams;
  private static B2bGlobalParameters b2bglobalparams;
  private BusinessPartner b2bpartner;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Retrieve Parameters

    final String name = (String) bundle.getParams().get("name");
    final String description = (String) bundle.getParams().get("description");
    final String emB2bmdmVendorname = (String) bundle.getParams().get("emB2bmdmVendorname");
    final String emB2bmdmVendorcode = (String) bundle.getParams().get("emB2bmdmVendorcode");
    final String cTaxcategoryId = (String) bundle.getParams().get("cTaxcategoryId");
    final String cUomId = (String) bundle.getParams().get("cUomId");
    final String M_Product_Id = (String) bundle.getParams().get("M_Product_ID");

    try {

      b2bpartner = OBDal.getInstance().get(BusinessPartner.class,
          OBContext.getOBContext().getUser().getB2bBpartnerid());

      if (b2bpartner == null) {
        throw new Exception("@b2bvmi_createasn_noBPfound@");
      }

      String defaultConf = getDefaults();

      Product newproduct = setNewProduct(defaultConf, name, description, emB2bmdmVendorname,
          emB2bmdmVendorcode, cTaxcategoryId, cUomId);

      // Save all new created object
      OBContext.getOBContext().setAdminMode();
      OBDal.getInstance().save(newproduct);
      OBDal.getInstance().flush();
      OBContext.getOBContext().restorePreviousMode();

      // If successful,
      ActivityLogUtils.log(OBContext.getOBContext().getUser(), "b2bmdm_activitylog_productcreated",
          newproduct);

      OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("@Success@");
      String messagestring = Utility.messageBD(bundle.getConnection(),
          "b2bmdm_addvendorproduct_success", bundle.getContext().getLanguage());

      messagestring = messagestring.replace("@productname@ ", name);

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

  private Product setNewProduct(String defaultConf, String name, String description,
      String emB2bmdmVendorname, String emB2bmdmVendorcode, String cTaxcategoryId, String cUomId) {

    String searchkey = getSearchKey(defaultConf);
    ProductCategory productcategory = getprodcategory(defaultConf);

    final Product newProduct = OBProvider.getInstance().get(Product.class);

    newProduct.setClient(b2bpartner.getClient());
    newProduct.setOrganization(b2bpartner.getOrganization());
    newProduct.setSearchKey(searchkey);
    newProduct.setName(name);
    newProduct.setDescription(description);

    newProduct.setB2bmdmVendor(b2bpartner);
    newProduct.setB2bmdmVendorcode(emB2bmdmVendorcode);
    newProduct.setB2bmdmVendorname(emB2bmdmVendorname);

    newProduct.setUOM(OBDal.getInstance().get(UOM.class, cUomId));
    newProduct.setTaxCategory(OBDal.getInstance().get(TaxCategory.class, cTaxcategoryId));

    newProduct.setProductCategory(productcategory);

    newProduct.setPurchase(true);
    newProduct.setStocked(true);
    newProduct.setProductType("I");

    return newProduct;
  }

  private String getSearchKey(String defaultConf) {
    Sequence seq;

    if (defaultConf.equals("Vendor")) {
      seq = b2bvendorparams.getDefaultB2BVendorProductSequenceForSearchKey();
    } else if (defaultConf.equals("Vendor")) {
      seq = b2borgparams.getDefaultB2BVendorProductSequenceForSearchKey();
    } else {
      seq = b2bglobalparams.getDefaultproductvalueseq();
    }

    OBContext.setAdminMode(true);
    String prefix = seq.getPrefix() != null ? seq.getPrefix() : "";
    String suffix = seq.getSuffix() != null ? seq.getSuffix() : "";
    String returnsearchkey = prefix + seq.getNextAssignedNumber() + suffix;
    seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
    OBContext.restorePreviousMode();

    return returnsearchkey;
  }

  private ProductCategory getprodcategory(String defaultConf) {
    if (defaultConf.equals("Vendor")) {
      return b2bvendorparams.getDefaultB2BVendorProductCategory();
    } else if (defaultConf.equals("Vendor")) {
      return b2borgparams.getDefaultB2BVendorProductCategory();
    } else {
      return b2bglobalparams.getDefaultproductcategory();
    }
  }

  private String getDefaults() throws Exception {
    if (getVendorDefaults()) {
      return "Vendor";
    } else if (getOrgDefaults()) {
      return "Org";
    } else if (getGlobalDefaults()) {
      return "Global";
    } else {
      throw new Exception("@b2bmdm_addvendorproduct_noconf@");
    }

  }

  private Boolean getGlobalDefaults() {
    OBCriteria<B2bGlobalParameters> obc = OBDal.getInstance().createCriteria(
        B2bGlobalParameters.class);

    b2bglobalparams = obc.list().get(0);
    if (b2bglobalparams.getDefaultproductcategory() == null
        || b2bglobalparams.getDefaultproductvalueseq() == null) {
      return false;
    } else {

      return true;
    }
  }

  private Boolean getOrgDefaults() {
    OBCriteria<B2bOrgParameters> obc = OBDal.getInstance().createCriteria(B2bOrgParameters.class);
    obc.add(Restrictions.eq(B2bOrgParameters.PROPERTY_ORGANIZATION, b2bpartner.getOrganization()));

    b2borgparams = obc.list().get(0);
    if (b2borgparams.getDefaultB2BVendorProductCategory() == null
        || b2borgparams.getDefaultB2BVendorProductSequenceForSearchKey() == null) {
      return false;
    } else {

      return true;
    }
  }

  private Boolean getVendorDefaults() {
    OBCriteria<B2bVendorParameters> obc = OBDal.getInstance().createCriteria(
        B2bVendorParameters.class);
    obc.add(Restrictions.eq(B2bVendorParameters.PROPERTY_BUSINESSPARTNER, b2bpartner));

    b2bvendorparams = obc.list().get(0);
    if (b2bvendorparams.getDefaultB2BVendorProductCategory() == null
        || b2bvendorparams.getDefaultB2BVendorProductSequenceForSearchKey() == null) {
      return false;
    } else {

      return true;
    }
  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

  }
}
