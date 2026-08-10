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

/// Collects all configuration parameters marked with
/// {@link org.junit.platform.configuration.api.ConfigurationParameter} into
/// [Spring Boot Configuration
/// Metadata](https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html)
/// and writes it to {@value #METADATA_PATH}. This enables IDE's and other tools
/// to process and validate Test Engine configuration.
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
/// The first paragraph from the doc string used to describe the property. If the
/// first paragraph ends with {@code : {@value}.} or {@code : {@value}} it will
/// be replaced with a {@code : {@value}.}.
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
			processingEnv.getElementUtils());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	@SuppressWarnings("DoNotClaimAnnotations")
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		configurationParameterHandler().process(roundEnv);

		if (roundEnv.processingOver()) {
			writeMetaData();
		}

		// Very simple check. Works because this processor only processes
		// ConfigurationParameter annotations.
		return !annotations.isEmpty();
	}

	private void writeMetaData() {
		try {
			var resource = processingEnvironment().getFiler() //
					.createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
			try (var out = new BufferedWriter(new OutputStreamWriter(resource.openOutputStream(), UTF_8))) {
				var converter = new JsonConverter();
				var value = converter.toJsonObject(metaData());
				Json.createWriter(out).write(value);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to write metadata to [%s]".formatted(METADATA_PATH), ex);
		}
	}

	private ConfigurationParameterHandler configurationParameterHandler() {
		return requireNonNull(configurationParameterHandler);
	}

	private ProcessingEnvironment processingEnvironment() {
		return requireNonNull(processingEnv);
	}

	private ConfigurationMetaData metaData() {
		return requireNonNull(metaData);
	}
}
