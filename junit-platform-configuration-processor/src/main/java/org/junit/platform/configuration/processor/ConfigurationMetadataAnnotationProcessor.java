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

import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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

@API(status = API.Status.EXPERIMENTAL)
@SupportedAnnotationTypes("org.junit.platform.configuration.api.ConfigurationProperty")
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {
	private static final String METADATA_PATH = "META-INF/junit-platform-configuration-metadata.json";
	private @Nullable ProcessingEnvironment environment;

	@Override
	public synchronized void init(ProcessingEnvironment environment) {
		super.init(environment);
		this.environment = environment;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			try {
				var resource = requireNonNull(environment).getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
					METADATA_PATH);
				try (var out = new PrintWriter(
					new BufferedWriter(new OutputStreamWriter(resource.openOutputStream(), StandardCharsets.UTF_8)))) {
					out.println("{}");
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}
}
