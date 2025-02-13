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
package org.apache.camel.dataformat.asn1.springboot;

import javax.annotation.Generated;
import org.apache.camel.spring.boot.DataFormatConfigurationPropertiesCommon;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Encode and decode data structures using Abstract Syntax Notation One (ASN.1).
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.springboot.maven.SpringBootAutoConfigurationMojo")
@ConfigurationProperties(prefix = "camel.dataformat.asn1")
public class ASN1DataFormatConfiguration
        extends
            DataFormatConfigurationPropertiesCommon {

    /**
     * Whether to enable auto configuration of the asn1 data format. This is
     * enabled by default.
     */
    private Boolean enabled;
    /**
     * Class to use when unmarshalling.
     */
    private String unmarshalType;
    /**
     * If the asn1 file has more than one entry, the setting this option to
     * true, allows working with the splitter EIP, to split the data using an
     * iterator in a streaming mode.
     */
    private Boolean usingIterator = false;

    public String getUnmarshalType() {
        return unmarshalType;
    }

    public void setUnmarshalType(String unmarshalType) {
        this.unmarshalType = unmarshalType;
    }

    public Boolean getUsingIterator() {
        return usingIterator;
    }

    public void setUsingIterator(Boolean usingIterator) {
        this.usingIterator = usingIterator;
    }
}