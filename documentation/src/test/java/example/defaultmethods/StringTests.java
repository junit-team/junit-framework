/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.defaultmethods;

// tag::user_guide[]
class StringTests implements ComparableContract<String>, EqualsContract<String> {

	@Override
	public String createValue() {
		return "banana";
	}

	@Override
	public String createSmallerValue() {
		return "apple"; // 'a' < 'b' in "banana"
	}

	@Override
	public String createNotEqualValue() {
		return "cherry";
	}

}
// end::user_guide[]
