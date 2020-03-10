package org.wirabumi.bprl.ad_process;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.costing.CostingAlgorithm;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.wirabumi.bprl.FIFOTransaction;


public class FIFOCosting extends CostingAlgorithm {

	@Override
	protected BigDecimal getOutgoingTransactionCost() {
		// trxType: Shipment, ReceiptNegative
		//TODO implement costDimension
		
		//tarik semua costing yang urut tanggal ascending, dengan field: m_costing_id, cost, dan availablable qty
		OBCriteria<Costing> costingC = OBDal.getInstance().createCriteria(Costing.class);
		costingC.add(Restrictions.eq(Costing.PROPERTY_PRODUCT, transaction.getProduct()));
		costingC.add(Restrictions.gt(Costing.PROPERTY_BPRLAVAILABLEQTY, BigDecimal.ZERO));
		costingC.add(Restrictions.le(Costing.PROPERTY_STARTINGDATE, transaction.getMovementDate()));
		costingC.addOrderBy(Costing.PROPERTY_STARTINGDATE, true);
		costingC.addOrderBy(Costing.PROPERTY_ID, true);
		//keep in mind, ini qty normalnya negatif sebab ini transaksi outgoing. oleh sebab itu dikali -1 (minus 1)
		BigDecimal transactionqty=transaction.getMovementQuantity().negate();
		BigDecimal akumulasitransactioncost = BigDecimal.ZERO;
		List<Costing>costingList = costingC.list();
		if (costingList==null || costingList.size()==0)
			return BigDecimal.ZERO; //ga ada costing batch yg tersedia, return zero
		
		int size = costingList.size();
		
		boolean qtycukup = cekQtyCukup(costingList, transactionqty.doubleValue());
		
		if (!qtycukup)
			return BigDecimal.ZERO;
		
		int i = 0;
		for (Costing costing : costingList){
			BigDecimal unitcost = costing.getCost();
			BigDecimal availableqty = costing.getBprlAvailableqty();
			BigDecimal sisa = availableqty.subtract(transactionqty);
			
			BigDecimal usedqty = null;
			if (sisa.compareTo(BigDecimal.ZERO)<0){
				usedqty=availableqty;
				costing.setBprlAvailableqty(BigDecimal.ZERO);
			} else {
				usedqty=transactionqty;
				costing.setBprlAvailableqty(sisa);
			}
			
			OBDal.getInstance().save(costing);
			OBDal.getInstance().flush();
			
			BigDecimal transactioncost = usedqty.multiply(unitcost);
			akumulasitransactioncost=akumulasitransactioncost.add(transactioncost);
			
			//insert FIFO transaction
			FIFOTransaction ft = OBProvider.getInstance().get(FIFOTransaction.class);
			ft.setOrganization(transaction.getOrganization());
			ft.setInventoryTransaction(transaction);
			ft.setCosting(costing);
			ft.setUnitCost(unitcost);
			ft.setQuantity(usedqty);
			ft.setCost(transactioncost);
			OBDal.getInstance().save(ft);
			
			i++;
			transactionqty=transactionqty.subtract(usedqty);
			
			if (i==size){
				if (sisa.compareTo(BigDecimal.ZERO)<0){
					//available qty is not enough
					throw new OBException("total available qty tidak cukup untuk product "+transaction.getProduct().getName()); 
				} else
					break; //artinya ada sisa, maka batch costing ini sudah cukup untuk melayani.
			}
			
			if (transactionqty.equals(BigDecimal.ZERO))
				break; //sudah selesai, tidak ada sisa, maka keluar.
				
		}
			
	    return akumulasitransactioncost;
		
	}
	
	private boolean cekQtyCukup(List<Costing> costingList, double trxQty) {
		double totalAvailableQty=0;
		for (Costing costing : costingList){
			BigDecimal available = costing.getBprlAvailableqty();
			if (available==null)
				continue;
			if (available.equals(BigDecimal.ZERO))
				continue;
			totalAvailableQty += available.doubleValue();
		}
		
		if (trxQty>totalAvailableQty)
			return false;
		else
			return true;
	}

	public BigDecimal getTransactionCost() throws OBException {
	    log4j.debug("Get transactions cost.");
	    if (transaction.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0
	        && getZeroMovementQtyCost() != null) {
	      return getZeroMovementQtyCost();
	    }
	    BigDecimal transactioncost = BigDecimal.ZERO;
	    switch (trxType) {
	    case Receipt:
	    	transactioncost = getReceiptCost();
	    	break;
	    case ShipmentNegative:
	    	transactioncost = getShipmentNegativeCost();
	    	break;
	    case Shipment:
	    	transactioncost = getOutgoingTransactionCost();
	    	break;
	    case ReceiptNegative:
	    	transactioncost = getReceiptNegativeCost();
	    	break;
	    case InventoryIncrease:
	    	transactioncost = getInventoryIncreaseCost();
	    	break;
	    case InventoryDecrease:
	    	transactioncost = getInventoryDecreaseCost();
	    	break;
	    case ShipmentReturn: //not implemented yet
//	      return getShipmentReturnCost();
	    case ShipmentVoid: //not implemented yet
//	      return getShipmentVoidCost();
	    case ReceiptReturn: //not implemented yet
//	      return getReceiptReturnCost();
	    case ReceiptVoid: //not implemented yet
//	      return getReceiptNegativeCost();
	    case InventoryOpening: //not implemented yet
//	      return getInventoryOpeningCost();
	    case InventoryClosing: //not implemented yet
//	      return getInventoryClosingCost();
	    case IntMovementFrom: //not implemenented yet
//	      return getIntMovementFromCost();
	    case IntMovementTo: //not implemented yet
//	      return getIntMovementToCost();
	    case InternalCons: //not implemented yet
//	      return getInternalConsCost();
	    case InternalConsNegative: //not implemented yet
//	      return getInternalConsNegativeCost();
	    case InternalConsVoid: //not implemented yet
//	      return getInternalConsVoidCost();
	    case BOMPart: //not implemented yet
//	      return getBOMPartCost();
	    case BOMProduct: //not implemented yet
//	      return getBOMProductCost();
	    case ManufacturingConsumed: //not implemented yet
	      // Manufacturing transactions are not fully implemented.
//	      return getManufacturingConsumedCost();
	    case ManufacturingProduced: //not implemented yet
	      // Manufacturing transactions are not fully implemented.
//	      return getManufacturingProducedCost();
	    case Unknown:
	      throw new OBException("@UnknownTrxType@: " + transaction.getIdentifier());
	    default:
	      throw new OBException("@UnknownTrxType@: " + transaction.getIdentifier());
	    }
	    
	    if (transactioncost.equals(BigDecimal.ZERO)){
			//sudah dicoba untuk hitung, tapi karena sesuatu hal, gagal mendapatkan batch FIFO costing yang sesuai kriteria
			//oleh karena itu costing status dinyatakan P (pending) 
			transaction.setCostingStatus("P");
			OBDal.getInstance().save(transaction);
		} else {
			//transaction costing kan dijalankan pakai background process, dan record ini sebelumnya punya status pending
			//dan karena sudah berhasil sampai sini, maka dia tidak pending lagi
			//di sini, costing status diset menjadi NC (not calculated),
			//dan nanti oleh costing server akan diubah menjadi CC (cost calculated)
			transaction.setCostingStatus("NC");
			OBDal.getInstance().save(transaction);
		}
	    
	    return transactioncost;
	}
	
	@Override
	protected BigDecimal getShipmentNegativeCost() {
		
		//1. pastikan sudah ada return ID
		ShipmentInOutLine ioLine = transaction.getGoodsShipmentLine();
		ShipmentInOutLine ioLineReturn = ioLine.getCanceledInoutLine();
		if (ioLineReturn==null)
			throw new OBException("receipt negative transaction without return ID for m_inoutline_id "+ioLine.getId());
		
		List<MaterialTransaction> mTransList = ioLineReturn.getMaterialMgmtMaterialTransactionList();
		if (mTransList==null)
			throw new OBException("invalid material transaction behaviour. goods shipment line has no material transaction record. m_inoutline_id "+ioLine.getId());
		if (mTransList.size()>1)
			throw new OBException("invalid material transaction behaviour. goods shipment line linked to 2 or more material transaction record. m_inoutline_id "+ioLine.getId());
		
		//2. dari return id, dapat fifo_transaction, maka dapat transaction cost untuk di return
		MaterialTransaction mTrans = mTransList.get(0);
		if (mTrans.getCostingStatus().equals("P"))
			return BigDecimal.ZERO; //dokumen yg diretur masih pending transaction cost-nya, sehingga dokumen ini juga mengikuti diset pending
		
		BigDecimal mTransCost = mTrans.getTransactionCost();
		BigDecimal mTransQTY = mTrans.getMovementQuantity().negate(); //normaly positive
		
		//3. unit cost didapatkan dari list material transaction terhubung ke 1 atau lebih fifo_transaction
		for (FIFOTransaction ft : mTrans.getBprlFifotransactionList()){
			Costing costing = ft.getCosting();
			BigDecimal unitcost = ft.getUnitCost();
			BigDecimal qty = ft.getQuantity();
			
			//5. buat fifo_transaction record dengan qty negatif
			FIFOTransaction ftReturn = OBProvider.getInstance().get(FIFOTransaction.class);
			ftReturn.setOrganization(transaction.getOrganization());
			ftReturn.setInventoryTransaction(transaction);
			ftReturn.setCosting(costing);
			ftReturn.setUnitCost(unitcost);
			
			if (qty.compareTo(mTransQTY)>=0){
				//bisa diretur semua
				ftReturn.setQuantity(mTransQTY.negate()); //dinegatifkan karena ini retur
				BigDecimal transCost = mTransQTY.multiply(unitcost);
				ftReturn.setCost(transCost);
				
				//4. tambahkan available qty sejumlah qty return
				costing.setBprlAvailableqty(costing.getBprlAvailableqty().add(mTransQTY));
				
			} else {
				//retur sebagian
				mTransQTY = mTransQTY.subtract(qty);
				ftReturn.setQuantity(qty.negate()); //dinegatifkan karena ini retur
				BigDecimal transCost = qty.multiply(unitcost);
				ftReturn.setCost(transCost);
				
				//4. tambahkan available qty sejumlah qty return
				costing.setBprlAvailableqty(costing.getBprlAvailableqty().add(qty));
			}
			
			OBDal.getInstance().save(ftReturn);
			OBDal.getInstance().save(costing);
			
			if (mTransQTY.equals(BigDecimal.ZERO))
				break;
			
		}
		
		return mTransCost;
	}
	
	@Override
	protected BigDecimal getReceiptNegativeCost() {
		BigDecimal qtyReturn = transaction.getMovementQuantity().negate();
		ShipmentInOutLine ioLine = transaction.getGoodsShipmentLine();
		ShipmentInOutLine ioLineReturn = ioLine.getCanceledInoutLine();
		if (ioLineReturn==null)
			throw new OBException("receipt negative transaction without return ID for m_inoutline_id "+ioLine.getId());
		
		List<Costing> costingList = ioLineReturn.getMaterialMgmtCostingList();
		if (costingList==null)
			throw new OBException("invalid costing behaviour. goods receipt line has no FIFO costing record. m_inoutline_id "+ioLine.getId());
		if (costingList.size()>1)
			throw new OBException("invalid costing behaviour. goods receipt line linked to 2 or more FIFO costing record. m_inoutline_id "+ioLine.getId());
		
		Costing costing = costingList.get(0);
		//avaialable qty dikembalikan dari besar mejadi kecil kembali.
		BigDecimal qty = costing.getBprlAvailableqty();
		costing.setBprlAvailableqty(qty.subtract(qtyReturn));
		OBDal.getInstance().save(costing);
		
		//buat FIFO transaction record
		FIFOTransaction ftReturn = OBProvider.getInstance().get(FIFOTransaction.class);
		ftReturn.setOrganization(transaction.getOrganization());
		ftReturn.setInventoryTransaction(transaction);
		ftReturn.setCosting(costing);
		BigDecimal unitcost = costing.getCost();
		ftReturn.setUnitCost(unitcost);
		ftReturn.setQuantity(qtyReturn);
		BigDecimal transCost = qtyReturn.multiply(unitcost);
		ftReturn.setCost(transCost);
		OBDal.getInstance().save(ftReturn);
		
		return transCost;
	}
	
	@Override
	protected BigDecimal getInventoryIncreaseCost() {
		BigDecimal transactionCost = super.getInventoryIncreaseCost();
		
		//insert FIFO costing batch
		Product product = transaction.getProduct();
		BigDecimal qty = transaction.getMovementQuantity();
		BigDecimal unitcost = transactionCost.divide(qty);
		Costing costing = OBProvider.getInstance().get(Costing.class);
		costing.setOrganization(product.getOrganization());
		costing.setStartingDate(transaction.getMovementDate());
		costing.setCost(unitcost);
		costing.setCostType("BPRL_FIFO");
		costing.setBprlAvailableqty(qty);
		costing.setQuantity(qty);
		costing.setProduct(product);
		costing.setInventoryTransaction(transaction);
		OBDal.getInstance().save(costing);
		
		return transactionCost;
	}
	
	@Override
	protected BigDecimal getReceiptCost(){
		BigDecimal transactionCost = super.getReceiptCost();
		
		//insert FIFO costing batch
		Product product = transaction.getProduct();
		BigDecimal qty = transaction.getMovementQuantity();
		BigDecimal unitcost = transactionCost.divide(qty);
		Costing costing = OBProvider.getInstance().get(Costing.class);
		costing.setOrganization(product.getOrganization());
		costing.setStartingDate(transaction.getMovementDate());
		costing.setCost(unitcost);
		costing.setCostType("BPRL_FIFO");
		costing.setBprlAvailableqty(qty);
		costing.setQuantity(qty);
		costing.setProduct(product);
		costing.setGoodsShipmentLine(transaction.getGoodsShipmentLine());
		costing.setInventoryTransaction(transaction);
		OBDal.getInstance().save(costing);
		
		return transactionCost;
	}
	
	
}
