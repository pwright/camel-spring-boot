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
package org.apache.camel.component.paho.springboot;



import java.io.UnsupportedEncodingException;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.paho.PahoComponent;
import org.apache.camel.component.paho.PahoConstants;
import org.apache.camel.component.paho.PahoEndpoint;
import org.apache.camel.component.paho.PahoMessage;
import org.apache.camel.component.paho.PahoPersistence;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.infra.activemq.services.ActiveMQEmbeddedService;
import org.apache.camel.test.infra.activemq.services.ActiveMQEmbeddedServiceBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.eclipse.paho.client.mqttv3.MqttMessage;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        PahoComponentTest.class,
        PahoComponentTest.TestConfiguration.class
    }
)
public class PahoComponentTest {
    
    static int mqttPort = AvailablePortFinder.getNextAvailable();

    @RegisterExtension
    public static ActiveMQEmbeddedService service = ActiveMQEmbeddedServiceBuilder
            .bare()
            .withPersistent(false)
            .withMqttTransport(mqttPort)
            .build();


    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:test")
    MockEndpoint mock;
    
    @EndpointInject("mock:testCustomizedPaho")
    MockEndpoint testCustomizedPahoMock;
    
    @Test
    public void checkOptions() {
        String uri = "paho:/test/topic" + "?clientId=sampleClient" + "&brokerUrl=tcp://localhost:" + mqttPort + "&qos=2"
                     + "&persistence=file";

        PahoEndpoint endpoint = context.getEndpoint(uri, PahoEndpoint.class);

        // Then
        assertEquals("/test/topic", endpoint.getTopic());
        assertEquals("sampleClient", endpoint.getConfiguration().getClientId());
        assertEquals("tcp://localhost:" + mqttPort, endpoint.getConfiguration().getBrokerUrl());
        assertEquals(2, endpoint.getConfiguration().getQos());
        assertEquals(PahoPersistence.FILE, endpoint.getConfiguration().getPersistence());
    }

    @Test
    public void shouldReadMessageFromMqtt() throws InterruptedException {
        mock.reset();
        // Given
        String msg = "msg";
        mock.expectedBodiesReceived(msg);

        // When
        template.sendBody("direct:test", msg);

        // Then
        mock.assertIsSatisfied();
    }

    @Test
    public void shouldNotReadMessageFromUnregisteredTopic() throws InterruptedException {
        mock.reset();
        // Given
        mock.expectedMessageCount(0);

        // When
        template.sendBody("paho:someRandomQueue?brokerUrl=tcp://localhost:" + mqttPort, "msg");

        // Then
        mock.assertIsSatisfied();
    }

    @Test
    public void shouldKeepDefaultMessageInHeader() throws InterruptedException, UnsupportedEncodingException {
        mock.reset();
        // Given
        final String msg = "msg";
        mock.expectedBodiesReceived(msg);

        // When
        template.sendBody("direct:test", msg);

        // Then
        mock.assertIsSatisfied();

        Exchange exchange = mock.getExchanges().get(0);
        String payload = new String((byte[]) exchange.getIn().getBody(), "utf-8");

        assertEquals("queue", exchange.getIn().getHeader(PahoConstants.MQTT_TOPIC));
        assertEquals(msg, payload);
    }

    @Test
    public void shouldKeepOriginalMessageInHeader() throws InterruptedException {
        mock.reset();
        // Given
        final String msg = "msg";
        mock.expectedBodiesReceived(msg);

        // When
        template.sendBody("direct:test2", msg);

        // Then
        mock.assertIsSatisfied();
        Exchange exchange = mock.getExchanges().get(0);

        MqttMessage message = exchange.getIn(PahoMessage.class).getMqttMessage();
        assertNotNull(message);
        assertEquals(msg, new String(message.getPayload()));
    }

    @Test
    public void shouldReadMessageFromCustomizedComponent() throws InterruptedException {
        testCustomizedPahoMock.reset();
        // Given
        String msg = "msg";
        testCustomizedPahoMock.expectedBodiesReceived(msg);

        // When
        template.sendBody("direct:testCustomizedPaho", msg);

        // Then
        testCustomizedPahoMock.assertIsSatisfied();
    }

    @Test
    public void shouldNotSendMessageAuthIsNotValid() throws InterruptedException {
        mock.reset();
        // Given
        mock.expectedMessageCount(0);

        // When
        template.sendBody("paho:someRandomQueue?brokerUrl=tcp://localhost:" + mqttPort + "&userName=test&password=test", "msg");

        // Then
        mock.assertIsSatisfied();
    }

    
    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {

        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    PahoComponent customizedPaho = new PahoComponent();
                    context.addComponent("customizedPaho", customizedPaho);

                    from("direct:test").to("paho:queue?brokerUrl=tcp://localhost:" + mqttPort);
                    from("paho:queue?brokerUrl=tcp://localhost:" + mqttPort).to("mock:test");

                    from("direct:test2").to("paho:queue?brokerUrl=tcp://localhost:" + mqttPort);

                    from("paho:persistenceTest?persistence=FILE&brokerUrl=tcp://localhost:" + mqttPort).to("mock:persistenceTest");

                    from("direct:testCustomizedPaho").to("customizedPaho:testCustomizedPaho?brokerUrl=tcp://localhost:" + mqttPort);
                    from("paho:testCustomizedPaho?brokerUrl=tcp://localhost:" + mqttPort).to("mock:testCustomizedPaho");
                }
            };
        }
    }
    
   

}
