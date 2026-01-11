/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static platform.tooling.support.tests.ManagedResource.Scope.GLOBAL;
import static platform.tooling.support.tests.ManagedResource.Scope.PER_CONTEXT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

@ManagedResource.Scoped(LocalMavenRepo.ScopeProvider.class)
public class LocalMavenRepo implements AutoCloseable {

	public static class ScopeProvider implements ManagedResource.Scoped.Provider {

		private static final Namespace NAMESPACE = Namespace.create(LocalMavenRepo.class);

		@Override
		public ManagedResource.Scope determineScope(ExtensionContext extensionContext) {
			var store = extensionContext.getRoot().getStore(NAMESPACE);
			var fileSystemType = store.computeIfAbsent("tempFileSystemType", key -> {
				var type = getFileSystemType(Path.of(System.getProperty("java.io.tmpdir")));
				extensionContext.getRoot().publishReportEntry("tempFileSystemType", type);
				return type;
			}, String.class);
			// Writing to the same file from multiple Maven processes may fail the build on Windows
			return "NTFS".equalsIgnoreCase(fileSystemType) ? PER_CONTEXT : GLOBAL;
		}

		private static String getFileSystemType(Path path) {
			try {
				return Files.getFileStore(path).type();
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private final Path tempDir;

	public LocalMavenRepo() {
		try {
			tempDir = Files.createTempDirectory("local-maven-repo-");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String toCliArgument() {
		return "-Dmaven.repo.local=" + tempDir;
	}

	@Override
	public void close() throws IOException {
		try (var files = Files.walk(tempDir)) {
			files.sorted(Comparator.<Path> naturalOrder().reversed()) //
					.forEach(path -> {
						try {
							Files.delete(path);
						}
						catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
		}
	}
}
