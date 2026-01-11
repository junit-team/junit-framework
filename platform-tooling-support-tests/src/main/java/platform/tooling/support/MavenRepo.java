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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MavenRepo {

	private MavenRepo() {
	}

	public static Path dir() {
		var candidates = Stream.of(Path.of("../build/repo"));
		var candidate = candidates.filter(Files::isDirectory).findFirst().orElse(Path.of("build/repo"));
		return Path.of(System.getProperty("maven.repo", candidate.toString()));
	}

	public static Path jar(String artifactId) {
		return artifact(artifactId, fileName -> fileName.endsWith(".jar") //
				&& !fileName.endsWith("-sources.jar") //
				&& !fileName.endsWith("-javadoc.jar"));
	}

	public static Path gradleModuleMetadata(String artifactId) {
		return artifact(artifactId, fileName -> fileName.endsWith(".module"));
	}

	public static Path pom(String artifactId) {
		return artifact(artifactId, fileName -> fileName.endsWith(".pom"));
	}

	private static Path artifact(String artifactId, Predicate<String> fileNamePredicate) {
		var parentDir = dir() //
				.resolve(groupId(artifactId).replace('.', File.separatorChar)) //
				.resolve(artifactId) //
				.resolve(Helper.version());
		try (var files = Files.list(parentDir)) {
			return files.filter(
				file -> fileNamePredicate.test(file.getFileName().toString())).findFirst().orElseThrow();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String groupId(String artifactId) {
		if (artifactId.startsWith("junit-jupiter")) {
			return "org.junit.jupiter";
		}
		if (artifactId.startsWith("junit-platform")) {
			return "org.junit.platform";
		}
		if (artifactId.startsWith("junit-vintage")) {
			return "org.junit.vintage";
		}
		return "org.junit";
	}
}
