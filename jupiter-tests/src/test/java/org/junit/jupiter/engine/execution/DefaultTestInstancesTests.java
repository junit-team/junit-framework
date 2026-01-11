/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultTestInstancesTests {

	@Test
	void topLevelClass() {
		DefaultTestInstances instances = DefaultTestInstances.of(this);

		assertThat(instances.getInnermostInstance()).isSameAs(this);
		assertThat(instances.getAllInstances()).containsExactly(this);
		assertThat(instances.getEnclosingInstances()).isEmpty();
		assertThat(instances.findInstance(Object.class)).contains(this);
		assertThat(instances.findInstance(String.class)).isEmpty();
	}

	@Test
	void nestedLevelClass() {
		DefaultTestInstancesTests outermost = this;
		Nested innermost = new Nested();
		DefaultTestInstances instances = DefaultTestInstances.of(DefaultTestInstances.of(outermost), innermost);

		assertThat(instances.getInnermostInstance()).isSameAs(innermost);
		assertThat(instances.getAllInstances()).containsExactly(outermost, innermost);
		assertThat(instances.getEnclosingInstances()).containsExactly(outermost);
		assertThat(instances.findInstance(Object.class)).contains(innermost);
		assertThat(instances.findInstance(Nested.class)).contains(innermost);
		assertThat(instances.findInstance(DefaultTestInstancesTests.class)).contains(outermost);
		assertThat(instances.findInstance(String.class)).isEmpty();
	}

	class Nested {
	}

}
