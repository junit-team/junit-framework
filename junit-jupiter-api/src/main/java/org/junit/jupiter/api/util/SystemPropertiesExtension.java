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
		findFirstRestoreAnnotationContext(allContexts) //
				.ifPresentOrElse(
					// Do a complete backup of the properties
					restoreAnnotationContext -> {
						var properties = System.getProperties();
						storeCompleteBackup(context, properties);
						var clonedProperties = cloneWithoutDefaults(restoreAnnotationContext, properties);
						modification.applyTo(clonedProperties);
						System.setProperties(clonedProperties);
					},
					// Backup only the modified properties
					() -> {
						var properties = System.getProperties();
						storePartialBackup(context, modification.createInverseApplyTo(properties));
						modification.applyTo(properties);
					});
	}

	private void storeCompleteBackup(ExtensionContext context, Properties backup) {
		getStore(context).put(createStoreKey(context, BackupType.COMPLETE), backup);
	}

	private void storePartialBackup(ExtensionContext context, SystemPropertiesModification backup) {
		getStore(context).put(createStoreKey(context, BackupType.PARTIAL), backup);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	private void restoreForAllContexts(ExtensionContext context) {
		// Try a complete restore first
		findCompleteBackup(context) //
				.ifPresentOrElse(System::setProperties, //
					// A complete backup is not available, so use partial backup
					() -> findPartialBackup(context) //
							.ifPresent(backup -> backup.applyTo(System.getProperties())));
	}

	private Optional<Properties> findCompleteBackup(ExtensionContext context) {
		var key = createStoreKey(context, BackupType.COMPLETE);
		var backup = getStore(context).get(key, Properties.class);
		return Optional.ofNullable(backup);
	}

	private Optional<SystemPropertiesModification> findPartialBackup(ExtensionContext context) {
		var key = createStoreKey(context, BackupType.PARTIAL);
		var backup = getStore(context).get(key, SystemPropertiesModification.class);
		return Optional.ofNullable(backup);
	}

	private StoreKey createStoreKey(ExtensionContext context, BackupType type) {
		return new StoreKey(context.getUniqueId(), type);
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(ExtensionContext.Namespace.create(getClass()));
	}

	private record StoreKey(String id, BackupType type) {
	}

	private enum BackupType {
		/**
		 * Store entry is for a partial backup object.
		 */
		PARTIAL,
		/**
		 * Store entry is for a complete backup object.
		 */
		COMPLETE
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
