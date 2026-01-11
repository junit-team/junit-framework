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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ThirdPartyJars {

	private ThirdPartyJars() {
	}

	public static void copy(Path targetDir, String group, String artifact) {
		Path source = find(group, artifact);
		copy(source, targetDir);
	}

	public static void copyAll(Path targetDir) {
		thirdPartyJars().forEach(path -> copy(path, targetDir));
	}

	private static void copy(Path source, Path targetDir) {
		try {
			Files.copy(source, targetDir.resolve(source.getFileName()), REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to copy %s to %s".formatted(source, targetDir), e);
		}
	}

	public static Path find(String group, String artifact) {
		return thirdPartyJars() //
				.filter(it -> it.toAbsolutePath().toString().replace(File.separator, "/").contains(
					"/" + group + "/" + artifact + "/")) //
				.findFirst() //
				.orElseThrow(() -> new AssertionError("Failed to find JAR file for " + group + ":" + artifact));
	}

	private static Stream<Path> thirdPartyJars() {
		return Stream.of(System.getProperty("thirdPartyJars").split(File.pathSeparator)) //
				.map(Path::of);
	}
}
