package org.wirabumi.bprl.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.financialmgmt.assetmgmt.Amortization;
import org.openbravo.model.financialmgmt.assetmgmt.AmortizationLine;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.wirabumi.cam.WorkOrder;
import org.wirabumi.cam.WorkOrderAsset;
import org.wirabumi.gen.oez.event.DocumentRoutingHandlerAction;

public class WorkOrderCompleteHandlerAction extends DocumentRoutingHandlerAction {
	private final String complete="co";
	private final String reactive="re";

  @Override
  public void doRouting(String adWindowId, String adTabId, String doc_status_to,
      VariablesSecureApp vars, List<String> recordId) {
	  
	  //loop for each record id
	  for (String recordID : recordId){
		  WorkOrder workOrder = OBDal.getInstance().get(WorkOrder.class, recordID);
		  if (workOrder==null)
			  throw new OBException(recordID+" is not valid asset ID");
		  
		  Date workOrderDate = workOrder.getStartingDate();
		  
		  if (doc_status_to.equalsIgnoreCase(complete)){//doc action to = CO maka doComplete
			  for (WorkOrderAsset woAsset : workOrder.getCamWorkorderassetList()){
				  Asset asset = woAsset.getAsset();
				  if (woAsset.isDisposed()){//case 1: asset disposal
					  //business process:
					  //1. aset di non aktifkan.
					  asset.setActive(false);
					  
					  //2. pending depresiasi dihapus.
					  Amortization latestValidAmortization = null;
					  List<AmortizationLine> amortizationLineToBeRemoved = new ArrayList<AmortizationLine>();
					  for (AmortizationLine amortizationline : asset.getFinancialMgmtAmortizationLineList()){
						  Amortization amortization = amortizationline.getAmortization();
						  if (!amortization.isProcessNow()){
							  amortizationLineToBeRemoved.add(amortizationline);
						  }
						  Date amortizationStartDate = amortization.getStartingDate();
						  Date amortizationEndDate = amortization.getEndingDate();
						  if (workOrderDate.after(amortizationStartDate) && amortizationEndDate.after(workOrderDate)){
							  latestValidAmortization=OBDal.getInstance().get(Amortization.class, amortization.getId());
						  }
					  }
					  deletePendingAmortizationLine(amortizationLineToBeRemoved);
					  OBDal.getInstance().flush();
					  OBDal.getInstance().refresh(asset);
					  
					  //3. dibentuk record amortisasi terakhir, yang otomatis menjurnal habis sisa akumulasi depresiasi amortisasi.
					  if (latestValidAmortization==null)
						  throw new OBException("can not find valid latest amortization header for asset "+asset.getName()+" on "+workOrderDate);
					  BigDecimal depreciatedamount = asset.getDepreciatedValue();
					  BigDecimal deprecicationplan = asset.getDepreciationAmt();
					  double pendingamortization = deprecicationplan.doubleValue()-depreciatedamount.doubleValue();
					  double pendingamortizationpercent = pendingamortization/deprecicationplan.doubleValue()*100.00;
					  AmortizationLine latestValidAmortizationLine = OBProvider.getInstance().get(AmortizationLine.class);
					  latestValidAmortizationLine.setAmortization(latestValidAmortization);
					  latestValidAmortizationLine.setAsset(asset);
					  latestValidAmortizationLine.setAmortizationAmount(new BigDecimal(pendingamortization));
					  latestValidAmortizationLine.setAmortizationPercentage(new BigDecimal(pendingamortizationpercent));
					  OBDal.getInstance().save(latestValidAmortizationLine);
					  
					  //TODO 4. dibentuk record gl journal untuk membalik akumulasi depresiasi pada aktiva tetap. 
				  }
				  
				  //case 7: asset opname atau case 2: pemindahan aset 
				  if ((woAsset.isAssetOpname())||
						  (woAsset.isAssetmovement() &&
								  (!asset.getCamLocation().getId().equalsIgnoreCase(woAsset.getAssetLocation().getId()) ||
										  !asset.getCamCostcenter().getId().equalsIgnoreCase(woAsset.getCostCenter().getId())))){  
					  
					  //business process:
					  //1. pindahkan location
					  if (woAsset.getAssetLocation()!=null)
						  asset.setCamLocation(woAsset.getAssetLocation());
					  
					  if (woAsset.getCostCenter()!=null){
						  //2. pindahkan cost center
						  asset.setCamCostcenter(woAsset.getCostCenter());
						
						  //3. pindahkan unprocessed amortizationline to new cost center
						  for (AmortizationLine amortizationline : asset.getFinancialMgmtAmortizationLineList()){
							  Amortization amortization = amortizationline.getAmortization();
							  if (!amortization.isProcessNow()){
								  amortizationline.setCostcenter(woAsset.getCostCenter());
							  }
						  }
						  
					  }
					  
					  //TODO 4. buat record gl journal untuk pemindahan asset
					  
				  }
				  
				  //TODO case 3: penjualan aset
				  //TODO case 4: absorbtion cost dari internal consumption ke asset
				  //TODO case 5: absorbtion cost dari dari PI ke asset
				  //TODO case 6: absorbtion cost dari gaji ke asset
				  
			  }
			  
		  } else if (doc_status_to.equalsIgnoreCase(reactive)){//doc action to = RE maka dorReactive
			  for (WorkOrderAsset woAsset : workOrder.getCamWorkorderassetList()){
				  //case 1: asset disposal >> tidak bisa di reactive
				  if (woAsset.isDisposed())
					  throw new OBException("work order contain asset disposal, then can not be reactived");
				  
				  //case 2: pemindahan aset >> tidak bisa di reactive
				  if (woAsset.isAssetmovement())
					  throw new OBException("work order contain asset movement, then can not be reactived");
				  
				  //case 7: pemindahan aset >> tidak bisa di reactive
				  if (woAsset.isAssetOpname())
					  throw new OBException("work order contain asset opname, then can not be reactived");
			  }
			  
			  //TODO case 3: penjualan aset >> tidak bisa di reactive
			  //TODO case 4: absorbtion cost dari internal consumption ke asset
			  //TODO case 5: absorbtion cost dari dari PI ke asset
			  //TODO case 6: absorbtion cost dari gaji ke asset
		  }
	  }
	
  }

  private void deletePendingAmortizationLine(
		List<AmortizationLine> amortizationLineToBeRemoved) {
	for (AmortizationLine amortizationLine : amortizationLineToBeRemoved){
		OBDal.getInstance().remove(amortizationLine);
	}
	
}

@Override
  public Boolean updateDocumentStatus(Entity entity, List<String> RecordId, String document_status_to,
      String column) {
    return super.updateDocumentStatus(entity, RecordId, document_status_to, column);
  }

  @Override
  public String getCoDocumentNo(String record, Tab tab) {
    // do nothing, cuma activate/deactivate pada kolom isactive
    return null;
  }

}
