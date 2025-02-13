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


import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.jsonpath.JsonPathConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        JsonPathCharsetTest.class,
        JsonPathCharsetTest.TestConfiguration.class
    }
)
public class JsonPathCharsetTest {

    @Autowired
    ProducerTemplate template;

    @EndpointInject("mock:authors")
    MockEndpoint mock;

    @Test
    public void testUTF16BEFile() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        sendBody("direct:start", new File("src/test/resources/booksUTF16BE.json"));

        mock.assertIsSatisfied();

        check();
    }

    @Test
    public void testUTF16LEFile() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        sendBody("direct:start", new File("src/test/resources/booksUTF16LE.json"));

        mock.assertIsSatisfied();

        check();
    }

    @Test
    public void testUTF16BEInputStream() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        InputStream input = JsonPathCharsetTest.class.getClassLoader().getResourceAsStream("booksUTF16BE.json");
        assertNotNull(input);
        sendBody("direct:start", input);

        mock.assertIsSatisfied();

        check();
    }

    @Test
    public void testUTF16BEURL() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        URL url = new URL("file:src/test/resources/booksUTF16BE.json");
        assertNotNull(url);
        sendBody("direct:start", url);

        check();
    }

    @Test
    public void testISO8859WithJsonHeaderCamelJsonInputEncoding() throws Exception {
        mock.reset();
        mock.expectedMessageCount(1);

        URL url = new URL("file:src/test/resources/germanbooks-iso-8859-1.json");
        assertNotNull(url);
        sendBody("direct:start", url,
                Collections.<String, Object> singletonMap(JsonPathConstants.HEADER_JSON_ENCODING, "ISO-8859-1"));

        check("Joseph und seine Brüder", "Götzendämmerung");
    }

    private void check() throws InterruptedException {
        check("Sayings of the Century", "Sword of Honour");
    }

    private void check(String title1, String title2) throws InterruptedException {
        mock.assertIsSatisfied();

        List<?> authors = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);

        assertEquals(title1, authors.get(0));
        assertEquals(title2, authors.get(1));
    }

    private void sendBody(String endpointUri, final Object body) {
        template.send(endpointUri, exchange -> {
            Message in = exchange.getIn();
            in.setBody(body);
        });
    }
    
    private void sendBody(String endpointUri, final Object body, final Map<String, Object> headers) {
        template.send(endpointUri, exchange -> {
            Message in = exchange.getIn();
            in.setBody(body);
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                in.setHeader(entry.getKey(), entry.getValue());
            }
        });
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
                    from("direct:start").transform().jsonpath("$.store.book[*].title").to("mock:authors");
                }
            };
        }
    }
}
