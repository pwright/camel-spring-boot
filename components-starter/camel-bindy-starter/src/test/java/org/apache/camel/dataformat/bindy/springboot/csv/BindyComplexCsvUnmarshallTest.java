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

import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        BindyComplexCsvUnmarshallTest.class,
        BindyComplexCsvUnmarshallTest.TestConfiguration.class
    }
)
public class BindyComplexCsvUnmarshallTest {

    private static final Class<?> TYPE = org.apache.camel.dataformat.bindy.model.complex.twoclassesandonelink.Order.class;

    @Produce("direct:start")
    protected ProducerTemplate template;

    private String record = "01,,Albert,Cartier,ISIN,BE12345678,SELL,,1500,EUR,08-01-2009\r\n"
                            + "02,A1,,Preud'Homme,ISIN,XD12345678,BUY,,2500,USD,08-01-2009\r\n"
                            + "03,A2,Jacques,,,BE12345678,SELL,,1500,EUR,08-01-2009\r\n"
                            + "04,A3,Michel,Dupond,,,BUY,,2500,USD,08-01-2009\r\n"
                            + "05,A4,Annie,Dutronc,ISIN,BE12345678,,,1500,EUR,08-01-2009\r\n" + "06,A5,Andr" + "\uc3a9"
                            + ",Rieux,ISIN,XD12345678,SELL,Share,,USD,08-01-2009\r\n"
                            + "07,A6,Myl" + "\uc3a8" + "ne,Farmer,ISIN,BE12345678,BUY,1500,,,08-01-2009\r\n"
                            + "08,A7,Eva,Longoria,ISIN,XD12345678,SELL,Share,2500,USD,\r\n"
                            + ",,,D,,BE12345678,SELL,,,,08-01-2009\r\n" + ",,,D,ISIN,BE12345678,,,,,08-01-2009\r\n"
                            + ",,,D,ISIN,LU123456789,,,,,\r\n"
                            + "10,A8,Pauline,M,ISIN,XD12345678,SELL,Share,2500,USD,08-01-2009\r\n"
                            + "10,A9,Pauline,M,ISIN,XD12345678,BUY,Share,2500.45,USD,08-01-2009";

    private String singleRecord = "01,,Albert,Cartier,ISIN,BE12345678,SELL,,1500,EUR,08-01-2009";

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;
    
    @Test
    public void testUnMarshallMessage() throws Exception {
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().isInstanceOf(List.class);

        template.sendBody(record);

        resultEndpoint.assertIsSatisfied();

        // there should be 13 element in the list
        List list = resultEndpoint.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertEquals(13, list.size());

        resultEndpoint.reset();

        // now single test
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().isInstanceOf(TYPE);

        template.sendBody(singleRecord);

        resultEndpoint.assertIsSatisfied();
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
                    from("direct:start")
                            .unmarshal(new BindyCsvDataFormat(TYPE))
                            .to("mock:result");
                }
            };
        }
    }
    
    

}
