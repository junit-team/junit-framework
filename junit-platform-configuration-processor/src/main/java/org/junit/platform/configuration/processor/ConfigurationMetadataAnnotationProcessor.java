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
import static org.junit.platform.configuration.processor.ConfigurationMetaData.Deprecation.Level.WARNING;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.StandardLocation;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.configuration.api.ConfigurationProperty;

@API(status = API.Status.EXPERIMENTAL)
@SupportedAnnotationTypes("org.junit.platform.configuration.api.ConfigurationProperty")
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {
	private static final String METADATA_PATH = "META-INF/junit-platform-configuration-metadata.json";
	private @Nullable ConfigurationMetaData metaData;

	@Override
	public synchronized void init(ProcessingEnvironment environment) {
		super.init(environment);
		this.metaData = new ConfigurationMetaData();
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		process(roundEnv);

		if (roundEnv.processingOver()) {
			writeMetaData();
		}
		return false;
	}

	private void writeMetaData() {
		try {
			var resource = processingEnvironment().getFiler() //
					.createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
			try (var out = new BufferedWriter(new OutputStreamWriter(resource.openOutputStream(), UTF_8))) {
				new JsonWriter().writeValue(out, metaData());
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to write metadata to [%s]".formatted(METADATA_PATH), ex);
		}
	}

	private void process(RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(ConfigurationProperty.class).stream() //
				.filter(VariableElement.class::isInstance) //
				.map(VariableElement.class::cast) //
				.map(this::createProperty) //
				.forEach(element -> metaData().addProperty(element));
	}

	private ConfigurationMetaData.Property createProperty(VariableElement element) {
		return new ConfigurationMetaData.Property( //
			processPropertyName(element), //
			null, // TODO:
			processDescription(element), //
			processSourceType(element), //
			null, // TODO:
			processDeprecation(element) //
		);
	}

	private ConfigurationMetaData.@Nullable Deprecation processDeprecation(VariableElement element) {
		var deprecated = element.getAnnotation(Deprecated.class);
		if (deprecated == null) {
			return null;
		}
		return new ConfigurationMetaData.Deprecation( //
			null, //
			null, // TODO:
			null, // TODO:
			null // TODO:
		);
	}

	private @Nullable String processDescription(VariableElement element) {
		var docComment = processingEnvironment().getElementUtils().getDocComment(element);
		return docComment == null ? null : cleanupDocComment(docComment);
	}

	private static String cleanupDocComment(String docComment) {
		// TODO: Creating patterns over and over is not very efficient
		var matcher = Pattern.compile("<p>|<h\\d>").matcher(docComment);
		var firstParagraph = !matcher.find() ? docComment : docComment.substring(0, matcher.start());
		return firstParagraph //
				// Replace newlines with space
				.replaceAll("[\n\r]", " ") //
				// Merge multiple spaces
				.replaceAll(" +", " ") //
				// Replace the `: {@value}` conventional syntax.
				.replaceAll(": \\{@value}\\.?", ".") //
				.trim();
	}

	private static String processSourceType(VariableElement element) {
		var enclosingTypeElement = getEnclosingTypeElement(element);
		return enclosingTypeElement.getQualifiedName().toString();
	}

	private static String processPropertyName(VariableElement element) {
		// TODO: Report preconditions problems with processingEnvironment().getMessager().printMessage() instead.
		var enclosingTypeElement = getEnclosingTypeElement(element);
		Preconditions.condition(isStatic(element), //
			() -> "Field [%s.%s] must be declared static" //
					.formatted(enclosingTypeElement.getQualifiedName(), element.getSimpleName()));
		Preconditions.condition(isFinal(element), //
			() -> "Field [%s.%s] must be declared final" //
					.formatted(enclosingTypeElement.getQualifiedName(), element.getSimpleName()));
		var constantValue = element.getConstantValue();
		Preconditions.condition(constantValue instanceof String, //
			() -> "Field [%s.%s] must have a constant string value" //
					.formatted(enclosingTypeElement.getQualifiedName(), element.getSimpleName()));
		return (String) constantValue;
	}

	private static TypeElement getEnclosingTypeElement(VariableElement element) {
		var enclosingElement = element.getEnclosingElement();
		Preconditions.condition(enclosingElement instanceof TypeElement, //
			() -> "[%s] did not have an enclosing type element" //
					.formatted(element.getSimpleName()));
		return (TypeElement) enclosingElement;
	}

	private static boolean isStatic(VariableElement variableElement) {
		return variableElement.getModifiers().contains(Modifier.STATIC);
	}

	private static boolean isFinal(VariableElement variableElement) {
		return variableElement.getModifiers().contains(Modifier.FINAL);
	}

	private ProcessingEnvironment processingEnvironment() {
		return requireNonNull(processingEnv);
	}

	private ConfigurationMetaData metaData() {
		return requireNonNull(metaData);
	}
}
