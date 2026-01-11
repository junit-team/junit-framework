/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.launcher.core.ClasspathAlignmentChecker.WELL_KNOWN_PACKAGES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.PackageInfo;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.DisabledInEclipse;

class ClasspathAlignmentCheckerTests {

	@Test
	void classpathIsAligned() {
		assertThat(ClasspathAlignmentChecker.check(new LinkageError())).isEmpty();
	}

	@Test
	void wrapsLinkageErrorForUnalignedClasspath() {
		var cause = new LinkageError();
		AtomicInteger counter = new AtomicInteger();
		Function<String, @Nullable Package> packageLookup = name -> {
			var pkg = mock(Package.class);
			when(pkg.getName()).thenReturn(name);
			when(pkg.getImplementationVersion()).thenReturn(counter.incrementAndGet() + ".0.0");
			return pkg;
		};

		var result = ClasspathAlignmentChecker.check(cause, packageLookup);

		assertThat(result).isPresent();
		assertThat(result.get()) //
				.hasMessageStartingWith("The wrapped LinkageError is likely caused by the versions of "
						+ "JUnit jars on the classpath/module path not being properly aligned.") //
				.hasMessageContaining("Please ensure consistent versions are used") //
				.hasMessageFindingMatch("https://docs\\.junit\\.org/.*/appendix.html#dependency-metadata") //
				.hasMessageContaining("The following conflicting versions were detected:") //
				.hasMessageContaining("- org.junit.jupiter.api: 1.0.0") //
				.hasMessageContaining("- org.junit.jupiter.engine: 2.0.0") //
				.cause().isSameAs(cause);
	}

	@Test
	@DisabledInEclipse
	void allRootPackagesAreChecked() {
		var allowedFileNames = Pattern.compile("junit-(?:platform|jupiter|vintage)-.+[\\d.]+(?:-SNAPSHOT)?\\.jar");
		var classGraph = new ClassGraph() //
				.acceptPackages("org.junit.platform", "org.junit.jupiter", "org.junit.vintage") //
				.rejectPackages("org.junit.platform.reporting.shadow", "org.junit.jupiter.params.shadow") //
				.filterClasspathElements(e -> {
					var path = Path.of(e);
					var fileName = path.getFileName().toString();
					return allowedFileNames.matcher(fileName).matches();
				});

		try (var scanResult = classGraph.scan()) {
			var foundPackages = scanResult.getPackageInfo().stream() //
					.filter(it -> !it.getClassInfo().isEmpty()) //
					.map(PackageInfo::getName) //
					.sorted() //
					.toList();

			assertThat(foundPackages) //
					.allMatch(name -> WELL_KNOWN_PACKAGES.stream().anyMatch(name::startsWith));
			assertThat(WELL_KNOWN_PACKAGES) //
					.allMatch(name -> foundPackages.stream().anyMatch(it -> it.startsWith(name)));
		}
	}

}
