package org.wirabumi.bprl.event;

import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

public class ShipmentEventHandler extends EntityPersistenceEventObserver {
	/*
	 * buatlah business event handler untuk event:
	 * 1.before update
	 * 2.before delte
	 * 
	 * dengan logika:
	 * 	jika sudah release 3, tidak bisa di hapus/edit selain orang umum
	 * 
	 */
	
	private static Entity[] entities = { ModelProvider.getInstance().getEntity(ShipmentInOut.ENTITY_NAME) };
	protected Logger logger = Logger.getLogger(this.getClass());

	@Override
	protected Entity[] getObservedEntities() {
		return entities;
	}
	
	public void onUpdate(@Observes EntityUpdateEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		
		final Entity inoutEntity = ModelProvider.getInstance().getEntity(ShipmentInOut.ENTITY_NAME);
		final Property docStausProperty = inoutEntity.getProperty(ShipmentInOut.PROPERTY_DOCUMENTSTATUS);
		String oldDocStatus = (String)event.getPreviousState(docStausProperty);
		if (oldDocStatus.equalsIgnoreCase("BPRL_RELEASE2") || oldDocStatus.equalsIgnoreCase("BPRL_RELEASE3")){
			ShipmentInOut header =(ShipmentInOut)event.getTargetInstance();
			doUpdateOrDelete(header);
		}
	}

	public void onSave(@Observes EntityNewEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		//based on use case, this event is not intended.
	}

	public void onDelete(@Observes EntityDeleteEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		
		final Entity inoutEntity = ModelProvider.getInstance().getEntity(ShipmentInOut.ENTITY_NAME);
		final Property docStausProperty = inoutEntity.getProperty(ShipmentInOut.PROPERTY_DOCUMENTSTATUS);
		String docStatus = (String)event.getCurrentState(docStausProperty);
		if (docStatus.equalsIgnoreCase("BPRL_RELEASE2") || docStatus.equalsIgnoreCase("BPRL_RELEASE3")){
			ShipmentInOut header =(ShipmentInOut)event.getTargetInstance();
			doUpdateOrDelete(header);
		}
		
	}
	
	private void doUpdateOrDelete(ShipmentInOut header) {		
		
		String docstatus = header.getDocumentStatus();
		if (docstatus.equalsIgnoreCase("BPRL_RELEASE2") || docstatus.equalsIgnoreCase("BPRL_RELEASE3")){
			//cek preference orang umum
			HttpServletRequest request = RequestContext.get().getRequest();
			VariablesSecureApp vars = new VariablesSecureApp(request);
			String orangumum=Utility.getPreference(vars, "orangumum", null);
			if (orangumum!=null & orangumum.equalsIgnoreCase("Y"))
				return; //orang umum mah bebas
			
			//bukan orang umum, maka tidak boleh update, tidak boleh delete.
			Utility.throwErrorMessage("bprl_release2Release3UpdateOrDeleteByOrangUmum");
		} else
			return;
	}

}
