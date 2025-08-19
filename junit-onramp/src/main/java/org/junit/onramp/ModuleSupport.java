/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.onramp;

import java.util.List;
import java.util.Optional;

import org.junit.platform.commons.JUnitException;

class ModuleSupport {
	private ModuleSupport() {
	}

	static final boolean SOURCE_MODE = System.getProperty("jdk.launcher.sourcefile") != null;

	static List<Class<?>> listClassesInModule(Module module) {
		var resolved = module.getLayer().configuration().findModule(module.getName()).orElseThrow();
		try (var reader = resolved.reference().open()) {
			return reader.list() //
					.map(name -> loadClassByResourceName(module, name)) //
					.flatMap(Optional::stream) //
					.distinct() //
					.toList();
		}
		catch (Exception exception) {
			throw new JUnitException("Listing classes in module %s failed".formatted(module), exception);
		}
	}

	static Optional<Class<?>> loadClassByResourceName(Module module, String name) {
		var className = name;
		if (SOURCE_MODE) {
			if (name.endsWith(".java")) {
				className = name.substring(0, name.length() - 5);
			}
		}
		if (name.endsWith(".class")) {
			className = name.substring(0, name.length() - 6);
		}
		try {
			return Optional.of(Class.forName(module, className.replace('/', '.')));
		}
		catch (Throwable ignored) {
			return Optional.empty();
		}
	}
}
