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
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

@API(status = EXPERIMENTAL, since = "6.0")
public final class JUnit {

	private JUnit() {
	}

	public static void run(Object instance) {
		if (instance instanceof Class<?> testClass) {
			run(testClass);
			return;
		}
		if (instance instanceof Module testModule) {
			run(testModule);
			return;
		}
		run(instance.getClass());
	}

	public static void run(Class<?> testClass) {
		run(discovery -> discovery.selectors(selectClass(testClass)));
	}

	public enum ModuleSelectionMode {
		PLATFORM_DEFAULT, AGGREGATOR_LOCAL
	}

	public static void run(Module testModule) {
		run(testModule, ModuleSelectionMode.PLATFORM_DEFAULT);
	}

	public static void run(Module testModule, ModuleSelectionMode mode) {
		switch (mode) {
			case PLATFORM_DEFAULT -> run(discovery -> discovery.selectors(selectModule(testModule)));
			case AGGREGATOR_LOCAL -> {
				var selectors = ModuleSupport.listClassesInModule(testModule).stream() //
						.map(DiscoverySelectors::selectClass).toList();
				run(discovery -> discovery.selectors(selectors));
			}
		}
	}

	// Don't transitively expose types from org.junit.platform.launcher module
	private static void run(UnaryOperator<LauncherDiscoveryRequestBuilder> discovery) {
		var listener = new SummaryGeneratingListener();
		var request = discovery.apply(request()).forExecution() //
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

	public static void main(String[] args) {
		var junit = ToolProvider.findFirst("junit").orElseThrow();
		var exitCode = junit.run(System.out, System.err, args);
		System.exit(exitCode);
	}
}
