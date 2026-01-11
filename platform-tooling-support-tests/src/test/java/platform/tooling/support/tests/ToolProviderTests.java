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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.DisabledOnOpenJ9;
import org.junit.jupiter.api.io.TempDir;

import platform.tooling.support.Helper;
import platform.tooling.support.MavenRepo;
import platform.tooling.support.ThirdPartyJars;

/**
 * @since 1.6
 */
@Order(Integer.MAX_VALUE)
class ToolProviderTests {

	@TempDir
	static Path lib;

	@BeforeAll
	static void prepareLocalLibraryDirectoryWithJUnitPlatformModules() {
		try {
			Files.createDirectories(lib);
			try (var directoryStream = Files.newDirectoryStream(lib, "*.jar")) {
				for (Path jarFile : directoryStream) {
					Files.delete(jarFile);
				}
			}
			for (var module : Helper.loadModuleDirectoryNames()) {
				if (module.startsWith("junit-platform")) {
					var jar = MavenRepo.jar(module);
					Files.copy(jar, lib.resolve(module + ".jar"));
				}
			}
			ThirdPartyJars.copy(lib, "org.apiguardian", "apiguardian-api");
			ThirdPartyJars.copy(lib, "org.opentest4j", "opentest4j");
			ThirdPartyJars.copy(lib, "org.opentest4j.reporting", "open-test-reporting-tooling-spi");
		}
		catch (Exception e) {
			throw new AssertionError("Preparing local library folder failed", e);
		}
	}

	@AfterAll
	static void triggerReleaseOfFileHandlesOnWindows() throws Exception {
		if (OS.current() == OS.WINDOWS) {
			System.gc();
			Thread.sleep(1_000);
		}
	}

	@Test
	void findAndRunJUnitOnTheClassPath() {
		try (var loader = new URLClassLoader("junit", urls(lib), ClassLoader.getPlatformClassLoader())) {
			var sl = ServiceLoader.load(ToolProvider.class, loader);
			var junit = StreamSupport.stream(sl.spliterator(), false).filter(p -> p.name().equals("junit")).findFirst();

			assertTrue(junit.isPresent(), "Tool 'junit' not found in: " + lib);
			assertJUnitPrintsHelpMessage(junit.get());
		}
		catch (IOException e) {
			throw new AssertionError("Closing URLClassLoader failed: " + e, e);
		}
	}

	@Test
	@DisabledOnOpenJ9
	void findAndRunJUnitOnTheModulePath() {
		var finder = ModuleFinder.of(lib);
		var modules = finder.findAll().stream() //
				.map(ModuleReference::descriptor) //
				.map(ModuleDescriptor::toNameAndVersion) //
				.sorted() //
				.toList();
		// modules.forEach(System.out::println);

		var bootLayer = ModuleLayer.boot();
		var configuration = bootLayer.configuration().resolveAndBind(finder, ModuleFinder.of(), Set.of());
		var layer = bootLayer.defineModulesWithOneLoader(configuration, ClassLoader.getPlatformClassLoader());

		var sl = ServiceLoader.load(layer, ToolProvider.class);
		var junit = StreamSupport.stream(sl.spliterator(), false).filter(p -> p.name().equals("junit")).findFirst();

		assertTrue(junit.isPresent(), "Tool 'junit' not found in modules: " + modules);
		assertJUnitPrintsHelpMessage(junit.get());
	}

	private static URL[] urls(Path directory) {
		try (var stream = Files.newDirectoryStream(directory, "*.jar")) {
			var paths = new ArrayList<URL>();
			stream.forEach(path -> paths.add(url(path)));
			return paths.toArray(URL[]::new);
		}
		catch (Exception e) {
			throw new AssertionError("Creating URL[] failed: " + e, e);
		}
	}

	private static URL url(Path path) {
		try {
			return path.toUri().toURL();
		}
		catch (MalformedURLException e) {
			throw new AssertionError("Converting path to URL failed: " + e, e);
		}
	}

	private static void assertJUnitPrintsHelpMessage(ToolProvider junit) {
		var out = new StringWriter();
		var err = new StringWriter();
		var code = junit.run(new PrintWriter(out), new PrintWriter(err), "--help");
		assertAll(() -> assertLinesMatch(List.of( //
			">> USAGE >>", //
			"Launches the JUnit Platform for test discovery and execution.", //
			">> OPTIONS >>"), //
			out.toString().lines().toList()), //
			() -> assertEquals("", err.toString()), //
			() -> assertEquals(0, code, "Expected exit of 0, but got: " + code) //
		);
	}

}
