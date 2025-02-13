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
package org.apache.camel.component.validator;

import java.io.FileNotFoundException;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                ValidatorLazyStartProducerTest.class
        }
)
public class ValidatorLazyStartProducerTest extends ContextTestSupport {

    @Test
    public void testLazyStartProducerFail() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
        mock.reset();
        mock.expectedMessageCount(0);

        try {
            template.sendBody("direct:fail",
                    "<mail xmlns='http://foo.com/bar'><subject>Hey</subject><body>Hello world!</body></mail>");
            fail("Should throw exception");
        } catch (Exception e) {
            assertIsInstanceOf(FileNotFoundException.class, e.getCause());
        }

        mock.assertIsSatisfied();
    }

    @Test
    public void testLazyStartProducerOk() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
        mock.reset();
        mock.expectedMessageCount(1);

        template.sendBody("direct:ok",
                "<mail xmlns='http://foo.com/bar'><subject>Hey</subject><body>Hello world!</body></mail>");

        mock.assertIsSatisfied();
    }

    @Bean
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:fail").to("validator:org/apache/camel/component/validator/unknown.xsd?lazyStartProducer=true")
                        .to("mock:result");

                from("direct:ok").to("validator:org/apache/camel/component/validator/schema.xsd?lazyStartProducer=true")
                        .to("mock:result");
            }
        };
    }
}
