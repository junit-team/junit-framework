/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.options;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import picocli.CommandLine;

class ClasspathEntriesConverter implements CommandLine.ITypeConverter<List<Path>> {

	@Override
	public List<Path> convert(String value) {
		return Stream.of(value.split(File.pathSeparator)).map(Paths::get).toList();
	}

}
