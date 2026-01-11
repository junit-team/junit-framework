/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.open.xml;

import org.junit.platform.engine.TestDescriptor;
import org.opentest4j.reporting.events.api.Factory;
import org.opentest4j.reporting.schema.Namespace;

class JUnitFactory {

	static Namespace NAMESPACE = Namespace.of("https://schemas.junit.org/open-test-reporting");

	private JUnitFactory() {
	}

	static Factory<UniqueId> uniqueId(String uniqueId) {
		return context -> new UniqueId(context, uniqueId);
	}

	static Factory<LegacyReportingName> legacyReportingName(String legacyReportingName) {
		return context -> new LegacyReportingName(context, legacyReportingName);
	}

	static Factory<Type> type(TestDescriptor.Type type) {
		return context -> new Type(context, type);
	}
}
