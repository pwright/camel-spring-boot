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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        BindyComplexRegexSeparatorTest.class,
        BindyComplexRegexSeparatorTest.TestConfiguration.class
    }
)
public class BindyComplexRegexSeparatorTest {

    @Autowired
    ProducerTemplate template;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @CsvRecord(separator = "[,;]", skipFirstLine = true)
    public static class Example {
        @DataField(pos = 1)
        private String field1;

        @DataField(pos = 2)
        private String field2;
    }

    @Test
    public void testUnmarshal() throws Exception {
        mock.expectedMessageCount(1);

        template.sendBody("direct:unmarshal", "header1,header2\n\"value1\",\"value,2\"");

        mock.assertIsSatisfied();
        Example body = mock.getReceivedExchanges().get(0).getIn().getBody(Example.class);
        assertEquals("value,2", body.field2);
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
                public void configure() {
                    from("direct:unmarshal").unmarshal().bindy(BindyType.Csv, Example.class).to("mock:result");
                }
            };
        }
    }
    
    

}
