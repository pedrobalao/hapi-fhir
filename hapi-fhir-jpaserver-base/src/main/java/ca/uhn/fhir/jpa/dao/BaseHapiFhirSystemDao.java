package ca.uhn.fhir.jpa.dao;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2015 University Health Network
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ca.uhn.fhir.jpa.entity.ResourceTable;
import ca.uhn.fhir.jpa.util.StopWatch;
import ca.uhn.fhir.model.api.TagList;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor.ActionRequestDetails;

public abstract class BaseHapiFhirSystemDao<T> extends BaseHapiFhirDao<IBaseResource>implements IFhirSystemDao<T> {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BaseHapiFhirSystemDao.class);

	@PersistenceContext()
	protected EntityManager myEntityManager;

	@Autowired
	private PlatformTransactionManager myTxManager;

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteAllTagsOnServer() {
		// Notify interceptors
		ActionRequestDetails requestDetails = new ActionRequestDetails(null, null);
		notifyInterceptors(RestOperationTypeEnum.DELETE_TAGS, requestDetails);

		myEntityManager.createQuery("DELETE from ResourceTag t").executeUpdate();
	}

	private int doPerformReindexingPass(final Integer theCount) {
		TransactionTemplate txTemplate = new TransactionTemplate(myTxManager);
		txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		return txTemplate.execute(new TransactionCallback<Integer>() {
			@SuppressWarnings("unchecked")
			@Override
			public Integer doInTransaction(TransactionStatus theStatus) {
				TypedQuery<ResourceTable> q = myEntityManager.createQuery("SELECT t FROM " + ResourceTable.class.getSimpleName() + " t WHERE t.myIndexStatus IS null", ResourceTable.class);

				int maxResult = 500;
				if (theCount != null) {
					maxResult = Math.min(theCount, 2000);
				}

				q.setMaxResults(maxResult);
				List<ResourceTable> resources = q.getResultList();
				if (resources.isEmpty()) {
					return 0;
				}

				ourLog.info("Indexing {} resources", resources.size());

				int count = 0;
				long start = System.currentTimeMillis();

				for (ResourceTable resourceTable : resources) {
					final IBaseResource resource;
					try {
						resource = toResource(resourceTable, false);
					} catch (DataFormatException e) {
						ourLog.warn("Failure parsing resource: {}", e.toString());
						throw new UnprocessableEntityException(Long.toString(resourceTable.getId()));
					}
					@SuppressWarnings("rawtypes")
					final IFhirResourceDao dao = getDao(resource.getClass());
					if (dao == null) {
						ourLog.warn("No DAO for type: {}", resource.getClass());
						throw new UnprocessableEntityException(Long.toString(resourceTable.getId()));
					}

					if (resource.getIdElement().isIdPartValid() == false) {
						ourLog.warn("Not going to try and index an invalid ID: {}", resource.getIdElement());
						throw new UnprocessableEntityException(Long.toString(resourceTable.getId()));
					}

					try {
						dao.update(resource, null, true);
					} catch (Exception e) {
						ourLog.error("Failed to index resource {}: {}", new Object[] { resource.getIdElement(), e.toString(), e });
						throw new UnprocessableEntityException(Long.toString(resourceTable.getId()));
					}
					count++;
				}

				long delay = System.currentTimeMillis() - start;
				ourLog.info("Indexed {} / {} resources in {}ms", new Object[] { count, resources.size(), delay });

				return resources.size();
			}
		});
	}

	@Override
	public TagList getAllTags() {
		// Notify interceptors
		ActionRequestDetails requestDetails = new ActionRequestDetails(null, null);
		notifyInterceptors(RestOperationTypeEnum.GET_TAGS, requestDetails);

		StopWatch w = new StopWatch();
		TagList retVal = super.getTags(null, null);
		ourLog.info("Processed getAllTags in {}ms", w.getMillisAndRestart());
		return retVal;
	}

	@Override
	public Map<String, Long> getResourceCounts() {
		CriteriaBuilder builder = myEntityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> cq = builder.createTupleQuery();
		Root<?> from = cq.from(ResourceTable.class);
		cq.multiselect(from.get("myResourceType").as(String.class), builder.count(from.get("myResourceType")).as(Long.class));
		cq.groupBy(from.get("myResourceType"));

		TypedQuery<Tuple> q = myEntityManager.createQuery(cq);

		Map<String, Long> retVal = new HashMap<String, Long>();
		for (Tuple next : q.getResultList()) {
			String resourceName = next.get(0, String.class);
			Long count = next.get(1, Long.class);
			retVal.put(resourceName, count);
		}
		return retVal;
	}

	protected boolean hasValue(InstantDt theInstantDt) {
		return theInstantDt != null && theInstantDt.isEmpty() == false;
	}

	@Override
	public IBundleProvider history(Date theSince) {
		// Notify interceptors
		ActionRequestDetails requestDetails = new ActionRequestDetails(null, null);
		notifyInterceptors(RestOperationTypeEnum.HISTORY_SYSTEM, requestDetails);

		StopWatch w = new StopWatch();
		IBundleProvider retVal = super.history(null, null, theSince);
		ourLog.info("Processed global history in {}ms", w.getMillisAndRestart());
		return retVal;
	}

	protected ResourceTable loadFirstEntityFromCandidateMatches(Set<Long> candidateMatches) {
		return myEntityManager.find(ResourceTable.class, candidateMatches.iterator().next());
	}

	@Transactional()
	@Override
	public int markAllResourcesForReindexing() {
		return myEntityManager.createQuery("UPDATE " + ResourceTable.class.getSimpleName() + " t SET t.myIndexStatus = null").executeUpdate();
	}

	private void markResourceAsIndexingFailed(final long theId) {
		TransactionTemplate txTemplate = new TransactionTemplate(myTxManager);
		txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		txTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus theStatus) {
				ourLog.info("Marking resource with PID {} as indexing_failed", new Object[] { theId });
				Query q = myEntityManager.createQuery("UPDATE ResourceTable t SET t.myIndexStatus = :status WHERE t.myId = :id");
				q.setParameter("status", INDEX_STATUS_INDEXING_FAILED);
				q.setParameter("id", theId);
				q.executeUpdate();
				return null;
			}
		});
	}

	@Override
	@Transactional(propagation=Propagation.NOT_SUPPORTED)
	public int performReindexingPass(final Integer theCount) {
		try {
			return doPerformReindexingPass(theCount);
		} catch (UnprocessableEntityException e) {
			markResourceAsIndexingFailed(Long.parseLong(e.getMessage()));
			return -1;
		}
	}

	protected ResourceTable tryToLoadEntity(IdDt nextId) {
		ResourceTable entity;
		try {
			Long pid = translateForcedIdToPid(nextId);
			entity = myEntityManager.find(ResourceTable.class, pid);
		} catch (ResourceNotFoundException e) {
			entity = null;
		}
		return entity;
	}

}
