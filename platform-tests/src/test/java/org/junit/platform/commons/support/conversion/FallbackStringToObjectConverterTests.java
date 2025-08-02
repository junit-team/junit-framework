/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.support.ReflectionSupport.findMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.conversion.FallbackStringToObjectConverter.IsFactoryConstructor;
import org.junit.platform.commons.support.conversion.FallbackStringToObjectConverter.IsFactoryMethod;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * Unit tests for {@link FallbackStringToObjectConverter}, {@link IsFactoryMethod},
 * and {@link IsFactoryConstructor}.
 *
 * @since 1.11 (originally since JUnit Jupiter 5.1)
 */
class FallbackStringToObjectConverterTests {

	private static final IsFactoryMethod isBookFactoryMethod = new IsFactoryMethod(Book.class);

	private static final FallbackStringToObjectConverter converter = new FallbackStringToObjectConverter();

	@Test
	void isNotFactoryMethodForWrongParameterType() {
		assertThat(isBookFactoryMethod).rejects(bookMethod("factory", Object.class));
	}

	@Test
	void isNotFactoryMethodForPrivateMethod() {
		assertThat(isBookFactoryMethod).rejects(bookMethod("privateFactory"));
	}

	@Test
	void isNotFactoryMethodForNonStaticMethod() {
		assertThat(isBookFactoryMethod).rejects(bookMethod("nonStaticFactory"));
	}

	@Test
	void isFactoryMethodForValidMethods() {
		assertThat(isBookFactoryMethod).accepts(bookMethod("factory"));
		assertThat(new IsFactoryMethod(Newspaper.class)).accepts(newspaperMethod("from"), newspaperMethod("of"));
		assertThat(new IsFactoryMethod(Magazine.class)).accepts(magazineMethod("from"), magazineMethod("of"));
	}

	@Test
	void isNotFactoryConstructorForPrivateConstructor() {
		assertThat(new IsFactoryConstructor(Magazine.class)).rejects(constructor(Magazine.class));
	}

	@Test
	void isFactoryConstructorForValidConstructors() {
		assertThat(new IsFactoryConstructor(Book.class)).accepts(constructor(Book.class));
		assertThat(new IsFactoryConstructor(Journal.class)).accepts(constructor(Journal.class));
		assertThat(new IsFactoryConstructor(Newspaper.class)).accepts(constructor(Newspaper.class));
	}

	@Test
	void convertsStringToBookViaStaticFactoryMethod() throws Exception {
		assertConverts("enigma", Book.class, Book.factory("enigma"));
	}

	@Test
	void convertsStringToJournalViaFactoryConstructor() throws Exception {
		assertConverts("enigma", Journal.class, new Journal("enigma"));
	}

	@Test
	void convertsStringToNewspaperViaConstructorIgnoringMultipleFactoryMethods() throws Exception {
		assertConverts("enigma", Newspaper.class, new Newspaper("enigma"));
	}

	@Test
	@DisplayName("Cannot convert String to Diary because Diary has neither a static factory method nor a factory constructor")
	void cannotConvertStringToDiary() {
		assertThat(converter.canConvert(Diary.class)).isFalse();
	}

	@Test
	@DisplayName("Cannot convert String to Magazine because Magazine has multiple static factory methods")
	void cannotConvertStringToMagazine() {
		assertThat(converter.canConvert(Magazine.class)).isFalse();
	}

	// -------------------------------------------------------------------------

	private static Constructor<?> constructor(Class<?> clazz) {
		return ReflectionUtils.findConstructors(clazz,
			ctr -> ctr.getParameterCount() == 1 && ctr.getParameterTypes()[0] == String.class).getFirst();
	}

	private static Method bookMethod(String methodName) {
		return bookMethod(methodName, String.class);
	}

	private static Method bookMethod(String methodName, Class<?> parameterType) {
		return findMethod(Book.class, methodName, parameterType).orElseThrow();
	}

	private static Method newspaperMethod(String methodName) {
		return findMethod(Newspaper.class, methodName, String.class).orElseThrow();
	}

	private static Method magazineMethod(String methodName) {
		return findMethod(Magazine.class, methodName, String.class).orElseThrow();
	}

	private static void assertConverts(String input, Class<?> targetType, Object expectedOutput) throws Exception {
		assertThat(converter.canConvert(targetType)).isTrue();

		var result = converter.convert(input, targetType);

		assertThat(result) //
				.describedAs(input + " --(" + targetType.getName() + ")--> " + expectedOutput) //
				.isEqualTo(expectedOutput);
	}

	static class Book {

		private final String title;

		Book(String title) {
			this.title = title;
		}

		// static and non-private
		static Book factory(String title) {
			return new Book(title);
		}

		// wrong parameter type
		static Book factory(Object obj) {
			return new Book(String.valueOf(obj));
		}

		@SuppressWarnings("unused")
		private static Book privateFactory(String title) {
			return new Book(title);
		}

		Book nonStaticFactory(String title) {
			return new Book(title);
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj) || (obj instanceof Book that && Objects.equals(this.title, that.title));
		}

		@Override
		public int hashCode() {
			return Objects.hash(title);
		}

	}

	static class Journal {

		private final String title;

		Journal(String title) {
			this.title = title;
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj) || (obj instanceof Journal that && Objects.equals(this.title, that.title));
		}

		@Override
		public int hashCode() {
			return Objects.hash(title);
		}

	}

	static class Newspaper {

		private final String title;

		Newspaper(String title) {
			this.title = title;
		}

		static Newspaper from(String title) {
			return new Newspaper(title);
		}

		static Newspaper of(String title) {
			return new Newspaper(title);
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj) || (obj instanceof Newspaper that && Objects.equals(this.title, that.title));
		}

		@Override
		public int hashCode() {
			return Objects.hash(title);
		}

	}

	static class Magazine {

		private Magazine(String title) {
		}

		static Magazine from(String title) {
			return new Magazine(title);
		}

		static Magazine of(String title) {
			return new Magazine(title);
		}

	}

	static class Diary {
	}

}
