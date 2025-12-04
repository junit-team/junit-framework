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

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * @since 1.11
 */
class ClasspathFileVisitor extends SimpleFileVisitor<Path> {

	private static final Logger logger = LoggerFactory.getLogger(ClasspathFileVisitor.class);

	private final Path basePath;
	private final BiConsumer<Path, Path> consumer;
	private final Predicate<Path> filter;

	ClasspathFileVisitor(Path basePath, Predicate<Path> filter, BiConsumer<Path, Path> consumer) {
		this.basePath = basePath;
		this.filter = filter;
		this.consumer = consumer;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
		if (filter.test(file)) {
			consumer.accept(basePath, file);
		}
		return CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException ex) {
		logger.warn(ex, () -> "I/O error visiting file: " + file);
		return CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException ex) {
		if (ex != null) {
			logger.warn(ex, () -> "I/O error visiting directory: " + dir);
		}
		return CONTINUE;
	}

}
