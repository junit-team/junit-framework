/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static java.util.Collections.emptyList;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.ModifierSupport.isStatic;
import static org.junit.platform.commons.util.KotlinReflectionUtils.getKotlinSuspendingFunctionParameterTypes;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinSuspendingFunction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code DisplayNameGenerator} defines the SPI for generating display names
 * programmatically.
 *
 * <p>Display names are typically used for test reporting in IDEs and build
 * tools and may contain spaces, special characters, and even emoji.
 *
 * <p>Concrete implementations must have a <em>default constructor</em>.
 *
 * <p>A {@link DisplayNameGenerator} can be configured <em>globally</em> for the
 * entire test suite via the {@value #DEFAULT_GENERATOR_PROPERTY_NAME}
 * configuration parameter (see the User Guide for details) or <em>locally</em>
 * for a test class via the {@link DisplayNameGeneration @DisplayNameGeneration}
 * annotation.
 *
 * <h2>Built-in Implementations</h2>
 * <ul>
 * <li>{@link Standard}</li>
 * <li>{@link Simple}</li>
 * <li>{@link ReplaceUnderscores}</li>
 * <li>{@link IndicativeSentences}</li>
 * </ul>
 *
 * @since 5.4
 * @see DisplayName @DisplayName
 * @see DisplayNameGeneration @DisplayNameGeneration
 */
@API(status = STABLE, since = "5.7")
public interface DisplayNameGenerator {

	/**
	 * Property name used to set the default display name generator class name:
	 * {@value}
	 *
	 * <h4>Supported Values</h4>
	 *
	 * <p>Supported values include fully qualified class names for types that
	 * implement {@link DisplayNameGenerator}.
	 *
	 * <p>If not specified, the default is
	 * {@link DisplayNameGenerator.Standard}.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_GENERATOR_PROPERTY_NAME = "junit.jupiter.displayname.generator.default";

	/**
	 * Generate a display name for the given top-level or {@code static} nested test class.
	 *
	 * <p>If this method returns {@code null}, the default display name
	 * generator will be used instead.
	 *
	 * @param testClass the class to generate a name for; never {@code null}
	 * @return the display name for the class; never blank
	 */
	String generateDisplayNameForClass(Class<?> testClass);

	/**
	 * Generate a display name for the given {@link Nested @Nested} inner test
	 * class.
	 *
	 * <p>If this method returns {@code null}, the default display name
	 * generator will be used instead.
	 *
	 * @param nestedClass the class to generate a name for; never {@code null}
	 * @return the display name for the nested class; never blank
	 * @deprecated in favor of {@link #generateDisplayNameForNestedClass(List, Class)}
	 */
	@API(status = DEPRECATED, since = "5.12")
	@Deprecated(since = "5.12")
	default String generateDisplayNameForNestedClass(Class<?> nestedClass) {
		throw new UnsupportedOperationException(
			"Implement generateDisplayNameForNestedClass(List<Class<?>>, Class<?>) instead");
	}

	/**
	 * Generate a display name for the given {@link Nested @Nested} inner test
	 * class.
	 *
	 * <p>If this method returns {@code null}, the default display name
	 * generator will be used instead.
	 *
	 * @implNote The classes supplied as {@code enclosingInstanceTypes} may
	 * differ from the classes returned from invocations of
	 * {@link Class#getEnclosingClass()} &mdash; for example, when a nested test
	 * class is inherited from a superclass.
	 *
	 * @param enclosingInstanceTypes the runtime types of the enclosing
	 * instances for the test class, ordered from outermost to innermost,
	 * excluding {@code nestedClass}; never {@code null}
	 * @param nestedClass the class to generate a name for; never {@code null}
	 * @return the display name for the nested class; never blank
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	default String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
		return generateDisplayNameForNestedClass(nestedClass);
	}

	/**
	 * Generate a display name for the given method.
	 *
	 * <p>If this method returns {@code null}, the default display name
	 * generator will be used instead.
	 *
	 * @implNote The class instance supplied as {@code testClass} may differ from
	 * the class returned by {@code testMethod.getDeclaringClass()} &mdash; for
	 * example, when a test method is inherited from a superclass.
	 *
	 * @param testClass the class the test method is invoked on; never {@code null}
	 * @param testMethod method to generate a display name for; never {@code null}
	 * @return the display name for the test; never blank
	 * @deprecated in favor of {@link #generateDisplayNameForMethod(List, Class, Method)}
	 */
	@API(status = DEPRECATED, since = "5.12")
	@Deprecated(since = "5.12")
	default String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
		throw new UnsupportedOperationException(
			"Implement generateDisplayNameForMethod(List<Class<?>>, Class<?>, Method) instead");
	}

	/**
	 * Generate a display name for the given method.
	 *
	 * <p>If this method returns {@code null}, the default display name
	 * generator will be used instead.
	 *
	 * @implNote The classes supplied as {@code enclosingInstanceTypes} may
	 * differ from the classes returned from invocations of
	 * {@link Class#getEnclosingClass()} &mdash; for example, when a nested test
	 * class is inherited from a superclass. Similarly, the class instance
	 * supplied as {@code testClass} may differ from the class returned by
	 * {@code testMethod.getDeclaringClass()} &mdash; for example, when a test
	 * method is inherited from a superclass.
	 *
	 * @param enclosingInstanceTypes the runtime types of the enclosing
	 * instances for the test class, ordered from outermost to innermost,
	 * excluding {@code testClass}; never {@code null}
	 * @param testClass the class the test method is invoked on; never {@code null}
	 * @param testMethod method to generate a display name for; never {@code null}
	 * @return the display name for the test; never blank
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	default String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
			Method testMethod) {
		return generateDisplayNameForMethod(testClass, testMethod);
	}

	/**
	 * Generate a string representation of the formal parameters of the supplied
	 * method, consisting of the {@linkplain Class#getSimpleName() simple names}
	 * of the parameter types, separated by commas, and enclosed in parentheses.
	 *
	 * @param method the method from to extract the parameter types from; never
	 * {@code null}
	 * @return a string representation of all parameter types of the supplied
	 * method or {@code "()"} if the method declares no parameters
	 */
	static String parameterTypesAsString(Method method) {
		Preconditions.notNull(method, "Method must not be null");
		var parameterTypes = isKotlinSuspendingFunction(method) //
				? getKotlinSuspendingFunctionParameterTypes(method) //
				: method.getParameterTypes();
		return '(' + ClassUtils.nullSafeToString(Class::getSimpleName, parameterTypes) + ')';
	}

	/**
	 * Standard {@code DisplayNameGenerator}.
	 *
	 * <p>This implementation matches the standard display name generation
	 * behavior in place since JUnit Jupiter was introduced.
	 */
	class Standard implements DisplayNameGenerator {

		static final DisplayNameGenerator INSTANCE = new Standard();

		public Standard() {
		}

		@Override
		public String generateDisplayNameForClass(Class<?> testClass) {
			String name = testClass.getName();
			int lastDot = name.lastIndexOf('.');
			return name.substring(lastDot + 1);
		}

		@Override
		public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
			return nestedClass.getSimpleName();
		}

		@Override
		public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
				Method testMethod) {
			return testMethod.getName() + parameterTypesAsString(testMethod);
		}
	}

	/**
	 * Simple {@code DisplayNameGenerator} that removes trailing parentheses
	 * for methods with no parameters.
	 *
	 * <p>This generator extends the functionality of {@link Standard} by
	 * removing parentheses ({@code '()'}) found at the end of method names
	 * with no parameters.
	 *
	 * @since 5.7
	 */
	@API(status = STABLE, since = "5.7")
	class Simple extends Standard {

		static final DisplayNameGenerator INSTANCE = new Simple();

		public Simple() {
		}

		@Override
		public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
				Method testMethod) {
			String displayName = testMethod.getName();
			if (hasParameters(testMethod)) {
				displayName += ' ' + parameterTypesAsString(testMethod);
			}
			return displayName;
		}

		private static boolean hasParameters(Method method) {
			return method.getParameterCount() > 0;
		}

	}

	/**
	 * {@code DisplayNameGenerator} that replaces underscores with spaces.
	 *
	 * <p>This generator extends the functionality of {@link Simple} by
	 * replacing all underscores ({@code '_'}) found in class and method names
	 * with spaces ({@code ' '}).
	 */
	class ReplaceUnderscores extends Simple {

		static final DisplayNameGenerator INSTANCE = new ReplaceUnderscores();

		public ReplaceUnderscores() {
		}

		@Override
		public String generateDisplayNameForClass(Class<?> testClass) {
			return replaceUnderscores(super.generateDisplayNameForClass(testClass));
		}

		@Override
		public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
			return replaceUnderscores(super.generateDisplayNameForNestedClass(enclosingInstanceTypes, nestedClass));
		}

		@Override
		public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
				Method testMethod) {
			return replaceUnderscores(
				super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod));
		}

		private static String replaceUnderscores(String name) {
			return name.replace('_', ' ');
		}

	}

	/**
	 * {@code DisplayNameGenerator} that generates complete sentences.
	 *
	 * <p>This generator generates display names that build up complete sentences
	 * by concatenating the names of the test and the enclosing classes. The
	 * sentence fragments are concatenated using a separator. The separator and
	 * the display name generator for individual sentence fragments can be configured
	 * via the {@link IndicativeSentencesGeneration @IndicativeSentencesGeneration}
	 * annotation.
	 *
	 * <p>If you do not want to rely on a display name generator for individual
	 * sentence fragments, you can supply custom text for individual fragments
	 * via the {@link SentenceFragment @SentenceFragment} annotation.
	 *
	 * @since 5.7
	 */
	@API(status = STABLE, since = "5.10")
	class IndicativeSentences implements DisplayNameGenerator {

		/**
		 * {@code @SentenceFragment} is used to configure a custom sentence fragment
		 * for a sentence generated by the {@link IndicativeSentences IndicativeSentences}
		 * {@code DisplayNameGenerator}.
		 *
		 * <p>Note that {@link DisplayName @DisplayName} always takes precedence
		 * over {@code @SentenceFragment}.
		 *
		 * @since 5.13
		 */
		@Target({ ElementType.TYPE, ElementType.METHOD })
		@Retention(RetentionPolicy.RUNTIME)
		@API(status = EXPERIMENTAL, since = "6.0")
		public @interface SentenceFragment {

			/**
			 * Custom sentence fragment for the annotated class or method.
			 *
			 * @return a custom sentence fragment; never blank or consisting solely
			 * of whitespace
			 */
			String value();

		}

		static final DisplayNameGenerator INSTANCE = new IndicativeSentences();

		private static final Predicate<Class<?>> notIndicativeSentences = clazz -> clazz != IndicativeSentences.class;

		public IndicativeSentences() {
		}

		@Override
		public String generateDisplayNameForClass(Class<?> testClass) {
			String sentenceFragment = getSentenceFragment(testClass);
			return (sentenceFragment != null ? sentenceFragment
					: getGeneratorFor(testClass, emptyList()).generateDisplayNameForClass(testClass));
		}

		@Override
		public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
			return getSentenceBeginning(nestedClass, enclosingInstanceTypes);
		}

		@Override
		public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
				Method testMethod) {

			String displayName = getSentenceBeginning(testClass, enclosingInstanceTypes)
					+ getFragmentSeparator(testClass, enclosingInstanceTypes);

			String sentenceFragment = getSentenceFragment(testMethod);
			displayName += (sentenceFragment != null ? sentenceFragment
					: getGeneratorFor(testClass, enclosingInstanceTypes).generateDisplayNameForMethod(
						enclosingInstanceTypes, testClass, testMethod));
			return displayName;
		}

		private String getSentenceBeginning(Class<?> testClass, List<Class<?>> enclosingInstanceTypes) {
			Class<?> enclosingClass = enclosingInstanceTypes.isEmpty() ? null
					: enclosingInstanceTypes.get(enclosingInstanceTypes.size() - 1);

			String sentenceFragment = findAnnotation(testClass, DisplayName.class)//
					.map(DisplayName::value)//
					.map(String::strip)//
					.orElseGet(() -> getSentenceFragment(testClass));

			if (enclosingClass == null || isStatic(testClass)) { // top-level class
				if (sentenceFragment != null) {
					return sentenceFragment;
				}
				Class<? extends DisplayNameGenerator> generatorClass = findDisplayNameGeneration(testClass,
					enclosingInstanceTypes)//
							.map(DisplayNameGeneration::value)//
							.filter(notIndicativeSentences)//
							.orElse(null);
				if (generatorClass != null) {
					return getDisplayNameGenerator(generatorClass).generateDisplayNameForClass(testClass);
				}
				return generateDisplayNameForClass(testClass);
			}

			List<Class<?>> remainingEnclosingInstanceTypes = enclosingInstanceTypes.isEmpty() ? emptyList()
					: enclosingInstanceTypes.subList(0, enclosingInstanceTypes.size() - 1);

			// Only build prefix based on the enclosing class if the enclosing
			// class is also configured to use the IndicativeSentences generator.
			boolean buildPrefix = findDisplayNameGeneration(enclosingClass, remainingEnclosingInstanceTypes)//
					.map(DisplayNameGeneration::value)//
					.filter(IndicativeSentences.class::equals)//
					.isPresent();

			String prefix = (buildPrefix
					? getSentenceBeginning(enclosingClass, remainingEnclosingInstanceTypes)
							+ getFragmentSeparator(testClass, enclosingInstanceTypes)
					: "");

			return prefix + (sentenceFragment != null ? sentenceFragment
					: getGeneratorFor(testClass, enclosingInstanceTypes).generateDisplayNameForNestedClass(
						remainingEnclosingInstanceTypes, testClass));
		}

		/**
		 * Get the sentence fragment separator.
		 *
		 * <p>If {@link IndicativeSentencesGeneration @IndicativeSentencesGeneration}
		 * is present (searching enclosing classes if not found locally), the
		 * configured {@link IndicativeSentencesGeneration#separator() separator}
		 * will be used. Otherwise, {@link IndicativeSentencesGeneration#DEFAULT_SEPARATOR}
		 * will be used.
		 *
		 * @param testClass the test class to search on for {@code @IndicativeSentencesGeneration}
		 * @param enclosingInstanceTypes the runtime types of the enclosing
		 * instances; never {@code null}
		 * @return the sentence fragment separator
		 */
		private static String getFragmentSeparator(Class<?> testClass, List<Class<?>> enclosingInstanceTypes) {
			return findIndicativeSentencesGeneration(testClass, enclosingInstanceTypes)//
					.map(IndicativeSentencesGeneration::separator)//
					.orElse(IndicativeSentencesGeneration.DEFAULT_SEPARATOR);
		}

		/**
		 * Get the display name generator to use for the supplied test class.
		 *
		 * <p>If {@link IndicativeSentencesGeneration @IndicativeSentencesGeneration}
		 * is present (searching enclosing classes if not found locally), the
		 * configured {@link IndicativeSentencesGeneration#generator() generator}
		 * will be used. Otherwise, {@link IndicativeSentencesGeneration#DEFAULT_GENERATOR}
		 * will be used.
		 *
		 * @param testClass the test class to search on for {@code @IndicativeSentencesGeneration}
		 * @param enclosingInstanceTypes the runtime types of the enclosing
		 * instances; never {@code null}
		 * @return the {@code DisplayNameGenerator} instance to use
		 */
		private static DisplayNameGenerator getGeneratorFor(Class<?> testClass, List<Class<?>> enclosingInstanceTypes) {
			return findIndicativeSentencesGeneration(testClass, enclosingInstanceTypes)//
					.map(IndicativeSentencesGeneration::generator)//
					.filter(notIndicativeSentences)//
					.map(DisplayNameGenerator::getDisplayNameGenerator)//
					.orElseGet(() -> getDisplayNameGenerator(IndicativeSentencesGeneration.DEFAULT_GENERATOR));
		}

		/**
		 * Find the first {@code DisplayNameGeneration} annotation that is either
		 * <em>directly present</em>, <em>meta-present</em>, or <em>indirectly present</em>
		 * on the supplied {@code testClass} or on an enclosing instance type.
		 *
		 * @param testClass the test class on which to find the annotation; never {@code null}
		 * @param enclosingInstanceTypes the runtime types of the enclosing
		 * instances; never {@code null}
		 * @return an {@code Optional} containing the annotation, potentially empty if not found
		 */
		@API(status = INTERNAL, since = "5.12")
		private static Optional<DisplayNameGeneration> findDisplayNameGeneration(Class<?> testClass,
				List<Class<?>> enclosingInstanceTypes) {
			return findAnnotation(testClass, DisplayNameGeneration.class, enclosingInstanceTypes);
		}

		/**
		 * Find the first {@code IndicativeSentencesGeneration} annotation that is either
		 * <em>directly present</em>, <em>meta-present</em>, or <em>indirectly present</em>
		 * on the supplied {@code testClass} or on an enclosing instance type.
		 *
		 * @param testClass the test class on which to find the annotation; never {@code null}
		 * @param enclosingInstanceTypes the runtime types of the enclosing
		 * instances; never {@code null}
		 * @return an {@code Optional} containing the annotation, potentially empty if not found
		 */
		private static Optional<IndicativeSentencesGeneration> findIndicativeSentencesGeneration(Class<?> testClass,
				List<Class<?>> enclosingInstanceTypes) {
			return findAnnotation(testClass, IndicativeSentencesGeneration.class, enclosingInstanceTypes);
		}

		private static @Nullable String getSentenceFragment(AnnotatedElement element) {
			return findAnnotation(element, SentenceFragment.class) //
					.map(SentenceFragment::value) //
					.map(sentenceFragment -> {
						Preconditions.notBlank(sentenceFragment,
							"@SentenceFragment on [%s] must be declared with a non-blank value.".formatted(element));
						return sentenceFragment.strip();
					}) //
					.orElse(null);
		}

	}

	/**
	 * Return the {@code DisplayNameGenerator} instance corresponding to the
	 * given {@code Class}.
	 *
	 * @param generatorClass the generator's {@code Class}; never {@code null},
	 * has to be a {@code DisplayNameGenerator} implementation
	 * @return a {@code DisplayNameGenerator} implementation instance
	 */
	static DisplayNameGenerator getDisplayNameGenerator(Class<?> generatorClass) {
		Preconditions.notNull(generatorClass, "Class must not be null");
		Preconditions.condition(DisplayNameGenerator.class.isAssignableFrom(generatorClass),
			"Class must be a DisplayNameGenerator implementation");
		if (generatorClass == Standard.class) {
			return Standard.INSTANCE;
		}
		if (generatorClass == Simple.class) {
			return Simple.INSTANCE;
		}
		if (generatorClass == ReplaceUnderscores.class) {
			return ReplaceUnderscores.INSTANCE;
		}
		if (generatorClass == IndicativeSentences.class) {
			return IndicativeSentences.INSTANCE;
		}
		return (DisplayNameGenerator) ReflectionSupport.newInstance(generatorClass);
	}

}
