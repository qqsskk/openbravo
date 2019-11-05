package org.infinite.costing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.costing.CostAdjustmentUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class AverageCostingCalculationThread extends Thread {
	private final Logger log4j = Logger.getLogger(AverageCostingCalculationThread.class);

	private final OBDal readOnlyObDal;
	private final String productId;
	private final ConcurrentHashMap<String, List<CostingPojo>> output;
	private final List<String> authorizedOrgAcess;
	private final Organization costingOrganization;
	private final boolean isMaintainWarehouseDimension;
	private final Date dateNow;
	private final Date dateDooms;
	private final Set<String> naturalOrganizationIdTree;
	private final List<Organization> naturalOrganizationTree = new ArrayList<>();
	private final Map<Product, List<Costing>> standardCostingCache = new HashMap<>();
	
	public AverageCostingCalculationThread(OBDal obdal, List<String> authorizedOrgAcess, String productId,
			Organization costingOrganization, boolean isMaintainWarehouseDimension,
			ConcurrentHashMap<String, List<CostingPojo>> output) {
		this.readOnlyObDal = obdal;
		this.productId = productId;
		this.output = output;
		this.authorizedOrgAcess = authorizedOrgAcess;
		this.costingOrganization = costingOrganization;
		this.isMaintainWarehouseDimension = isMaintainWarehouseDimension;

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		dateNow = cal.getTime();
		cal.set(9999, 12, 31, 0, 0, 0);
		dateDooms = cal.getTime();

		this.naturalOrganizationIdTree = OBContext.getOBContext().getOrganizationStructureProvider()
				.getNaturalTree(costingOrganization.getId());

		for (String orgId : this.naturalOrganizationIdTree) {
			Organization org = readOnlyObDal.get(Organization.class, orgId);
			this.naturalOrganizationTree.add(org);
		}
	}
	
	public void run() {
		OBContext.setAdminMode();
		Product product = readOnlyObDal.get(Product.class, productId);
		log4j.debug("start cost calculation for product " + product.getName());

		List<Costing> latestCostings = getLatestCosting();
		if (latestCostings != null && latestCostings.size() > 1) {
			String message = String.format(
					"invalid costing record, there are %d valid costing record(s) for product %s, skipp all following material transaction for those product.",
					latestCostings.size(), product.getIdentifier());
			log4j.debug(message);

			return;
		}

		BigDecimal prevCummulativeQty = BigDecimal.ZERO;
		BigDecimal prevUnitCost = BigDecimal.ZERO;
		BigDecimal prevCummulativeCost = BigDecimal.ZERO;
		BigDecimal prevUnitPrice = BigDecimal.ZERO;
		Date prevStartDate = dateNow;
		Date prevEndDate = dateDooms;

		if (latestCostings != null && latestCostings.size() == 1) {
			Costing latestCost = latestCostings.get(0);
			prevCummulativeQty = latestCost.getTotalMovementQuantity();
			prevUnitCost = latestCost.getCost();
			prevCummulativeCost = latestCost.getTotalStockValuation();
			prevUnitPrice = latestCost.getPrice();
			prevStartDate = latestCost.getStartingDate();
			prevEndDate = latestCost.getEndingDate();
		}

		List<CostingPojo> costingsResult = new ArrayList<>();

		List<String> trxs = getTransactionsBatch(authorizedOrgAcess);
		for (String trxId : trxs) {

			log4j.debug("processing transaction id: " + trxId);

			try {
				log4j.debug("start calculating transaction cost");

				MaterialTransaction transaction = readOnlyObDal.get(MaterialTransaction.class, trxId);
				BigDecimal movementqty = transaction.getMovementQuantity();
				if (movementqty.compareTo(BigDecimal.ZERO) < 0) { // outgoing transaction
					prevCummulativeQty = prevCummulativeQty.add(movementqty);
					prevCummulativeCost = prevCummulativeQty.multiply(prevUnitCost);
					Warehouse warehouse = null;
					if (isMaintainWarehouseDimension)
						warehouse = transaction.getStorageBin().getWarehouse();
					CostingPojo newcosting = new CostingPojo(costingOrganization, movementqty, prevCummulativeQty, prevUnitCost,
							prevCummulativeCost, prevUnitPrice, transaction, prevStartDate, prevEndDate, warehouse, "AVA", false,
							false);

					costingsResult.add(newcosting);
					
				} else { //incoming transaction

					prevCummulativeQty = prevCummulativeQty.add(movementqty);
					// TODO prevUnitCost = new unit cost; //where this incoming from? PO, PI,

					String movementType = transaction.getMovementType();
					switch (movementType) {
					case "V+":
					case "V-":
					case "C+":
					case "C-": {
						// TODO implement more accurate approach to get transaction cost
						// for now, we get unit price from PO. need more accurate approach in the
						// future.
						ShipmentInOutLine inoutLine = transaction.getGoodsShipmentLine();
						BigDecimal transactionCost = null;
						if (inoutLine.getSalesOrderLine() != null) {
							transactionCost = inoutLine.getSalesOrderLine().getLineNetAmount();
							prevUnitPrice = transactionCost.divide(movementqty);
						} else {// get standard cost
							prevUnitPrice = getStandardCost(product, transaction.getMovementDate());
							transactionCost = prevUnitPrice.multiply(movementqty);
						}

						if (transactionCost == null)
							throw new OBException("can not get cost for material transaction id: " + trxId);

						break;
					}

					case "I+":
					case "I-": {
						// physical inventory

						break;
					}

					case "M+":
					case "M-": {
						// goods movement

						break;
					}

					case "D+":
					case "D-": {
						// internal consumption

						break;
					}

					case "P+":
					case "P-":
					case "W+":
					case "W-": {
						// production and work order
						throw new IllegalArgumentException("unsupported movement type: " + movementType);
					}

					default:
						throw new IllegalArgumentException("unknown movement type: " + movementType);
					}


					// unsupported
//					P-
//					P+
//					W+
//					W-

					// sudah dapat prevUnitPrice, saatnya menetukan record costing yg baru (either
					// non create new or not)

					prevCummulativeCost = prevCummulativeQty.multiply(prevUnitCost);
					Warehouse warehouse = null;
					if (isMaintainWarehouseDimension)
						warehouse = transaction.getStorageBin().getWarehouse();

					// terminate the previous costing pojo
					CostingPojo prevcosting = getPreviousInsertCostingRecord(costingsResult);
					if (prevcosting != null)
						prevcosting.setEndDate(dateNow);

					CostingPojo newcosting = new CostingPojo(costingOrganization, movementqty, prevCummulativeQty, prevUnitCost,
							prevCummulativeCost, prevUnitPrice, transaction, dateNow, dateDooms, warehouse, "AVA", false,
							true);

					costingsResult.add(newcosting);
				}

				log4j.debug("end calculating transaction cost");

			} catch (Exception e) {
				String message = String.format(
						"failed to calculate product cost %s, skipp all following material transaction for those product.",
						product.getIdentifier());
				message += "|-- " + e.getMessage();
				log4j.debug(message);

				break;
			}

			// preserve previous loop data
			CostingPojo latestCost = costingsResult.get(costingsResult.size() - 1);
			prevCummulativeQty = latestCost.getCummulativeQty();
			prevUnitCost = latestCost.getUnitCost();
			prevCummulativeCost = latestCost.getCummulativeCost();
			prevUnitPrice = latestCost.getUnitPrice();
			prevStartDate = latestCost.getStartDate();
			prevEndDate = latestCost.getEndDate();
		}

		this.output.put(productId, costingsResult);

		OBContext.restorePreviousMode();

	}

	private BigDecimal getStandardCost(Product product, Date movementDate) {

		if (!standardCostingCache.containsKey(product)) {
			OBCriteria<Costing> costingQuery = readOnlyObDal.createCriteria(Costing.class);
			costingQuery.add(Restrictions.eq(Costing.PROPERTY_PRODUCT, product));
			costingQuery.add(Restrictions.eq(Costing.PROPERTY_COSTTYPE, "STA"));
			costingQuery.add(Restrictions.in(Costing.PROPERTY_ORGANIZATION, this.naturalOrganizationTree));

			List<Costing> costings = costingQuery.list();
			
			standardCostingCache.put(product, costings);
		}
		
		if (!standardCostingCache.containsKey(product))
			return null;

		Costing standardCosting = null;
		for (Costing costing : standardCostingCache.get(product)) {
			if (costing.getStartingDate().before(movementDate) && costing.getEndingDate().after(movementDate)) {
				standardCosting = costing;
				break;
			}
		}

		return standardCosting.getCost();
	}

	private CostingPojo getPreviousInsertCostingRecord(List<CostingPojo> costingsResult) {
		for (int i = (costingsResult.size() - 1); i >= 0; i--) {
			CostingPojo costing = costingsResult.get(i);
			if (costing.isInsertNewCostingRecord())
				return costing;
		}
		return null;
	}

	private List<Costing> getLatestCosting() {
		String where = " as cs\n" + "where cs.product.id='" + this.productId + "'\n"
				+ "and cs.startingDate<=current_date()\n" + "and cs.endingDate>=current_date()\n"
				+ "and ad_isorgincluded('" + this.costingOrganization.getId() + "', cs.organization.id, cs.client.id) != -1\n"
				+ "order by endingDate desc, startingDate desc, creationDate desc";
		OBQuery<Costing> costingQuery = readOnlyObDal.createQuery(Costing.class, where);
		costingQuery.setMaxResult(1);
		List<Costing> costings = costingQuery.list();

		return costings;
	}

	@SuppressWarnings("unchecked")
	private List<String> getTransactionsBatch(List<String> authorizedOrgAcess) {
		log4j.debug("retrieve transaction from db");

		StringBuffer where = new StringBuffer();
		where.append(" select trx." + MaterialTransaction.PROPERTY_ID + " as id ");
		where.append(" from " + MaterialTransaction.ENTITY_NAME + " as trx");
		where.append(" join trx." + MaterialTransaction.PROPERTY_PRODUCT + " as p");
		where.append("\n , " + org.openbravo.model.ad.domain.List.ENTITY_NAME + " as trxtype");
		where.append("\n where trx." + MaterialTransaction.PROPERTY_ISPROCESSED + " = false");
		where.append("   and trx." + MaterialTransaction.PROPERTY_COSTINGSTATUS + " <> 'S'");
		where.append("   and trx." + MaterialTransaction.PROPERTY_PRODUCT + ".id = :productId");
		where.append("   and p." + Product.PROPERTY_PRODUCTTYPE + " = 'I'");
		where.append("   and p." + Product.PROPERTY_STOCKED + " = true");
		where.append("   and trxtype." + CostAdjustmentUtils.propADListReference + ".id = :refid");
		where.append("   and trxtype." + CostAdjustmentUtils.propADListValue + " = trx."
				+ MaterialTransaction.PROPERTY_MOVEMENTTYPE);
		where.append("   and trx." + MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE + " <= :now");
		where.append("   and trx." + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
		where.append(" order by trx." + MaterialTransaction.PROPERTY_PRODUCT);
		where.append(" , trx." + MaterialTransaction.PROPERTY_MOVEMENTDATE);
		where.append(" , trxtype." + CostAdjustmentUtils.propADListPriority);
		where.append(" , trx." + MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE);
		where.append(" , trx." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY + " desc");
		where.append(" , trx." + MaterialTransaction.PROPERTY_ID);
		Query<String> trxQry = readOnlyObDal.getSession().createQuery(where.toString());

		trxQry.setParameter("productId", this.productId);
		trxQry.setParameter("refid", CostAdjustmentUtils.MovementTypeRefID);
		trxQry.setParameter("now", new Date());
		trxQry.setParameterList("orgs", authorizedOrgAcess);

		List<String> output = trxQry.list();

		log4j.debug("transaction from db retrieved. transactioncount: " + output.size());

		return output;

	}

}
