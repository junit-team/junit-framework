/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

import jakarta.json.Json;

/// Writes all configuration parameters marked with
/// {@link org.junit.platform.configuration.api.ConfigurationParameter} to
/// {@value #METADATA_PATH} in [Spring Boot's Configuration
/// Metadata](https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html)
/// format. This enables IDEs and other tools to process and validate Test Engine
/// configuration.
///
/// <h4>Usage</h4>
///
/// <pre>{@code
/// /**
///   * A brief multi-line description of
///   * this property: {@value}.
///   *
///   * <p>Followed by an additional paragraph.
///   */
///  @ConfigurationProperty
///  public static final String EXAMPLE_PROPERTY_NAME = "org.example.property";
///
/// }</pre>
///
/// The first paragraph from the doc string will be used to describe the
/// property. If the first paragraph ends with {@code : {@value}.} or
/// {@code : {@value}} it will be replaced with a {@code : {@value}.}.
///
@API(status = API.Status.EXPERIMENTAL)
@SupportedAnnotationTypes("org.junit.platform.configuration.api.ConfigurationParameter")
public final class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {
	private static final String METADATA_PATH = "META-INF/junit-platform-configuration-metadata.json";
	private @Nullable ConfigurationMetaData metaData;
	private @Nullable ConfigurationParameterHandler configurationParameterHandler;

	@Override
	public synchronized void init(ProcessingEnvironment environment) {
		super.init(environment);
		this.metaData = new ConfigurationMetaData();
		this.configurationParameterHandler = new ConfigurationParameterHandler(metaData,
			processingEnv.getElementUtils(), processingEnv.getMessager());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	@SuppressWarnings("DoNotClaimAnnotations")
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		requireNonNull(configurationParameterHandler).process(roundEnv);

		if (roundEnv.processingOver()) {
			writeMetaData();
		}

		// Very simple check. Works because this processor only processes
		// ConfigurationParameter annotations.
		return !annotations.isEmpty();
	}

	private void writeMetaData() {
		var converter = new JsonConverter();
		try (var out = new BufferedWriter(new OutputStreamWriter(openOutputStream(), UTF_8))) {
			var value = converter.toJsonObject(requireNonNull(metaData));
			Json.createWriter(out).write(value);
		}
		catch (Exception ex) {
			var message = "Failed to write metadata to [%s]".formatted(METADATA_PATH);
			throw new ConfigurationMetadataAnnotationProcessorException(message, ex);
		}
	}

	private OutputStream openOutputStream() throws IOException {
		var filer = requireNonNull(processingEnv).getFiler();
		var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
		return resource.openOutputStream();
	}

}
