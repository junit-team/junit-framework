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
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.getAnnotationMirror;
import static org.junit.platform.configuration.processor.AnnotationMirrorUtil.toMap;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.jspecify.annotations.Nullable;
import org.junit.platform.configuration.api.ConfigurationParameterGroup;
import org.junit.platform.configuration.processor.ConfigurationMetadata.Group;

final class ConfigurationParameterGroupHandler {

	private final ConfigurationMetadata metaData;
	private final Elements elementUtils;
	private final Messager messager;

	ConfigurationParameterGroupHandler(ConfigurationMetadata metaData, Elements elementUtils, Messager messager) {
		this.metaData = metaData;
		this.elementUtils = elementUtils;
		this.messager = messager;
	}

	void process(RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(ConfigurationParameterGroup.class).forEach(this::processElement);
	}

	private void processElement(Element element) {
		if (!(element instanceof TypeElement typeElement)) {
			messager.printMessage(ERROR, "@ConfigurationParameterGroup annotated element was not a type", element);
			return;
		}
		var name = processName(typeElement);
		if (name == null) {
			return;
		}
		var sourceType = processSourceType(typeElement);
		var description = processDescription(typeElement);
		metaData.addGroup(new Group(name, sourceType, description));
	}

	private @Nullable String processName(TypeElement element) {
		var annotationMirror = requireNonNull(getAnnotationMirror(element, ConfigurationParameterGroup.class));
		var values = toMap(annotationMirror);

		String name = (String) values.get("value");
		if (name == null || name.isEmpty()) {
			messager.printMessage(ERROR, "@ConfigurationParameterGroup.value must not be empty", element);
			return null;
		}
		return name;
	}

	private @Nullable String processDescription(TypeElement element) {
		var docComment = elementUtils.getDocComment(element);
		return DocumentationUtil.extractFirstParagraph(docComment);
	}

	private String processSourceType(TypeElement element) {
		return element.getQualifiedName().toString();
	}

}
