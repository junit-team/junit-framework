/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * @since 1.11
 */
class ClasspathFilters {

	static final String CLASS_FILE_SUFFIX = ".class";
	static final String SOURCE_FILE_SUFFIX = ".java";

	// System property defined since Java 12: https://bugs.java/bugdatabase/JDK-8210877
	private static final boolean SOURCE_MODE = System.getProperty("jdk.launcher.sourcefile") != null;

	private static final String PACKAGE_INFO_FILE_NAME = "package-info" + CLASS_FILE_SUFFIX;
	private static final String MODULE_INFO_FILE_NAME = "module-info" + CLASS_FILE_SUFFIX;

	static boolean isClassOrSourceFileName(String name) {
		return name.endsWith(CLASS_FILE_SUFFIX) || (SOURCE_MODE && name.endsWith(SOURCE_FILE_SUFFIX));
	}

	static Predicate<Path> classFiles() {
		return file -> isNotPackageInfo(file) && isNotModuleInfo(file)
				&& (isClassFile(file) || (SOURCE_MODE && isSourceFile(file)));
	}

	static Predicate<Path> resourceFiles() {
		return file -> !isClassFile(file);
	}

	private static boolean isNotPackageInfo(Path path) {
		return !path.endsWith(PACKAGE_INFO_FILE_NAME);
	}

	private static boolean isNotModuleInfo(Path path) {
		return !path.endsWith(MODULE_INFO_FILE_NAME);
	}

	private static boolean isClassFile(Path file) {
		return file.getFileName().toString().endsWith(CLASS_FILE_SUFFIX);
	}

	private static boolean isSourceFile(Path file) {
		return file.getFileName().toString().endsWith(SOURCE_FILE_SUFFIX);
	}

	private ClasspathFilters() {
	}

}
