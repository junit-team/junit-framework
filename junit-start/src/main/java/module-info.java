/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */


/// Defines the API of the JUnit Start module for writing and running tests.
///
/// Usage example:
/// ```java
/// import module org.junit.start;
///
///  void main() {
///    JUnit.run();
///  }
///
///  @Test
///  void addition() {
///    Assertions.assertEquals(2, 1 + 1, "Addition error detected!");
///   }
/// }
/// ```
module org.junit.start {
	requires static transitive org.apiguardian.api;
	requires static transitive org.jspecify;

	requires transitive org.junit.jupiter;
	requires org.junit.platform.launcher;
	requires org.junit.platform.console;

	exports org.junit.start;
}
