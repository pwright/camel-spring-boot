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
package org.apache.camel.component.jsonpath.springboot.test;



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        JsonPathSuppressTest.class,
        JsonPathSuppressTest.TestConfiguration.class
    }
)
public class JsonPathSuppressTest {

    @Autowired
    CamelContext context;
    
    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:middle")
    MockEndpoint mockMiddle;
    
    @EndpointInject("mock:other")
    MockEndpoint mockOther;

    @Test
    public void testMiddle() throws Exception {
        String json = "{\"person\" : {\"firstname\" : \"foo\", \"middlename\" : \"foo2\", \"lastname\" : \"bar\"}}";
        MockEndpoint.resetMocks(context);
        mockMiddle.expectedMessageCount(1);
        mockOther.expectedMessageCount(0);

        template.sendBody("direct:start", json);

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testNoMiddle() throws Exception {
        String json = "{\"person\" : {\"firstname\" : \"foo\", \"lastname\" : \"bar\"}}";
        MockEndpoint.resetMocks(context);
        mockMiddle.expectedMessageCount(0);
        mockOther.expectedMessageCount(1);

        template.sendBody("direct:start", json);

        MockEndpoint.assertIsSatisfied(context);
    }

    

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                            .choice()
                            .when().jsonpath("person.middlename", true)
                            .to("mock:middle")
                            .otherwise()
                            .to("mock:other");
                }
            };
        }
    }
}
