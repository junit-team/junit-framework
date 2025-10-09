/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.onramp;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

@API(status = EXPERIMENTAL, since = "6.0")
public final class JUnit {

	private JUnit() {
	}

	public static void run(Object instance) {
		var listener = new SummaryGeneratingListener();
		var builder = request();

		if (instance instanceof Class<?> testClass) {
			// handling `JUnit.run(getClass())`
			builder.selectors(selectClass(testClass));
		}
		else if (instance instanceof Module testModule) {
			// handling `JUnit.run(getClass().getModule())`
			builder.selectors(selectModule(testModule));
		}
		else {
			// handling `JUnit.run(this)`
			builder.selectors(selectClass(instance.getClass()));
		}

		var request = builder.forExecution() //
				.listeners(listener, new ContainerFeedPrintingListener()) //
				.build();
		var launcher = LauncherFactory.create();
		launcher.execute(request);
		var summary = listener.getSummary();

		if (summary.getTotalFailureCount() == 0)
			return;

		summary.printFailuresTo(new PrintWriter(System.err, true, Charset.defaultCharset()));
		throw new JUnitException("JUnit run finished with %d failure%s".formatted( //
			summary.getTotalFailureCount(), //
			summary.getTotalFailureCount() == 1 ? "" : "s"));
	}
}
