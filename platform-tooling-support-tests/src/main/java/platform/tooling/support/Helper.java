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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @since 1.3
 */
public class Helper {

	private static final Path ROOT = Path.of("..");
	private static final Path GRADLE_PROPERTIES = ROOT.resolve("gradle.properties");
	private static final Path SETTINGS_GRADLE = ROOT.resolve("settings.gradle.kts");

	private static final Properties gradleProperties = new Properties();

	static {
		try {
			gradleProperties.load(Files.newInputStream(GRADLE_PROPERTIES));
		}
		catch (Exception e) {
			throw new AssertionError("loading gradle.properties failed", e);
		}
	}

	public static String version() {
		return gradleProperties.getProperty("version");
	}

	public static List<String> loadModuleDirectoryNames() {
		var moduleLinePattern = Pattern.compile("include\\(\"(.+)\"\\)");
		try (var stream = Files.lines(SETTINGS_GRADLE)) {
			return stream.map(moduleLinePattern::matcher) //
					.filter(Matcher::matches) //
					.map(matcher -> matcher.group(1)) //
					.filter(name -> name.startsWith("junit-")) //
					.filter(name -> !"junit-bom".equals(name)) //
					.filter(name -> !"junit-platform-console-standalone".equals(name)).toList();
		}
		catch (Exception e) {
			throw new AssertionError("loading module directory names failed: " + SETTINGS_GRADLE);
		}
	}

	public static Optional<Path> getJavaHome(int version) {
		// First, try various system sources...
		var sources = Stream.of( //
			System.getProperty("java.home." + version), //
			System.getProperty("java." + version), //
			System.getProperty("jdk.home." + version), //
			System.getProperty("jdk." + version), //
			System.getenv("JAVA_HOME_" + version), //
			System.getenv("JAVA_" + version), //
			System.getenv("JDK" + version) //
		);
		return sources.filter(Objects::nonNull).findFirst().map(Path::of);
	}

	private Helper() {
	}
}
