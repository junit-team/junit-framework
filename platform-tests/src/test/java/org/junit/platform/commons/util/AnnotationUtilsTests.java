/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedMethods;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.findPublicAnnotatedFields;
import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.TOP_DOWN;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.pkg1.ClassLevelDir;
import org.junit.platform.commons.util.pkg1.InstanceLevelDir;
import org.junit.platform.commons.util.pkg1.SuperclassWithStaticPackagePrivateBeforeMethod;
import org.junit.platform.commons.util.pkg1.SuperclassWithStaticPackagePrivateTempDirField;
import org.junit.platform.commons.util.pkg1.subpkg.SubclassWithNonStaticPackagePrivateBeforeMethod;
import org.junit.platform.commons.util.pkg1.subpkg.SubclassWithNonStaticPackagePrivateTempDirField;

/**
 * Unit tests for {@link AnnotationUtils}.
 *
 * @since 1.0
 */
class AnnotationUtilsTests {

	private static final Predicate<Field> isStringField = field -> field.getType() == String.class;

	@Test
	void findAnnotationForNullOptional() {
		assertThat(findAnnotation((Optional<AnnotatedElement>) null, Annotation1.class)).isEmpty();
	}

	@Test
	void findAnnotationForEmptyOptional() {
		assertThat(findAnnotation(Optional.empty(), Annotation1.class)).isEmpty();
	}

	@Test
	void findAnnotationForNullAnnotatedElement() {
		assertThat(findAnnotation((AnnotatedElement) null, Annotation1.class)).isEmpty();
	}

	@Test
	void findAnnotationOnClassWithoutAnnotation() {
		assertThat(findAnnotation(Annotation1Class.class, Annotation2.class)).isNotPresent();
	}

	@Test
	void findAnnotationIndirectlyPresentOnOptionalClass() {
		Optional<Class<?>> optional = Optional.of(SubInheritedAnnotationClass.class);
		assertThat(findAnnotation(optional, InheritedAnnotation.class)).isPresent();
	}

	@Test
	void findAnnotationIndirectlyPresentOnClass() {
		assertThat(findAnnotation(SubInheritedAnnotationClass.class, InheritedAnnotation.class)).isPresent();
	}

	/**
	 * Test for https://github.com/junit-team/junit-framework/issues/1133
	 */
	@Test
	void findInheritedAnnotationMetaPresentOnNonInheritedComposedAnnotationPresentOnSuperclass() {
		assertThat(findAnnotation(SubNonInheritedCompositionOfInheritedAnnotationClass.class,
			InheritedAnnotation.class)).isPresent();
	}

	@Test
	void findAnnotationDirectlyPresentOnClass() {
		assertThat(findAnnotation(Annotation1Class.class, Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationMetaPresentOnClass() {
		assertThat(findAnnotation(ComposedAnnotationClass.class, Annotation1.class)).isPresent();
	}

	/**
	 * <b>Note:</b> there is no findAnnotationIndirectlyMetaPresentOnMethod
	 * counterpart because the {@code @Inherited} annotation has no effect if
	 * the annotation type is used to annotate anything other than a class.
	 *
	 * @see Inherited
	 */
	@Test
	void findAnnotationIndirectlyMetaPresentOnClass() {
		assertThat(findAnnotation(SubInheritedComposedAnnotationClass.class, Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationDirectlyPresentOnImplementedInterface() {
		assertThat(findAnnotation(TestingTraitClass.class, Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationMetaPresentOnImplementedInterface() {
		assertThat(findAnnotation(ComposedTestingTraitClass.class, Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationDirectlyPresentOnMethod() throws Exception {
		var method = Annotation2Class.class.getDeclaredMethod("method");
		assertThat(findAnnotation(method, Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationMetaPresentOnMethod() throws Exception {
		var method = ComposedAnnotationClass.class.getDeclaredMethod("method");
		assertThat(findAnnotation(method, Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationMetaPresentOnOptionalMethod() throws Exception {
		var method = ComposedAnnotationClass.class.getDeclaredMethod("method");
		assertThat(findAnnotation(Optional.of(method), Annotation1.class)).isPresent();
	}

	@Test
	void findAnnotationDirectlyPresentOnEnclosingClass() throws Exception {
		Class<?> clazz = Annotation1Class.InnerClass.class;
		assertThat(findAnnotation(clazz, Annotation1.class, false)).isNotPresent();
		assertThat(findAnnotation(clazz, Annotation1.class, true)).isPresent();

		clazz = Annotation1Class.InnerClass.InnerInnerClass.class;
		assertThat(findAnnotation(clazz, Annotation1.class, false)).isNotPresent();
		assertThat(findAnnotation(clazz, Annotation1.class, true)).isPresent();

		clazz = Annotation1Class.NestedClass.class;
		assertThat(findAnnotation(clazz, Annotation1.class, false)).isNotPresent();
		assertThat(findAnnotation(clazz, Annotation1.class, true)).isNotPresent();
	}

	@Test
	void findAnnotationMetaPresentOnEnclosingClass() throws Exception {
		Class<?> clazz = ComposedAnnotationClass.InnerClass.class;
		assertThat(findAnnotation(clazz, Annotation1.class, false)).isNotPresent();
		assertThat(findAnnotation(clazz, Annotation1.class, true)).isPresent();

		clazz = ComposedAnnotationClass.InnerClass.InnerInnerClass.class;
		assertThat(findAnnotation(clazz, Annotation1.class, false)).isNotPresent();
		assertThat(findAnnotation(clazz, Annotation1.class, true)).isPresent();
	}

	@Test
	void isAnnotatedForClassWithoutAnnotation() {
		assertFalse(isAnnotated(Annotation1Class.class, Annotation2.class));
	}

	@Test
	void isAnnotatedWhenIndirectlyPresentOnClass() {
		assertTrue(isAnnotated(SubInheritedAnnotationClass.class, InheritedAnnotation.class));
	}

	@Test
	void isAnnotatedWhenDirectlyPresentOnClass() {
		assertTrue(isAnnotated(Annotation1Class.class, Annotation1.class));
	}

	@Test
	void isAnnotatedWhenMetaPresentOnClass() {
		assertTrue(isAnnotated(ComposedAnnotationClass.class, Annotation1.class));
	}

	@Test
	void isAnnotatedWhenDirectlyPresentOnMethod() throws Exception {
		assertTrue(isAnnotated(Annotation2Class.class.getDeclaredMethod("method"), Annotation1.class));
	}

	@Test
	void isAnnotatedWhenMetaPresentOnMethod() throws Exception {
		assertTrue(isAnnotated(ComposedAnnotationClass.class.getDeclaredMethod("method"), Annotation1.class));
	}

	@Test
	void findRepeatableAnnotationsForNotRepeatableAnnotation() {
		assertPreconditionViolationFor(() -> findRepeatableAnnotations(getClass(), Inherited.class)).withMessage(
			Inherited.class.getName() + " must be @Repeatable");
	}

	@Test
	void findRepeatableAnnotationsForNullOptionalAnnotatedElement() {
		assertThat(findRepeatableAnnotations((Optional<AnnotatedElement>) null, Tag.class)).isEmpty();
	}

	@Test
	void findRepeatableAnnotationsForEmptyOptionalAnnotatedElement() {
		assertThat(findRepeatableAnnotations(Optional.empty(), Tag.class)).isEmpty();
	}

	@Test
	void findRepeatableAnnotationsForNullAnnotatedElement() {
		assertThat(findRepeatableAnnotations((AnnotatedElement) null, Tag.class)).isEmpty();
	}

	@Test
	void findRepeatableAnnotationsWithSingleTag() {
		assertTagsFound(SingleTaggedClass.class, "a");
	}

	@Test
	void findRepeatableAnnotationsWithSingleComposedTag() {
		assertTagsFound(SingleComposedTaggedClass.class, "fast");
	}

	@Test
	void findRepeatableAnnotationsWithSingleComposedTagOnImplementedInterface() {
		assertTagsFound(TaggedInterfaceClass.class, "fast");
	}

	@Test
	void findRepeatableAnnotationsWithLocalComposedTagAndComposedTagOnImplementedInterface() {
		assertTagsFound(LocalTagOnTaggedInterfaceClass.class, "fast", "smoke");
	}

	@Test
	void findRepeatableAnnotationsWithMultipleTags() {
		assertTagsFound(MultiTaggedClass.class, "a", "b", "c");
	}

	@Test
	void findRepeatableAnnotationsWithMultipleComposedTags() {
		assertTagsFound(MultiComposedTaggedClass.class, "fast", "smoke");
		assertTagsFound(FastAndSmokyTaggedClass.class, "fast", "smoke");
	}

	@Test
	void findRepeatableAnnotationsWithContainer() {
		assertTagsFound(ContainerTaggedClass.class, "a", "b", "c", "d");
	}

	@Test
	void findRepeatableAnnotationsWithComposedTagBeforeContainer() {
		assertTagsFound(ContainerAfterComposedTaggedClass.class, "fast", "a", "b", "c");
	}

	private void assertTagsFound(Class<?> clazz, String... tags) {
		assertEquals(List.of(tags), findRepeatableAnnotations(clazz, Tag.class).stream().map(Tag::value).toList(),
			() -> "Tags found for class " + clazz.getName());
	}

	@Test
	void findInheritedRepeatableAnnotationsWithSingleAnnotationOnSuperclass() {
		assertExtensionsFound(SingleExtensionClass.class, "a");
		assertExtensionsFound(SubSingleExtensionClass.class, "a");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithMultipleAnnotationsOnSuperclass() {
		assertExtensionsFound(MultiExtensionClass.class, "a", "b", "c");
		assertExtensionsFound(SubMultiExtensionClass.class, "a", "b", "c", "x", "y", "z");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithContainerAnnotationOnSuperclass() {
		assertExtensionsFound(ContainerExtensionClass.class, "a", "b", "c");
		assertExtensionsFound(SubContainerExtensionClass.class, "a", "b", "c", "x");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithSingleComposedAnnotation() {
		assertExtensionsFound(SingleComposedExtensionClass.class, "foo");
	}

	/**
	 * @since 1.5
	 */
	@Test
	void findInheritedRepeatableAnnotationsWithComposedAnnotationsInNestedContainer() {
		assertExtensionsFound(MultipleFoos1.class, "foo");
		assertExtensionsFound(MultipleFoos2.class, "foo");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithSingleComposedAnnotationOnSuperclass() {
		assertExtensionsFound(SubSingleComposedExtensionClass.class, "foo");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithMultipleComposedAnnotations() {
		assertExtensionsFound(MultiComposedExtensionClass.class, "foo", "bar");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithMultipleComposedAnnotationsOnSuperclass() {
		assertExtensionsFound(SubMultiComposedExtensionClass.class, "foo", "bar");
	}

	@Test
	void findInheritedRepeatableAnnotationsWithMultipleComposedAnnotationsOnSuperclassAndLocalContainerAndComposed() {
		assertExtensionsFound(ContainerPlusSubMultiComposedExtensionClass.class, "foo", "bar", "x", "y", "z");
	}

	private void assertExtensionsFound(Class<?> clazz, String... tags) {
		assertEquals(List.of(tags),
			findRepeatableAnnotations(clazz, ExtendWith.class).stream().map(ExtendWith::value).toList(),
			() -> "Extensions found for class " + clazz.getName());
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAnnotatedMethodsForNullClass() {
		assertPreconditionViolationFor(() -> findAnnotatedMethods(null, Annotation1.class, TOP_DOWN));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAnnotatedMethodsForNullAnnotationType() {
		assertPreconditionViolationFor(() -> findAnnotatedMethods(ClassWithAnnotatedMethods.class, null, TOP_DOWN));
	}

	@Test
	void findAnnotatedMethodsForAnnotationThatIsNotPresent() {
		assertThat(findAnnotatedMethods(ClassWithAnnotatedMethods.class, Fast.class, TOP_DOWN)).isEmpty();
	}

	@Test
	void findAnnotatedMethodsForAnnotationOnMethodsInClassUsingHierarchyDownMode() throws Exception {
		var method2 = ClassWithAnnotatedMethods.class.getDeclaredMethod("method2");
		var method3 = ClassWithAnnotatedMethods.class.getDeclaredMethod("method3");

		var methods = findAnnotatedMethods(ClassWithAnnotatedMethods.class, Annotation2.class, TOP_DOWN);

		assertThat(methods).containsOnly(method2, method3);
	}

	@Test
	void findAnnotatedMethodsForAnnotationOnMethodsInClassHierarchyUsingHierarchyUpMode() throws Exception {
		var method1 = ClassWithAnnotatedMethods.class.getDeclaredMethod("method1");
		var method3 = ClassWithAnnotatedMethods.class.getDeclaredMethod("method3");
		var superMethod = SuperclassWithAnnotatedMethod.class.getDeclaredMethod("superMethod");

		var methods = findAnnotatedMethods(ClassWithAnnotatedMethods.class, Annotation1.class, BOTTOM_UP);

		assertEquals(3, methods.size());
		assertThat(methods.subList(0, 2)).containsOnly(method1, method3);
		assertEquals(superMethod, methods.get(2));
	}

	@Test
	void findAnnotatedMethodsForAnnotationUsedInClassAndSuperclassHierarchyDown() throws Exception {
		var method1 = ClassWithAnnotatedMethods.class.getDeclaredMethod("method1");
		var method3 = ClassWithAnnotatedMethods.class.getDeclaredMethod("method3");
		var superMethod = SuperclassWithAnnotatedMethod.class.getDeclaredMethod("superMethod");

		var methods = findAnnotatedMethods(ClassWithAnnotatedMethods.class, Annotation1.class, TOP_DOWN);

		assertEquals(3, methods.size());
		assertEquals(superMethod, methods.getFirst());
		assertThat(methods.subList(1, 3)).containsOnly(method1, method3);
	}

	/*
	 * see https://github.com/junit-team/junit-framework/issues/3553
	 */
	@Test
	void findAnnotatedMethodsDoesNotAllowInstanceMethodToHideStaticMethod() throws Exception {
		final String BEFORE = "before";
		Class<?> superclass = SuperclassWithStaticPackagePrivateBeforeMethod.class;
		Method beforeAllMethod = superclass.getDeclaredMethod(BEFORE);
		Class<?> subclass = SubclassWithNonStaticPackagePrivateBeforeMethod.class;
		Method beforeEachMethod = subclass.getDeclaredMethod(BEFORE);

		// Prerequisite
		var methods = findAnnotatedMethods(superclass, BeforeAll.class, TOP_DOWN);
		assertThat(methods).containsExactly(beforeAllMethod);

		// Actual use cases for this test
		methods = findAnnotatedMethods(subclass, BeforeAll.class, TOP_DOWN);
		assertThat(methods).containsExactly(beforeAllMethod);
		methods = findAnnotatedMethods(subclass, BeforeEach.class, TOP_DOWN);
		assertThat(methods).containsExactly(beforeEachMethod);
	}

	@Test
	void findAnnotatedMethodsForAnnotationUsedInInterface() throws Exception {
		var interfaceMethod = InterfaceWithAnnotatedDefaultMethod.class.getDeclaredMethod("interfaceMethod");

		var methods = findAnnotatedMethods(ClassWithAnnotatedMethods.class, Annotation3.class, BOTTOM_UP);

		assertThat(methods).containsExactly(interfaceMethod);
	}

	// === findAnnotatedFields() ===============================================

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAnnotatedFieldsForNullClass() {
		assertPreconditionViolationFor(() -> findAnnotatedFields(null, Annotation1.class, isStringField, TOP_DOWN));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAnnotatedFieldsForNullAnnotationType() {
		assertPreconditionViolationFor(
			() -> findAnnotatedFields(ClassWithAnnotatedFields.class, null, isStringField, TOP_DOWN));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findAnnotatedFieldsForNullPredicate() {
		assertPreconditionViolationFor(
			() -> findAnnotatedFields(ClassWithAnnotatedFields.class, Annotation1.class, null, TOP_DOWN));
	}

	@Test
	void findAnnotatedFieldsForAnnotationThatIsNotPresent() {
		assertThat(findAnnotatedFields(ClassWithAnnotatedFields.class, Fast.class, isStringField, TOP_DOWN)).isEmpty();
	}

	@Test
	void findAnnotatedFieldsForAnnotationOnFieldsInClassUsingHierarchyDownMode() throws Exception {
		var field2 = ClassWithAnnotatedFields.class.getDeclaredField("field2");
		var field3 = ClassWithAnnotatedFields.class.getDeclaredField("field3");

		var fields = findAnnotatedFields(ClassWithAnnotatedFields.class, Annotation2.class, isStringField, TOP_DOWN);

		assertThat(fields).containsOnly(field2, field3);
	}

	@Test
	void findAnnotatedFieldsForAnnotationOnFieldsInClassHierarchyUsingHierarchyUpMode() throws Exception {
		var field1 = ClassWithAnnotatedFields.class.getDeclaredField("field1");
		var field3 = ClassWithAnnotatedFields.class.getDeclaredField("field3");
		var superField = SuperclassWithAnnotatedField.class.getDeclaredField("superField");

		var fields = findAnnotatedFields(ClassWithAnnotatedFields.class, Annotation1.class, isStringField, BOTTOM_UP);

		assertEquals(3, fields.size());
		assertThat(fields.subList(0, 2)).containsOnly(field1, field3);
		assertEquals(superField, fields.get(2));
	}

	@Test
	void findAnnotatedFieldsForAnnotationUsedInClassAndSuperclassHierarchyDown() throws Exception {
		var field1 = ClassWithAnnotatedFields.class.getDeclaredField("field1");
		var field3 = ClassWithAnnotatedFields.class.getDeclaredField("field3");
		var superField = SuperclassWithAnnotatedField.class.getDeclaredField("superField");

		var fields = findAnnotatedFields(ClassWithAnnotatedFields.class, Annotation1.class, isStringField, TOP_DOWN);

		assertEquals(3, fields.size());
		assertEquals(superField, fields.getFirst());
		assertThat(fields.subList(1, 3)).containsOnly(field1, field3);
	}

	@Test
	void findAnnotatedFieldsForAnnotationUsedInInterface() throws Exception {
		var interfaceField = InterfaceWithAnnotatedField.class.getDeclaredField("interfaceField");

		var fields = findAnnotatedFields(ClassWithAnnotatedFields.class, Annotation3.class, isStringField, BOTTOM_UP);

		assertThat(fields).containsExactly(interfaceField);
	}

	@Test
	void findAnnotatedFieldsFindsAllFieldsInTypeHierarchy() {
		assertThat(findShadowingAnnotatedFields(Annotation1.class))//
				.containsExactly("super", "foo", "baz", "super-shadow", "foo-shadow", "baz-shadow");
		assertThat(findShadowingAnnotatedFields(Annotation2.class))//
				.containsExactly("bar", "baz", "bar-shadow", "baz-shadow");
		assertThat(findShadowingAnnotatedFields(Annotation3.class))//
				.containsExactly("interface", "interface-shadow");
	}

	private List<String> findShadowingAnnotatedFields(Class<? extends Annotation> annotationType) {
		var fields = findAnnotatedFields(ClassWithShadowedAnnotatedFields.class, annotationType, isStringField);
		var values = ReflectionUtils.readFieldValues(fields, new ClassWithShadowedAnnotatedFields());
		return values.stream().map(String::valueOf).toList();
	}

	/*
	 * see https://github.com/junit-team/junit-framework/issues/3553
	 */
	@Test
	void findAnnotatedFieldsDoesNotAllowInstanceFieldToHideStaticField() throws Exception {
		final String TEMP_DIR = "tempDir";
		Class<?> superclass = SuperclassWithStaticPackagePrivateTempDirField.class;
		Field staticField = superclass.getDeclaredField(TEMP_DIR);
		Class<?> subclass = SubclassWithNonStaticPackagePrivateTempDirField.class;
		Field nonStaticField = subclass.getDeclaredField(TEMP_DIR);

		// Prerequisite
		var fields = findAnnotatedFields(superclass, ClassLevelDir.class, field -> true);
		assertThat(fields).containsExactly(staticField);

		// Actual use cases for this test
		fields = findAnnotatedFields(subclass, ClassLevelDir.class, field -> true);
		assertThat(fields).containsExactly(staticField);
		fields = findAnnotatedFields(subclass, InstanceLevelDir.class, field -> true);
		assertThat(fields).containsExactly(nonStaticField);
	}

	// === findPublicAnnotatedFields() =========================================

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findPublicAnnotatedFieldsForNullClass() {
		assertPreconditionViolationFor(() -> findPublicAnnotatedFields(null, String.class, Annotation1.class));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findPublicAnnotatedFieldsForNullFieldType() {
		assertPreconditionViolationFor(() -> findPublicAnnotatedFields(getClass(), null, Annotation1.class));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void findPublicAnnotatedFieldsForNullAnnotationType() {
		assertPreconditionViolationFor(() -> findPublicAnnotatedFields(getClass(), String.class, null));
	}

	@Test
	void findPublicAnnotatedFieldsForPrivateField() {
		var fields = findPublicAnnotatedFields(getClass(), Boolean.class, Annotation1.class);
		assertNotNull(fields);
		assertEquals(0, fields.size());
	}

	@Test
	void findPublicAnnotatedFieldsForDirectlyAnnotatedFieldOfWrongFieldType() {
		var fields = findPublicAnnotatedFields(getClass(), BigDecimal.class, Annotation1.class);
		assertNotNull(fields);
		assertEquals(0, fields.size());
	}

	@Test
	void findPublicAnnotatedFieldsForDirectlyAnnotatedField() {
		var fields = findPublicAnnotatedFields(getClass(), String.class, Annotation1.class);
		assertNotNull(fields);
		assertIterableEquals(List.of("directlyAnnotatedField"), asNames(fields));
	}

	@Test
	void findPublicAnnotatedFieldsForMetaAnnotatedField() {
		var fields = findPublicAnnotatedFields(getClass(), Number.class, Annotation1.class);
		assertNotNull(fields);
		assertEquals(1, fields.size());
		assertIterableEquals(List.of("metaAnnotatedField"), asNames(fields));
	}

	@Test
	void findPublicAnnotatedFieldsForDirectlyAnnotatedFieldInInterface() {
		var fields = findPublicAnnotatedFields(InterfaceWithAnnotatedFields.class, String.class, Annotation1.class);
		assertNotNull(fields);
		assertIterableEquals(List.of("foo"), asNames(fields));
	}

	@Test
	void findPublicAnnotatedFieldsForDirectlyAnnotatedFieldsInClassAndInterface() {
		var fields = findPublicAnnotatedFields(ClassWithPublicAnnotatedFieldsFromInterface.class, String.class,
			Annotation1.class);
		assertNotNull(fields);
		assertThat(asNames(fields)).containsExactlyInAnyOrder("foo", "bar");
	}

	private List<String> asNames(List<Field> fields) {
		return fields.stream().map(Field::getName).toList();
	}

	// -------------------------------------------------------------------------

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnotationWithDefaultValue {

		String value() default "default";
	}

	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface Annotation1 {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface Annotation2 {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface Annotation3 {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface InheritedAnnotation {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@Annotation1
	@interface ComposedAnnotation {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Annotation1
	@Inherited
	@interface InheritedComposedAnnotation {
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@InheritedAnnotation
	// DO NOT make this @Inherited.
	@interface NonInheritedCompositionOfInheritedAnnotation {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	// DO NOT make this @Inherited.
	@interface Tags {

		Tag[] value();
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(Tags.class)
	// DO NOT make this @Inherited.
	@interface Tag {

		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Tag("fast")
	@interface Fast {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Tag("smoke")
	@interface Smoke {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Fast
	@Smoke
	@interface FastAndSmoky {
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Extensions {

		ExtendWith[] value();
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Repeatable(Extensions.class)
	@interface ExtendWith {

		String value();
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@ExtendWith("foo")
	@Repeatable(FooExtensions.class)
	@interface ExtendWithFoo {

		String info() default "";
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface FooExtensions {

		ExtendWithFoo[] value();
	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	// Intentionally NOT @Inherited in order to ensure that the algorithm for
	// findRepeatableAnnotations() in fact lives up to the claims in the
	// Javadoc regarding searching for repeatable annotations with implicit
	// "inheritance" if the repeatable annotation is @Inherited but the
	// custom composed annotation is not @Inherited.
	// @Inherited
	@ExtendWith("bar")
	@interface ExtendWithBar {
	}

	@AnnotationWithDefaultValue
	static class AnnotationWithDefaultValueClass {
	}

	@Annotation1
	static class Annotation1Class {
		class InnerClass {
			class InnerInnerClass {
			}
		}
		static class NestedClass {
		}
	}

	@Annotation2
	static class Annotation2Class {

		@Annotation1
		void method() {
		}
	}

	@InheritedAnnotation
	static class InheritedAnnotationClass {
	}

	static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
	}

	@ComposedAnnotation
	static class ComposedAnnotationClass {

		@ComposedAnnotation
		void method() {
		}

		class InnerClass {
			class InnerInnerClass {
			}
		}
	}

	@InheritedComposedAnnotation
	static class InheritedComposedAnnotationClass {

		@InheritedComposedAnnotation
		void method() {
		}
	}

	static class SubInheritedComposedAnnotationClass extends InheritedComposedAnnotationClass {
	}

	@NonInheritedCompositionOfInheritedAnnotation
	static class NonInheritedCompositionOfInheritedAnnotationClass {
	}

	static class SubNonInheritedCompositionOfInheritedAnnotationClass
			extends NonInheritedCompositionOfInheritedAnnotationClass {
	}

	@Annotation1
	interface TestingTrait {
	}

	static class TestingTraitClass implements TestingTrait {
	}

	@ComposedAnnotation
	interface ComposedTestingTrait {
	}

	static class ComposedTestingTraitClass implements ComposedTestingTrait {
	}

	@Tag("a")
	static class SingleTaggedClass {
	}

	@Fast
	static class SingleComposedTaggedClass {
	}

	@Tag("a")
	@Tag("b")
	@Tag("c")
	static class MultiTaggedClass {
	}

	@Fast
	@Smoke
	static class MultiComposedTaggedClass {
	}

	@FastAndSmoky
	static class FastAndSmokyTaggedClass {
	}

	@Fast
	interface TaggedInterface {
	}

	static class TaggedInterfaceClass implements TaggedInterface {
	}

	@Smoke
	static class LocalTagOnTaggedInterfaceClass implements TaggedInterface {
	}

	@Tags({ @Tag("a"), @Tag("b"), @Tag("c") })
	@Tag("d")
	static class ContainerTaggedClass {
	}

	@Fast
	@Tags({ @Tag("a"), @Tag("b"), @Tag("c") })
	static class ContainerAfterComposedTaggedClass {
	}

	@ExtendWith("a")
	static class SingleExtensionClass {
	}

	static class SubSingleExtensionClass extends SingleExtensionClass {
	}

	@ExtendWith("a")
	@ExtendWith("b")
	@ExtendWith("c")
	static class MultiExtensionClass {
	}

	@ExtendWith("x")
	@ExtendWith("y")
	@ExtendWith("b") // duplicates parent
	@ExtendWith("z")
	@ExtendWith("a") // duplicates parent
	static class SubMultiExtensionClass extends MultiExtensionClass {
	}

	@Extensions({ @ExtendWith("a"), @ExtendWith("b"), @ExtendWith("c"), @ExtendWith("a") })
	static class ContainerExtensionClass {
	}

	@ExtendWith("x")
	static class SubContainerExtensionClass extends ContainerExtensionClass {
	}

	@ExtendWithFoo
	static class SingleComposedExtensionClass {
	}

	@ExtendWithFoo(info = "A")
	@ExtendWithFoo(info = "B")
	static class MultipleFoos1 {
	}

	@FooExtensions({ @ExtendWithFoo(info = "A"), @ExtendWithFoo(info = "B") })
	static class MultipleFoos2 {
	}

	static class SubSingleComposedExtensionClass extends SingleComposedExtensionClass {
	}

	@ExtendWithFoo
	@ExtendWithBar
	static class MultiComposedExtensionClass {
	}

	static class SubMultiComposedExtensionClass extends MultiComposedExtensionClass {
	}

	@ExtendWith("x")
	@Extensions({ @ExtendWith("y"), @ExtendWith("z") })
	@ExtendWithBar
	static class ContainerPlusSubMultiComposedExtensionClass extends MultiComposedExtensionClass {
	}

	interface InterfaceWithAnnotatedDefaultMethod {

		@Annotation3
		default void interfaceMethod() {
		}
	}

	static class SuperclassWithAnnotatedMethod {

		@Annotation1
		void superMethod() {
		}
	}

	static class ClassWithAnnotatedMethods extends SuperclassWithAnnotatedMethod
			implements InterfaceWithAnnotatedDefaultMethod {

		@Annotation1
		void method1() {
		}

		@Annotation2
		void method2() {
		}

		@Annotation1
		@Annotation2
		void method3() {
		}
	}

	// -------------------------------------------------------------------------

	interface InterfaceWithAnnotatedField {

		@Annotation3
		String interfaceField = "interface";
	}

	static class SuperclassWithAnnotatedField {

		@Annotation1
		String superField = "super";
	}

	static class ClassWithAnnotatedFields extends SuperclassWithAnnotatedField implements InterfaceWithAnnotatedField {

		@Annotation1
		protected Object field0 = "?";

		@Annotation1
		protected String field1 = "foo";

		@Annotation2
		protected String field2 = "bar";

		@Annotation1
		@Annotation2
		protected String field3 = "baz";

	}

	static class ClassWithShadowedAnnotatedFields extends ClassWithAnnotatedFields {

		@Annotation3
		String interfaceField = "interface-shadow";

		@Annotation1
		String superField = "super-shadow";

		@Annotation1
		protected String field1 = "foo-shadow";

		@Annotation2
		protected String field2 = "bar-shadow";

		@Annotation1
		@Annotation2
		protected String field3 = "baz-shadow";

	}

	// -------------------------------------------------------------------------

	@Annotation1
	private @Nullable Boolean privateDirectlyAnnotatedField;

	@Annotation1
	public @Nullable String directlyAnnotatedField;

	@ComposedAnnotation
	public @Nullable Integer metaAnnotatedField;

	interface InterfaceWithAnnotatedFields {

		@Annotation1
		String foo = "bar";

		@Annotation1
		boolean wrongType = false;
	}

	class ClassWithPublicAnnotatedFieldsFromInterface implements InterfaceWithAnnotatedFields {

		@Annotation1
		public String bar = "baz";

		@Annotation1
		public boolean notAString = true;
	}

}
