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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import io.github.classgraph.AnnotationEnumValue;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

sealed interface Declaration extends Comparable<Declaration> {

	String moduleName();

	String packageName();

	String fullName();

	String name();

	String kind();

	Status status();

	String since();

	@Override
	default int compareTo(Declaration o) {
		return fullName().compareTo(o.fullName());
	}

	record Type(ClassInfo classInfo) implements Declaration {

		@Override
		public String moduleName() {
			return requireNonNull(classInfo.getModuleReference()).descriptor().name();
		}

		@Override
		public String packageName() {
			return classInfo.getPackageName();
		}

		@Override
		public String fullName() {
			return classInfo.getName();
		}

		@Override
		public String name() {
			var shortClassName = getShortClassName(classInfo);
			return classInfo.isAnnotation() ? "@" + shortClassName : shortClassName;
		}

		@Override
		public String kind() {
			return switch (classInfo) {
				case ClassInfo ignored when classInfo.isRecord() -> "record";
				case ClassInfo ignored when classInfo.isAnnotation() -> "annotation";
				case ClassInfo ignored when classInfo.isEnum() -> "enum";
				case ClassInfo ignored when classInfo.isInterface() -> "interface";
				default -> "class";
			};
		}

		@Override
		public Status status() {
			return readStatus(getParameterValues());
		}

		@Override
		public String since() {
			return readSince(getParameterValues());
		}

		private AnnotationParameterValueList getParameterValues() {
			return requireNonNull(classInfo.getAllAnnotationInfo(API.class)).getParameterValues();
		}
	}

	record Method(MethodInfo methodInfo) implements Declaration {

		@Override
		public String moduleName() {
			return requireNonNull(classInfo().getModuleReference()).descriptor().name();
		}

		@Override
		public String packageName() {
			return classInfo().getPackageName();
		}

		@Override
		public String fullName() {
			return "%s.%s".formatted(classInfo().getName(), methodSignature());
		}

		@Override
		public String name() {
			if (classInfo().isAnnotation()) {
				return "@%s(%s=...)".formatted(getShortClassName(classInfo()), methodInfo.getName());
			}
			if (methodInfo.isConstructor()) {
				return "%s%s".formatted(getShortClassName(classInfo()), methodParameters());
			}
			return "%s.%s".formatted(getShortClassName(classInfo()), methodSignature());
		}

		private String methodSignature() {
			return methodInfo.getName() + methodParameters();
		}

		private String methodParameters() {
			return methodInfo.getParameterInfo().stream() //
					.map(parameterInfo -> requireNonNull(
						parameterInfo.getTypeSignatureOrTypeDescriptor()).toStringWithSimpleNames()) //
					.collect(joining(", ", "(", ")"));
		}

		@Override
		public String kind() {
			if (methodInfo.isConstructor()) {
				return "constructor";
			}
			if (classInfo().isAnnotation()) {
				return "annotation attribute";
			}
			return "method";
		}

		@Override
		public Status status() {
			return readStatus(getParameterValues());
		}

		@Override
		public String since() {
			return readSince(getParameterValues());
		}

		private AnnotationParameterValueList getParameterValues() {
			return requireNonNull(methodInfo.getAllAnnotationInfo(API.class)).getParameterValues();
		}

		public ClassInfo classInfo() {
			return methodInfo.getClassInfo();
		}
	}

	private static Status readStatus(AnnotationParameterValueList parameterValues) {
		return Status.valueOf(
			((AnnotationEnumValue) requireNonNull(parameterValues.getValue("status"))).getValueName());
	}

	private static String readSince(AnnotationParameterValueList parameterValues) {
		return (String) parameterValues.getValue("since");
	}

	private static String getShortClassName(ClassInfo classInfo) {
		var typeName = classInfo.getName();
		var packageName = classInfo.getPackageName();
		if (typeName.startsWith(packageName + '.')) {
			typeName = typeName.substring(packageName.length() + 1);
		}
		return typeName.replace('$', '.');
	}
}
