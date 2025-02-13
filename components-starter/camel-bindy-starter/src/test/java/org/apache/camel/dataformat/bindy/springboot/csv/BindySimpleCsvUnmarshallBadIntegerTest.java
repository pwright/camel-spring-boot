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
package org.apache.camel.dataformat.bindy.springboot.csv;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        BindySimpleCsvUnmarshallBadIntegerTest.class,
        BindySimpleCsvUnmarshallBadIntegerTest.TestConfiguration.class
    }
)
public class BindySimpleCsvUnmarshallBadIntegerTest {

    private static final Logger LOG = LoggerFactory.getLogger(BindySimpleCsvUnmarshallBadIntegerTest.class);

    private static final String URI_MOCK_RESULT = "mock:result";
    private static final String URI_MOCK_ERROR = "mock:error";
    private static final String URI_DIRECT_START = "direct:start";

    @Produce(URI_DIRECT_START)
    protected ProducerTemplate template;

    private String record;

    @EndpointInject(URI_MOCK_RESULT)
    private MockEndpoint result;

    @EndpointInject(URI_MOCK_ERROR)
    private MockEndpoint error;

    @Test
    @DirtiesContext
    public void testIntegerMessage() throws Exception {

        record = "10000,25.10";

        template.sendBody(record);

        result.expectedMessageCount(1);
        result.assertIsSatisfied();

        Object data = result.getReceivedExchanges().get(0).getIn().getBody();

        LOG.info(">>> Model generated : " + data.getClass().getName());
    }

    @Test
    @DirtiesContext
    public void testIntegerTooBigError() throws Exception {
        record = "1000000000000000000000000000000000000,25.10";

        template.sendBody(record);

        // We don't expect to have a message as an error will be raised
        result.expectedMessageCount(0);

        // Message has been delivered to the mock error
        error.expectedMessageCount(1);

        result.assertIsSatisfied();
        error.assertIsSatisfied();

        // and check that we have the caused exception stored
        Exception cause = error.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        assertInstanceOf(Exception.class, cause.getCause());
        assertEquals("Parsing error detected for field defined at the position: 1, line: 1", cause.getMessage());

    }

    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                
                BindyCsvDataFormat orderBindyDataFormat
                    = new BindyCsvDataFormat(org.apache.camel.dataformat.bindy.model.simple.oneclassmath.Math.class);

                @Override
                public void configure() {

                    // default should errors go to mock:error
                    errorHandler(deadLetterChannel(URI_MOCK_ERROR));

                    onException(Exception.class).maximumRedeliveries(0).handled(true);

                    from(URI_DIRECT_START).unmarshal(orderBindyDataFormat).to(URI_MOCK_RESULT);

                }
            };
        }
    }
    
    

}
