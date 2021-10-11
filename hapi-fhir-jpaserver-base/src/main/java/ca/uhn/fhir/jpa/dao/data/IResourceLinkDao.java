package ca.uhn.fhir.jpa.dao.data;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2021 Smile CDR, Inc.
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

import ca.uhn.fhir.jpa.model.entity.ResourceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IResourceLinkDao extends JpaRepository<ResourceLink, Long>, IHapiFhirJpaRepository {

	@Modifying
	@Query("DELETE FROM ResourceLink t WHERE t.mySourceResourcePid = :resId")
	void deleteByResourceId(@Param("resId") Long theResourcePid);

	@Query("SELECT t FROM ResourceLink t WHERE t.mySourceResourcePid = :resId")
	List<ResourceLink> findAllForSourceResourceId(@Param("resId") Long thePatientId);

	@Query("SELECT t FROM ResourceLink t WHERE t.myTargetResourcePid in :resIds")
	List<ResourceLink> findWithTargetPidIn(@Param("resIds") List<Long> thePids);
}
