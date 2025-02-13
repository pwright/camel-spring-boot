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
package org.apache.camel.component.fhir.dataformat;

import java.io.InputStream;
import java.io.InputStreamReader;
import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FhirJsonDataFormatTest.class,
                FhirJsonDataFormatTest.TestConfiguration.class,
        }
)
public class FhirJsonDataFormatTest {

    private static final String PATIENT = "{\"resourceType\":\"Patient\","
                                          + "\"name\":[{\"family\":\"Holmes\",\"given\":[\"Sherlock\"]}],"
                                          + "\"address\":[{\"line\":[\"221b Baker St, Marylebone, London NW1 6XE, UK\"]}]}";

    private MockEndpoint mockEndpoint;

    @Autowired
    CamelContext camelContext;

    @Autowired
    protected ProducerTemplate template;

    @BeforeEach
    public void setUp() throws Exception {
        mockEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.reset();
    }

    @Test
    public void unmarshal() throws Exception {
        mockEndpoint.expectedMessageCount(1);

        template.sendBody("direct:unmarshal", PATIENT);

        mockEndpoint.assertIsSatisfied();

        Exchange exchange = mockEndpoint.getExchanges().get(0);
        Patient patient = (Patient) exchange.getIn().getBody();
        assertTrue(patient.equalsDeep(getPatient()), "Patients should be equal!");
    }

    @Test
    public void marshal() throws Exception {
        mockEndpoint.expectedMessageCount(1);

        Patient patient = getPatient();
        template.sendBody("direct:marshal", patient);

        mockEndpoint.assertIsSatisfied();

        Exchange exchange = mockEndpoint.getExchanges().get(0);
        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        IBaseResource iBaseResource = FhirContext.forR4().newJsonParser().parseResource(new InputStreamReader(inputStream));
        assertTrue(patient.equalsDeep((Base) iBaseResource), "Patients should be equal!");
    }

    private Patient getPatient() {
        Patient patient = new Patient();
        patient.addName(new HumanName().addGiven("Sherlock").setFamily("Holmes"))
                .addAddress(new Address().addLine("221b Baker St, Marylebone, London NW1 6XE, UK"));
        return patient;
    }

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:marshal")
                            .marshal().fhirJson("R4")
                            .to("mock:result");

                    from("direct:unmarshal")
                            .unmarshal().fhirJson()
                            .to("mock:result");
                }
            };
        }
    }
}
