/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.support.ReflectionSupport.makeAccessible;
import static org.junit.platform.commons.util.KotlinReflectionUtils.getKotlinSuspendingFunctionParameters;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinSuspendingFunction;
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.aggregator.SimpleArgumentsAggregator;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.DefaultArgumentConverter;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.jupiter.params.support.FieldContext;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;

class ResolverFacade {

	static ResolverFacade create(Class<?> clazz, List<Field> fields) {
		Preconditions.notEmpty(fields, "Fields must not be empty");

		NavigableMap<Integer, List<FieldParameterDeclaration>> allIndexedParameters = new TreeMap<>();
		Set<FieldParameterDeclaration> aggregatorParameters = new LinkedHashSet<>();

		for (Field field : fields) {
			Parameter annotation = findAnnotation(field, Parameter.class) //
					.orElseThrow(() -> new JUnitException("No @Parameter annotation present"));
			int index = annotation.value();

			FieldParameterDeclaration declaration = new FieldParameterDeclaration(field, annotation.value());
			if (declaration.isAggregator()) {
				aggregatorParameters.add(declaration);
			}
			else {
				if (fields.size() == 1 && index == Parameter.UNSET_INDEX) {
					index = 0;
					declaration = new FieldParameterDeclaration(field, 0);
				}
				allIndexedParameters.computeIfAbsent(index, __ -> new ArrayList<>()) //
						.add(declaration);
			}
		}

		NavigableMap<Integer, FieldParameterDeclaration> uniqueIndexedParameters = validateFieldDeclarations(
			allIndexedParameters, aggregatorParameters);

		Stream.concat(uniqueIndexedParameters.values().stream(), aggregatorParameters.stream()) //
				.forEach(declaration -> makeAccessible(declaration.getField()));

		var requiredParameterCount = new RequiredParameterCount(uniqueIndexedParameters.size(), "field injection");

		return new ResolverFacade(clazz, uniqueIndexedParameters, aggregatorParameters, 0, requiredParameterCount);
	}

	static ResolverFacade create(Constructor<?> constructor, ParameterizedClass annotation) {
		// Inner classes get the outer instance as first (implicit) parameter
		int implicitParameters = isInnerClass(constructor.getDeclaringClass()) ? 1 : 0;
		return create(constructor, annotation, implicitParameters);
	}

	static ResolverFacade create(Method method, Annotation annotation) {
		if (isKotlinSuspendingFunction(method)) {
			return create(method, annotation, 0, getKotlinSuspendingFunctionParameters(method));
		}
		return create(method, annotation, 0);
	}

	/**
	 * Create a new {@link ResolverFacade} for the supplied {@link Executable}.
	 *
	 * <p>This method takes a best-effort approach at enforcing the following
	 * policy for parameterized class constructors and parameterized test
	 * methods that accept aggregators as arguments.
	 * <ol>
	 * <li>zero or more <em>indexed arguments</em> come first.</li>
	 * <li>zero or more <em>aggregators</em> come next.</li>
	 * <li>zero or more arguments supplied by other {@code ParameterResolver}
	 * implementations come last.</li>
	 * </ol>
	 */
	private static ResolverFacade create(Executable executable, Annotation annotation, int indexOffset) {
		return create(executable, annotation, indexOffset, executable.getParameters());
	}

	private static ResolverFacade create(Executable executable, Annotation annotation, int indexOffset,
			java.lang.reflect.Parameter[] parameters) {
		NavigableMap<Integer, ExecutableParameterDeclaration> indexedParameters = new TreeMap<>();
		NavigableMap<Integer, ExecutableParameterDeclaration> aggregatorParameters = new TreeMap<>();
		for (int index = indexOffset; index < parameters.length; index++) {
			ExecutableParameterDeclaration declaration = new ExecutableParameterDeclaration(parameters[index], index,
				indexOffset);
			if (declaration.isAggregator()) {
				Preconditions.condition(
					aggregatorParameters.isEmpty()
							|| aggregatorParameters.lastKey() == declaration.getParameterIndex() - 1,
					() -> """
							@%s %s declares formal parameters in an invalid order: \
							argument aggregators must be declared after any indexed arguments \
							and before any arguments resolved by another ParameterResolver.""".formatted(
						annotation.annotationType().getSimpleName(),
						DefaultParameterDeclarations.describe(executable)));
				aggregatorParameters.put(declaration.getParameterIndex(), declaration);
			}
			else if (aggregatorParameters.isEmpty()) {
				indexedParameters.put(declaration.getParameterIndex(), declaration);
			}
		}
		return new ResolverFacade(executable, indexedParameters, new LinkedHashSet<>(aggregatorParameters.values()),
			indexOffset, null);
	}

	private final int parameterIndexOffset;
	private final Map<ParameterDeclaration, Resolver> resolvers;
	private final DefaultParameterDeclarations indexedParameterDeclarations;
	private final Set<? extends ResolvableParameterDeclaration> aggregatorParameters;
	private final @Nullable RequiredParameterCount requiredParameterCount;

	private ResolverFacade(AnnotatedElement sourceElement,
			NavigableMap<Integer, ? extends ResolvableParameterDeclaration> indexedParameters,
			Set<? extends ResolvableParameterDeclaration> aggregatorParameters, int parameterIndexOffset,
			@Nullable RequiredParameterCount requiredParameterCount) {
		this.aggregatorParameters = aggregatorParameters;
		this.parameterIndexOffset = parameterIndexOffset;
		this.resolvers = new ConcurrentHashMap<>(indexedParameters.size() + aggregatorParameters.size());
		this.indexedParameterDeclarations = new DefaultParameterDeclarations(sourceElement, indexedParameters);
		this.requiredParameterCount = requiredParameterCount;
	}

	ParameterDeclarations getIndexedParameterDeclarations() {
		return this.indexedParameterDeclarations;
	}

	@Nullable
	RequiredParameterCount getRequiredParameterCount() {
		return this.requiredParameterCount;
	}

	boolean isSupportedParameter(ParameterContext parameterContext, EvaluatedArgumentSet arguments) {
		int index = toLogicalIndex(parameterContext);
		if (this.indexedParameterDeclarations.get(index).isPresent()) {
			return index < arguments.getConsumedLength();
		}
		return !this.aggregatorParameters.isEmpty()
				&& this.aggregatorParameters.stream().anyMatch(it -> it.getParameterIndex() == index);
	}

	/**
	 * Get the name of the parameter with the supplied index, if it is present
	 * and declared before the aggregators.
	 *
	 * @return an {@code Optional} containing the name of the parameter
	 */
	Optional<String> getParameterName(int parameterIndex) {
		return this.indexedParameterDeclarations.get(parameterIndex) //
				.flatMap(ParameterDeclaration::getParameterName);
	}

	/**
	 * Determine the length of the arguments array that is considered consumed
	 * by the parameter declarations in this resolver.
	 *
	 * <p>If an aggregator is present, all arguments are considered consumed.
	 * Otherwise, the consumed argument length is the minimum of the total
	 * length and the number of indexed parameter declarations.
	 */
	int determineConsumedArgumentLength(int totalLength) {
		NavigableMap<Integer, ? extends ParameterDeclaration> declarationsByIndex = this.indexedParameterDeclarations.declarationsByIndex;
		return this.aggregatorParameters.isEmpty() //
				? Math.min(totalLength, declarationsByIndex.isEmpty() ? 0 : declarationsByIndex.lastKey() + 1) //
				: totalLength;
	}

	/**
	 * Determine the number of arguments that are considered consumed by the
	 * parameter declarations in this resolver.
	 *
	 * <p>If an aggregator is present, all arguments are considered consumed.
	 * Otherwise, the consumed argument count, is the number of indexes that
	 * correspond to indexed parameter declarations.
	 */
	int determineConsumedArgumentCount(EvaluatedArgumentSet arguments) {
		if (this.aggregatorParameters.isEmpty()) {
			return this.indexedParameterDeclarations.declarationsByIndex.subMap(0,
				arguments.getConsumedLength()).size();
		}
		return arguments.getTotalLength();
	}

	ArgumentSetLifecycleMethod.ParameterResolver createLifecycleMethodParameterResolver(Method method,
			Annotation annotation) {
		ResolverFacade originalResolverFacade = this;
		ResolverFacade lifecycleMethodResolverFacade = create(method, annotation);

		Map<ParameterDeclaration, ResolvableParameterDeclaration> parameterDeclarationMapping = new HashMap<>();
		List<String> errors = validateLifecycleMethodParameters(originalResolverFacade, lifecycleMethodResolverFacade,
			parameterDeclarationMapping);

		return Try //
				.call(() -> configurationErrorOrSuccess(errors,
					() -> new DefaultArgumentSetLifecycleMethodParameterResolver(originalResolverFacade,
						lifecycleMethodResolverFacade, parameterDeclarationMapping))) //
				.getNonNullOrThrow(cause -> new ExtensionConfigurationException(
					"Invalid @%s lifecycle method declaration: %s".formatted(
						annotation.annotationType().getSimpleName(), method.toGenericString()),
					cause));
	}

	/**
	 * Resolve the parameter for the supplied context using the supplied
	 * arguments.
	 */
	@Nullable
	Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext, EvaluatedArgumentSet arguments,
			int invocationIndex, ResolutionCache resolutionCache) {

		int parameterIndex = toLogicalIndex(parameterContext);
		ResolvableParameterDeclaration declaration = findDeclaration(parameterIndex) //
				.orElseThrow(
					() -> new ParameterResolutionException("Parameter index out of bounds: " + parameterIndex));

		return resolutionCache.resolve(declaration,
			() -> resolve(declaration, extensionContext, arguments, invocationIndex, Optional.of(parameterContext)));
	}

	private Optional<? extends ResolvableParameterDeclaration> findDeclaration(int parameterIndex) {
		ResolvableParameterDeclaration declaration = this.indexedParameterDeclarations.declarationsByIndex //
				.get(parameterIndex);
		if (declaration == null) {
			return this.aggregatorParameters.stream() //
					.filter(it -> it.getParameterIndex() == parameterIndex) //
					.findFirst();
		}
		return Optional.of(declaration);
	}

	void resolveAndInjectFields(Object testInstance, ExtensionContext extensionContext, EvaluatedArgumentSet arguments,
			int invocationIndex, ResolutionCache resolutionCache) {

		if (this.indexedParameterDeclarations.sourceElement.equals(testInstance.getClass())) {
			getAllParameterDeclarations() //
					.filter(FieldParameterDeclaration.class::isInstance) //
					.map(FieldParameterDeclaration.class::cast) //
					.forEach(declaration -> setField(testInstance, declaration, extensionContext, arguments,
						invocationIndex, resolutionCache));
		}
	}

	private Stream<ParameterDeclaration> getAllParameterDeclarations() {
		return Stream.concat(this.indexedParameterDeclarations.declarationsByIndex.values().stream(),
			aggregatorParameters.stream());
	}

	private void setField(Object testInstance, FieldParameterDeclaration declaration, ExtensionContext extensionContext,
			EvaluatedArgumentSet arguments, int invocationIndex, ResolutionCache resolutionCache) {

		Object argument = resolutionCache.resolve(declaration,
			() -> resolve(declaration, extensionContext, arguments, invocationIndex, Optional.empty()));
		try {
			declaration.getField().set(testInstance, argument);
		}
		catch (Exception e) {
			throw new JUnitException("Failed to inject parameter value into field: " + declaration.getField(), e);
		}
	}

	private @Nullable Object resolve(ResolvableParameterDeclaration parameterDeclaration,
			ExtensionContext extensionContext, EvaluatedArgumentSet arguments, int invocationIndex,
			Optional<ParameterContext> parameterContext) {
		Resolver resolver = getResolver(extensionContext, parameterDeclaration);
		return parameterDeclaration.resolve(resolver, extensionContext, arguments, invocationIndex, parameterContext);
	}

	private Resolver getResolver(ExtensionContext extensionContext, ResolvableParameterDeclaration declaration) {
		return this.resolvers.computeIfAbsent(declaration, __ -> this.aggregatorParameters.contains(declaration) //
				? createAggregator(declaration, extensionContext) //
				: createConverter(declaration, extensionContext));
	}

	private int toLogicalIndex(ParameterContext parameterContext) {
		int index = parameterContext.getIndex() - this.parameterIndexOffset;
		Preconditions.condition(index >= 0, () -> "Parameter index must be greater than or equal to zero");
		return index;
	}

	private static NavigableMap<Integer, FieldParameterDeclaration> validateFieldDeclarations(
			NavigableMap<Integer, List<FieldParameterDeclaration>> indexedParameters,
			Set<FieldParameterDeclaration> aggregatorParameters) {

		List<String> errors = new ArrayList<>();
		validateIndexedParameters(indexedParameters, errors);
		validateAggregatorParameters(aggregatorParameters, errors);

		return configurationErrorOrSuccess(errors, () -> indexedParameters.entrySet().stream() //
				.collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get(0), (d, __) -> d, TreeMap::new)));
	}

	private static List<String> validateLifecycleMethodParameters(ResolverFacade originalResolverFacade,
			ResolverFacade lifecycleMethodResolverFacade,
			Map<ParameterDeclaration, ResolvableParameterDeclaration> parameterDeclarationMapping) {
		List<ParameterDeclaration> actualDeclarations = lifecycleMethodResolverFacade.indexedParameterDeclarations.getAll();
		List<String> errors = new ArrayList<>();
		for (int parameterIndex = 0; parameterIndex < actualDeclarations.size(); parameterIndex++) {
			ParameterDeclaration actualDeclaration = actualDeclarations.get(parameterIndex);
			ResolvableParameterDeclaration originalDeclaration = originalResolverFacade.indexedParameterDeclarations.declarationsByIndex //
					.get(parameterIndex);
			if (originalDeclaration == null) {
				break;
			}
			if (!actualDeclaration.getParameterType().equals(originalDeclaration.getParameterType())) {
				errors.add(
					"parameter%s with index %d is incompatible with the parameter declared on the parameterized class: expected type '%s' but found '%s'".formatted(
						parameterName(actualDeclaration), parameterIndex, originalDeclaration.getParameterType(),
						actualDeclaration.getParameterType()));
			}
			else if (findAnnotation(actualDeclaration.getAnnotatedElement(), ConvertWith.class).isPresent()) {
				errors.add("parameter%s with index %d must not be annotated with @ConvertWith".formatted(
					parameterName(actualDeclaration), parameterIndex));
			}
			else if (errors.isEmpty()) {
				parameterDeclarationMapping.put(actualDeclaration, originalDeclaration);
			}
		}
		return errors;
	}

	private static String parameterName(ParameterDeclaration actualDeclaration) {
		return actualDeclaration.getParameterName().map(name -> " '" + name + "'").orElse("");
	}

	private static <T> T configurationErrorOrSuccess(List<String> errors, Supplier<T> successfulResult) {
		if (errors.isEmpty()) {
			return successfulResult.get();
		}
		else if (errors.size() == 1) {
			throw new PreconditionViolationException("Configuration error: " + errors.get(0) + ".");
		}
		else {
			throw new PreconditionViolationException("%d configuration errors:%n%s".formatted(errors.size(),
				errors.stream().collect(joining(lineSeparator() + "- ", "- ", ""))));
		}
	}

	private static void validateIndexedParameters(
			NavigableMap<Integer, List<FieldParameterDeclaration>> indexedParameters, List<String> errors) {

		if (indexedParameters.isEmpty()) {
			return;
		}

		indexedParameters.forEach(
			(index, declarations) -> validateIndexedParameterDeclarations(index, declarations, errors));

		for (int index = 0; index <= indexedParameters.lastKey(); index++) {
			if (!indexedParameters.containsKey(index)) {
				errors.add("no field annotated with @Parameter(%d) declared".formatted(index));
			}
		}
	}

	private static void validateIndexedParameterDeclarations(int index, List<FieldParameterDeclaration> declarations,
			List<String> errors) {
		List<Field> fields = declarations.stream().map(FieldParameterDeclaration::getField).toList();
		if (index < 0) {
			declarations.stream() //
					.map(
						declaration -> "index must be greater than or equal to zero in @Parameter(%d) annotation on field [%s]".formatted(
							index, declaration.getField())) //
					.forEach(errors::add);
		}
		else if (declarations.size() > 1) {
			errors.add("duplicate index declared in @Parameter(%d) annotation on fields %s".formatted(index, fields));
		}
		fields.stream() //
				.filter(ModifierSupport::isFinal) //
				.map("@Parameter field [%s] must not be declared as final"::formatted) //
				.forEach(errors::add);
	}

	private static void validateAggregatorParameters(Set<FieldParameterDeclaration> aggregatorParameters,
			List<String> errors) {
		aggregatorParameters.stream() //
				.filter(declaration -> declaration.getParameterIndex() != Parameter.UNSET_INDEX) //
				.map(
					declaration -> "no index may be declared in @Parameter(%d) annotation on aggregator field [%s]".formatted(
						declaration.getParameterIndex(), declaration.getField())) //
				.forEach(errors::add);
	}

	private static Converter createConverter(ParameterDeclaration declaration, ExtensionContext extensionContext) {
		try { // @formatter:off
			return findAnnotation(declaration.getAnnotatedElement(), ConvertWith.class)
					.map(ConvertWith::value)
					.map(clazz -> ParameterizedTestSpiInstantiator.instantiate(ArgumentConverter.class, clazz, extensionContext))
					.map(converter -> AnnotationConsumerInitializer.initialize(declaration.getAnnotatedElement(), converter))
					.map(Converter::new)
					.orElse(Converter.DEFAULT);
		} // @formatter:on
		catch (Exception ex) {
			throw parameterResolutionException("Error creating ArgumentConverter", ex, declaration.getParameterIndex());
		}
	}

	private static Aggregator createAggregator(ParameterDeclaration declaration, ExtensionContext extensionContext) {
		try { // @formatter:off
			return findAnnotation(declaration.getAnnotatedElement(), AggregateWith.class)
					.map(AggregateWith::value)
					.map(clazz -> ParameterizedTestSpiInstantiator.instantiate(ArgumentsAggregator.class, clazz, extensionContext))
					.map(Aggregator::new)
					.orElse(Aggregator.DEFAULT);
		} // @formatter:on
		catch (Exception ex) {
			throw parameterResolutionException("Error creating ArgumentsAggregator", ex,
				declaration.getParameterIndex());
		}
	}

	private static ParameterResolutionException parameterResolutionException(String message, Exception cause,
			int index) {
		String fullMessage = message + " at index " + index;
		if (StringUtils.isNotBlank(cause.getMessage())) {
			fullMessage += ": " + cause.getMessage();
		}
		return new ParameterResolutionException(fullMessage, cause);
	}

	private interface Resolver {

		@Nullable
		Object resolve(ParameterContext parameterContext, int parameterIndex, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex);

		@Nullable
		Object resolve(FieldContext fieldContext, ExtensionContext extensionContext, EvaluatedArgumentSet arguments,
				int invocationIndex);

	}

	private record Converter(ArgumentConverter argumentConverter) implements Resolver {

		static final Converter DEFAULT = new Converter(DefaultArgumentConverter.INSTANCE);

		@Override
		public @Nullable Object resolve(ParameterContext parameterContext, int parameterIndex,
				ExtensionContext extensionContext, EvaluatedArgumentSet arguments, int invocationIndex) {
			Object argument = arguments.getConsumedPayload(parameterIndex);
			try {
				return this.argumentConverter.convert(argument, parameterContext);
			}
			catch (Exception ex) {
				throw parameterResolutionException("Error converting parameter", ex, parameterContext.getIndex());
			}
		}

		@Override
		public @Nullable Object resolve(FieldContext fieldContext, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex) {

			Object argument = arguments.getConsumedPayload(fieldContext.getParameterIndex());
			try {
				return this.argumentConverter.convert(argument, fieldContext);
			}
			catch (Exception ex) {
				throw parameterResolutionException("Error converting parameter", ex, fieldContext.getParameterIndex());
			}
		}
	}

	private record Aggregator(ArgumentsAggregator argumentsAggregator) implements Resolver {

		private static final Aggregator DEFAULT = new Aggregator(new SimpleArgumentsAggregator() {
			@Override
			protected Object aggregateArguments(ArgumentsAccessor accessor, Class<?> targetType,
					AnnotatedElementContext context, int parameterIndex) throws ArgumentsAggregationException {
				return accessor;
			}
		});

		@Override
		public @Nullable Object resolve(ParameterContext parameterContext, int parameterIndex,
				ExtensionContext extensionContext, EvaluatedArgumentSet arguments, int invocationIndex) {
			ArgumentsAccessor accessor = requireNonNull(ParameterInfo.get(extensionContext)).getArguments();
			try {
				return this.argumentsAggregator.aggregateArguments(accessor, parameterContext);
			}
			catch (Exception ex) {
				throw parameterResolutionException("Error aggregating arguments for parameter", ex,
					parameterContext.getIndex());
			}
		}

		@Override
		public @Nullable Object resolve(FieldContext fieldContext, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex) {
			ArgumentsAccessor accessor = requireNonNull(ParameterInfo.get(extensionContext)).getArguments();
			try {
				return this.argumentsAggregator.aggregateArguments(accessor, fieldContext);
			}
			catch (Exception ex) {
				throw parameterResolutionException("Error aggregating arguments for parameter", ex,
					fieldContext.getParameterIndex());
			}
		}
	}

	private record DefaultParameterDeclarations(AnnotatedElement sourceElement,
			NavigableMap<Integer, ? extends ResolvableParameterDeclaration> declarationsByIndex)
			implements ParameterDeclarations {

		@Override
		public AnnotatedElement getSourceElement() {
			return this.sourceElement;
		}

		@Override
		public Optional<ParameterDeclaration> getFirst() {
			return this.declarationsByIndex.isEmpty() //
					? Optional.empty() //
					: Optional.of(this.declarationsByIndex.firstEntry().getValue());
		}

		@Override
		public List<ParameterDeclaration> getAll() {
			return List.copyOf(this.declarationsByIndex.values());
		}

		@Override
		public Optional<ParameterDeclaration> get(int parameterIndex) {
			return Optional.ofNullable(this.declarationsByIndex.get(parameterIndex));
		}

		@Override
		public String getSourceElementDescription() {
			return describe(this.sourceElement);
		}

		static String describe(AnnotatedElement sourceElement) {
			if (sourceElement instanceof Method method) {
				return "method [%s]".formatted(method.toGenericString());
			}
			if (sourceElement instanceof Constructor<?> constructor) {
				return "constructor [%s]".formatted(constructor.toGenericString());
			}
			if (sourceElement instanceof Class<?> clazz) {
				return "class [%s]".formatted(clazz.getName());
			}
			return sourceElement.toString();
		}
	}

	private abstract static class ResolvableParameterDeclaration implements ParameterDeclaration {

		/**
		 * Determine if the supplied {@link Parameter} is an aggregator (i.e., of
		 * type {@link ArgumentsAccessor} or annotated with {@link AggregateWith}).
		 *
		 * @return {@code true} if the parameter is an aggregator
		 */
		boolean isAggregator() {
			return ArgumentsAccessor.class.isAssignableFrom(getParameterType())
					|| isAnnotated(getAnnotatedElement(), AggregateWith.class);
		}

		abstract @Nullable Object resolve(Resolver resolver, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex,
				Optional<ParameterContext> originalParameterContext);
	}

	private static class FieldParameterDeclaration extends ResolvableParameterDeclaration implements FieldContext {

		private final Field field;
		private final int index;

		FieldParameterDeclaration(Field field, int index) {
			this.field = field;
			this.index = index;
		}

		@Override
		public Field getField() {
			return this.field;
		}

		@Override
		public Field getAnnotatedElement() {
			return this.field;
		}

		@Override
		public Class<?> getParameterType() {
			return this.field.getType();
		}

		@Override
		public int getParameterIndex() {
			return index;
		}

		@Override
		public Optional<String> getParameterName() {
			return Optional.of(this.field.getName());
		}

		@Override
		public @Nullable Object resolve(Resolver resolver, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex,
				Optional<ParameterContext> originalParameterContext) {
			return resolver.resolve(this, extensionContext, arguments, invocationIndex);
		}
	}

	private static class ExecutableParameterDeclaration extends ResolvableParameterDeclaration {

		private final java.lang.reflect.Parameter parameter;
		private final int index;
		private final int indexOffset;

		ExecutableParameterDeclaration(java.lang.reflect.Parameter parameter, int index, int indexOffset) {
			this.parameter = parameter;
			this.index = index;
			this.indexOffset = indexOffset;
		}

		@Override
		public java.lang.reflect.Parameter getAnnotatedElement() {
			return this.parameter;
		}

		@Override
		public Class<?> getParameterType() {
			return this.parameter.getType();
		}

		@Override
		public int getParameterIndex() {
			return this.index - this.indexOffset;
		}

		@Override
		public Optional<String> getParameterName() {
			return this.parameter.isNamePresent() ? Optional.of(this.parameter.getName()) : Optional.empty();
		}

		@Override
		public @Nullable Object resolve(Resolver resolver, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex,
				Optional<ParameterContext> originalParameterContext) {
			ParameterContext parameterContext = originalParameterContext //
					.filter(it -> it.getParameter().equals(this.parameter)) //
					.orElseGet(() -> toParameterContext(extensionContext, originalParameterContext));
			return resolver.resolve(parameterContext, getParameterIndex(), extensionContext, arguments,
				invocationIndex);
		}

		private ParameterContext toParameterContext(ExtensionContext extensionContext,
				Optional<ParameterContext> originalParameterContext) {
			Optional<Object> target = originalParameterContext.flatMap(ParameterContext::getTarget);
			if (target.isEmpty()) {
				target = extensionContext.getTestInstance();
			}
			return toParameterContext(target);
		}

		private ParameterContext toParameterContext(Optional<Object> target) {
			return new ParameterContext() {
				@Override
				public java.lang.reflect.Parameter getParameter() {
					return ExecutableParameterDeclaration.this.parameter;
				}

				@Override
				public int getIndex() {
					return ExecutableParameterDeclaration.this.index;
				}

				@Override
				public Optional<Object> getTarget() {
					return target;
				}
			};
		}
	}

	private record DefaultArgumentSetLifecycleMethodParameterResolver(ResolverFacade originalResolverFacade,
			ResolverFacade lifecycleMethodResolverFacade,
			Map<ParameterDeclaration, ResolvableParameterDeclaration> parameterDeclarationMapping)
			implements ArgumentSetLifecycleMethod.ParameterResolver {

		@Override
		public boolean supports(ParameterContext parameterContext) {
			return this.lifecycleMethodResolverFacade.findDeclaration(parameterContext.getIndex()) //
					.filter(it -> this.parameterDeclarationMapping.containsKey(it) || it.isAggregator()) //
					.isPresent();
		}

		@Override
		public @Nullable Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext,
				EvaluatedArgumentSet arguments, int invocationIndex, ResolutionCache resolutionCache) {

			ResolvableParameterDeclaration actualDeclaration = this.lifecycleMethodResolverFacade //
					.findDeclaration(parameterContext.getIndex()) //
					.orElseThrow(() -> new ParameterResolutionException(
						"Parameter index out of bounds: " + parameterContext.getIndex()));

			ResolvableParameterDeclaration originalDeclaration = this.parameterDeclarationMapping //
					.get(actualDeclaration);
			if (originalDeclaration == null) {
				return this.lifecycleMethodResolverFacade.resolve(actualDeclaration, extensionContext, arguments,
					invocationIndex, Optional.of(parameterContext));
			}
			return resolutionCache.resolve(originalDeclaration,
				() -> this.originalResolverFacade.resolve(originalDeclaration, extensionContext, arguments,
					invocationIndex, Optional.of(parameterContext)));
		}
	}

	record RequiredParameterCount(int value, String reason) {
	}
}
