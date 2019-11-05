package org.infinite.costing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.costing.CostAdjustmentUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class CostingBackground extends DalBaseProcess implements KillableProcess {
	private final Logger log4j = LogManager.getLogger();
	private final Date dateNow;
	private final Date dateTo;
	private final Organization costingOrganization;
	private final Currency costingCurrency;

	private final int processorCount;
	private volatile int activeThreadCount = 0;

	private volatile boolean killProcess = false;

	public CostingBackground() {
		super();

		processorCount = Runtime.getRuntime().availableProcessors();

		Calendar cal = Calendar.getInstance();
		dateNow = cal.getTime();

		cal.set(9999, 12, 31, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		dateTo = cal.getTime();

		Client clientLogin = OBContext.getOBContext().getCurrentClient();
		Organization orgLogin = OBContext.getOBContext().getCurrentOrganization();

		costingOrganization = OBContext.getOBContext().getOrganizationStructureProvider(clientLogin.getId())
				.getLegalEntity(orgLogin);
		costingCurrency = costingOrganization.getCurrency();
	}

	@Override
	public void kill(ProcessBundle processBundle) throws Exception {
		this.killProcess = true;

	}

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		ProcessLogger logger = bundle.getLogger();

		// TODO put processor count concern when implement parallelism
		log4j.debug("current active thread: " + activeThreadCount);
		log4j.debug("processor count: " + processorCount);

		OBError result = new OBError();
		result.setType("Success");
		result.setTitle(OBMessageUtils.messageBD("Success"));

		String currentClientId = bundle.getContext().getClient();
		String currentOrganizationId = bundle.getContext().getOrganization();

		List<String> organizationWithCostingRule = getOrganizationWithCostingRule(currentClientId, currentOrganizationId);
		if (organizationWithCostingRule.size() == 0) {
			result.setMessage("No organizations with Costing Rule defined");
			bundle.setResult(result);

			log4j.debug(result.getMessage());
			logger.logln(result.getMessage());

			return;
		}

		CostingRule validCostingRule = getValidCostingRule(currentClientId, currentOrganizationId);
		if (validCostingRule == null) {
			result.setType("Error");
			result.setMessage("can not find valid costing rule for organization id: " + currentOrganizationId);
			bundle.setResult(result);
			logger.log(result.getMessage());
			log4j.debug(result.getMessage());

			return;
		}
		
		boolean isMaintainWarehouseDimension = validCostingRule.isWarehouseDimension();

		// TODO for now AVA only
		if (!validCostingRule.getCostingAlgorithm().getId().equals("B069080A0AE149A79CF1FA0E24F16AB")) {
			result.setType("Error");
			result.setMessage("for now, only moving average costing algorithm is supported.");
			bundle.setResult(result);
			logger.log(result.getMessage());
			log4j.debug(result.getMessage());

			return;
		}

		List<String> pendingProductCostingTrxId = getPendingProductCostingTransaction(organizationWithCostingRule);

		// output bag
		List<AverageCostingCalculationThread> costingThreads = new ArrayList<>();
		ConcurrentHashMap<String, List<CostingPojo>> costCalculationOutput = new ConcurrentHashMap<>();
				
		for (String productId : pendingProductCostingTrxId) {

			if (killProcess)
				break;

			Product product = OBDal.getInstance().get(Product.class, productId);

			if (!evaluateCostingRuleMigration(product, validCostingRule)) {
				log4j.debug(String.format("costing rule of product %s is not migrated yet,", product.getIdentifier()));
				continue;
			}

			log4j.debug("start processing product " + product.getIdentifier());

			AverageCostingCalculationThread costingThread = new AverageCostingCalculationThread(OBDal.getReadOnlyInstance(),
					organizationWithCostingRule, productId, costingOrganization, isMaintainWarehouseDimension,
					costCalculationOutput);

			costingThreads.add(costingThread);
			costingThread.start();

			log4j.debug("processing product " + product.getIdentifier() + " completed.");

		}

		for (AverageCostingCalculationThread costingThread : costingThreads) {
			costingThread.join();
		}

		// TODO process consolidated output
		for (String productId : costCalculationOutput.keySet()) {
			if (killProcess)
				break;

			log4j.debug(String.format("saving transaction cost for product: %s", productId));
			Product product = OBDal.getInstance().get(Product.class, productId);

			for (CostingPojo costing : costCalculationOutput.get(productId)) {

				if (killProcess)
					break;

				MaterialTransaction transaction = costing.getMaterialTransaction();
				log4j.debug("-- transaction: " + transaction.getId());

				transaction.setCostingStatus("CC"); // cost calculated
				transaction.setTransactionProcessDate(dateNow);
				transaction.setCostingAlgorithm(validCostingRule.getCostingAlgorithm());
				transaction.setCostCalculated(true);

				BigDecimal transactionCost = costing.getUnitCost().multiply(costing.getQty());
				transaction.setTransactionCost(transactionCost);

				OBDal.getInstance().save(transactionCost);

				if (!costing.isInsertNewCostingRecord())
					continue;

				Costing cost = OBProvider.getInstance().get(Costing.class);
				cost.setCost(transactionCost);
				cost.setCurrency(costingCurrency);
				cost.setStartingDate(transaction.getTransactionProcessDate());
				cost.setEndingDate(dateTo);
				cost.setInventoryTransaction(transaction);
				cost.setProduct(product);
				if (product.isProduction()) {
					cost.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
				} else {
					cost.setOrganization(costingOrganization);
				}
				cost.setQuantity(costing.getQty());
				cost.setTotalMovementQuantity(costing.getCummulativeQty());
				cost.setTotalStockValuation(costing.getCummulativeCost());
				cost.setPrice(costing.getUnitPrice());
				cost.setCostType("AVA"); // TODO for now AVA only
				cost.setManual(false);
				cost.setPermanent(true);
				cost.setWarehouse(costing.getWarehouse());
				OBDal.getInstance().save(cost);

			}

			OBDal.getInstance().flush();
		}

		OBDal.getInstance().flush();
		OBDal.getInstance().commitAndClose();

		bundle.setResult(result);
	}

	private CostingRule getValidCostingRule(String currentClientId, String currentOrganizationId) {

		String where = "as cr\n" + "where cr.client.id='" + currentClientId + "'\n" + "and ad_isorgincluded('"
				+ currentOrganizationId + "', cr.organization.id, cr.client.id) != -1\n" + "";

		OBQuery<CostingRule> costingRuleQuery = OBDal.getInstance().createQuery(CostingRule.class, where);
		costingRuleQuery.setMaxResult(1);
		List<CostingRule> costingRules = costingRuleQuery.list();
		if (costingRules.size() > 0)
			costingRules.get(0);

		return null;
	}

	private boolean evaluateCostingRuleMigration(Product product, CostingRule validCostingRule) {
		List<CostingRule> costingRules = product.getCostingRuleList();
		boolean isCostingMigrated = false;
		for (CostingRule costingRule : costingRules) {
			if (costingRule.getId() == validCostingRule.getId()) {
				isCostingMigrated = true;
				break;
			}
		}

		return isCostingMigrated;
	}

	private List<String> getOrganizationWithCostingRule(String client, String organization) {
		List<String> output = new ArrayList<>();
		// Get organizations with costing rules.
		StringBuffer where = new StringBuffer();
		where.append(" as o");
		where.append(" where exists (");
		where.append("    select 1 from " + CostingRule.ENTITY_NAME + " as cr");
		where.append("    where ad_isorgincluded(o.id, cr." + CostingRule.PROPERTY_ORGANIZATION + ".id, "
				+ CostingRule.PROPERTY_CLIENT + ".id) <> -1 ");
		where.append("      and cr." + CostingRule.PROPERTY_VALIDATED + " is true");
		where.append(" )");
		where.append("    and ad_isorgincluded(o.id, '" + organization + "', '" + client + "') <> -1 ");
		OBQuery<Organization> orgQry = OBDal.getInstance().createQuery(Organization.class, where.toString());
		List<Organization> orgs = orgQry.list();
		for (Organization org : orgs) {
			output.add(org.getId());
		}

		return output;
	}

	private List<String> getPendingProductCostingTransaction(List<String> organizationWithCostingRule) {
		StringBuffer where = new StringBuffer();
		where.append(" select distinct trx." + MaterialTransaction.PROPERTY_PRODUCT + ".id as id ");
		where.append(" from " + MaterialTransaction.ENTITY_NAME + " as trx");
		where.append(" join trx." + MaterialTransaction.PROPERTY_PRODUCT + " as p");
		where.append("\n , " + org.openbravo.model.ad.domain.List.ENTITY_NAME + " as trxtype");
		where.append("\n where trx." + MaterialTransaction.PROPERTY_ISPROCESSED + " = false");
		where.append("   and trx." + MaterialTransaction.PROPERTY_COSTINGSTATUS + " <> 'S'");
		where.append("   and p." + Product.PROPERTY_PRODUCTTYPE + " = 'I'");
		where.append("   and p." + Product.PROPERTY_STOCKED + " = true");
		where.append("   and trxtype." + CostAdjustmentUtils.propADListReference + ".id = :refid");
		where.append("   and trxtype." + CostAdjustmentUtils.propADListValue + " = trx."
				+ MaterialTransaction.PROPERTY_MOVEMENTTYPE);
		where.append("   and trx." + MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE + " <= :now");
		where.append("   and trx." + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
		where.append(" order by trx." + MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE);
		where.append(" , trxtype." + CostAdjustmentUtils.propADListPriority);
		where.append(" , trx." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY + " desc");
		where.append(" , trx." + MaterialTransaction.PROPERTY_ID);
		Query<String> trxQry = OBDal.getInstance().getSession().createQuery(where.toString(), String.class);

		trxQry.setParameter("refid", CostAdjustmentUtils.MovementTypeRefID);
		trxQry.setParameter("now", new Date());
		trxQry.setParameterList("orgs", organizationWithCostingRule);

		return trxQry.list();
	}

}
