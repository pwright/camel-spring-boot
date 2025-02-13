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
package org.apache.camel.dataformat.bindy.springboot.fix;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.kvp.BindyKeyValuePairDataFormat;
import org.apache.camel.dataformat.bindy.model.fix.complex.onetomany.Order;
import org.apache.camel.dataformat.bindy.springboot.CommonBindyTest;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        BindyComplexOneToManyKeyValuePairUnMarshallTest.class,
        BindyComplexOneToManyKeyValuePairUnMarshallTest.TestConfiguration.class
    }
)
public class BindyComplexOneToManyKeyValuePairUnMarshallTest extends CommonBindyTest {

    @Test
    @DirtiesContext
    public void testUnMarshallMessage() throws Exception {

        String message = "8=FIX 4.19=2034=135=049=INVMGR56=BRKR"
                         + "1=BE.CHM.00111=CHM0001-0158=this is a camel - bindy test" + "22=448=BE000124567854=1"
                         + "22=548=BE000987654354=2" + "22=648=BE000999999954=3" + "10=220";
        String message2 = "8=FIX 4.19=2034=135=049=INVMGR56=BRKR"
                          + "1=BE.CHM.00111=CHM0001-0158=this is a camel - bindy test10=220";

        result.expectedMessageCount(2);
        template.sendBody(message);
        template.sendBody(message2);

        result.assertIsSatisfied();

        Order order1 = result.getReceivedExchanges().get(0).getIn().getBody(Order.class);
        Order order2 = result.getReceivedExchanges().get(1).getIn().getBody(Order.class);

        assertTrue(order1.toString().contains("BE.CHM.001, 11: CHM0001-01, 58: this is a camel - bindy test"));
        assertTrue(order1.getSecurities().get(0).toString().contains("22: 4, 48: BE0001245678, 54: 1"));
        assertTrue(order1.getSecurities().get(1).toString().contains("22: 5, 48: BE0009876543, 54: 2"));
        assertTrue(order1.getSecurities().get(2).toString().contains("22: 6, 48: BE0009999999, 54: 3"));
        assertTrue(order2.getHeader().toString().contains("FIX 4.1, 9: 20, 34: 1 , 35: 0, 49: INVMGR, 56: BRKR"));
        assertTrue(order2.getTrailer().toString().contains("10: 220"));
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
                    BindyKeyValuePairDataFormat kvpBindyDataFormat
                        = new BindyKeyValuePairDataFormat(org.apache.camel.dataformat.bindy.model.fix.complex.onetomany.Order.class);

                    from(URI_DIRECT_START).unmarshal(kvpBindyDataFormat).to(URI_MOCK_RESULT);
                }
            };
        }
    }
    
    

}
