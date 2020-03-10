package org.wirabumi.bprl.event;

import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class ShipmentLineEventHandler extends EntityPersistenceEventObserver {
	/*
	 * buatlah business event handler untuk event:
	 * 1.before insert/update/delete
	 * 2.before edit
	 * 
	 * dengan logika:
	 * 	jika sudah release 2 atau release 3: tidak bisa di edit/insert selain orang umum
	 * 	jika sudah relaase 3: tidak bisa di hapus selain orang umum
	 * 1.cek apakah status header sudah release 2.
	 * 2.jika ya, maka cek apakah preference umum nilainya Y.
	 * 3.jika ya, maka continue. jika tidak, maka raise exception
	 */
	
	private static Entity[] entities = { ModelProvider.getInstance().getEntity(ShipmentInOutLine.ENTITY_NAME) };
	protected Logger logger = Logger.getLogger(this.getClass());

	@Override
	protected Entity[] getObservedEntities() {
		return entities;
	}
	
	public void onUpdate(@Observes EntityUpdateEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		ShipmentInOutLine line =(ShipmentInOutLine)event.getTargetInstance();
		doCreateOrUpdate(line);
	}

	public void onSave(@Observes EntityNewEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		ShipmentInOutLine line =(ShipmentInOutLine)event.getTargetInstance();
		doCreateOrUpdate(line);
	}

	public void onDelete(@Observes EntityDeleteEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
		ShipmentInOutLine line =(ShipmentInOutLine)event.getTargetInstance();
		doDelete(line);
	}
	
	private void doDelete(ShipmentInOutLine line) {
		ShipmentInOut header = line.getShipmentReceipt();
		String docstatus = header.getDocumentStatus();
		if (!docstatus.equalsIgnoreCase("BPRL_RELEASE3"))
			return; //hanya unutk release 3
		
		//cek preference orang umum
		HttpServletRequest request = RequestContext.get().getRequest();
		VariablesSecureApp vars = new VariablesSecureApp(request);
		String orangumum=Utility.getPreference(vars, "orangumum", null);
		if (orangumum!=null & orangumum.equalsIgnoreCase("Y"))
			return; //orang umum mah bebas

		//bukan orang umum, release 3 hanya boleh dihapus oleh orang umum saja.
		Utility.throwErrorMessage("bprl_release3DeleteByOrangUmum");
		
	}

	private void doCreateOrUpdate(ShipmentInOutLine line) {		
		ShipmentInOut header = line.getShipmentReceipt();
		String docstatus = header.getDocumentStatus();
		if (docstatus.equalsIgnoreCase("BPRL_RELEASE2") || docstatus.equalsIgnoreCase("BPRL_RELEASE3")){
			//cek preference orang umum
			HttpServletRequest request = RequestContext.get().getRequest();
			VariablesSecureApp vars = new VariablesSecureApp(request);
			String orangumum=Utility.getPreference(vars, "orangumum", null);
			if (orangumum!=null & orangumum.equalsIgnoreCase("Y"))
				return; //orang umum mah bebas
			else
				Utility.throwErrorMessage("bprl_release2Release3InsertOrUpdateByOrangUmum"); //bukan orang umum, bolehnya cuma delete only.	
		} else
			return; //hanya unutk release 2 atau release 3
		
	}

}
