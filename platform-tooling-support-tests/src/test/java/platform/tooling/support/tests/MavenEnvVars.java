/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.condition.JRE;

final class MavenEnvVars {

	private static final List<String> FOR_JDK24_AND_LATER = List.of( //
		"--enable-native-access=ALL-UNNAMED", // https://issues.apache.org/jira/browse/MNG-8248
		"--sun-misc-unsafe-memory-access=allow" // https://issues.apache.org/jira/browse/MNG-8399
	);
	private static final List<String> FOR_JDK26_AND_LATER = List.of( //
		"--enable-final-field-mutation=ALL-UNNAMED" // https://github.com/junit-team/junit-framework/issues/5173
	);

	static Map<String, String> forJre(JRE jre) {
		var list = new ArrayList<String>();
		if (jre.compareTo(JRE.JAVA_24) >= 0)
			list.addAll(FOR_JDK24_AND_LATER);
		if (jre.compareTo(JRE.JAVA_26) >= 0) {
			// exclude "leyden" and "valhalla" builds
			if (Runtime.version().build().orElse(0) >= 25) {
				list.addAll(FOR_JDK26_AND_LATER);
			}
		}
		return list.isEmpty() ? Map.of() : Map.of("MAVEN_OPTS", String.join(" ", list));
	}

	private MavenEnvVars() {
	}
}
