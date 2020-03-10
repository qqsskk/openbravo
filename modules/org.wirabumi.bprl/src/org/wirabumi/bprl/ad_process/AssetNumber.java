package org.wirabumi.bprl.ad_process;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class AssetNumber extends DalBaseProcess {

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		OBCriteria<Asset> assetC = OBDal.getInstance().createCriteria(Asset.class);
//		assetC.addOrderBy(Asset.PROPERTY_PURCHASEDATE, true);
		assetC.addOrderBy(Asset.PROPERTY_NAME, true);
		List<Asset> assetList = assetC.list();
		
		updateDuplicatedAssetName(assetList);
		
//		generateduplicatedasset(assetList);
//		updateAssetKey(assetList, true);

	}
	
	private void updateDuplicatedAssetName(List<Asset> assetList) {
		String oldAssetName="";
		int seqno=1;
		for (Asset asset : assetList){
			String currentAssetName=asset.getName();
			if (!currentAssetName.equalsIgnoreCase(oldAssetName)){
				seqno=1;
				oldAssetName=currentAssetName.toString();
			}
			else{
				asset.setName(oldAssetName+" "+seqno);
				seqno++;
				OBDal.getInstance().save(asset);
			}
		}
		
		OBDal.getInstance().commitAndClose();
	}

	public void updateAssetKey(List<Asset> assetList, boolean isDataMigration){
		Integer seqnoINV=10000;
		Integer seqnoBRG=20000;
		String parentGroup;
		int bulan=0, tahun=0;
		for (Asset asset:assetList){
			StringBuilder sb = new StringBuilder();
			if (asset.isBprlIsinventaris()==false)
				parentGroup = "BRG";
			else
				parentGroup = "INV";
			
			sb.append(parentGroup).append("/");
			
			double tarifdepresiasi=0;
			String depreciationType=asset.getDepreciationType();
			if (asset.getAnnualDepreciation()!=null)
					tarifdepresiasi=asset.getAnnualDepreciation().doubleValue();
			if (tarifdepresiasi==50)
				sb.append("A");
			else if (tarifdepresiasi==25)
				sb.append("B");
			else if (tarifdepresiasi==20)
				sb.append("C");
			else if (tarifdepresiasi==0)
				sb.append("D");
			if (!depreciationType.equalsIgnoreCase("CAM_DOUBLEDECLINING"))
				sb.append("E");
			if (asset.getAssetCategory()!=null)
				sb.append("/").append(asset.getAssetCategory().getName()).append("/");
			Date purchasedate = asset.getPurchaseDate();
			if (purchasedate!=null){
				SimpleDateFormat df = new SimpleDateFormat("MMyy");
				sb.append(df.format(purchasedate)).append("/");
				Calendar cal = Calendar.getInstance();
				cal.setTime(purchasedate);
				int bulan2 = cal.get(Calendar.MONTH);
				int tahun2 = cal.get(Calendar.YEAR);
				if (bulan2!=bulan || tahun2!=tahun)
					if (parentGroup.equalsIgnoreCase("INV"))
						seqnoINV=1000;
					else
						seqnoBRG=2000;
				
				if (parentGroup.equalsIgnoreCase("INV"))
					seqnoINV++;
				else
					seqnoBRG++;
				
				bulan=bulan2; tahun=tahun2;
				
				if (isDataMigration){
					if (parentGroup.equalsIgnoreCase("INV"))
						sb.append(seqnoINV.toString());
					else
						sb.append(seqnoBRG.toString());
				} else {
					//ambil max document no dengan bulan dan tahun sesuai bulan2 dan tahun2
					OBCriteria<Asset> assetC2 = OBDal.getInstance().createCriteria(Asset.class);
					cal.set(Calendar.DAY_OF_MONTH, 1);
					cal.set(Calendar.MONTH, bulan);
					cal.set(Calendar.YEAR, tahun);
					Date startdate=cal.getTime();
					cal.add(Calendar.MONTH, 1);
					Date enddate = cal.getTime();
					assetC2.add(Restrictions.ge(Asset.PROPERTY_PURCHASEDATE, startdate));
					assetC2.add(Restrictions.le(Asset.PROPERTY_PURCHASEDATE, enddate));
					assetC2.add(Restrictions.like(Asset.PROPERTY_SEARCHKEY, parentGroup));
					assetC2.addOrderBy(Asset.PROPERTY_DOCUMENTNO, false);
					List<Asset> assetList2 = assetC2.list();
					if (assetList2.size()>0){
						String lastdocumentno = assetList2.get(0).getDocumentNo();
						if (lastdocumentno!=null && !lastdocumentno.isEmpty()){
							int lastdocumentno_i = Integer.parseInt(lastdocumentno);
							lastdocumentno_i++;
							if (parentGroup.equalsIgnoreCase("INV"))
								seqnoINV=lastdocumentno_i;
							else
								seqnoBRG=lastdocumentno_i;
						}
					} else {
						if (parentGroup.equalsIgnoreCase("INV"))
							seqnoINV=1000;
						else
							seqnoBRG=2000;
					}
				}	
			}
	
			String documentno = sb.toString();
			if (documentno.length()>=40)
				continue;
			asset.setSearchKey(documentno);
			if (parentGroup.equalsIgnoreCase("INV"))
				asset.setDocumentNo(seqnoINV.toString());
			else
				asset.setDocumentNo(seqnoBRG.toString());
			
			OBDal.getInstance().save(asset);
		}
		
		OBDal.getInstance().commitAndClose();
		
	}
	
	public void getNewAssetNumber(Asset asset, StringBuilder assetKey, StringBuilder documentNo){

		Integer seqnoINV=1000;
		Integer seqnoBRG=2000;
		String parentGroup;
		int bulan=0, tahun=0;
		StringBuilder sb = new StringBuilder();
		if (asset.isBprlIsinventaris())
			parentGroup="INV";
		else
			parentGroup="BRG";
			
		sb.append(parentGroup).append("/");
		
		double tarifdepresiasi=0;
		String depreciationType=asset.getDepreciationType();
		if (asset.getAnnualDepreciation()!=null)
				tarifdepresiasi=asset.getAnnualDepreciation().doubleValue();
		if (tarifdepresiasi==50)
			sb.append("A");
		else if (tarifdepresiasi==25)
			sb.append("B");
		else if (tarifdepresiasi==20)
			sb.append("C");
		else if (tarifdepresiasi==0)
			sb.append("D");
		if (!depreciationType.equalsIgnoreCase("CAM_DOUBLEDECLINING"))
			sb.append("E");
		if (asset.getAssetCategory()!=null)
			sb.append("/").append(asset.getAssetCategory().getName()).append("/");
		Date purchasedate = asset.getPurchaseDate();
		if (purchasedate!=null){
			SimpleDateFormat df = new SimpleDateFormat("MMyy");
			sb.append(df.format(purchasedate)).append("/");
			Calendar cal = Calendar.getInstance();
			cal.setTime(purchasedate);
			int bulan2 = cal.get(Calendar.MONTH);
			int tahun2 = cal.get(Calendar.YEAR);
			if (bulan2!=bulan || tahun2!=tahun)
				if (parentGroup.equalsIgnoreCase("INV"))
					seqnoINV=1000;
				else
					seqnoBRG=2000;
			
			if (parentGroup.equalsIgnoreCase("INV"))
				seqnoINV++;
			else
				seqnoBRG++;
			
			bulan=bulan2; tahun=tahun2;
			
			//ambil max document no dengan bulan dan tahun sesuai bulan2 dan tahun2
			OBCriteria<Asset> assetC2 = OBDal.getInstance().createCriteria(Asset.class);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.MONTH, bulan);
			cal.set(Calendar.YEAR, tahun);
			Date startdate=cal.getTime();
			cal.add(Calendar.MONTH, 1);
			Date enddate = cal.getTime();
			assetC2.add(Restrictions.ge(Asset.PROPERTY_PURCHASEDATE, startdate));
			assetC2.add(Restrictions.le(Asset.PROPERTY_PURCHASEDATE, enddate));
			assetC2.add(Restrictions.like(Asset.PROPERTY_SEARCHKEY, parentGroup+"%"));
			assetC2.addOrderBy(Asset.PROPERTY_DOCUMENTNO, false);
			List<Asset> assetList2 = assetC2.list();
			if (assetList2.size()>0){
				String lastdocumentno = assetList2.get(0).getDocumentNo();
				if (lastdocumentno!=null && !lastdocumentno.isEmpty()){
					int lastdocumentno_i = Integer.parseInt(lastdocumentno);
					lastdocumentno_i++;
					if (parentGroup.equalsIgnoreCase("INV"))
						seqnoINV=lastdocumentno_i;
					else
						seqnoBRG=lastdocumentno_i;
				}
			} else {
				if (parentGroup.equalsIgnoreCase("INV"))
					seqnoINV=1000;
				else
					seqnoBRG=2000;
			}
		}
		
		if (parentGroup.equalsIgnoreCase("INV"))
			sb.append(seqnoINV.toString());
		else
			sb.append(seqnoBRG.toString());

		String assetkey = sb.toString();
		if (assetkey.length()>=40){
			assetKey=null;
			documentNo=null;
			return;
		}
		
		if (assetKey==null || documentNo==null)
			return;
		
		assetKey.append(assetkey);
		
		if (parentGroup.equalsIgnoreCase("INV"))
			documentNo.append(seqnoINV.toString());
		else
			documentNo.append(seqnoBRG.toString());
		
	}

	private void generateduplicatedasset(List<Asset> assetList){
		for (Asset asset : assetList){
			BigDecimal i = asset.getQuantity();
			if (i==null || i.compareTo(BigDecimal.ZERO)==0)
				continue;
			int j = i.intValue();
			for (int k=0; k<j-1; k++){
				Asset assetcopy=(Asset)DalUtil.copy(asset, false, true);
				StringBuilder assetKey=new StringBuilder();
				StringBuilder documentNo=new StringBuilder();
				getNewAssetNumber(assetcopy, assetKey, documentNo);
				assetcopy.setSearchKey(assetKey.toString());
				assetcopy.setDocumentNo(documentNo.toString());
				OBDal.getInstance().save(assetcopy);
			}
		}
		
		OBDal.getInstance().commitAndClose();
	}
}