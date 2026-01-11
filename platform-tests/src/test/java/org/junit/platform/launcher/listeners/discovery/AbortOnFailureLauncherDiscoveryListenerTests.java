/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;
import static org.junit.platform.launcher.listeners.discovery.LauncherDiscoveryListeners.abortOnFailure;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.fakes.TestEngineStub;

class AbortOnFailureLauncherDiscoveryListenerTests {

	@Test
	void abortsDiscoveryOnEngineDiscoveryFailure() {
		var rootCause = new RuntimeException();
		var engine = new TestEngineStub("some-engine") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				throw rootCause;
			}
		};
		var request = request() //
				.listeners(abortOnFailure()) //
				.selectors(selectUniqueId(UniqueId.forEngine(engine.getId()))) //
				.build();
		var launcher = createLauncher(engine);

		var exception = assertThrows(JUnitException.class, () -> launcher.discover(request));
		assertThat(exception) //
				.hasMessage("TestEngine with ID 'some-engine' failed to discover tests") //
				.cause().isSameAs(rootCause);
	}

}
