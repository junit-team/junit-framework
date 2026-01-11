/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import com.example.vintage.VintageTest;

import org.junit.Ignore;

/**
 * Reproducer for https://github.com/junit-team/junit-framework/issues/4076
 */
@Ignore
public class DefaultPackageTest extends VintageTest {
	void packagePrivateMethod() {
	}
}
