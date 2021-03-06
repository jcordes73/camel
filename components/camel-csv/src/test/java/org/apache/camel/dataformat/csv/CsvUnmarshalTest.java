/**
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
package org.apache.camel.dataformat.csv;

import java.util.Iterator;
import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.apache.camel.dataformat.csv.TestUtils.asMap;

/**
 * This class tests standard unmarshalling
 */
public class CsvUnmarshalTest extends CamelTestSupport {
    private static final String CSV_SAMPLE = "A,B,C\r1,2,3\rone,two,three";

    @EndpointInject(uri = "mock:output")
    MockEndpoint output;

    @Test
    public void shouldUseDefaultFormat() throws Exception {
        output.expectedMessageCount(1);

        template.sendBody("direct:default", CSV_SAMPLE);
        output.assertIsSatisfied();

        List body = assertIsInstanceOf(List.class, output.getExchanges().get(0).getIn().getBody());
        assertEquals(3, body.size());
        assertEquals(asList("A", "B", "C"), body.get(0));
        assertEquals(asList("1", "2", "3"), body.get(1));
        assertEquals(asList("one", "two", "three"), body.get(2));
    }

    @Test
    public void shouldUseDelimiter() throws Exception {
        output.expectedMessageCount(1);

        template.sendBody("direct:delimiter", CSV_SAMPLE.replace(',', '_'));
        output.assertIsSatisfied();

        List body = assertIsInstanceOf(List.class, output.getExchanges().get(0).getIn().getBody());
        assertEquals(asList("A", "B", "C"), body.get(0));
        assertEquals(asList("1", "2", "3"), body.get(1));
        assertEquals(asList("one", "two", "three"), body.get(2));
    }

    @Test
    public void shouldUseLazyLoading() throws Exception {
        output.expectedMessageCount(1);

        template.sendBody("direct:lazy", CSV_SAMPLE);
        output.assertIsSatisfied();

        Iterator body = assertIsInstanceOf(Iterator.class, output.getExchanges().get(0).getIn().getBody());
        assertEquals(asList("A", "B", "C"), body.next());
        assertEquals(asList("1", "2", "3"), body.next());
        assertEquals(asList("one", "two", "three"), body.next());
        assertFalse(body.hasNext());
    }

    @Test
    public void shouldUseMaps() throws Exception {
        output.expectedMessageCount(1);

        template.sendBody("direct:map", CSV_SAMPLE);
        output.assertIsSatisfied();

        List body = assertIsInstanceOf(List.class, output.getExchanges().get(0).getIn().getBody());
        assertEquals(2, body.size());
        assertEquals(asMap("A", "1", "B", "2", "C", "3"), body.get(0));
        assertEquals(asMap("A", "one", "B", "two", "C", "three"), body.get(1));
    }

    @Test
    public void shouldUseLazyLoadingAndMaps() throws Exception {
        output.expectedMessageCount(1);

        template.sendBody("direct:lazy_map", CSV_SAMPLE);
        output.assertIsSatisfied();

        Iterator body = assertIsInstanceOf(Iterator.class, output.getExchanges().get(0).getIn().getBody());
        assertEquals(asMap("A", "1", "B", "2", "C", "3"), body.next());
        assertEquals(asMap("A", "one", "B", "two", "C", "three"), body.next());
        assertFalse(body.hasNext());
    }

    @Test
    public void shouldUseMapsAndHeaders() throws Exception {
        output.expectedMessageCount(1);

        template.sendBody("direct:map_headers", CSV_SAMPLE);
        output.assertIsSatisfied();

        List body = assertIsInstanceOf(List.class, output.getExchanges().get(0).getIn().getBody());
        assertEquals(2, body.size());
        assertEquals(asMap("AA", "1", "BB", "2", "CC", "3"), body.get(0));
        assertEquals(asMap("AA", "one", "BB", "two", "CC", "three"), body.get(1));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Default format
                from("direct:default")
                        .unmarshal(new CsvDataFormat())
                        .to("mock:output");

                // Format with special delimiter
                from("direct:delimiter")
                        .unmarshal(new CsvDataFormat().setDelimiter('_'))
                        .to("mock:output");

                // Lazy load
                from("direct:lazy")
                        .unmarshal(new CsvDataFormat().setLazyLoad(true))
                        .to("mock:output");

                // Use maps
                from("direct:map")
                        .unmarshal(new CsvDataFormat().setUseMaps(true))
                        .to("mock:output");

                // Use lazy load and maps
                from("direct:lazy_map")
                        .unmarshal(new CsvDataFormat().setLazyLoad(true).setUseMaps(true))
                        .to("mock:output");

                // Use map without first line and headers
                from("direct:map_headers")
                        .unmarshal(new CsvDataFormat().setUseMaps(true).setSkipHeaderRecord(true).setHeader(new String[]{"AA", "BB", "CC"}))
                        .to("mock:output");
            }
        };
    }
}
