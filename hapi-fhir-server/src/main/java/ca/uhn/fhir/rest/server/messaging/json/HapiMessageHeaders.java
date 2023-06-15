/*-
 * #%L
 * HAPI FHIR - Server Framework
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
package ca.uhn.fhir.rest.server.messaging.json;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.MessageHeaders;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.uhn.fhir.model.api.IModelJson;

import static java.util.Objects.isNull;

/**
 * This class is for holding headers for BaseJsonMessages. Any serializable data can be thrown into
 * the header map. There are also three special headers, defined by the constants in this class,
 * which are for use in message handling retrying. There are also matching helper functions for
 * fetching those special variables; however they can also be accessed in standard map fashion with
 * a `get` on the map.
 */
public class HapiMessageHeaders implements IModelJson {
    public static final String RETRY_COUNT_KEY = "retryCount";
    public static final String FIRST_FAILURE_KEY = "firstFailureTimestamp";
    public static final String LAST_FAILURE_KEY = "lastFailureTimestamp";

    @JsonProperty(RETRY_COUNT_KEY)
    private Integer myRetryCount = 0;

    @JsonProperty(FIRST_FAILURE_KEY)
    private Long myFirstFailureTimestamp;

    @JsonProperty(LAST_FAILURE_KEY)
    private Long myLastFailureTimestamp;

    @JsonProperty("customHeaders")
    private final Map<String, Object> headers;

    public HapiMessageHeaders(Map<String, Object> theHeaders) {
        headers = theHeaders;
    }

    public HapiMessageHeaders() {
        headers = new HashMap<>();
    }

    public Integer getRetryCount() {
        if (isNull(this.myRetryCount)) {
            return 0;
        }
        return this.myRetryCount;
    }

    public Long getFirstFailureTimestamp() {
        return this.myFirstFailureTimestamp;
    }

    public Long getLastFailureTimestamp() {
        return this.myLastFailureTimestamp;
    }

    public void setRetryCount(Integer theRetryCount) {
        this.myRetryCount = theRetryCount;
    }

    public void setLastFailureTimestamp(Long theLastFailureTimestamp) {
        this.myLastFailureTimestamp = theLastFailureTimestamp;
    }

    public void setFirstFailureTimestamp(Long theFirstFailureTimestamp) {
        this.myFirstFailureTimestamp = theFirstFailureTimestamp;
    }

    public Map<String, Object> getCustomHeaders() {
        if (this.headers == null) {
            return new HashMap<>();
        }
        return this.headers;
    }

    public MessageHeaders toMessageHeaders() {
        Map<String, Object> returnedHeaders = new HashMap<>(this.headers);
        returnedHeaders.put(RETRY_COUNT_KEY, myRetryCount);
        returnedHeaders.put(FIRST_FAILURE_KEY, myFirstFailureTimestamp);
        returnedHeaders.put(LAST_FAILURE_KEY, myLastFailureTimestamp);
        return new MessageHeaders(returnedHeaders);
    }
}
