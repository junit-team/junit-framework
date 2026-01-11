/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ClassNamePatternFilterUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * Collection of constants related to {@link Launcher}.
 *
 * @since 1.3
 * @see org.junit.platform.engine.ConfigurationParameters
 */
@API(status = STABLE, since = "1.7")
public class LauncherConstants {

	/**
	 * Property name used to enable capturing output to {@link System#out}:
	 * {@value}
	 *
	 * <p>By default, output to {@link System#out} is not captured.
	 *
	 * <p>If enabled, the JUnit Platform captures the corresponding output and
	 * publishes it as a {@link ReportEntry} using the
	 * {@value #STDOUT_REPORT_ENTRY_KEY} key immediately before reporting the
	 * test identifier as finished.
	 *
	 * @see #STDOUT_REPORT_ENTRY_KEY
	 * @see ReportEntry
	 * @see TestExecutionListener#reportingEntryPublished(TestIdentifier, ReportEntry)
	 */
	public static final String CAPTURE_STDOUT_PROPERTY_NAME = "junit.platform.output.capture.stdout";

	/**
	 * Property name used to enable capturing output to {@link System#err}:
	 * {@value}
	 *
	 * <p>By default, output to {@link System#err} is not captured.
	 *
	 * <p>If enabled, the JUnit Platform captures the corresponding output and
	 * publishes it as a {@link ReportEntry} using the
	 * {@value #STDERR_REPORT_ENTRY_KEY} key immediately before reporting the
	 * test identifier as finished.
	 *
	 * @see #STDERR_REPORT_ENTRY_KEY
	 * @see ReportEntry
	 * @see TestExecutionListener#reportingEntryPublished(TestIdentifier, ReportEntry)
	 */
	public static final String CAPTURE_STDERR_PROPERTY_NAME = "junit.platform.output.capture.stderr";

	/**
	 * Property name used to configure the maximum number of bytes for buffering
	 * to use per thread and output type if output capturing is enabled:
	 * {@value}
	 *
	 * <p>Value must be an integer; defaults to {@value #CAPTURE_MAX_BUFFER_DEFAULT}.
	 *
	 * @see #CAPTURE_MAX_BUFFER_DEFAULT
	 */
	public static final String CAPTURE_MAX_BUFFER_PROPERTY_NAME = "junit.platform.output.capture.maxBuffer";

	/**
	 * Default maximum number of bytes for buffering to use per thread and
	 * output type if output capturing is enabled.
	 *
	 * @see #CAPTURE_MAX_BUFFER_PROPERTY_NAME
	 */
	public static final int CAPTURE_MAX_BUFFER_DEFAULT = 4 * 1024 * 1024;

	/**
	 * Key used to publish captured output to {@link System#out} as part of a
	 * {@link ReportEntry}: {@value}
	 */
	public static final String STDOUT_REPORT_ENTRY_KEY = "stdout";

	/**
	 * Key used to publish captured output to {@link System#err} as part of a
	 * {@link ReportEntry}: {@value}
	 */
	public static final String STDERR_REPORT_ENTRY_KEY = "stderr";

	/**
	 * Property name used to provide patterns for deactivating
	 * {@linkplain TestExecutionListener listeners} registered via the
	 * {@link java.util.ServiceLoader ServiceLoader} mechanism: {@value}
	 *
	 * <h4>Pattern Matching Syntax</h4>
	 *
	 * <p>If the property value consists solely of an asterisk ({@code *}), all
	 * listeners will be deactivated. Otherwise, the property value will be treated
	 * as a comma-separated list of patterns where each individual pattern will be
	 * matched against the fully qualified class name (<em>FQCN</em>) of each registered
	 * listener. Any dot ({@code .}) in a pattern will match against a dot ({@code .})
	 * or a dollar sign ({@code $}) in a FQCN. Any asterisk ({@code *}) will match
	 * against one or more characters in a FQCN. All other characters in a pattern
	 * will be matched one-to-one against a FQCN.
	 *
	 * <h4>Examples</h4>
	 *
	 * <ul>
	 * <li>{@code *}: deactivates all listeners.
	 * <li>{@code org.junit.*}: deactivates every listener under the {@code org.junit}
	 * base package and any of its subpackages.
	 * <li>{@code *.MyListener}: deactivates every listener whose simple class name is
	 * exactly {@code MyListener}.
	 * <li>{@code *System*, *Dev*}: deactivates every listener whose FQCN contains
	 * {@code System} or {@code Dev}.
	 * <li>{@code org.example.MyListener, org.example.TheirListener}: deactivates
	 * listeners whose FQCN is exactly {@code org.example.MyListener} or
	 * {@code org.example.TheirListener}.
	 * </ul>
	 *
	 * <p>Only listeners registered via the {@code ServiceLoader} mechanism can
	 * be deactivated. In other words, any listener registered explicitly via the
	 * {@link LauncherDiscoveryRequest} cannot be deactivated via this
	 * configuration parameter.
	 *
	 * <p>In addition, since execution listeners are registered before the test
	 * run starts, this configuration parameter can only be supplied as a JVM
	 * system property or via the JUnit Platform configuration file but cannot
	 * be supplied in the {@link LauncherDiscoveryRequest}} that is passed to
	 * the {@link Launcher}.
	 *
	 * @see #DEACTIVATE_ALL_LISTENERS_PATTERN
	 * @see org.junit.platform.launcher.TestExecutionListener
	 */
	public static final String DEACTIVATE_LISTENERS_PATTERN_PROPERTY_NAME = "junit.platform.execution.listeners.deactivate";

	/**
	 * Wildcard pattern which signals that all listeners registered via the
	 * {@link java.util.ServiceLoader ServiceLoader} mechanism should be deactivated:
	 * {@value}
	 *
	 * @see #DEACTIVATE_LISTENERS_PATTERN_PROPERTY_NAME
	 * @see org.junit.platform.launcher.TestExecutionListener
	 */
	public static final String DEACTIVATE_ALL_LISTENERS_PATTERN = ClassNamePatternFilterUtils.ALL_PATTERN;

	/**
	 * Property name used to enable support for
	 * {@link LauncherInterceptor} instances to be registered via the
	 * {@link java.util.ServiceLoader ServiceLoader} mechanism: {@value}
	 *
	 * <p>By default, interceptor registration is disabled.
	 *
	 * <p>Since interceptors are registered before the test run starts, this
	 * configuration parameter can only be supplied as a JVM system property or
	 * via the JUnit Platform configuration file but cannot be supplied in the
	 * {@link LauncherDiscoveryRequest}} that is passed to the {@link Launcher}.
	 *
	 * @see LauncherInterceptor
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String ENABLE_LAUNCHER_INTERCEPTORS = "junit.platform.launcher.interceptors.enabled";

	/**
	 * Property name used to enable dry-run mode for test execution.
	 *
	 * <p>When dry-run mode is enabled, no tests will be executed. Instead, all
	 * registered {@link TestExecutionListener TestExecutionListeners} will
	 * receive events for all test descriptors that are part of the discovered
	 * {@link TestPlan}. All containers will be reported as successful and all
	 * tests as skipped. This can be useful to test changes in the configuration
	 * of a build or to verify a listener is called as expected without having
	 * to wait for all tests to be executed.
	 *
	 * <p>Value must be either {@code true} or {@code false}; defaults to {@code false}.
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String DRY_RUN_PROPERTY_NAME = "junit.platform.execution.dryRun.enabled";

	/**
	 * Property name used to enable or disable stack trace pruning.
	 *
	 * <p>By default, stack trace pruning is enabled.
	 *
	 * @see org.junit.platform.launcher.core.EngineExecutionOrchestrator
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME = "junit.platform.stacktrace.pruning.enabled";

	/**
	 * Property name used to configure the output directory for reporting.
	 *
	 * <p>If set, value must be a valid path that will be created if it doesn't
	 * exist. If not set, the default output directory will be determined by the
	 * reporting engine based on the current working directory.
	 *
	 * @since 1.12
	 * @see #OUTPUT_DIR_UNIQUE_NUMBER_PLACEHOLDER
	 * @see org.junit.platform.engine.OutputDirectoryCreator
	 * @see EngineDiscoveryRequest#getOutputDirectoryCreator()
	 * @see TestPlan#getOutputDirectoryCreator()
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String OUTPUT_DIR_PROPERTY_NAME = "junit.platform.reporting.output.dir";

	/**
	 * Placeholder for use in {@link #OUTPUT_DIR_PROPERTY_NAME} that will be
	 * replaced with a unique number.
	 *
	 * <p>This can be used to create a unique output directory for each test
	 * run. For example, if multiple forks are used, each fork can be configured
	 * to write its output to a separate directory.
	 *
	 * @since 1.12
	 * @see #OUTPUT_DIR_PROPERTY_NAME
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public static final String OUTPUT_DIR_UNIQUE_NUMBER_PLACEHOLDER = "{uniqueNumber}";

	/**
	 * Property name used to configure the critical severity of issues
	 * encountered during test discovery.
	 *
	 * <p>If an engine reports an issue with a severity equal to or higher than
	 * the configured critical severity, its tests will not be executed.
	 * Depending on {@link #DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME}, a
	 * {@link org.junit.platform.launcher.core.DiscoveryIssueException} listing
	 * all critical issues will be thrown during discovery or be reported as
	 * engine-level failure during execution.
	 *
	 * <h4>Supported Values</h4>
	 *
	 * <p>Supported values include names of enum constants defined in
	 * {@link org.junit.platform.engine.DiscoveryIssue.Severity Severity},
	 * ignoring case.
	 *
	 * <p>If not specified, the default is "error" which corresponds to
	 * {@code Severity.ERROR)}.
	 *
	 * @since 1.13
	 * @see org.junit.platform.engine.DiscoveryIssue.Severity
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static final String CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME = "junit.platform.discovery.issue.severity.critical";

	/**
	 * Property name used to configure the phase that critical discovery issues
	 * should cause a failure
	 *
	 * <h4>Supported Values</h4>
	 *
	 * <p>Supported values are "discovery" or "execution".
	 *
	 * <p>If not specified, the {@code Launcher} will report discovery issues
	 * during the discovery phase if
	 * {@link Launcher#discover(LauncherDiscoveryRequest)} is called, and during
	 * the execution phase if {@link Launcher#execute(LauncherExecutionRequest)}
	 * is called.
	 *
	 * @since 1.13
	 * @see #CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	public static final String DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME = "junit.platform.discovery.issue.failure.phase";

	private LauncherConstants() {
		/* no-op */
	}

}
