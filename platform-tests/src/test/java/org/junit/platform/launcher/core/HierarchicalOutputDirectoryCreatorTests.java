/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
public class HierarchicalOutputDirectoryCreatorTests {

	@TempDir
	Path tempDir;

	@Mock
	Supplier<Path> rootDirSupplier;

	@Mock
	TestDescriptor testDescriptor;

	@InjectMocks
	HierarchicalOutputDirectoryCreator provider;

	@BeforeEach
	void prepareMock() {
		when(rootDirSupplier.get()).thenReturn(tempDir);
	}

	@Test
	void returnsConfiguredRootDir() {
		assertThat(provider.getRootDirectory()).isEqualTo(tempDir);
		assertThat(provider.getRootDirectory()).isEqualTo(tempDir);
		verify(rootDirSupplier, times(1)).get();
	}

	@Test
	void createsSubDirectoriesBasedOnUniqueId() throws Exception {
		var uniqueId = UniqueId.forEngine("engine") //
				.append("irrelevant", "foo") //
				.append("irrelevant", "bar");
		when(testDescriptor.getUniqueId()).thenReturn(uniqueId);

		var outputDir = provider.createOutputDirectory(testDescriptor);

		assertThat(outputDir) //
				.isEqualTo(tempDir.resolve(Path.of("engine", "foo", "bar"))) //
				.exists();
	}

	@Test
	void replacesForbiddenCharacters() throws Exception {
		var uniqueId = UniqueId.forEngine("Engine<>") //
				.append("irrelevant", "*/abc");
		when(testDescriptor.getUniqueId()).thenReturn(uniqueId);

		var outputDir = provider.createOutputDirectory(testDescriptor);

		assertThat(outputDir) //
				.isEqualTo(tempDir.resolve(Path.of("Engine__", "__abc"))) //
				.exists();
	}
}
