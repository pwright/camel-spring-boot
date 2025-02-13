/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.fhir;

import java.util.Map;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.infra.fhir.services.FhirService;
import org.apache.camel.test.infra.fhir.services.FhirServiceFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractFhirTestSupport {

    @RegisterExtension
    public static FhirService service = FhirServiceFactory.createSingletonService();

    protected Patient patient;
    @Autowired
    protected FhirContext fhirContext;
    @Autowired
    protected CamelContext camelContext;
    @Autowired
    protected IGenericClient fhirClient;
    @Autowired
    protected ProducerTemplate template;

    @BeforeEach
    public void cleanFhirServerState() {
        if (patientExists()) {
            deletePatient();
            assertFalse(patientExists());
        }
        createPatient();
        assertTrue(patientExists());
    }

    boolean patientExists() {
        try {
            Bundle bundle
                    = fhirClient.search().byUrl("Patient?given=Vincent&family=Freeman").returnBundle(Bundle.class).execute();
            return !bundle.getEntry().isEmpty();
        } catch (ResourceGoneException e) {
            return false;
        }
    }

    private void deletePatient() {
        fhirClient.delete().resourceConditionalByUrl("Patient?given=Vincent&family=Freeman").execute();
    }

    private void createPatient() {
        this.patient = new Patient().addName(new HumanName().addGiven("Vincent").setFamily("Freeman")).setActive(false);
        this.patient.setId(fhirClient.create().resource(patient).execute().getId());
    }

    <T> T requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers)
            throws CamelExecutionException {
        return (T) template.requestBodyAndHeaders(endpointUri, body, headers);
    }

    @SuppressWarnings("unchecked")
    <T> T requestBody(String endpoint, Object body) throws CamelExecutionException {
        return (T) template.requestBody(endpoint, body);
    }
}
