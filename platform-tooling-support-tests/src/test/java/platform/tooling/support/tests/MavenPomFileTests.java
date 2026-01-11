/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import platform.tooling.support.MavenRepo;

/**
 * @since 1.4
 */
@Order(Integer.MAX_VALUE)
class MavenPomFileTests {

	@Test
	void jupiterAggregatorPomDependencies() throws Exception {

		var expected = List.of(">> HEAD >>", //
			"  <dependencyManagement>", //
			"    <dependencies>", //
			"      <dependency>", //
			"        <groupId>org.junit</groupId>", //
			"        <artifactId>junit-bom</artifactId>", //
			">> VERSION >>", //
			"        <type>pom</type>", //
			"        <scope>import</scope>", //
			"      </dependency>", //
			"    </dependencies>", //
			"  </dependencyManagement>", //
			"  <dependencies>", //
			"    <dependency>", //
			"      <groupId>org.junit.jupiter</groupId>", //
			"      <artifactId>junit-jupiter-api</artifactId>", //
			">> VERSION >>", //
			"      <scope>compile</scope>", //
			"    </dependency>", //
			"    <dependency>", //
			"      <groupId>org.junit.jupiter</groupId>", //
			"      <artifactId>junit-jupiter-params</artifactId>", //
			">> VERSION >>", //
			"      <scope>compile</scope>", //
			"    </dependency>", //
			"    <dependency>", //
			"      <groupId>org.junit.jupiter</groupId>", //
			"      <artifactId>junit-jupiter-engine</artifactId>", //
			">> VERSION >>", //
			"      <scope>runtime</scope>", //
			"    </dependency>", //
			"  </dependencies>", //
			">> TAIL >>");

		assertLinesMatch(expected, Files.readAllLines(MavenRepo.pom("junit-jupiter")));
	}

	@Test
	void jupiterAggregatorGradleMetadataMarker() throws Exception {

		var expected = List.of(">> HEAD >>", //
			"  <!-- do_not_remove: published-with-gradle-metadata -->", //
			">> TAIL >>");

		assertLinesMatch(expected, Files.readAllLines(MavenRepo.pom("junit-jupiter")));
	}
}
