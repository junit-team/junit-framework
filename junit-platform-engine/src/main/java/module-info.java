/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.DiscoverySelectorIdentifierParser;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.NestedMethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;

/**
 * Public API for test engines.
 *
 * <p>Provides the {@link org.junit.platform.engine.TestEngine} interface,
 * test discovery, and execution reporting support.
 *
 * @since 1.0
 */
module org.junit.platform.engine {

	requires static transitive org.apiguardian.api;
	requires static transitive org.jspecify;

	requires transitive org.junit.platform.commons;
	requires transitive org.opentest4j;

	exports org.junit.platform.engine;
	exports org.junit.platform.engine.discovery;
	exports org.junit.platform.engine.reporting;
	// exports org.junit.platform.engine.support; empty package
	exports org.junit.platform.engine.support.config;
	exports org.junit.platform.engine.support.descriptor;
	exports org.junit.platform.engine.support.discovery;
	exports org.junit.platform.engine.support.hierarchical;
	exports org.junit.platform.engine.support.store;

	uses DiscoverySelectorIdentifierParser;

	provides DiscoverySelectorIdentifierParser with
			ClassSelector.IdentifierParser,
			ClasspathResourceSelector.IdentifierParser,
			ClasspathRootSelector.IdentifierParser,
			DirectorySelector.IdentifierParser,
			FileSelector.IdentifierParser,
			IterationSelector.IdentifierParser,
			MethodSelector.IdentifierParser,
			ModuleSelector.IdentifierParser,
			NestedClassSelector.IdentifierParser,
			NestedMethodSelector.IdentifierParser,
			PackageSelector.IdentifierParser,
			UniqueIdSelector.IdentifierParser,
			UriSelector.IdentifierParser;

}
