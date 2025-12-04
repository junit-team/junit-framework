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
import static org.junit.platform.commons.util.ReflectionUtils.getDeclaredConstructor;

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

	private static final IsFactoryMethod isBookFactoryMethod = new IsFactoryMethod(Book.class, String.class);

	private static final FallbackStringToObjectConverter converter = new FallbackStringToObjectConverter();

	@Test
	void isNotFactoryMethodForWrongParameterType() {
		assertThat(isBookFactoryMethod).rejects(bookMethod("factory", Object.class));
		assertThat(isBookFactoryMethod).rejects(bookMethod("factory", Number.class));
		assertThat(isBookFactoryMethod).rejects(bookMethod("factory", StringBuilder.class));
		assertThat(new IsFactoryMethod(Record2.class, String.class)).rejects(record2Method("from"));
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
		assertThat(new IsFactoryMethod(Book.class, String.class))//
				.accepts(bookMethod("factory", String.class));
		assertThat(new IsFactoryMethod(Book.class, CharSequence.class))//
				.accepts(bookMethod("factory", CharSequence.class));
		assertThat(new IsFactoryMethod(Newspaper.class, String.class))//
				.accepts(newspaperMethod("from"), newspaperMethod("of"));
		assertThat(new IsFactoryMethod(Magazine.class, String.class))//
				.accepts(magazineMethod("from"), magazineMethod("of"));
		assertThat(new IsFactoryMethod(Record2.class, CharSequence.class))//
				.accepts(record2Method("from"));
	}

	@Test
	void isNotFactoryConstructorForPrivateConstructor() {
		assertThat(new IsFactoryConstructor(Magazine.class, String.class)).rejects(constructor(Magazine.class));
	}

	@Test
	void isNotFactoryConstructorForWrongParameterType() {
		assertThat(new IsFactoryConstructor(Record1.class, String.class))//
				.rejects(getDeclaredConstructor(Record1.class));
		assertThat(new IsFactoryConstructor(Record2.class, String.class))//
				.rejects(getDeclaredConstructor(Record2.class));
	}

	@Test
	void isFactoryConstructorForValidConstructors() {
		assertThat(new IsFactoryConstructor(Book.class, String.class))//
				.accepts(constructor(Book.class));
		assertThat(new IsFactoryConstructor(Journal.class, String.class))//
				.accepts(constructor(Journal.class));
		assertThat(new IsFactoryConstructor(Newspaper.class, String.class))//
				.accepts(constructor(Newspaper.class));
		assertThat(new IsFactoryConstructor(Record1.class, CharSequence.class))//
				.accepts(getDeclaredConstructor(Record1.class));
		assertThat(new IsFactoryConstructor(Record2.class, CharSequence.class))//
				.accepts(getDeclaredConstructor(Record2.class));
	}

	@Test
	void convertsStringToBookViaStaticFactoryMethod() throws Exception {
		assertConverts("enigma", Book.class, new Book("factory(String): enigma"));
	}

	@Test
	void convertsStringToRecord2ViaStaticFactoryMethodAcceptingCharSequence() throws Exception {
		assertConvertsRecord2("enigma", Record2.from(new StringBuffer("enigma")));
	}

	@Test
	void convertsStringToJournalViaFactoryConstructor() throws Exception {
		assertConverts("enigma", Journal.class, new Journal("enigma"));
	}

	@Test
	void convertsStringToRecord1ViaFactoryConstructorAcceptingCharSequence() throws Exception {
		assertConvertsRecord1("enigma", new Record1(new StringBuffer("enigma")));
	}

	@Test
	void convertsStringToNewspaperViaConstructorIgnoringMultipleFactoryMethods() throws Exception {
		assertConverts("enigma", Newspaper.class, new Newspaper("enigma"));
	}

	@Test
	@DisplayName("Cannot convert String to Diary because Diary has neither a static factory method nor a factory constructor")
	void cannotConvertStringToDiary() {
		assertThat(converter.canConvertTo(Diary.class)).isFalse();
	}

	@Test
	@DisplayName("Cannot convert String to Magazine because Magazine has multiple static factory methods")
	void cannotConvertStringToMagazine() {
		assertThat(converter.canConvertTo(Magazine.class)).isFalse();
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

	private static Method record2Method(String methodName) {
		return findMethod(Record2.class, methodName, CharSequence.class).orElseThrow();
	}

	private static void assertConverts(String input, Class<?> targetType, Object expectedOutput) throws Exception {
		assertCanConvertTo(targetType);

		var result = converter.convert(input, targetType);

		assertThat(result) //
				.as(input + " (" + targetType.getSimpleName() + ") --> " + expectedOutput) //
				.isEqualTo(expectedOutput);
	}

	private static void assertConvertsRecord1(String input, Record1 expected) throws Exception {
		Class<?> targetType = Record1.class;
		assertCanConvertTo(targetType);

		Record1 result = (Record1) converter.convert(input, targetType);

		assertThat(result).isNotNull();
		assertThat(result.title.toString()).isEqualTo(expected.title.toString());
	}

	private static void assertConvertsRecord2(String input, Record2 expected) throws Exception {
		Class<?> targetType = Record2.class;
		assertCanConvertTo(targetType);

		var result = converter.convert(input, targetType);

		assertThat(result).isEqualTo(expected);
	}

	private static void assertCanConvertTo(Class<?> targetType) {
		assertThat(converter.canConvertTo(targetType)).as("canConvertTo(%s)", targetType.getSimpleName()).isTrue();
	}

	static class Book {

		private final String title;

		Book(String title) {
			this.title = title;
		}

		// static and non-private
		static Book factory(String title) {
			return new Book("factory(String): " + title);
		}

		/**
		 * Static and non-private, but intentionally overloads {@link #factory(String)}
		 * with a {@link CharSequence} argument to ensure that we don't introduce a
		 * regression in 6.0, since the String-based factory method should take
		 * precedence over a CharSequence-based factory method.
		 */
		static Book factory(CharSequence title) {
			return new Book("factory(CharSequence): " + title);
		}

		// wrong parameter type
		static Book factory(Object obj) {
			throw new UnsupportedOperationException();
		}

		// wrong parameter type
		static Book factory(Number number) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Wrong parameter type, intentionally a subtype of {@link CharSequence}
		 * other than {@link String}.
		 */
		static Book factory(StringBuilder builder) {
			throw new UnsupportedOperationException();
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

		@Override
		public String toString() {
			return "Book [title=" + this.title + "]";
		}
	}

	static class Journal {

		private final String title;

		Journal(String title) {
			this.title = title;
		}

		/**
		 * Intentionally overloads {@link #Journal(String)} with a {@link CharSequence}
		 * argument to ensure that we don't introduce a regression in 6.0, since the
		 * String-based constructor should take precedence over a CharSequence-based
		 * constructor.
		 */
		Journal(CharSequence title) {
			this("Journal(CharSequence): " + title);
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj) || (obj instanceof Journal that && Objects.equals(this.title, that.title));
		}

		@Override
		public int hashCode() {
			return Objects.hash(title);
		}

		@Override
		public String toString() {
			return "Journal [title=" + this.title + "]";
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

		@Override
		public String toString() {
			return "Newspaper [title=" + this.title + "]";
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

	record Record1(CharSequence title) {
	}

	record Record2(CharSequence title) {

		static Record2 from(CharSequence title) {
			return new Record2("Record2(CharSequence): " + title);
		}
	}

	static class Diary {
	}

}
