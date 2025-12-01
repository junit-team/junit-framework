/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.reporting.open.xml.JUnitContributor;
import org.junit.platform.reporting.open.xml.OpenTestReportGeneratingListener;
import org.opentest4j.reporting.tooling.spi.htmlreport.Contributor;

/**
 * Defines the JUnit Platform Reporting API.
 *
 * @since 1.4
 */
module org.junit.platform.reporting {

	requires static transitive org.apiguardian.api;
	requires static transitive org.jspecify;

	requires java.xml;
	requires org.junit.platform.commons;
	requires transitive org.junit.platform.engine;
	requires transitive org.junit.platform.launcher;
	requires org.opentest4j.reporting.tooling.spi;

	// exports org.junit.platform.reporting; empty package
	exports org.junit.platform.reporting.legacy;
	exports org.junit.platform.reporting.legacy.xml;
	exports org.junit.platform.reporting.open.xml;

	provides TestExecutionListener
			with OpenTestReportGeneratingListener;

	provides Contributor
			with JUnitContributor;
}
