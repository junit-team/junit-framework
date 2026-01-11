/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import java.util.Properties;

import org.assertj.core.api.AbstractAssert;

/**
 * Allows comparison of {@link Properties} with optional awareness of their structure,
 * rather than just treating them as Maps. Object values, which are marginally supported
 * by {@code Properties}, are supported in assertions as much as possible.
 */
final class PropertiesAssertions extends AbstractAssert<PropertiesAssertions, Properties> {

	/**
	 * Make an assertion on a {@link Properties} instance.
	 *
	 * @param actual The {@link Properties} instance the assertion is made with respect to
	 * @return Assertion instance
	 */
	static PropertiesAssertions assertThat(Properties actual) {
		return new PropertiesAssertions(actual);
	}

	PropertiesAssertions(Properties actual) {
		super(actual, PropertiesAssertions.class);
	}

	/**
	 * Assert Properties has the same effective values as the passed instance, but not
	 * the same nested default structure.
	 *
	 * <p>Properties are considered <em>effectively equal</em> if they have the same property
	 * names returned by {@code Properties.propertyNames()} and the same values returned by
	 * {@code getProperty(name)}. Properties may come from the properties instance itself,
	 * or from a nested default instance, indiscriminately.
	 *
	 * <p>Properties partially supports object values, but return null for {@code getProperty(name)}
	 * when the value is a non-string. This assertion follows the same rules:  Any non-String
	 * value is considered null for comparison purposes.
	 *
	 * @param expected The actual is expected to be effectively the same as this Properties
	 * @return Assertion instance
	 */
	PropertiesAssertions isEffectivelyEqualsTo(Properties expected) {
		// Compare values as maps
		objects.assertEqual(info, actual, expected);

		// Compare values present in actual
		actual.propertyNames().asIterator().forEachRemaining(k -> {

			String kStr = k.toString();

			String actValue = actual.getProperty(kStr);
			String expValue = expected.getProperty(kStr);

			if (actValue == null) {
				if (expValue != null) {
					// An object value is the only way to get a null from getProperty()
					throw failure("For the property '<%s>', "
							+ "the actual value was an object but the expected the string '<%s>'.",
						k, expValue);
				}
			}
			else if (!actValue.equals(expValue)) {
				throw failure("For the property '<%s>', the actual value was <%s> but <%s> was expected", k, actValue,
					expValue);
			}
		});

		// Compare values present in expected - Anything not matching must not have been present in actual
		expected.propertyNames().asIterator().forEachRemaining(k -> {

			String kStr = k.toString();

			String actValue = actual.getProperty(kStr);
			String expValue = expected.getProperty(kStr);

			if (expValue == null) {
				if (actValue != null) {

					// An object value is the only way to get a null from getProperty()
					throw failure("For the property '<%s>', "
							+ "the actual value was the string '<%s>', but an object was expected.",
						k, actValue);
				}
			}
			else if (!expValue.equals(actValue)) {
				throw failure("The property <%s> was expected to be <%s>, but was missing", k, expValue);
			}
		});

		return this;
	}

}
