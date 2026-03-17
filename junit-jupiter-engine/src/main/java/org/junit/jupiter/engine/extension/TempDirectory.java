/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;
import static org.junit.jupiter.api.io.CleanupMode.DEFAULT;
import static org.junit.jupiter.api.io.CleanupMode.NEVER;
import static org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.ReflectionSupport.makeAccessible;
import static org.junit.platform.commons.util.ReflectionUtils.isRecordObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirDeletionStrategy;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * {@code TempDirectory} is a JUnit Jupiter extension that creates and cleans
 * up temporary directories if a field in a test class or a parameter in a
 * test class constructor, lifecycle method, or test method is annotated with
 * {@code @TempDir}.
 *
 * <p>Consult the Javadoc for {@link TempDir} for details on the contract.
 *
 * @since 5.4
 * @see TempDir @TempDir
 * @see Files#createTempDirectory
 */
class TempDirectory implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

	private static final Namespace NAMESPACE = Namespace.create(TempDirectory.class);
	private static final String KEY = "temp.dir";
	private static final String FAILURE_TRACKER = "failure.tracker";
	private static final String CHILD_FAILED = "child.failed";

	private final JupiterConfiguration configuration;

	TempDirectory(JupiterConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return TEST_METHOD;
	}

	/**
	 * Perform field injection for non-private, {@code static} fields (i.e.,
	 * class fields) of type {@link Path} or {@link File} that are annotated with
	 * {@link TempDir @TempDir}.
	 */
	@Override
	public void beforeAll(ExtensionContext context) {
		installFailureTracker(context);
		injectStaticFields(context, context.getRequiredTestClass());
	}

	/**
	 * Perform field injection for non-private, non-static fields (i.e.,
	 * instance fields) of type {@link Path} or {@link File} that are annotated
	 * with {@link TempDir @TempDir}.
	 */
	@Override
	public void beforeEach(ExtensionContext context) {
		installFailureTracker(context);
		context.getRequiredTestInstances().getAllInstances() //
				.forEach(instance -> injectInstanceFields(context, instance));
	}

	private static void installFailureTracker(ExtensionContext context) {
		context.getParent() //
				.filter(parentContext -> !context.getRoot().equals(parentContext)) //
				.ifPresent(parentContext -> installFailureTracker(context, parentContext));
	}

	private static void installFailureTracker(ExtensionContext context, ExtensionContext parentContext) {
		context.getStore(NAMESPACE).put(FAILURE_TRACKER, new FailureTracker(context, parentContext));
	}

	private void injectStaticFields(ExtensionContext context, Class<?> testClass) {
		injectFields(context, null, testClass, ModifierSupport::isStatic);
	}

	private void injectInstanceFields(ExtensionContext context, Object instance) {
		if (!isRecordObject(instance)) {
			injectFields(context, instance, instance.getClass(), ModifierSupport::isNotStatic);
		}
	}

	private void injectFields(ExtensionContext context, @Nullable Object testInstance, Class<?> testClass,
			Predicate<Field> predicate) {

		findAnnotatedFields(testClass, TempDir.class, predicate).forEach(field -> {
			assertNonFinalField(field);
			assertSupportedType("field", field.getType());

			try {
				TempDir tempDir = findAnnotationOnField(field);
				makeAccessible(field).set(testInstance,
					getPathOrFile(field.getType(), new FieldContext(field), context, tempDir));
			}
			catch (Throwable t) {
				throw ExceptionUtils.throwAsUncheckedException(t);
			}
		});
	}

	/**
	 * Determine if the {@link Parameter} in the supplied {@link ParameterContext}
	 * is annotated with {@link TempDir @TempDir}.
	 */
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.isAnnotated(TempDir.class);
	}

	/**
	 * Resolve the current temporary directory for the {@link Parameter} in the
	 * supplied {@link ParameterContext}.
	 */
	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> parameterType = parameterContext.getParameter().getType();
		assertSupportedType("parameter", parameterType);
		TempDir tempDir = findAnnotationOnParameter(parameterContext);
		return getPathOrFile(parameterType, parameterContext, extensionContext, tempDir);
	}

	private static TempDir findAnnotationOnField(Field field) {
		return findAnnotation(field, TempDir.class).orElseThrow(
			() -> new JUnitException("Field " + field + " must be annotated with @TempDir"));
	}

	private static TempDir findAnnotationOnParameter(ParameterContext parameterContext) {
		return parameterContext.findAnnotation(TempDir.class).orElseThrow(() -> new JUnitException(
			"Parameter " + parameterContext.getParameter() + " must be annotated with @TempDir"));
	}

	private CleanupMode determineCleanupMode(TempDir annotation) {
		var cleanupMode = annotation.cleanup();
		return cleanupMode == DEFAULT ? this.configuration.getDefaultTempDirCleanupMode() : cleanupMode;
	}

	private Supplier<TempDirDeletionStrategy> determineDeletionStrategy(TempDir annotation) {
		var strategyClass = annotation.deletionStrategy();
		return strategyClass == TempDirDeletionStrategy.class //
				? this.configuration.getDefaultTempDirDeletionStrategySupplier() //
				: () -> ReflectionSupport.newInstance(strategyClass);
	}

	private TempDirFactory determineTempDirFactory(TempDir tempDir) {
		Class<? extends TempDirFactory> factory = tempDir.factory();

		return factory == TempDirFactory.class //
				? this.configuration.getDefaultTempDirFactorySupplier().get()
				: ReflectionSupport.newInstance(factory);
	}

	private static void assertNonFinalField(Field field) {
		if (ModifierSupport.isFinal(field)) {
			throw new ExtensionConfigurationException("@TempDir field [" + field + "] must not be declared as final.");
		}
	}

	private static void assertSupportedType(String target, Class<?> type) {
		if (type != Path.class && type != File.class) {
			throw new ExtensionConfigurationException("Can only resolve @TempDir " + target + " of type "
					+ Path.class.getName() + " or " + File.class.getName() + " but was: " + type.getName());
		}
	}

	private Object getPathOrFile(Class<?> elementType, AnnotatedElementContext elementContext,
			ExtensionContext extensionContext, TempDir tempDir) {
		TempDirFactory factory = determineTempDirFactory(tempDir);
		Cleanup cleanup = new Cleanup(determineCleanupMode(tempDir), determineDeletionStrategy(tempDir));
		return getPathOrFile(elementType, elementContext, factory, cleanup, extensionContext);
	}

	private static Object getPathOrFile(Class<?> elementType, AnnotatedElementContext elementContext,
			TempDirFactory factory, Cleanup cleanup, ExtensionContext extensionContext) {

		Path path = extensionContext.getStore(NAMESPACE.append(elementContext)) //
				.computeIfAbsent(KEY,
					__ -> createTempDir(factory, cleanup, elementType, elementContext, extensionContext),
					CloseablePath.class) //
				.get();

		return (elementType == Path.class) ? path : path.toFile();
	}

	static CloseablePath createTempDir(TempDirFactory factory, Cleanup cleanup, Class<?> elementType,
			AnnotatedElementContext elementContext, ExtensionContext extensionContext) {

		try {
			return new CloseablePath(factory, cleanup, elementType, elementContext, extensionContext);
		}
		catch (Exception ex) {
			throw new ExtensionConfigurationException("Failed to create default temp directory", ex);
		}
	}

	private static boolean selfOrChildFailed(ExtensionContext context) {
		return context.getExecutionException().isPresent() //
				|| getContextSpecificStore(context).getOrDefault(CHILD_FAILED, Boolean.class, false);
	}

	private static ExtensionContext.Store getContextSpecificStore(ExtensionContext context) {
		return context.getStore(NAMESPACE.append(context));
	}

	@SuppressWarnings("deprecation")
	static class CloseablePath implements Store.CloseableResource, AutoCloseable {

		private final @Nullable Path dir;
		private final TempDirFactory factory;
		private final Cleanup cleanup;
		private final AnnotatedElementContext elementContext;
		private final ExtensionContext extensionContext;

		private CloseablePath(TempDirFactory factory, Cleanup cleanup, Class<?> elementType,
				AnnotatedElementContext elementContext, ExtensionContext extensionContext) throws Exception {
			this.dir = factory.createTempDirectory(elementContext, extensionContext);
			this.factory = factory;
			this.cleanup = cleanup;
			this.elementContext = elementContext;
			this.extensionContext = extensionContext;

			if (this.dir == null || !Files.isDirectory(this.dir)) {
				close();
				throw new PreconditionViolationException("temp directory must be a directory");
			}

			if (elementType == File.class && !this.dir.getFileSystem().equals(FileSystems.getDefault())) {
				close();
				throw new PreconditionViolationException(
					"temp directory with non-default file system cannot be injected into " + File.class.getName()
							+ " target");
			}
		}

		Path get() {
			return requireNonNull(this.dir);
		}

		@Override
		public void close() throws IOException {
			try {
				if (this.dir != null) {
					this.cleanup.run(this.dir, this.elementContext, this.extensionContext);
				}
			}
			finally {
				this.factory.close();
			}
		}
	}

	private record FieldContext(Field field) implements AnnotatedElementContext {

		private FieldContext(Field field) {
			this.field = Preconditions.notNull(field, "field must not be null");
		}

		@Override
		public AnnotatedElement getAnnotatedElement() {
			return this.field;
		}

		@Override
		public String toString() {
			// @formatter:off
			return new ToStringBuilder(this)
					.append("field", this.field)
					.toString();
			// @formatter:on
		}

	}

	@SuppressWarnings("deprecation")
	private record FailureTracker(ExtensionContext context, ExtensionContext parentContext)
			implements Store.CloseableResource, AutoCloseable {

		@Override
		public void close() {
			if (selfOrChildFailed(context)) {
				getContextSpecificStore(parentContext).put(CHILD_FAILED, true);
			}
		}
	}

	record Cleanup(CleanupMode cleanupMode, Supplier<TempDirDeletionStrategy> deletionStrategy) {

		private static final Logger LOGGER = LoggerFactory.getLogger(Cleanup.class);

		void run(Path dir, AnnotatedElementContext elementContext, ExtensionContext extensionContext)
				throws IOException {
			if (cleanupMode == NEVER || (cleanupMode == ON_SUCCESS && selfOrChildFailed(extensionContext))) {
				LOGGER.info(() -> "Skipping cleanup of temp dir %s for %s due to CleanupMode.%s.".formatted(dir,
					descriptionFor(elementContext.getAnnotatedElement()), cleanupMode.name()));
				return;
			}

			LOGGER.trace(() -> "Cleaning up temp dir " + dir);
			if (Files.exists(dir)) {
				var result = deletionStrategy.get().delete(dir, elementContext, extensionContext);
				if (!result.isSuccessful()) {
					throw result.toException();
				}
			}
		}

		private static String descriptionFor(AnnotatedElement annotatedElement) {
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

}
