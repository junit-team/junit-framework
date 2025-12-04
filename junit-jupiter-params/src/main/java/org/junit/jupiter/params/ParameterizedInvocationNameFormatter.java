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

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.ARGUMENTS_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.ARGUMENTS_WITH_NAMES_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.ARGUMENT_SET_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.DISPLAY_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.INDEX_PLACEHOLDER;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterNameAndArgument;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;

/**
 * @since 5.0
 */
class ParameterizedInvocationNameFormatter {

	/**
	 * Global cache for {arguments} pattern strings, keyed by the number of arguments.
	 * @since 6.0
	 */
	private static final Map<Integer, String> argumentsPatternCache = new ConcurrentHashMap<>(8);

	static final String DEFAULT_DISPLAY_NAME = "{default_display_name}";
	static final String DEFAULT_DISPLAY_NAME_PATTERN = "[" + INDEX_PLACEHOLDER + "] "
			+ ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER;
	static final String DISPLAY_NAME_PATTERN_KEY = "junit.jupiter.params.displayname.default";
	static final String ARGUMENT_MAX_LENGTH_KEY = "junit.jupiter.params.displayname.argument.maxlength";

	static ParameterizedInvocationNameFormatter create(ExtensionContext extensionContext,
			ParameterizedDeclarationContext<?> declarationContext) {

		String name = declarationContext.getDisplayNamePattern();
		String pattern = DEFAULT_DISPLAY_NAME.equals(name)
				? extensionContext.getConfigurationParameter(DISPLAY_NAME_PATTERN_KEY) //
						.orElse(DEFAULT_DISPLAY_NAME_PATTERN)
				: name;
		pattern = Preconditions.notBlank(pattern.strip(),
			() -> "Configuration error: @%s on %s must be declared with a non-empty name.".formatted(
				declarationContext.getAnnotationName(),
				declarationContext.getResolverFacade().getIndexedParameterDeclarations().getSourceElementDescription()));

		int argumentMaxLength = extensionContext.getConfigurationParameter(ARGUMENT_MAX_LENGTH_KEY, Integer::parseInt) //
				.orElse(512);

		return new ParameterizedInvocationNameFormatter(pattern, extensionContext.getDisplayName(), declarationContext,
			argumentMaxLength);
	}

	private final PartialFormatter[] partialFormatters;

	ParameterizedInvocationNameFormatter(String pattern, String displayName,
			ParameterizedDeclarationContext<?> declarationContext, int argumentMaxLength) {
		try {
			this.partialFormatters = parse(pattern, displayName, declarationContext, argumentMaxLength);
		}
		catch (Exception ex) {
			String message = "The display name pattern defined for the parameterized test is invalid. "
					+ "See nested exception for further details.";
			throw new JUnitException(message, ex);
		}
	}

	String format(int invocationIndex, EvaluatedArgumentSet arguments, boolean quoteTextArguments) {
		try {
			return formatSafely(invocationIndex, arguments, quoteTextArguments);
		}
		catch (Exception ex) {
			String message = "Failed to format display name for parameterized test. "
					+ "See nested exception for further details.";
			throw new JUnitException(message, ex);
		}
	}

	@SuppressWarnings("JdkObsolete")
	private String formatSafely(int invocationIndex, EvaluatedArgumentSet arguments, boolean quoteTextArguments) {
		ArgumentsContext context = new ArgumentsContext(invocationIndex, arguments.getConsumedArguments(),
			arguments.getName(), quoteTextArguments);
		StringBuffer result = new StringBuffer(); // used instead of StringBuilder so MessageFormat can append directly
		for (PartialFormatter partialFormatter : this.partialFormatters) {
			partialFormatter.append(context, result);
		}
		return result.toString();
	}

	private PartialFormatter[] parse(String pattern, String displayName,
			ParameterizedDeclarationContext<?> declarationContext, int argumentMaxLength) {

		List<PartialFormatter> result = new ArrayList<>();
		PartialFormatters formatters = createPartialFormatters(displayName, declarationContext, argumentMaxLength);
		String unparsedSegment = pattern;

		while (isNotBlank(unparsedSegment)) {
			PlaceholderPosition position = findFirstPlaceholder(formatters, unparsedSegment);
			if (position == null) {
				result.add(determineNonPlaceholderFormatter(unparsedSegment, argumentMaxLength));
				break;
			}
			if (position.index > 0) {
				String before = unparsedSegment.substring(0, position.index);
				result.add(determineNonPlaceholderFormatter(before, argumentMaxLength));
			}
			result.add(formatters.get(position.placeholder));
			unparsedSegment = unparsedSegment.substring(position.index + position.placeholder.length());
		}

		return result.toArray(new PartialFormatter[0]);
	}

	private static @Nullable PlaceholderPosition findFirstPlaceholder(PartialFormatters formatters, String segment) {
		if (segment.length() < formatters.minimumPlaceholderLength) {
			return null;
		}
		PlaceholderPosition minimum = null;
		for (String placeholder : formatters.placeholders()) {
			int index = segment.indexOf(placeholder);
			if (index >= 0) {
				if (index < formatters.minimumPlaceholderLength) {
					return new PlaceholderPosition(index, placeholder);
				}
				else if (minimum == null || index < minimum.index) {
					minimum = new PlaceholderPosition(index, placeholder);
				}
			}
		}
		return minimum;
	}

	private static PartialFormatter determineNonPlaceholderFormatter(String segment, int argumentMaxLength) {
		return segment.contains("{") //
				? new MessageFormatPartialFormatter(segment, argumentMaxLength) //
				: (context, result) -> result.append(segment);
	}

	private PartialFormatters createPartialFormatters(String displayName,
			ParameterizedDeclarationContext<?> declarationContext, int argumentMaxLength) {

		PartialFormatter argumentsWithNamesFormatter = new CachingByArgumentsLengthPartialFormatter(
			length -> new MessageFormatPartialFormatter(argumentsPattern(length), argumentMaxLength, true,
				declarationContext.getResolverFacade()));

		PartialFormatter argumentSetNameFormatter = new ArgumentSetNameFormatter(
			declarationContext.getAnnotationName());

		PartialFormatters formatters = new PartialFormatters();
		formatters.put(INDEX_PLACEHOLDER, PartialFormatter.INDEX);
		formatters.put(DISPLAY_NAME_PLACEHOLDER, (context, result) -> result.append(displayName));
		formatters.put(ARGUMENT_SET_NAME_PLACEHOLDER, argumentSetNameFormatter);
		formatters.put(ARGUMENTS_WITH_NAMES_PLACEHOLDER, argumentsWithNamesFormatter);
		formatters.put(ARGUMENTS_PLACEHOLDER, new CachingByArgumentsLengthPartialFormatter(
			length -> new MessageFormatPartialFormatter(argumentsPattern(length), argumentMaxLength)));
		formatters.put(ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER, (context, result) -> {
			PartialFormatter formatterToUse = context.argumentSetName.isPresent() //
					? argumentSetNameFormatter //
					: argumentsWithNamesFormatter;
			formatterToUse.append(context, result);
		});
		return formatters;
	}

	private static String argumentsPattern(int length) {
		return argumentsPatternCache.computeIfAbsent(length, //
			key -> {
				StringJoiner sj = new StringJoiner(", ");
				for (int i = 0; i < length; i++) {
					sj.add("{" + i + "}");
				}
				return sj.toString();
			});
	}

	private record PlaceholderPosition(int index, String placeholder) {
	}

	@SuppressWarnings("ArrayRecordComponent")
	private record ArgumentsContext(int invocationIndex, @Nullable Object[] consumedArguments,
			Optional<String> argumentSetName, boolean quoteTextArguments) {
	}

	@FunctionalInterface
	private interface PartialFormatter {

		PartialFormatter INDEX = (context, result) -> result.append(context.invocationIndex);

		void append(ArgumentsContext context, StringBuffer result);

	}

	private record ArgumentSetNameFormatter(String annotationName) implements PartialFormatter {

		@Override
		public void append(ArgumentsContext context, StringBuffer result) {
			if (context.argumentSetName.isPresent()) {
				result.append(context.argumentSetName.get());
				return;
			}
			throw new ExtensionConfigurationException(
				"When the display name pattern for a @%s contains %s, the arguments must be supplied as an ArgumentSet.".formatted(
					this.annotationName, ARGUMENT_SET_NAME_PLACEHOLDER));
		}
	}

	private static class MessageFormatPartialFormatter implements PartialFormatter {

		@SuppressWarnings("UnnecessaryUnicodeEscape")
		private static final char ELLIPSIS = '\u2026';

		private final MessageFormat messageFormat;
		private final int argumentMaxLength;
		private final boolean generateNameValuePairs;
		private final @Nullable ResolverFacade resolverFacade;

		MessageFormatPartialFormatter(String pattern, int argumentMaxLength) {
			this(pattern, argumentMaxLength, false, null);
		}

		MessageFormatPartialFormatter(String pattern, int argumentMaxLength, boolean generateNameValuePairs,
				@Nullable ResolverFacade resolverFacade) {
			this.messageFormat = new MessageFormat(pattern);
			this.argumentMaxLength = argumentMaxLength;
			this.generateNameValuePairs = generateNameValuePairs;
			this.resolverFacade = resolverFacade;
		}

		// synchronized because MessageFormat is not thread-safe
		@Override
		public synchronized void append(ArgumentsContext context, StringBuffer result) {
			this.messageFormat.format(makeReadable(context.consumedArguments, context.quoteTextArguments), result,
				new FieldPosition(0));
		}

		private @Nullable Object[] makeReadable(@Nullable Object[] arguments, boolean quoteTextArguments) {
			@Nullable
			Format[] formats = messageFormat.getFormatsByArgumentIndex();
			@Nullable
			Object[] result = Arrays.copyOf(arguments, Math.min(arguments.length, formats.length), Object[].class);
			for (int i = 0; i < result.length; i++) {
				if (formats[i] == null) {
					Object argument = arguments[i];
					String prefix = "";

					if (argument instanceof ParameterNameAndArgument parameterNameAndArgument) {
						// This supports the useHeadersInDisplayName attributes in @CsvSource and @CsvFileSource.
						prefix = parameterNameAndArgument.getName() + " = ";
						argument = parameterNameAndArgument.getPayload();
					}
					else if (this.generateNameValuePairs && this.resolverFacade != null) {
						Optional<String> parameterName = this.resolverFacade.getParameterName(i);
						if (parameterName.isPresent()) {
							// This supports the {argumentsWithNames} pattern.
							prefix = parameterName.get() + " = ";
						}
					}

					if (argument instanceof Character ch) {
						result[i] = prefix + (quoteTextArguments ? QuoteUtils.quote(ch) : ch);
					}
					else {
						String argumentText = (argument == null ? "null"
								: truncateIfExceedsMaxLength(StringUtils.nullSafeToString(argument)));
						result[i] = prefix + (quoteTextArguments && argument instanceof CharSequence//
								? QuoteUtils.quote(argumentText)
								: argumentText);
					}
				}
			}
			return result;
		}

		private String truncateIfExceedsMaxLength(String argument) {
			if (argument.length() > this.argumentMaxLength) {
				return argument.substring(0, this.argumentMaxLength - 1) + ELLIPSIS;
			}
			return argument;
		}
	}

	/**
	 * Caches formatters by the length of the consumed <em>arguments</em> which
	 * may differ from the number of declared parameters.
	 *
	 * <p>For example, when using multiple providers or a provider that returns
	 * argument arrays of different length, such as:
	 *
	 * <pre>
	 * &#064;ParameterizedTest
	 * &#064;CsvSource({"a", "a,b", "a,b,c"})
	 * void test(ArgumentsAccessor accessor) {}
	 * </pre>
	 */
	private static class CachingByArgumentsLengthPartialFormatter implements PartialFormatter {

		private final ConcurrentMap<Integer, PartialFormatter> cache = new ConcurrentHashMap<>(1);
		private final Function<Integer, PartialFormatter> factory;

		CachingByArgumentsLengthPartialFormatter(Function<Integer, PartialFormatter> factory) {
			this.factory = factory;
		}

		@Override
		public void append(ArgumentsContext context, StringBuffer result) {
			cache.computeIfAbsent(context.consumedArguments.length, factory).append(context, result);
		}
	}

	private static class PartialFormatters {

		private final Map<String, PartialFormatter> formattersByPlaceholder = new LinkedHashMap<>();
		private int minimumPlaceholderLength = Integer.MAX_VALUE;

		void put(String placeholder, PartialFormatter formatter) {
			formattersByPlaceholder.put(placeholder, formatter);
			int newPlaceholderLength = placeholder.length();
			if (newPlaceholderLength < minimumPlaceholderLength) {
				minimumPlaceholderLength = newPlaceholderLength;
			}
		}

		PartialFormatter get(String placeholder) {
			return requireNonNull(formattersByPlaceholder.get(placeholder));
		}

		Set<String> placeholders() {
			return formattersByPlaceholder.keySet();
		}
	}

}
