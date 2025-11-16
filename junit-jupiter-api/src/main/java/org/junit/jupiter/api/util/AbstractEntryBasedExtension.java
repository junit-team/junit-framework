/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.util.SystemPropertyExtensionUtils.findAllContexts;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * An abstract base class for entry-based extensions, where entries (key-value
 * pairs) can be cleared, set, or restored.
 *
 * @param <K> The entry key type.
 * @param <V> The entry value type.
 * @param <C> The clear annotation type.
 * @param <S> The set annotation type.
 * @param <R> The restore annotation type.
 */
abstract class AbstractEntryBasedExtension<K, V, C extends Annotation, S extends Annotation, R extends Annotation>
		implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

	/**
	 * Key to indicate storage is for an incremental backup object.
	 */
	private static final String INCREMENTAL_KEY = "inc";

	/**
	 * Key to indicate storage is for a complete backup object.
	 */
	private static final String COMPLETE_KEY = "full";

	@Override
	public void beforeAll(ExtensionContext context) {
		applyForAllContexts(context);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		applyForAllContexts(context);
	}

	private void applyForAllContexts(ExtensionContext originalContext) {
		AnnotatedElement element = originalContext.getElement().orElse(null);
		boolean fullRestore = AnnotationSupport.findAnnotation(element, getRestoreAnnotationType()).isPresent();

		if (fullRestore) {
			Properties bulk = this.prepareToEnterRestorableContext();
			storeOriginalCompleteEntries(originalContext, bulk);
		}

		/*
		 * We cannot use PioneerAnnotationUtils#findAllEnclosingRepeatableAnnotations(ExtensionContext, Class) or the
		 * like as clearing and setting might interfere. Therefore, we have to apply the extension from the outermost
		 * to the innermost ExtensionContext.
		 */
		List<ExtensionContext> contexts = findAllContexts(originalContext);
		Collections.reverse(contexts);
		contexts.forEach(currentContext -> clearAndSetEntries(currentContext, originalContext, !fullRestore));
	}

	private void clearAndSetEntries(ExtensionContext currentContext, ExtensionContext originalContext,
			boolean doIncrementalBackup) {
		currentContext.getElement().ifPresent(element -> {
			Set<K> entriesToClear;
			Map<K, V> entriesToSet;

			try {
				entriesToClear = findEntriesToClear(element);
				entriesToSet = findEntriesToSet(element);
				preventClearAndSetSameEntries(entriesToClear, entriesToSet.keySet());
			}
			catch (IllegalStateException ex) {
				throw new ExtensionConfigurationException("Don't clear/set the same entry more than once.", ex);
			}

			if (entriesToClear.isEmpty() && entriesToSet.isEmpty())
				return;

			reportWarning(currentContext);

			// Only backup original values if we didn't already do bulk storage of the original state
			if (doIncrementalBackup) {
				storeOriginalIncrementalEntries(originalContext, entriesToClear, entriesToSet.keySet());
			}

			clearEntries(entriesToClear);
			setEntries(entriesToSet);
		});
	}

	private Set<K> findEntriesToClear(AnnotatedElement element) {
		return findAnnotations(element, getClearAnnotationType()).map(clearKeyMapper()).collect(
			SystemPropertyExtensionUtils.distinctToSet());
	}

	private Map<K, V> findEntriesToSet(AnnotatedElement element) {
		return findAnnotations(element, getSetAnnotationType()).collect(toMap(setKeyMapper(), setValueMapper()));
	}

	private <A extends Annotation> Stream<A> findAnnotations(AnnotatedElement element, Class<A> clazz) {
		return AnnotationSupport.findRepeatableAnnotations(element, clazz).stream();
	}

	@SuppressWarnings("unchecked")
	private Class<C> getClearAnnotationType() {
		return (Class<C>) getActualTypeArgumentAt(2);
	}

	@SuppressWarnings("unchecked")
	private Class<S> getSetAnnotationType() {
		return (Class<S>) getActualTypeArgumentAt(3);
	}

	@SuppressWarnings("unchecked")
	private Class<R> getRestoreAnnotationType() {
		return (Class<R>) getActualTypeArgumentAt(4);
	}

	private Type getActualTypeArgumentAt(int index) {
		ParameterizedType abstractEntryBasedExtensionType = (ParameterizedType) getClass().getGenericSuperclass();
		Type type = abstractEntryBasedExtensionType.getActualTypeArguments()[index];
		if (type instanceof ParameterizedType parameterizedType) {
			return parameterizedType.getRawType();
		}
		else {
			return type;
		}
	}

	private void preventClearAndSetSameEntries(Collection<K> entriesToClear, Collection<K> entriesToSet) {
		String duplicateEntries = entriesToClear.stream().filter(entriesToSet::contains).map(Object::toString).collect(
			joining(", "));
		if (!duplicateEntries.isEmpty())
			throw new IllegalStateException(
				"Cannot clear and set the following entries at the same time: " + duplicateEntries);
	}

	private void storeOriginalIncrementalEntries(ExtensionContext context, Collection<K> entriesToClear,
			Collection<K> entriesToSet) {
		getStore(context).put(getStoreKey(context, INCREMENTAL_KEY), new EntriesBackup(entriesToClear, entriesToSet));
	}

	private void storeOriginalCompleteEntries(ExtensionContext context, Properties originalEntries) {
		getStore(context).put(getStoreKey(context, COMPLETE_KEY), originalEntries);
	}

	/**
	 * Restore the complete original state of the entries as they were prior to this {@code ExtensionContext},
	 * if the complete state was initially stored in a before all/each event.
	 *
	 * @param context The {@code ExtensionContext} which may have a bulk backup stored.
	 * @return true if a complete backup exists and was used to restore, false if not.
	 */
	private boolean restoreOriginalCompleteEntries(ExtensionContext context) {
		Properties bulk = getStore(context).get(getStoreKey(context, COMPLETE_KEY), Properties.class);

		if (bulk == null) {
			// No complete backup - false will let the caller know to continue w/ an incremental restore
			return false;
		}
		else {
			this.prepareToExitRestorableContext(bulk);
			return true;
		}
	}

	private void clearEntries(Collection<K> entriesToClear) {
		entriesToClear.forEach(this::clearEntry);
	}

	private void setEntries(Map<K, V> entriesToSet) {
		entriesToSet.forEach(this::setEntry);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		restoreForAllContexts(context);
	}

	private void restoreForAllContexts(ExtensionContext originalContext) {
		// Try a complete restore first
		if (!restoreOriginalCompleteEntries(originalContext)) {
			// A complete backup is not available, so restore incrementally from innermost to outermost
			findAllContexts(originalContext).forEach(__ -> restoreOriginalIncrementalEntries(originalContext));
		}
	}

	private void restoreOriginalIncrementalEntries(ExtensionContext originalContext) {
		getStore(originalContext).getOrDefault(getStoreKey(originalContext, INCREMENTAL_KEY), EntriesBackup.class,
			new EntriesBackup()).restoreBackup();
	}

	private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass()));
	}

	private String getStoreKey(ExtensionContext context, String discriminator) {
		return context.getUniqueId() + "-" + this.getClass().getSimpleName() + "-" + discriminator;
	}

	private class EntriesBackup {

		private final Set<K> entriesToClear = new HashSet<>();
		private final Map<K, V> entriesToSet = new HashMap<>();

		EntriesBackup() {
			// empty backup
		}

		EntriesBackup(Collection<K> entriesToClear, Collection<K> entriesToSet) {
			Stream.concat(entriesToClear.stream(), entriesToSet.stream()).forEach(entry -> {
				V backup = AbstractEntryBasedExtension.this.getEntry(entry);
				if (backup == null)
					this.entriesToClear.add(entry);
				else
					this.entriesToSet.put(entry, backup);
			});
		}

		void restoreBackup() {
			entriesToClear.forEach(AbstractEntryBasedExtension.this::clearEntry);
			entriesToSet.forEach(AbstractEntryBasedExtension.this::setEntry);
		}

	}

	/**
	 * @return Mapper function to get the key from a clear annotation.
	 */
	protected abstract Function<C, K> clearKeyMapper();

	/**
	 * @return Mapper function to get the key from a set annotation.
	 */
	protected abstract Function<S, K> setKeyMapper();

	/**
	 * @return Mapper function to get the value from a set annotation.
	 */
	protected abstract Function<S, V> setValueMapper();

	/**
	 * Removes the entry indicated by the specified key.
	 */
	protected abstract void clearEntry(K key);

	/**
	 * Gets the entry indicated by the specified key.
	 */
	protected abstract V getEntry(K key);

	/**
	 * Sets the entry indicated by the specified key.
	 */
	protected abstract void setEntry(K key, V value);

	/**
	 * Reports a warning about potentially unsafe practices.
	 */
	protected void reportWarning(ExtensionContext context) {
		// nothing reported by default
	}

	/**
	 * Prepare the entry-based environment for entering a context that must be restorable.
	 *
	 * <p>Implementations may choose one of two strategies:</p>
	 *
	 * <ul>
	 * <li><em>Post swap</em>, where the original entry-based environment is left in place and a clone is returned.
	 * In this case {@link #prepareToExitRestorableContext} will restore the clone.
	 * <li><em>Preemptive swap</em>, where the current entry-based environment is replaced with a clone and the
	 * original is returned.
	 * In this case the {@link #prepareToExitRestorableContext} will restore the original environment.</li>
	 * </ul>
	 *
	 * <p>The returned {@code Properties} must not be null and its key-value pairs must follow the rules for
	 * entries of its type. E.g., environment variables contain only Strings while System {@code Properties}
	 * may contain Objects.</p>
	 *
	 * @return A non-null {@code Properties} that contains all entries of the entry environment.
	 */
	protected abstract Properties prepareToEnterRestorableContext();

	/**
	 * Prepare to exit a restorable context for the entry based environment.
	 *
	 * <p>The entry environment will be restored to the state passed in as {@code Properties}.
	 * The {@code Properties} entries must follow the rules for entries of this environment,
	 * e.g., environment variables contain only Strings while System {@code Properties} may contain Objects.</p>
	 *
	 * @param entries A non-null {@code Properties} that contains all entries of the entry environment.
	 */
	protected abstract void prepareToExitRestorableContext(Properties entries);
}
