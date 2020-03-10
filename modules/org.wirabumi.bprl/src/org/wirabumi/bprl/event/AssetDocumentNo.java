package org.wirabumi.bprl.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.wirabumi.bprl.ad_process.AssetNumber;

public class AssetDocumentNo extends EntityPersistenceEventObserver {
	private static Entity[] entities = { ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME) };
	  protected Logger logger = Logger.getLogger(this.getClass());

	@Override
	protected Entity[] getObservedEntities() {
		return entities;
	}
	
	public void onSave(@Observes EntityNewEvent event) {
		final Entity assetEntity = ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME);
		final Property assetKeyProperty = assetEntity.getProperty(Asset.PROPERTY_SEARCHKEY);
		final Property assetNoProperty = assetEntity.getProperty(Asset.PROPERTY_DOCUMENTNO);
	    if (!isValidEvent(event)) {
	      return;
	    }
	    Asset asset = (Asset) event.getTargetInstance();
	    logger.info("Asset " + (asset.getName() + " is being created"));
	    AssetNumber assetNumber = new AssetNumber();
	    StringBuilder assetnumber = new StringBuilder(), documentno = new StringBuilder();
	    assetNumber.getNewAssetNumber(asset, assetnumber, documentno);
	    event.setCurrentState(assetKeyProperty, assetnumber.toString());
	    event.setCurrentState(assetNoProperty, documentno.toString());
	    
	  }

}
