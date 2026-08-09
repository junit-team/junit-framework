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
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.StandardLocation;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.configuration.api.ConfigurationParameter;

/// Collects all configuration parameters marked with
/// {@link ConfigurationParameter} into
/// [Spring Boot Configuration Metadata](https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html).
/// This enables IDE's and other tools to process and validate Test Engine
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
/// The first paragraph from the doc string used to describe the property. If the
/// first paragraph ends with {@code : {@value}.} or {@code : {@value}} it will
/// be replaced with a {@code : {@value}.}.
///
@API(status = API.Status.EXPERIMENTAL)
@SupportedAnnotationTypes("org.junit.platform.configuration.api.ConfigurationParameter")
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
		// TODO: Consider setting to true?
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
		roundEnv.getElementsAnnotatedWith(ConfigurationParameter.class).stream() //
				.filter(VariableElement.class::isInstance) //
				.map(VariableElement.class::cast) //
				.map(this::createProperty) //
				.forEach(element -> metaData().addProperty(element));
	}

	private ConfigurationMetaData.Property createProperty(VariableElement element) {
		return new ConfigurationMetaData.Property( //
			processName(element), //
			null, // TODO:
			processDescription(element), //
			processSourceType(element), //
			null, // TODO:
			processDeprecation(element) //
		);
	}

	private ConfigurationMetaData.@Nullable Deprecation processDeprecation(VariableElement element) {
		var deprecated = getAnnotation(element, Deprecated.class);
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

	private @Nullable AnnotationMirror getAnnotation(Element element, Class<? extends Annotation> annotationType) {
		var annotationTypeName = annotationType.getName();
		for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
			if (annotationTypeName.equals(annotation.getAnnotationType().toString())) {
				return annotation;
			}
		}
		return null;
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

	private String processSourceType(VariableElement element) {
		var enclosingTypeElement = getEnclosingTypeElement(element);
		return enclosingTypeElement.getQualifiedName().toString();
	}

	private String processName(VariableElement element) {
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

	private TypeElement getEnclosingTypeElement(VariableElement element) {
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
