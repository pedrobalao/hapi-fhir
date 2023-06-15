package ca.uhn.fhir.jpa.config;

import java.util.List;
import java.util.Locale;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

public class MandatoryTransactionListener implements ProxyDataSourceBuilder.SingleQueryExecution {
    @Override
    public void execute(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            for (QueryInfo nextQuery : queryInfoList) {
                String query = nextQuery.getQuery().toLowerCase(Locale.US);
                if (query.contains("hfj_") || query.contains("trm_")) {
                    if (query.startsWith("select ")
                            || query.startsWith("insert ")
                            || query.startsWith("update ")) {
                        throw new IllegalStateException(
                                "No transaction active executing query: " + nextQuery.getQuery());
                    }
                }
            }
        }
    }
}
