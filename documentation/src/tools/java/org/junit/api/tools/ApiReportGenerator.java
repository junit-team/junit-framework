/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.api.tools;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * @since 1.0
 */
class ApiReportGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiReportGenerator.class);
	private static final String EOL = System.lineSeparator();

	public static void main(String... args) {

		// CAUTION: The output produced by this method is used to
		//          generate a table in the User Guide.

		try (var scanResult = scanClasspath()) {

			var apiReport = generateReport(scanResult);

			// ApiReportWriter reportWriter = new MarkdownApiReportWriter(apiReport);
			ApiReportWriter reportWriter = new AsciidocApiReportWriter(apiReport);
			// ApiReportWriter reportWriter = new HtmlApiReportWriter(apiReport);

			// reportWriter.printReportHeader(new PrintWriter(System.out, true));

			// Print report for all Usage enum constants
			// reportWriter.printDeclarationInfo(new PrintWriter(System.out, true), EnumSet.allOf(Status.class));

			// Print report only for specific Status constants, defaults to only EXPERIMENTAL
			parseArgs(args).forEach((status, opener) -> {
				try (var stream = opener.openStream()) {
					var writer = new PrintWriter(stream == null ? System.out : stream, true, UTF_8);
					reportWriter.printDeclarationInfo(writer, EnumSet.of(status));
				}
				catch (IOException e) {
					throw new UncheckedIOException("Failed to write report", e);
				}
			});
		}
	}

	// -------------------------------------------------------------------------

	private static Map<Status, StreamOpener> parseArgs(String[] args) {
		Map<Status, StreamOpener> outputByStatus = new EnumMap<>(Status.class);
		if (args.length == 0) {
			outputByStatus.put(Status.EXPERIMENTAL, () -> null);
		}
		else {
			Arrays.stream(args) //
					.map(arg -> arg.split("=", 2)) //
					.forEach(parts -> outputByStatus.put(//
						Status.valueOf(parts[0]), //
						() -> parts.length < 2 //
								? null //
								: new BufferedOutputStream(Files.newOutputStream(Path.of(parts[1]))) //
					));
		}
		return outputByStatus;
	}

	private interface StreamOpener {
		OutputStream openStream() throws IOException;
	}

	private static ApiReport generateReport(ScanResult scanResult) {
		Map<Status, List<Declaration>> declarations = new EnumMap<>(Status.class);
		for (var status : Status.values()) {
			declarations.put(status, new ArrayList<>());
		}

		var types = collectTypes(scanResult);
		types.stream() //
				.map(Declaration.Type::new) //
				.forEach(type -> declarations.get(type.status()).add(type));

		collectMethods(scanResult) //
				.map(Declaration.Method::new) //
				.filter(method -> !declarations.get(method.status()) //
						.contains(new Declaration.Type(method.classInfo()))) //
				.forEach(method -> {
					types.add(method.classInfo());
					declarations.get(method.status()).add(method);
				});

		declarations.values().forEach(list -> list.sort(null));

		return new ApiReport(types, declarations);
	}

	private static ScanResult scanClasspath() {
		// scan all types below "org.junit" package
		var classGraph = new ClassGraph() //
				.acceptPackages("org.junit") //
				.rejectPackages("*.shadow.*", "org.opentest4j.*", "org.junit.platform.commons.logging",
					"org.junit.platform.commons.util") //
				.disableNestedJarScanning() //
				.enableClassInfo() //
				.enableMethodInfo() //
				.enableAnnotationInfo(); //
		var apiClasspath = System.getProperty("api.modulePath");
		var apiModules = System.getProperty("api.moduleNames");
		if (apiClasspath != null && apiModules != null) {
			var paths = Arrays.stream(apiClasspath.split(File.pathSeparator)).map(Path::of).toArray(Path[]::new);
			var bootLayer = ModuleLayer.boot();
			var roots = Arrays.stream(apiModules.split(",")).collect(toUnmodifiableSet());
			var configuration = bootLayer.configuration().resolveAndBind(ModuleFinder.of(), ModuleFinder.of(paths),
				roots);
			var layer = bootLayer.defineModulesWithOneLoader(configuration, ClassLoader.getPlatformClassLoader());
			classGraph = classGraph.overrideModuleLayers(layer);
		}
		return classGraph.scan();
	}

	private static SortedSet<ClassInfo> collectTypes(ScanResult scanResult) {
		var types = scanResult.getClassesWithAnnotation(API.class).stream() //
				.filter(it -> !it.getAnnotationInfo(API.class).isInherited()) //
				.collect(toCollection(TreeSet::new));

		LOGGER.debug(() -> {
			var builder = new StringBuilder("Listing of all " + types.size() + " annotated types:");
			builder.append(EOL);
			types.forEach(e -> builder.append(e.getName()).append(EOL));
			return builder.toString();
		});

		return types;
	}

	private static Stream<MethodInfo> collectMethods(ScanResult scanResult) {
		return scanResult.getClassesWithMethodAnnotation(API.class).stream() //
				.flatMap(type -> type.getDeclaredMethodAndConstructorInfo().stream()) //
				.filter(m -> m.getAnnotationInfo(API.class) != null);
	}

	private ApiReportGenerator() {
	}

}
