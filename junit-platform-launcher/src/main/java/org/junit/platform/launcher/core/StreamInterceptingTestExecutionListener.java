/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.launcher.LauncherConstants.CAPTURE_MAX_BUFFER_DEFAULT;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_MAX_BUFFER_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.CompositeTestExecutionListener.EagerTestExecutionListener;

/**
 * @since 1.3
 */
class StreamInterceptingTestExecutionListener implements EagerTestExecutionListener {

	private final Optional<StreamInterceptor> stdoutInterceptor;
	private final Optional<StreamInterceptor> stderrInterceptor;
	private final BiConsumer<TestIdentifier, ReportEntry> reporter;

	static Optional<StreamInterceptingTestExecutionListener> create(ConfigurationParameters configurationParameters,
			BiConsumer<TestIdentifier, ReportEntry> reporter) {

		boolean captureStdout = configurationParameters.getBoolean(CAPTURE_STDOUT_PROPERTY_NAME).orElse(false);
		boolean captureStderr = configurationParameters.getBoolean(CAPTURE_STDERR_PROPERTY_NAME).orElse(false);
		if (!captureStdout && !captureStderr) {
			return Optional.empty();
		}

		int maxSize = configurationParameters.get(CAPTURE_MAX_BUFFER_PROPERTY_NAME, Integer::valueOf) //
				.orElse(CAPTURE_MAX_BUFFER_DEFAULT);

		Optional<StreamInterceptor> stdoutInterceptor = captureStdout ? StreamInterceptor.registerStdout(maxSize)
				: Optional.empty();
		Optional<StreamInterceptor> stderrInterceptor = captureStderr ? StreamInterceptor.registerStderr(maxSize)
				: Optional.empty();

		if ((stdoutInterceptor.isEmpty() && captureStdout) || (stderrInterceptor.isEmpty() && captureStderr)) {
			stdoutInterceptor.ifPresent(StreamInterceptor::unregister);
			stderrInterceptor.ifPresent(StreamInterceptor::unregister);
			return Optional.empty();
		}
		return Optional.of(new StreamInterceptingTestExecutionListener(stdoutInterceptor, stderrInterceptor, reporter));
	}

	private StreamInterceptingTestExecutionListener(Optional<StreamInterceptor> stdoutInterceptor,
			Optional<StreamInterceptor> stderrInterceptor, BiConsumer<TestIdentifier, ReportEntry> reporter) {
		this.stdoutInterceptor = stdoutInterceptor;
		this.stderrInterceptor = stderrInterceptor;
		this.reporter = reporter;
	}

	void unregister() {
		stdoutInterceptor.ifPresent(StreamInterceptor::unregister);
		stderrInterceptor.ifPresent(StreamInterceptor::unregister);
	}

	@Override
	public void executionJustStarted(TestIdentifier testIdentifier) {
		stdoutInterceptor.ifPresent(StreamInterceptor::capture);
		stderrInterceptor.ifPresent(StreamInterceptor::capture);
	}

	@Override
	public void executionJustFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		Map<String, String> map = new HashMap<>();
		String out = stdoutInterceptor.map(StreamInterceptor::consume).orElse("");
		if (StringUtils.isNotBlank(out)) {
			map.put(STDOUT_REPORT_ENTRY_KEY, out);
		}
		String err = stderrInterceptor.map(StreamInterceptor::consume).orElse("");
		if (StringUtils.isNotBlank(err)) {
			map.put(STDERR_REPORT_ENTRY_KEY, err);
		}
		if (!map.isEmpty()) {
			reporter.accept(testIdentifier, ReportEntry.from(map));
		}
	}
}
