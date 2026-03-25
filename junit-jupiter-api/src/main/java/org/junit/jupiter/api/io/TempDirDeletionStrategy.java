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
import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code TempDirDeletionStrategy} defines the SPI for deleting temporary
 * directories programmatically.
 *
 * <p>A deletion strategy controls how a temporary directory is cleaned up
 * when the end of its scope is reached.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * <p>A {@link TempDirDeletionStrategy} can be configured <em>globally</em>
 * for the entire test suite via the
 * {@value TempDir#DEFAULT_DELETION_STRATEGY_PROPERTY_NAME} configuration
 * parameter (see the User Guide for details) or <em>locally</em> for a test
 * class field or method parameter via the {@link TempDir @TempDir} annotation.
 *
 * @since 6.1
 * @see TempDir @TempDir
 */
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

	/**
	 * A {@link TempDirDeletionStrategy} that delegates to {@link Standard} but
	 * suppresses deletion failures by logging a warning instead of propagating
	 * them.
	 */
	final class IgnoreFailures implements TempDirDeletionStrategy {

		private static final Logger logger = LoggerFactory.getLogger(IgnoreFailures.class);
		private final TempDirDeletionStrategy delegate;

		/**
		 * Create a new {@code IgnoreFailures} strategy that delegates to
		 * {@link Standard}.
		 */
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

			result.toException().ifPresent(ex -> logWarning(elementContext, ex));

			return DeletionResult.builder(tempDir).build();
		}

		private void logWarning(AnnotatedElementContext elementContext, DeletionException exception) {
			logger.warn(exception, () -> "Failed to delete all temporary files for %s".formatted(
				descriptionFor(elementContext.getAnnotatedElement())));
		}

		@API(status = INTERNAL, since = "6.1")
		public static String descriptionFor(AnnotatedElement annotatedElement) {
			if (annotatedElement instanceof Field field) {
				return "field " + field.getDeclaringClass().getSimpleName() + "." + field.getName();
			}
			if (annotatedElement instanceof Parameter parameter) {
				Executable executable = parameter.getDeclaringExecutable();
				return "parameter '" + parameter.getName() + "' in " + descriptionFor(executable);
			}
			throw new IllegalStateException("Unsupported AnnotatedElement type for @TempDir: " + annotatedElement);
		}

		private static String descriptionFor(Executable executable) {
			boolean isConstructor = executable instanceof Constructor<?>;
			String type = isConstructor ? "constructor" : "method";
			String name = isConstructor ? executable.getDeclaringClass().getSimpleName() : executable.getName();
			return "%s %s(%s)".formatted(type, name,
				ClassUtils.nullSafeToString(Class::getSimpleName, executable.getParameterTypes()));
		}
	}

	/**
	 * Standard {@link TempDirDeletionStrategy} implementation that recursively
	 * deletes all files and directories within the temporary directory.
	 *
	 * <p>Symbolic and other types of links, such as junctions on Windows, are
	 * not followed. A warning is logged when deleting a link that targets a
	 * location outside the temporary directory.
	 *
	 * <p>If a file or directory cannot be deleted, its permissions are reset
	 * and deletion is attempted again. If deletion still fails, the path is
	 * scheduled for deletion on JVM exit via
	 * {@link java.io.File#deleteOnExit()}, if it belongs to the default file
	 * system.
	 */
	final class Standard implements TempDirDeletionStrategy {

		/**
		 * The singleton instance of {@code Standard}.
		 */
		public static final Standard INSTANCE = new Standard();

		private static final Logger logger = LoggerFactory.getLogger(Standard.class);

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
					logger.trace(() -> "preVisitDirectory: " + dir);
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
					logger.trace(exc, () -> "visitFileFailed: " + file);
					if (exc instanceof NoSuchFileException && !Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
						return CONTINUE;
					}
					// IOException includes `AccessDeniedException` thrown by non-readable or non-executable flags
					resetPermissionsAndTryToDeleteAgain(file, exc);
					return CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
					logger.trace(() -> "visitFile: " + file);
					if (Files.isSymbolicLink(file) && isLinkWithTargetOutsideTempDir(file)) {
						warnAboutLinkWithTargetOutsideTempDir("symbolic link", file);
					}
					delete(file, fileOperations);
					return CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
					logger.trace(exc, () -> "postVisitDirectory: " + dir);
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
						logger.trace(e,
							() -> "Failed to determine real path for " + path + "; assuming it is not a link");
						return false;
					}
				}

				private void warnAboutLinkWithTargetOutsideTempDir(String linkType, Path file) throws IOException {
					Path realPath = file.toRealPath();
					logger.warn(() -> """
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
			logger.trace(() -> "Attempting to delete " + file);
			try {
				fileOperations.delete(file);
				logger.trace(() -> "Successfully deleted " + file);
			}
			catch (IOException e) {
				logger.trace(e, () -> "Failed to delete " + file);
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
				if (FileSystems.getDefault().equals(path.getFileSystem())) {
					path.toFile().deleteOnExit();
				}
			}
			catch (UnsupportedOperationException ignore) {
			}
		}

		// For testing only
		interface FileOperations {

			void delete(Path path) throws IOException;

		}
	}

	/**
	 * Represents the result of a {@link TempDirDeletionStrategy#delete} operation,
	 * including any paths that could not be deleted.
	 */
	sealed interface DeletionResult permits DefaultDeletionResult {

		/**
		 * Create a new {@link Builder} for the supplied root directory.
		 *
		 * @param rootDir the root temporary directory; never {@code null}
		 * @return a new {@code Builder}; never {@code null}
		 */
		static Builder builder(Path rootDir) {
			return new DefaultDeletionResult.Builder(Preconditions.notNull(rootDir, "rootDir must not be null"));
		}

		/**
		 * Return the root temporary directory of this deletion operation.
		 *
		 * @return the root directory; never {@code null}
		 */
		Path rootDir();

		/**
		 * Return the list of failures that occurred during deletion.
		 *
		 * @return the list of failures; never {@code null}
		 */
		List<DeletionFailure> failures();

		/**
		 * Return {@code true} if the deletion was successful, i.e., no
		 * {@linkplain #failures() failures} were recorded.
		 */
		default boolean isSuccessful() {
			return failures().isEmpty();
		}

		/**
		 * Convert this result to a {@link DeletionException} summarizing all
		 * failures.
		 *
		 * <p>Must only be called if {@link #isSuccessful()} returns
		 * {@code false}.
		 *
		 * @return an {@link DeletionException}, if the deletion
		 * {@linkplain #isSuccessful() was successful; otherwise, empty}; never
		 * {@code null}
		 */
		Optional<DeletionException> toException();

		/**
		 * Builder for {@link DeletionResult}.
		 */
		sealed interface Builder permits DefaultDeletionResult.Builder {

			/**
			 * Record a failure for the supplied path.
			 *
			 * @param path the path that could not be deleted; never {@code null}
			 * @param cause the exception that caused the failure; never {@code null}
			 * @return this builder; never {@code null}
			 */
			Builder addFailure(Path path, Exception cause);

			/**
			 * Build the {@link DeletionResult}.
			 *
			 * @return a new {@link DeletionResult}; never {@code null}
			 */
			DeletionResult build();

		}

	}

	/**
	 * Represents a single failure that occurred while attempting to delete a
	 * path during a {@link TempDirDeletionStrategy#delete} operation.
	 */
	sealed interface DeletionFailure permits DefaultDeletionResult.DefaultDeletionFailure {

		/**
		 * Return the path that could not be deleted.
		 *
		 * @return the path; never {@code null}
		 */
		Path path();

		/**
		 * Return the exception that caused the failure.
		 *
		 * @return the cause; never {@code null}
		 */
		Exception cause();

	}

	/**
	 * Exception thrown when one or more paths in a temporary directory could
	 * not be deleted by a {@link TempDirDeletionStrategy}.
	 */
	final class DeletionException extends JUnitException {

		@Serial
		private static final long serialVersionUID = 1L;

		DeletionException(String message) {
			super(message, null, true, false);
		}
	}

}
