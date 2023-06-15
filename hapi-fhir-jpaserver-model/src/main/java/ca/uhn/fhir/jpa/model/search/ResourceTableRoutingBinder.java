/*-
 * #%L
 * HAPI FHIR JPA Model
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
package ca.uhn.fhir.jpa.model.search;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

import ca.uhn.fhir.jpa.model.entity.ResourceTable;

public class ResourceTableRoutingBinder implements RoutingBinder {
    @Override
    public void bind(RoutingBindingContext theRoutingBindingContext) {
        theRoutingBindingContext.dependencies().use("myDeleted").use("myIndexStatus");
        theRoutingBindingContext.bridge(ResourceTable.class, new ResourceTableBridge());
    }

    private static class ResourceTableBridge implements RoutingBridge<ResourceTable> {

        @Override
        public void route(
                DocumentRoutes theDocumentRoutes,
                Object theO,
                ResourceTable theResourceTable,
                RoutingBridgeRouteContext theRoutingBridgeRouteContext) {
            if (theResourceTable.getDeleted() == null
                    && theResourceTable.getIndexStatus() != null) {
                theDocumentRoutes.addRoute();
            } else {
                theDocumentRoutes.notIndexed();
            }
        }

        @Override
        public void previousRoutes(
                DocumentRoutes theDocumentRoutes,
                Object theO,
                ResourceTable theResourceTable,
                RoutingBridgeRouteContext theRoutingBridgeRouteContext) {
            theDocumentRoutes.addRoute();
        }
    }
}
