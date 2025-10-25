/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedMethods;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.support.HierarchyTraversalMode.BOTTOM_UP;
import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.support.ReflectionSupport.findFields;
import static org.junit.platform.commons.util.ReflectionUtils.isRecordClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.util.ReflectionUtils;

class ParameterizedClassContext implements ParameterizedDeclarationContext<ParameterizedClassInvocationContext> {

	private final Class<?> testClass;
	private final ParameterizedClass annotation;
	private final TestInstance.Lifecycle testInstanceLifecycle;
	private final ResolverFacade resolverFacade;
	private final InjectionType injectionType;
	private final List<ArgumentSetLifecycleMethod> beforeMethods;
	private final List<ArgumentSetLifecycleMethod> afterMethods;

	ParameterizedClassContext(Class<?> testClass, ParameterizedClass annotation,
			TestInstance.Lifecycle testInstanceLifecycle) {
		this.testClass = testClass;
		this.annotation = annotation;
		this.testInstanceLifecycle = testInstanceLifecycle;

		List<Field> fields = findParameterAnnotatedFields(testClass);
		if (fields.isEmpty()) {
			this.resolverFacade = ResolverFacade.create(ReflectionUtils.getDeclaredConstructor(testClass), annotation);
			this.injectionType = InjectionType.CONSTRUCTOR;
		}
		else {
			this.resolverFacade = ResolverFacade.create(testClass, fields);
			this.injectionType = InjectionType.FIELDS;
		}

		this.beforeMethods = findLifecycleMethodsAndAssertStaticAndNonPrivate(testClass, TOP_DOWN,
			BeforeParameterizedClassInvocation.class, BeforeParameterizedClassInvocation::injectArguments,
			this.resolverFacade);

		// Make a local copy since findAnnotatedMethods() returns an immutable list.
		this.afterMethods = new ArrayList<>(findLifecycleMethodsAndAssertStaticAndNonPrivate(testClass, BOTTOM_UP,
			AfterParameterizedClassInvocation.class, AfterParameterizedClassInvocation::injectArguments,
			this.resolverFacade));

		// Since the bottom-up ordering of afterMethods will later be reversed when the
		// AfterParameterizedClassInvocationMethodInvoker extensions are executed within
		// ClassTemplateInvocationTestDescriptor, we have to reverse them to put them
		// in top-down order before we register them as extensions.
		reverse(afterMethods);
	}

	private static List<Field> findParameterAnnotatedFields(Class<?> clazz) {
		if (isRecordClass(clazz)) {
			return emptyList();
		}
		return findFields(clazz, it -> isAnnotated(it, Parameter.class), BOTTOM_UP);
	}

	@Override
	public Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public ParameterizedClass getAnnotation() {
		return this.annotation;
	}

	@Override
	public Class<?> getAnnotatedElement() {
		return this.testClass;
	}

	@Override
	public String getDisplayNamePattern() {
		return this.annotation.name();
	}

	@Override
	public boolean quoteTextArguments() {
		return this.annotation.quoteTextArguments();
	}

	@Override
	public boolean isAutoClosingArguments() {
		return this.annotation.autoCloseArguments();
	}

	@Override
	public boolean isAllowingZeroInvocations() {
		return this.annotation.allowZeroInvocations();
	}

	@Override
	public ArgumentCountValidationMode getArgumentCountValidationMode() {
		return this.annotation.argumentCountValidation();
	}

	@Override
	public ResolverFacade getResolverFacade() {
		return this.resolverFacade;
	}

	@Override
	public ParameterizedClassInvocationContext createInvocationContext(ParameterizedInvocationNameFormatter formatter,
			Arguments arguments, int invocationIndex) {

		return new ParameterizedClassInvocationContext(this, formatter, arguments, invocationIndex);
	}

	TestInstance.Lifecycle getTestInstanceLifecycle() {
		return testInstanceLifecycle;
	}

	InjectionType getInjectionType() {
		return injectionType;
	}

	List<ArgumentSetLifecycleMethod> getBeforeMethods() {
		return beforeMethods;
	}

	List<ArgumentSetLifecycleMethod> getAfterMethods() {
		return afterMethods;
	}

	private static <A extends Annotation> List<ArgumentSetLifecycleMethod> findLifecycleMethodsAndAssertStaticAndNonPrivate(
			Class<?> testClass, HierarchyTraversalMode traversalMode, Class<A> annotationType,
			Predicate<A> injectArgumentsPredicate, ResolverFacade resolverFacade) {

		List<Method> methods = findAnnotatedMethods(testClass, annotationType, traversalMode);

		return methods.stream() //
				.map(method -> {
					A annotation = getAnnotation(method, annotationType);
					if (injectArgumentsPredicate.test(annotation)) {
						return new ArgumentSetLifecycleMethod(method,
							resolverFacade.createLifecycleMethodParameterResolver(method, annotation));
					}
					return new ArgumentSetLifecycleMethod(method);
				}) //
				.toList();
	}

	private static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		return findAnnotation(method, annotationType) //
				.orElseThrow(() -> new JUnitException("Method not annotated with @" + annotationType.getSimpleName()));
	}

	enum InjectionType {
		CONSTRUCTOR, FIELDS
	}
}
