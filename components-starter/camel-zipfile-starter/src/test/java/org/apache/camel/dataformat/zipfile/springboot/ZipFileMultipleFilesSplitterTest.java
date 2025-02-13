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
package org.apache.camel.dataformat.zipfile.springboot;


import java.util.Iterator;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;
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
        ZipFileMultipleFilesSplitterTest.class,
        ZipFileMultipleFilesSplitterTest.TestConfiguration.class
    }
)
public class ZipFileMultipleFilesSplitterTest extends ZipSplitterRouteTest {

    static final String PROCESSED_FILES_HEADER_NAME = "processedFiles";
    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
   
    
    @EndpointInject("mock:splitResult")
    MockEndpoint splitResult;
    
    
    @Override
    @Test
    public void testSplitter() throws InterruptedException {
        processZipEntry.expectedBodiesReceivedInAnyOrder("chau", "hi", "hola", "another_chiau", "another_hi");
        splitResult.expectedBodiesReceivedInAnyOrder("chiau.txt", "hi.txt", "hola.txt", "directoryOne/another_chiau.txt",
                "directoryOne/another_hi.txt");
        processZipEntry.assertIsSatisfied();
        splitResult.assertIsSatisfied();
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
                    // Unzip file and Split it according to FileEntry
                    ZipFileDataFormat zipFile = new ZipFileDataFormat();
                    zipFile.setUsingIterator(true);
                    from("file:src/test/resources/org/apache/camel/dataformat/zipfile/data/?delay=1000&noop=true")
                            .unmarshal(zipFile)
                            .split(bodyAs(Iterator.class))
                            .streaming()
                            .aggregationStrategy(updateHeader())
                            .convertBodyTo(String.class)
                            .to("mock:processZipEntry")
                            .end()
                            .log("Done processing big file: ${header.CamelFileName}")
                            .setBody().header(PROCESSED_FILES_HEADER_NAME)
                            .split().body()
                            .to("mock:splitResult");
                }
                
                private AggregationStrategy updateHeader() {
                    return new AggregationStrategy() {
                        @Override
                        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                            if (oldExchange != null) {
                                String processedFiles = oldExchange.getIn().getHeader(PROCESSED_FILES_HEADER_NAME, String.class);
                                if (processedFiles == null) {
                                    processedFiles = oldExchange.getIn().getHeader("zipFileName", String.class);
                                }
                                processedFiles = processedFiles + "," + newExchange.getIn().getHeader("zipFileName", String.class);
                                newExchange.getIn().setHeader(PROCESSED_FILES_HEADER_NAME, processedFiles);
                            }
                            return newExchange;
                        }

                    };
                }
            };
        }
    }
    
    
}
