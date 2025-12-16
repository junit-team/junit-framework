/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.start;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.console.output.ColorPalette;
import org.junit.platform.console.output.Theme;
import org.junit.platform.console.output.TreePrintingListener;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/// This class provides simple helpers to discover and execute tests.
@API(status = EXPERIMENTAL, since = "6.1")
public final class JUnit {
	/// Run all tests defined in the caller class.
	public static void run() {
		var walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
		run(selectClass(walker.getCallerClass()));
	}

	/// Run all tests defined in the given test class.
	///
	/// @param testClass the class to discover and execute tests in
	public static void run(Class<?> testClass) {
		run(selectClass(testClass));
	}

	/// Run all tests defined in the given module.
	///
	/// @param testModule the module to discover and execute tests in
	public static void run(Module testModule) {
		run(selectModule(testModule));
	}

	private static void run(DiscoverySelector selector) {
		var listener = new SummaryGeneratingListener();
		var charset = Charset.defaultCharset();
		var writer = new PrintWriter(System.out, true, charset);
		var palette = System.getenv("NO_COLOR") != null ? ColorPalette.NONE : ColorPalette.DEFAULT;
		var theme = Theme.valueOf(charset);
		var printer = new TreePrintingListener(writer, palette, theme);
		var request = request().selectors(selector).forExecution() //
				.listeners(listener, printer) //
				.build();
		var launcher = LauncherFactory.create();
		launcher.execute(request);
		var summary = listener.getSummary();

		if (summary.getTotalFailureCount() == 0)
			return;

		summary.printFailuresTo(new PrintWriter(System.err, true, charset));
		throw new JUnitException("JUnit run finished with %d failure%s".formatted( //
			summary.getTotalFailureCount(), //
			summary.getTotalFailureCount() == 1 ? "" : "s"));
	}

	private JUnit() {
	}
}
