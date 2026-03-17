/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.io;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

@API(status = EXPERIMENTAL, since = "6.1")
public interface TempDirDeletionStrategy {

	/**
	 * Delete the supplied temporary directory and all of its contents.
	 *
	 * <p>Depending on the used {@link TempDirFactory}, the supplied
	 * {@link Path} may or may not be associated with the
	 * {@linkplain java.nio.file.FileSystems#getDefault() default FileSystem}.
	 *
	 * @param tempDir the temporary directory to delete; never {@code null}
	 * @param elementContext the context of the field or parameter where
	 * {@code @TempDir} is declared; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return a {@link DeletionResult}, potentially containing failures for
	 * {@link Path Paths} that could not be deleted or no failures if deletion
	 * was successful; never {@code null}
	 * @throws IOException in case of general failures
	 */
	DeletionResult delete(Path tempDir, AnnotatedElementContext elementContext, ExtensionContext extensionContext)
			throws IOException;

	final class IgnoreFailures implements TempDirDeletionStrategy {

		private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreFailures.class);
		private final TempDirDeletionStrategy delegate;

		public IgnoreFailures() {
			this(Standard.INSTANCE);
		}

		IgnoreFailures(TempDirDeletionStrategy delegate) {
			this.delegate = delegate;
		}

		@Override
		public DeletionResult delete(Path tempDir, AnnotatedElementContext elementContext,
				ExtensionContext extensionContext) throws IOException {

			var result = delegate.delete(tempDir, elementContext, extensionContext);

			if (!result.isSuccessful()) {
				var exception = result.toException();
				LOGGER.warn(exception, exception::getMessage);
			}

			return DeletionResult.builder(tempDir).build();
		}
	}

	final class Standard implements TempDirDeletionStrategy {

		public static final Standard INSTANCE = new Standard();

		private static final Logger LOGGER = LoggerFactory.getLogger(Standard.class);

		private Standard() {
		}

		@Override
		public DeletionResult delete(Path tempDir, AnnotatedElementContext elementContext,
				ExtensionContext extensionContext) throws IOException {

			return delete(tempDir, Files::delete);
		}

		// package-private for testing
		DeletionResult delete(Path tempDir, FileOperations fileOperations) throws IOException {
			var result = DeletionResult.builder(tempDir);
			delete(tempDir, fileOperations, (path, cause) -> {
				result.addFailure(path, cause);
				tryToDeleteOnExit(path);
			});
			return result.build();
		}

		private void delete(Path tempDir, FileOperations fileOperations, BiConsumer<Path, Exception> failureHandler)
				throws IOException {
			Set<Path> retriedPaths = new HashSet<>();
			Path rootRealPath = tempDir.toRealPath();

			tryToResetPermissions(tempDir);
			Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					LOGGER.trace(() -> "preVisitDirectory: " + dir);
					if (isLinkWithTargetOutsideTempDir(dir)) {
						warnAboutLinkWithTargetOutsideTempDir("link", dir);
						delete(dir, fileOperations);
						return SKIP_SUBTREE;
					}
					if (!dir.equals(tempDir)) {
						tryToResetPermissions(dir);
					}
					return CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					LOGGER.trace(exc, () -> "visitFileFailed: " + file);
					if (exc instanceof NoSuchFileException && !Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
						return CONTINUE;
					}
					// IOException includes `AccessDeniedException` thrown by non-readable or non-executable flags
					resetPermissionsAndTryToDeleteAgain(file, exc);
					return CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
					LOGGER.trace(() -> "visitFile: " + file);
					if (Files.isSymbolicLink(file) && isLinkWithTargetOutsideTempDir(file)) {
						warnAboutLinkWithTargetOutsideTempDir("symbolic link", file);
					}
					delete(file, fileOperations);
					return CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
					LOGGER.trace(exc, () -> "postVisitDirectory: " + dir);
					delete(dir, fileOperations);
					return CONTINUE;
				}

				private boolean isLinkWithTargetOutsideTempDir(Path path) {
					// While `Files.walkFileTree` does not follow symbolic links, it may follow other links
					// such as "junctions" on Windows
					try {
						return !path.toRealPath().startsWith(rootRealPath);
					}
					catch (IOException e) {
						LOGGER.trace(e,
							() -> "Failed to determine real path for " + path + "; assuming it is not a link");
						return false;
					}
				}

				private void warnAboutLinkWithTargetOutsideTempDir(String linkType, Path file) throws IOException {
					Path realPath = file.toRealPath();
					LOGGER.warn(() -> """
							Deleting %s from location inside of temp dir (%s) \
							to location outside of temp dir (%s) but not the target file/directory""".formatted(
						linkType, file, realPath));
				}

				private void delete(Path path, FileOperations fileOperations) {
					try {
						deleteWithLogging(path, fileOperations);
					}
					catch (NoSuchFileException ignore) {
						// ignore
					}
					catch (DirectoryNotEmptyException exception) {
						failureHandler.accept(path, exception);
					}
					catch (IOException exception) {
						// IOException includes `AccessDeniedException` thrown by non-readable or non-executable flags
						resetPermissionsAndTryToDeleteAgain(path, exception);
					}
				}

				private void resetPermissionsAndTryToDeleteAgain(Path path, IOException exception) {
					boolean notYetRetried = retriedPaths.add(path);
					if (notYetRetried) {
						try {
							tryToResetPermissions(path);
							if (Files.isDirectory(path)) {
								Files.walkFileTree(path, this);
							}
							else {
								deleteWithLogging(path, fileOperations);
							}
						}
						catch (Exception suppressed) {
							exception.addSuppressed(suppressed);
							failureHandler.accept(path, exception);
						}
					}
					else {
						failureHandler.accept(path, exception);
					}
				}
			});
		}

		private void deleteWithLogging(Path file, FileOperations fileOperations) throws IOException {
			LOGGER.trace(() -> "Attempting to delete " + file);
			try {
				fileOperations.delete(file);
				LOGGER.trace(() -> "Successfully deleted " + file);
			}
			catch (IOException e) {
				LOGGER.trace(e, () -> "Failed to delete " + file);
				throw e;
			}
		}

		@SuppressWarnings("ResultOfMethodCallIgnored")
		private static void tryToResetPermissions(Path path) {
			File file;
			try {
				file = path.toFile();
			}
			catch (UnsupportedOperationException ignore) {
				// Might happen when the `TempDirFactory` uses a custom `FileSystem`
				return;
			}
			file.setReadable(true);
			file.setWritable(true);
			if (Files.isDirectory(path)) {
				file.setExecutable(true);
			}
			DosFileAttributeView dos = Files.getFileAttributeView(path, DosFileAttributeView.class);
			if (dos != null) {
				try {
					dos.setReadOnly(false);
				}
				catch (IOException ignore) {
					// nothing we can do
				}
			}
		}

		@SuppressWarnings("EmptyCatch")
		private static void tryToDeleteOnExit(Path path) {
			try {
				path.toFile().deleteOnExit();
			}
			catch (UnsupportedOperationException ignore) {
			}
		}

		// For testing only
		interface FileOperations {

			void delete(Path path) throws IOException;

		}
	}

	sealed interface DeletionResult permits DefaultDeletionResult {

		static Builder builder(Path rootDir) {
			return new DefaultDeletionResult.Builder(rootDir);
		}

		Path rootDir();

		List<DeletionFailure> failures();

		default boolean isSuccessful() {
			return failures().isEmpty();
		}

		DeletionException toException();

		sealed interface Builder permits DefaultDeletionResult.Builder {

			Builder addFailure(Path path, Exception cause);

			DeletionResult build();

		}

	}

	sealed interface DeletionFailure permits DefaultDeletionResult.DefaultDeletionFailure {

		Path path();

		Exception cause();

	}

	final class DeletionException extends JUnitException {

		@Serial
		private static final long serialVersionUID = 1L;

		DeletionException(String message) {
			super(message, null, true, false);
		}
	}

}
