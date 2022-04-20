package ca.uhn.fhir.batch2.jobs.config;

/*-
 * #%L
 * hapi-fhir-storage-batch2-jobs
 * %%
 * Copyright (C) 2014 - 2022 Smile CDR, Inc.
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

import ca.uhn.fhir.batch2.jobs.export.BulkExportAppCtx;
import ca.uhn.fhir.batch2.jobs.imprt.BulkImportAppCtx;
import ca.uhn.fhir.batch2.jobs.reindex.ReindexAppCtx;
import ca.uhn.fhir.batch2.jobs.services.Batch2JobRunnerImpl;
import ca.uhn.fhir.jpa.api.svc.IBatch2JobRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

//When you define a new batch job, add it here.
@Configuration
@Import({
	BulkImportAppCtx.class,
	ReindexAppCtx.class,
	BulkExportAppCtx.class
})
public class Batch2JobsConfig {

	// our batch2 job runner bean
	@Bean
	public IBatch2JobRunner batch2JobRunner() {
		return new Batch2JobRunnerImpl();
	}
}