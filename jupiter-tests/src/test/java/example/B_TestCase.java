/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * @since 5.8
 */
public class B_TestCase {

	public static List<String> callSequence;

	@BeforeEach
	void trackInvocations(TestInfo testInfo) {
		if (callSequence != null) {
			callSequence.add(testInfo.getTestClass().get().getName());
		}
	}

	@Test
	void a() {
	}

}
