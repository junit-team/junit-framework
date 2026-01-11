/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.condition.OS;
import org.junit.platform.tests.process.ProcessStarter;
import org.opentest4j.TestAbortedException;

public class ProcessStarters {

	public static ProcessStarter java() {
		return javaCommand(currentJdkHome(), "java");
	}

	public static Path currentJdkHome() {
		var executable = ProcessHandle.current().info().command().map(Path::of).orElseThrow();
		// path element count is 3 or higher: "<JAVA_HOME>/bin/java[.exe]"
		return executable.getParent().getParent().toAbsolutePath();
	}

	public static ProcessStarter java(Path javaHome) {
		return javaCommand(javaHome, "java");
	}

	public static ProcessStarter javaCommand(String commandName) {
		return javaCommand(currentJdkHome(), commandName);
	}

	public static ProcessStarter javaCommand(Path javaHome, String commandName) {
		return new ProcessStarter() //
				.executable(javaHome.resolve("bin").resolve(commandName)) //
				.putEnvironment("JAVA_HOME", javaHome);
	}

	public static ProcessStarter gradlew() {
		return new ProcessStarter() //
				.executable(Path.of("..").resolve(windowsOrOtherExecutable("gradlew.bat", "gradlew")).toAbsolutePath()) //
				.putEnvironment("JAVA_HOME", getGradleJavaHome().orElseThrow(TestAbortedException::new)) //
				.addArguments("-PjunitVersion=" + Helper.version());
	}

	public static ProcessStarter maven(Path javaHome) {
		return new ProcessStarter() //
				.executable(Path.of(System.getProperty("mavenDistribution")).resolve("bin").resolve(
					windowsOrOtherExecutable("mvn.cmd", "mvn")).toAbsolutePath()) //
				.putEnvironment("JAVA_HOME", javaHome) //
				.addArguments("-Djunit.version=" + Helper.version());
	}

	private static String windowsOrOtherExecutable(String cmdOrExe, String other) {
		return OS.current() == OS.WINDOWS ? cmdOrExe : other;
	}

	public static Optional<Path> getGradleJavaHome() {
		return Helper.getJavaHome(getGradleJavaVersion());
	}

	public static int getGradleJavaVersion() {
		return Integer.parseInt(System.getProperty("gradle.java.version"));
	}

	private ProcessStarters() {
	}
}
