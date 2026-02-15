/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.error;

import java.util.Optional;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;
import org.junit.platform.engine.discovery.DiscoverySelectorIdentifierParser;

public class ErrorSelectorIdentifierParser implements DiscoverySelectorIdentifierParser {

	@Override
	public String getPrefix() {
		return "error";
	}

	@Override
	public Optional<? extends DiscoverySelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
		return Optional.of(new ErrorSelector(identifier.getValue()));
	}
}
