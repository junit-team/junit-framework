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

		var annotationMirror = requireNonNull(getAnnotationMirror(typeElement, ConfigurationParameterGroup.class));
		var values = toMap(annotationMirror);
		var name = (String) values.get("value");
		if (name == null || name.isEmpty()) {
			messager.printMessage(ERROR, "@ConfigurationParameterGroup.value must be non-empty", typeElement);
			return;
		}

		var sourceType = processSourceType(typeElement);
		var description = processDescription(typeElement);
		metaData.addGroup(new Group(name, sourceType, description));
	}

	private @Nullable String processDescription(TypeElement element) {
		var docComment = elementUtils.getDocComment(element);
		if (docComment == null) {
			return null;
		}
		return DocumentationUtil.extractFirstParagraph(docComment);
	}

	private String processSourceType(TypeElement element) {
		return element.getQualifiedName().toString();
	}

}
