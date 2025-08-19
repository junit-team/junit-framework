/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

/**
 * A {@link DiscoverySelector} that selects a module name so that
 * {@link org.junit.platform.engine.TestEngine TestEngines} can discover
 * tests or containers based on modules.
 *
 * @since 1.1
 * @see DiscoverySelectors#selectModule(String)
 * @see DiscoverySelectors#selectModules(java.util.Set)
 */
@API(status = STABLE, since = "1.1")
public final class ModuleSelector implements DiscoverySelector {

	@Nullable
	private final Module module;
	private final String moduleName;

	ModuleSelector(Module module) {
		this.module = module;
		this.moduleName = module.getName();
	}

	ModuleSelector(String moduleName) {
		this.module = null;
		this.moduleName = moduleName;
	}

	/**
	 * Get the selected module wrapped in an Optional.
	 */
	public Optional<Module> getModule() {
		return Optional.ofNullable(module);
	}

	/**
	 * Get the selected module name.
	 */
	public String getModuleName() {
		return this.moduleName;
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ModuleSelector that = (ModuleSelector) o;
		return Objects.equals(this.moduleName, that.moduleName);
	}

	/**
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return this.moduleName.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("moduleName", this.moduleName).toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, this.moduleName));
	}

	/**
	 * The {@link DiscoverySelectorIdentifierParser} for {@link ModuleSelector
	 * ModuleSelectors}.
	 */
	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "module";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<ModuleSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(DiscoverySelectors.selectModule(identifier.getValue()));
		}

	}

}
