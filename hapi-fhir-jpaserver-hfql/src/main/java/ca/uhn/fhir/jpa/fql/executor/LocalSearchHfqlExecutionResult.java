/*-
 * #%L
 * HAPI FHIR JPA Server - HFQL Driver
 * %%
 * Copyright (C) 2014 - 2023 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.fql.executor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.fql.parser.HfqlStatement;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @see IHfqlExecutionResult for information about the purpose of this class
 */
public class LocalSearchHfqlExecutionResult implements IHfqlExecutionResult {
	private static final Logger ourLog = LoggerFactory.getLogger(LocalSearchHfqlExecutionResult.class);

	private final IBundleProvider mySearchResult;
	private final HfqlExecutor.HfqlExecutionContext myExecutionContext;
	private final Integer myLimit;
	private final HfqlStatement myStatement;
	private final List<String> myColumnNames;
	private final List<HfqlDataTypeEnum> myColumnDataTypes;
	private final Predicate<IBaseResource> myWhereClausePredicate;
	private final IParser myParser;
	private int myTotalRowsFetched = 0;
	private int myNextSearchResultRow;
	private int myNextBatchRow = 0;
	private List<IBaseResource> myNextBatch;
	private IBaseResource myNextResource;
	private boolean myExhausted = false;
	private int myNextResourceSearchRow;
	private Row myErrorRow;

	public LocalSearchHfqlExecutionResult(
			HfqlStatement theStatement,
			IBundleProvider theSearchResult,
			HfqlExecutor.HfqlExecutionContext theExecutionContext,
			Integer theLimit,
			int theInitialOffset,
			List<HfqlDataTypeEnum> theColumnDataTypes,
			Predicate<IBaseResource> theWhereClausePredicate,
			FhirContext theFhirContext) {
		myStatement = theStatement;
		mySearchResult = theSearchResult;
		myExecutionContext = theExecutionContext;
		myLimit = theLimit;
		myNextSearchResultRow = theInitialOffset;
		myColumnDataTypes = theColumnDataTypes;
		myWhereClausePredicate = theWhereClausePredicate;
		myColumnNames = myStatement.getSelectClauses().stream()
				.map(HfqlStatement.SelectClause::getAlias)
				.collect(Collectors.toUnmodifiableList());
		myParser = theFhirContext.newJsonParser();
	}

	@Override
	public List<String> getColumnNames() {
		return myColumnNames;
	}

	@Override
	public List<HfqlDataTypeEnum> getColumnTypes() {
		return myColumnDataTypes;
	}

	@Override
	public boolean hasNext() {
		fetchNextResource();
		return myNextResource != null;
	}

	private void fetchNextResource() {
		if (myNextResource != null) {
			return;
		}
		try {
			while (myNextResource == null && !myExhausted) {
				if (myNextBatch == null) {
					int from = myNextSearchResultRow;
					int to = myNextSearchResultRow + HfqlExecutor.BATCH_SIZE;
					myNextBatch = mySearchResult.getResources(from, to);
					ourLog.info(
							"HFQL fetching resources {}-{} - Total {} fetched, {} retained and limit {}",
							from,
							to,
							myNextSearchResultRow,
							myTotalRowsFetched,
							myLimit);
					myNextBatchRow = 0;
					myNextSearchResultRow += HfqlExecutor.BATCH_SIZE;
				}
				if (myNextBatch.isEmpty()) {
					myExhausted = true;
				} else if (myNextBatch.size() > myNextBatchRow) {
					myNextResource = myNextBatch.get(myNextBatchRow);
					myNextResourceSearchRow = (myNextSearchResultRow - HfqlExecutor.BATCH_SIZE) + myNextBatchRow;
					myNextBatchRow++;
				} else {
					myNextBatch = null;
				}

				if (myNextResource != null && !myWhereClausePredicate.test(myNextResource)) {
					myNextResource = null;
				}
			}

			if (myNextResource != null) {
				myTotalRowsFetched++;
				if (myLimit != null && myTotalRowsFetched >= myLimit) {
					myExhausted = true;
				}
			}
		} catch (Exception e) {
			createAndStoreErrorRow(e.getMessage());
		}
	}

	@Override
	public Row getNextRow() {
		fetchNextResource();
		if (myErrorRow != null) {
			Row errorRow = myErrorRow;
			myErrorRow = null;
			return errorRow;
		}

		Validate.isTrue(myNextResource != null, "No more results");

		List<Object> values = new ArrayList<>();
		for (int columnIndex = 0; columnIndex < myStatement.getSelectClauses().size(); columnIndex++) {
			HfqlStatement.SelectClause nextColumn =
					myStatement.getSelectClauses().get(columnIndex);
			String clause = nextColumn.getClause();
			List<IBase> columnValues;
			try {
				columnValues = myExecutionContext.evaluate(myNextResource, clause, IBase.class);
			} catch (Exception e) {
				String errorMessage =
						"Failed to evaluate FHIRPath expression \"" + clause + "\". Error: " + e.getMessage();
				return createAndStoreErrorRow(errorMessage);
			}
			String value = null;
			if (!columnValues.isEmpty()) {
				IBase firstColumnValue = columnValues.get(0);
				if (firstColumnValue instanceof IIdType) {
					value = ((IIdType) firstColumnValue).getIdPart();
				} else if (firstColumnValue != null) {
					value = myParser.encodeToString(firstColumnValue);
				}
			}

			// FIXME: remove
			//			if (value != null) {
			//				if (columnDataType.equals(HfqlDataTypeEnum.INTEGER)) {
			//					try {
			//						values.add(Integer.parseInt(value));
			//						continue;
			//					} catch (NumberFormatException e) {
			//						String errorMessage = "Failed to evaluate result of FHIRPath expression \"" + clause + "\" as
			// INTEGER. Error: " + e.getMessage();
			//						return createAndStoreErrorRow(errorMessage);
			//					}
			//				}
			//			}

			values.add(value);
		}

		myNextResource = null;
		return new Row(myNextResourceSearchRow, values);
	}

	private Row createAndStoreErrorRow(String errorMessage) {
		myExhausted = true;
		myNextResource = null;
		myErrorRow = new Row(IHfqlExecutionResult.ROW_OFFSET_ERROR, List.of(errorMessage));
		return myErrorRow;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void close() {
		// ignore
	}

	@Override
	public String getSearchId() {
		return mySearchResult.getUuid();
	}

	@Override
	public int getLimit() {
		return myLimit != null ? myLimit : -1;
	}

	@Override
	public HfqlStatement getStatement() {
		return myStatement;
	}
}