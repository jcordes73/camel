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
package org.apache.camel.dsl.jbang.core.commands.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import com.github.freva.asciitable.OverflowBehaviour;
import org.apache.camel.dsl.jbang.core.commands.CamelJBangMain;
import org.apache.camel.util.TimeUtils;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "service", aliases = { "service", "services" },
         description = "Get services of Camel integrations")
public class ListService extends ProcessBaseCommand {

    @CommandLine.Parameters(description = "Name or pid of running Camel integration", arity = "0..1")
    String name = "*";

    @CommandLine.Option(names = { "--sort" },
                        description = "Sort by pid, name or age", defaultValue = "pid")
    String sort;

    public ListService(CamelJBangMain main) {
        super(main);
    }

    @Override
    public Integer call() throws Exception {
        List<Row> rows = new ArrayList<>();

        List<Long> pids = findPids(name);
        ProcessHandle.allProcesses()
                .filter(ph -> pids.contains(ph.pid()))
                .forEach(ph -> {
                    JsonObject root = loadStatus(ph.pid());
                    // there must be a status file for the running Camel integration
                    if (root != null) {
                        Row row = new Row();
                        JsonObject context = (JsonObject) root.get("context");
                        row.name = context.getString("name");
                        if ("CamelJBang".equals(row.name)) {
                            row.name = extractName(root, ph);
                        }
                        row.pid = "" + ph.pid();
                        row.uptime = extractSince(ph);
                        row.age = TimeUtils.printSince(row.uptime);

                        JsonObject jo = (JsonObject) root.get("services");
                        if (jo != null) {
                            jo = (JsonObject) jo.get("platform-http");
                        }
                        if (jo != null) {
                            JsonArray arr = (JsonArray) jo.get("endpoints");
                            for (int i = 0; i < arr.size(); i++) {
                                if (i > 0) {
                                    // create a copy for 2+ secrets
                                    row = row.copy();
                                }
                                jo = (JsonObject) arr.get(i);
                                row.component = "platform-http";
                                row.protocol = "http";
                                row.service = jo.getString("url");
                                row.verbs = jo.getString("verbs");
                                rows.add(row);
                            }
                        }
                    }
                });

        // sort rows
        rows.sort(this::sortRow);

        if (!rows.isEmpty()) {
            System.out.println(AsciiTable.getTable(AsciiTable.NO_BORDERS, rows, Arrays.asList(
                    new Column().header("PID").headerAlign(HorizontalAlign.CENTER).with(r -> r.pid),
                    new Column().header("NAME").dataAlign(HorizontalAlign.LEFT).maxWidth(30, OverflowBehaviour.ELLIPSIS_RIGHT)
                            .with(r -> r.name),
                    new Column().header("COMPONENT").dataAlign(HorizontalAlign.LEFT).with(r -> r.component),
                    new Column().header("PROTOCOL").dataAlign(HorizontalAlign.LEFT).with(this::getProtocol),
                    new Column().header("SERVICE").dataAlign(HorizontalAlign.LEFT).with(this::getService))));
        }

        return 0;
    }

    private String extractPlatform(ProcessHandle ph, JsonObject runtime) {
        String answer = runtime != null ? runtime.getString("platform") : null;
        if ("Camel".equals(answer)) {
            // generic camel, we need to check if we run in JBang
            String cl = ph.info().commandLine().orElse("");
            if (cl.contains("main.CamelJBang run")) {
                answer = "JBang";
            }
        }
        return answer;
    }

    protected int sortRow(Row o1, Row o2) {
        String s = sort;
        int negate = 1;
        if (s.startsWith("-")) {
            s = s.substring(1);
            negate = -1;
        }
        switch (s) {
            case "pid":
                return Long.compare(Long.parseLong(o1.pid), Long.parseLong(o2.pid)) * negate;
            case "name":
                return o1.name.compareToIgnoreCase(o2.name) * negate;
            case "age":
                return Long.compare(o1.uptime, o2.uptime) * negate;
            default:
                return 0;
        }
    }

    private String getProtocol(Row r) {
        String s = r.protocol;
        if (r.verbs != null) {
            s = "rest";
        }
        return s;
    }

    private String getService(Row r) {
        String s = r.service;
        if (r.verbs != null) {
            s += " (" + r.verbs + ")";
        }
        return s;
    }

    private static class Row implements Cloneable {
        String pid;
        String name;
        String age;
        long uptime;
        String component;
        String protocol;
        String service;
        String verbs;

        Row copy() {
            try {
                return (Row) clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

}
