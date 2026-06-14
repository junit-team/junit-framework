/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static org.junit.jupiter.api.util.JupiterPropertyUtils.cloneWithoutDefaults;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code Extension} which provides support for the following annotations.
 *
 * <ul>
 * <li>{@link SetSystemProperty @SetSystemProperty}</li>
 * <li>{@link ClearSystemProperty @ClearSystemProperty}</li>
 * <li>{@link RestoreSystemProperties @RestoreSystemProperties}</li>
 * </ul>
 *
 * @since 6.1
 */
final class SystemPropertiesExtension
		implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
		applyForAllContexts(context);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		applyForAllContexts(context);
	}

	private void applyForAllContexts(ExtensionContext context) {
		var allContexts = findAllExtensionContexts(context);
		var modification = SystemPropertiesModification.create(allContexts);

		// Please do not refactor out the common parts.
		var backup = findFirstRestoreAnnotationContext(allContexts) //
				.<Backup> map(restoreAnnotationContext -> {
					// Do a complete backup of the properties
					var properties = System.getProperties();
					var clonedProperties = cloneWithoutDefaults(restoreAnnotationContext, properties);
					modification.applyTo(clonedProperties);
					System.setProperties(clonedProperties);
					return new Backup.Complete(properties);
				}).orElseGet(() -> {
					// Backup only the modified properties
					var properties = System.getProperties();
					var partialBackup = new Backup.Partial(modification.createInverseApplyTo(properties));
					modification.applyTo(properties);
					return partialBackup;
				});

		storeBackup(context, backup);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		restoreBackup(context);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		restoreBackup(context);
	}

	private void restoreBackup(ExtensionContext context) {
		findBackup(context).ifPresent(Backup::restore);
	}

	private void storeBackup(ExtensionContext context, Backup backup) {
		getStore(context).put(context.getUniqueId(), backup);
	}

	private Optional<Backup> findBackup(ExtensionContext context) {
		var backup = getStore(context).get(context.getUniqueId(), Backup.class);
		return Optional.ofNullable(backup);
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(ExtensionContext.Namespace.create(getClass()));
	}

	private sealed interface Backup {

		void restore();

		/// Partial backup
		record Partial(SystemPropertiesModification inverseModification) implements Backup {
			@Override
			public void restore() {
				inverseModification.applyTo(System.getProperties());
			}
		}

		/// Complete backup
		record Complete(Properties originalProperties) implements Backup {
			@Override
			public void restore() {
				System.setProperties(originalProperties);
			}
		}
	}

	private static List<ExtensionContext> findAllExtensionContexts(ExtensionContext context) {
		var contexts = new ArrayList<ExtensionContext>();
		do {
			contexts.add(context);
			context = context.getParent().orElse(null);
		} while (context != null);
		return contexts;
	}

	private static Optional<ExtensionContext> findFirstRestoreAnnotationContext(List<ExtensionContext> contexts) {
		return contexts.stream() //
				.filter(context -> isAnnotated(context.getElement(), RestoreSystemProperties.class)) //
				.findFirst();
	}
}
