/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.open.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.reporting.tooling.core.htmlreport.DefaultHtmlReportWriter;

public class JUnitContributorTests {

	@Test
	void contributesJUnitSpecificMetadata(@TempDir Path tempDir) throws Exception {
		var xmlFile = Files.writeString(tempDir.resolve("report.xml"),
			"""
					<e:events xmlns="https://schemas.opentest4j.org/reporting/core/0.2.0" xmlns:e="https://schemas.opentest4j.org/reporting/events/0.2.0" xmlns:java="https://schemas.opentest4j.org/reporting/java/0.2.0"
					          xmlns:junit="https://schemas.junit.org/open-test-reporting" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					          xsi:schemaLocation="https://schemas.junit.org/open-test-reporting https://schemas.junit.org/open-test-reporting/junit-1.9.xsd">
					    <e:started id="1" name="dummy" time="2024-11-10T16:31:35.000Z">
					        <metadata>
					            <junit:uniqueId>[engine:dummy]</junit:uniqueId>
					            <junit:legacyReportingName>dummy</junit:legacyReportingName>
					            <junit:type>CONTAINER</junit:type>
					        </metadata>
					    </e:started>
					    <e:started id="2" name="method" parentId="1" time="2024-11-10T16:31:35.001Z">
					        <metadata>
					            <junit:uniqueId>[engine:dummy]/[test:method]</junit:uniqueId>
					            <junit:legacyReportingName>method()</junit:legacyReportingName>
					            <junit:type>TEST</junit:type>
					        </metadata>
					    </e:started>
					    <e:finished id="2" time="2024-11-10T16:31:35.002Z">
					        <result status="SUCCESSFUL"/>
					    </e:finished>
					    <e:finished id="1" time="2024-11-10T16:31:35.003Z">
					        <result status="SUCCESSFUL"/>
					    </e:finished>
					</e:events>
					""");
		var htmlReport = tempDir.resolve("report.html");

		new DefaultHtmlReportWriter().writeHtmlReport(List.of(xmlFile), htmlReport);

		assertThat(htmlReport).content() //
				.contains("JUnit metadata") //
				.contains("Type").contains("CONTAINER") //
				.contains("Unique ID").contains("[engine:dummy]") //
				.contains("Legacy reporting name").contains("dummy") //
				.contains("Type").contains("TEST") //
				.contains("Unique ID").contains("[engine:dummy]/[test:method]") //
				.contains("Legacy reporting name").contains("method()");
	}
}
