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

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.stream.IntStream;

import org.junit.platform.commons.JUnitException;

/**
 * @since 1.11
 */
class SearchPathUtils {

	static final char PACKAGE_SEPARATOR_CHAR = '.';
	static final String PACKAGE_SEPARATOR_STRING = String.valueOf(PACKAGE_SEPARATOR_CHAR);
	private static final char FILE_NAME_EXTENSION_SEPARATOR_CHAR = '.';

	private static final String CLASS_FILE_SUFFIX = ".class";
	private static final String SOURCE_FILE_SUFFIX = ".java";

	private static final String PACKAGE_INFO_FILE_NAME = "package-info";
	private static final String MODULE_INFO_FILE_NAME = "module-info";

	// System property defined since Java 12: https://bugs.java/bugdatabase/JDK-8210877
	private static final boolean SOURCE_MODE = System.getProperty("jdk.launcher.sourcefile") != null;

	static boolean isResourceFile(Path file) {
		return !isClassFile(file);
	}

	static boolean isClassOrSourceFile(Path file) {
		var fileName = file.getFileName().toString();
		return isClassOrSourceFile(fileName) && !isModuleInfoOrPackageInfo(fileName);
	}

	private static boolean isModuleInfoOrPackageInfo(String fileName) {
		var fileNameWithoutExtension = removeExtension(fileName);
		return PACKAGE_INFO_FILE_NAME.equals(fileNameWithoutExtension) //
				|| MODULE_INFO_FILE_NAME.equals(fileNameWithoutExtension);
	}

	static String determineFullyQualifiedClassName(Path path) {
		var simpleClassName = determineSimpleClassName(path);
		var parent = path.getParent();
		return parent == null ? simpleClassName : joinPathNamesWithPackageSeparator(parent.resolve(simpleClassName));
	}

	private static String joinPathNamesWithPackageSeparator(Path path) {
		return IntStream.range(0, path.getNameCount()) //
				.mapToObj(i -> path.getName(i).toString()) //
				.collect(joining(PACKAGE_SEPARATOR_STRING));
	}

	static String determineSimpleClassName(Path file) {
		return removeExtension(file.getFileName().toString());
	}

	private static String removeExtension(String fileName) {
		int lastDot = fileName.lastIndexOf(FILE_NAME_EXTENSION_SEPARATOR_CHAR);
		if (lastDot < 0) {
			throw new JUnitException("Expected file name with file extension, but got: " + fileName);
		}
		return fileName.substring(0, lastDot);
	}

	private static boolean isClassOrSourceFile(String name) {
		return name.endsWith(CLASS_FILE_SUFFIX) || (SOURCE_MODE && name.endsWith(SOURCE_FILE_SUFFIX));
	}

	private static boolean isClassFile(Path file) {
		return file.getFileName().toString().endsWith(CLASS_FILE_SUFFIX);
	}

	private SearchPathUtils() {
	}
}
