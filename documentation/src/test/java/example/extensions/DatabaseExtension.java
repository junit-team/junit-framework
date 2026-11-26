/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.extensions;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.store.Namespace;

// tag::user_guide[]
public class DatabaseExtension implements BeforeAllCallback, ParameterResolver {

	private static final Namespace NAMESPACE = Namespace.create(DatabaseExtension.class);

	@Override
	public void beforeAll(ExtensionContext context) {
		Database database = getOrCreateDatabase(context); // <1>
		database.insert("testClass", context.getRequiredTestClass().getSimpleName());
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return Database.class.equals(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return getOrCreateDatabase(extensionContext); // <2>
	}

	private static Database getOrCreateDatabase(ExtensionContext context) {
		return context.getStore(NAMESPACE).computeIfAbsent(Database.class, __ -> {
			Database database = new Database(); // <3>
			database.insert("greeting", "Hello");
			return database;
		}, Database.class);
	}
}
// end::user_guide[]
