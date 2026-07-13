/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

import example.domain.Person
import example.domain.Person.Gender
import example.util.StringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.api.extension.AnnotatedElementContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.params.ArgumentCountValidationMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.AggregateWith
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.SimpleArgumentsAggregator
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.converter.JavaTimeConversionPattern
import org.junit.jupiter.params.converter.SimpleArgumentConverter
import org.junit.jupiter.params.converter.TypedArgumentConverter
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.CsvFileSource
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.EnumSource.Mode.MATCH_ALL
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.support.ParameterDeclarations
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.EnumSet
import java.util.function.Supplier
import java.util.stream.IntStream
import java.util.stream.Stream

@Execution(SAME_THREAD)
class ParameterizedTestDemo {
    @BeforeEach
    fun printDisplayName(testInfo: TestInfo) {
        println(testInfo.displayName)
    }

    // tag::first_example[]
    @ParameterizedTest
    @ValueSource(strings = ["racecar", "radar", "able was I ere I saw elba"])
    fun palindromes(candidate: String) {
        assertTrue(StringUtils.isPalindrome(candidate))
    }
    // end::first_example[]

    // tag::ValueSource_example[]
    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun testWithValueSource(argument: Int) {
        assertTrue(argument in 1..<4)
    }
    // end::ValueSource_example[]

    @Nested
    inner class NullAndEmptySource1 {
        // tag::NullAndEmptySource_example1[]
        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = [" ", "   ", "\t", "\n"])
        fun nullEmptyAndBlankStrings(text: String?) {
            assertTrue(text.isNullOrBlank())
        }
        // end::NullAndEmptySource_example1[]
    }

    @Nested
    inner class NullAndEmptySource2 {
        // tag::NullAndEmptySource_example2[]
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = [" ", "   ", "\t", "\n"])
        fun nullEmptyAndBlankStrings(text: String?) {
            assertTrue(text.isNullOrBlank())
        }
        // end::NullAndEmptySource_example2[]
    }

    // tag::EnumSource_example[]
    @ParameterizedTest
    @EnumSource(ChronoUnit::class)
    fun testWithEnumSource(unit: TemporalUnit) {
        assertNotNull(unit)
    }
    // end::EnumSource_example[]

    // tag::EnumSource_example_autodetection[]
    @ParameterizedTest
    @EnumSource
    fun testWithEnumSourceWithAutoDetection(unit: ChronoUnit) {
        assertNotNull(unit)
    }
    // end::EnumSource_example_autodetection[]

    // tag::EnumSource_include_example[]
    @ParameterizedTest
    @EnumSource(names = ["DAYS", "HOURS"])
    fun testWithEnumSourceInclude(unit: ChronoUnit) {
        assertTrue(EnumSet.of(ChronoUnit.DAYS, ChronoUnit.HOURS).contains(unit))
    }
    // end::EnumSource_include_example[]

    // tag::EnumSource_range_example[]
    @ParameterizedTest
    @EnumSource(from = "HOURS", to = "DAYS")
    fun testWithEnumSourceRange(unit: ChronoUnit) {
        assertTrue(EnumSet.of(ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, ChronoUnit.DAYS).contains(unit))
    }
    // end::EnumSource_range_example[]

    // tag::EnumSource_exclude_example[]
    @ParameterizedTest
    @EnumSource(mode = EXCLUDE, names = ["ERAS", "FOREVER"])
    fun testWithEnumSourceExclude(unit: ChronoUnit) {
        assertFalse(EnumSet.of(ChronoUnit.ERAS, ChronoUnit.FOREVER).contains(unit))
    }
    // end::EnumSource_exclude_example[]

    // tag::EnumSource_regex_example[]
    @ParameterizedTest
    @EnumSource(mode = MATCH_ALL, names = ["^.*DAYS$"])
    fun testWithEnumSourceRegex(unit: ChronoUnit) {
        assertTrue(unit.name.endsWith("DAYS"))
    }
    // end::EnumSource_regex_example[]

    // tag::EnumSource_range_exclude_example[]
    @ParameterizedTest
    @EnumSource(from = "HOURS", to = "DAYS", mode = EXCLUDE, names = ["HALF_DAYS"])
    fun testWithEnumSourceRangeExclude(unit: ChronoUnit) {
        assertTrue(EnumSet.of(ChronoUnit.HOURS, ChronoUnit.DAYS).contains(unit))
        assertFalse(EnumSet.of(ChronoUnit.HALF_DAYS).contains(unit))
    }
    // end::EnumSource_range_exclude_example[]

    // tag::simple_MethodSource_example[]
    @ParameterizedTest
    @MethodSource("stringProvider")
    fun testWithExplicitLocalMethodSource(argument: String) {
        assertNotNull(argument)
    }
    // end::simple_MethodSource_example[]

    // tag::simple_MethodSource_without_value_example[]
    @ParameterizedTest
    @MethodSource
    fun testWithDefaultLocalMethodSource(argument: String) {
        assertNotNull(argument)
    }
    // end::simple_MethodSource_without_value_example[]

    // tag::primitive_MethodSource_example[]
    @ParameterizedTest
    @MethodSource("range")
    fun testWithRangeMethodSource(argument: Int) {
        assertNotEquals(9, argument)
    }
    // end::primitive_MethodSource_example[]

    // tag::multi_arg_MethodSource_example[]
    @ParameterizedTest
    @MethodSource("stringIntAndListProvider")
    fun testWithMultiArgMethodSource(
        str: String,
        num: Int,
        list: List<String>
    ) {
        assertEquals(5, str.length)
        assertTrue(num in 1..2)
        assertEquals(2, list.size)
    }
    // end::multi_arg_MethodSource_example[]

    // tag::default_field_FieldSource_example[]
    @ParameterizedTest
    @FieldSource
    fun arrayOfFruits(fruit: String) {
        assertFruit(fruit)
    }
    // end::default_field_FieldSource_example[]

    // tag::explicit_field_FieldSource_example[]
    @ParameterizedTest
    @FieldSource("listOfFruits")
    fun singleFieldSource(fruit: String) {
        assertFruit(fruit)
    }
    // end::explicit_field_FieldSource_example[]

    // tag::multiple_fields_FieldSource_example[]
    @ParameterizedTest
    @FieldSource("listOfFruits", "additionalFruits")
    fun multipleFieldSources(fruit: String) {
        assertFruit(fruit)
    }
    // end::multiple_fields_FieldSource_example[]

    // tag::named_arguments_FieldSource_example[]
    @ParameterizedTest
    @FieldSource
    fun namedArgumentsSupplier(fruit: String) {
        assertFruit(fruit)
    }
    // end::named_arguments_FieldSource_example[]

    private fun assertFruit(fruit: String) {
        assertTrue(listOf("apple", "banana", "cherry", "dewberry").contains(fruit))
    }

    // tag::multi_arg_FieldSource_example[]
    @ParameterizedTest
    @FieldSource("stringIntAndListArguments")
    fun testWithMultiArgFieldSource(
        str: String,
        num: Int,
        list: List<String>
    ) {
        assertEquals(5, str.length)
        assertTrue(num in 1..2)
        assertEquals(2, list.size)
    }
    // end::multi_arg_FieldSource_example[]

    // tag::CsvSource_example[]
    @ParameterizedTest
    @CsvSource(
        "apple,         1",
        "banana,        2",
        "'lemon, lime', 0xF1",
        "strawberry,    700_000"
    )
    fun testWithCsvSource(
        fruit: String,
        rank: Int
    ) {
        assertNotNull(fruit)
        assertNotEquals(0, rank)
    }
    // end::CsvSource_example[]

    // tag::CsvFileSource_example[]
    @ParameterizedTest
    @CsvFileSource(resources = ["/two-column.csv"], numLinesToSkip = 1)
    fun testWithCsvFileSourceFromClasspath(
        country: String,
        reference: Int
    ) {
        assertNotNull(country)
        assertNotEquals(0, reference)
    }

    @ParameterizedTest
    @CsvFileSource(files = ["src/test/resources/two-column.csv"], numLinesToSkip = 1)
    fun testWithCsvFileSourceFromFile(
        country: String,
        reference: Int
    ) {
        assertNotNull(country)
        assertNotEquals(0, reference)
    }

    @ParameterizedTest
    @CsvFileSource(resources = ["/two-column.csv"], useHeadersInDisplayName = true)
    fun testWithCsvFileSourceAndHeaders(
        country: String,
        reference: Int
    ) {
        assertNotNull(country)
        assertNotEquals(0, reference)
    }
    // end::CsvFileSource_example[]

    // tag::ArgumentsSource_example[]
    @ParameterizedTest
    @ArgumentsSource(MyArgumentsProvider::class)
    fun testWithArgumentsSource(argument: String) {
        assertNotNull(argument)
    }

    // end::ArgumentsSource_example[]
    // tag::ArgumentsProvider_example[]
    class MyArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext
        ): Stream<out Arguments> = Stream.of("apple", "banana").map(Arguments::of)
    }
    // end::ArgumentsProvider_example[]

    @ParameterizedTest
    @ArgumentsSource(MyArgumentsProviderWithConstructorInjection::class)
    fun testWithArgumentsSourceWithConstructorInjection(argument: String) {
        assertNotNull(argument)
    }

    // tag::ArgumentsProviderWithConstructorInjection_example[]
    class MyArgumentsProviderWithConstructorInjection(
        private val testInfo: TestInfo
    ) : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext
        ): Stream<out Arguments> = Stream.of(arguments(testInfo.displayName))
    }
    // end::ArgumentsProviderWithConstructorInjection_example[]

    // tag::ParameterResolver_example[]
    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        // ...
    }

    @ParameterizedTest
    @ValueSource(strings = ["apple"])
    fun testWithRegularParameterResolver(
        argument: String,
        testReporter: TestReporter
    ) {
        testReporter.publishEntry("argument", argument)
    }

    @AfterEach
    fun afterEach(testInfo: TestInfo) {
        // ...
    }
    // end::ParameterResolver_example[]

    // tag::implicit_conversion_example[]
    @ParameterizedTest
    @ValueSource(strings = ["SECONDS"])
    fun testWithImplicitArgumentConversion(argument: ChronoUnit) {
        assertNotNull(argument.name)
    }
    // end::implicit_conversion_example[]

    // tag::implicit_fallback_conversion_example[]
    @ParameterizedTest
    @ValueSource(strings = ["42 Cats"])
    fun testWithImplicitFallbackArgumentConversion(book: Book) {
        assertEquals("42 Cats", book.title)
    }

    // end::implicit_fallback_conversion_example[]
    // tag::implicit_fallback_conversion_example_Book[]
    class Book private constructor(
        val title: String
    ) {
        companion object {
            @JvmStatic
            fun fromTitle(title: String): Book = Book(title)
        }
    }
    // end::implicit_fallback_conversion_example_Book[]

    // tag::explicit_conversion_example[]
    @ParameterizedTest
    @EnumSource(ChronoUnit::class)
    fun testWithExplicitArgumentConversion(
        @ConvertWith(ToStringArgumentConverter::class) argument: String
    ) {
        assertNotNull(ChronoUnit.valueOf(argument))
    }

    // end::explicit_conversion_example[]
    // tag::explicit_conversion_example_ToStringArgumentConverter[]
    class ToStringArgumentConverter : SimpleArgumentConverter() {
        override fun convert(
            source: Any?,
            targetType: Class<*>
        ): Any {
            assertEquals(String::class.java, targetType, "Can only convert to String")
            return if (source is Enum<*>) source.name else source.toString()
        }
    }
    // end::explicit_conversion_example_ToStringArgumentConverter[]

    // tag::explicit_conversion_example_TypedArgumentConverter[]
    class ToLengthArgumentConverter :
        TypedArgumentConverter<String, Int>(
            String::class.java,
            Int::class.javaObjectType
        ) {
        override fun convert(source: String?): Int = source?.length ?: 0
    }
    // end::explicit_conversion_example_TypedArgumentConverter[]

    // tag::explicit_java_time_converter[]
    @ParameterizedTest
    @ValueSource(strings = ["01.01.2017", "31.12.2017"])
    fun testWithExplicitJavaTimeConverter(
        @JavaTimeConversionPattern("dd.MM.yyyy") argument: LocalDate
    ) {
        assertEquals(2017, argument.year)
    }
    // end::explicit_java_time_converter[]

    // tag::ArgumentsAccessor_example[]
    @ParameterizedTest
    @CsvSource(
        "Jane, Doe, F, 1990-05-20",
        "John, Doe, M, 1990-10-22"
    )
    fun testWithArgumentsAccessor(arguments: ArgumentsAccessor) {
        val person =
            Person(
                arguments.getString(0),
                arguments.getString(1),
                arguments.get(2, Gender::class.java),
                arguments.get(3, LocalDate::class.java)
            )

        if (person.firstName == "Jane") {
            assertEquals(Gender.F, person.gender)
        } else {
            assertEquals(Gender.M, person.gender)
        }
        assertEquals("Doe", person.lastName)
        assertEquals(1990, person.dateOfBirth.year)
    }
    // end::ArgumentsAccessor_example[]

    // tag::ArgumentsAggregator_example[]
    @ParameterizedTest
    @CsvSource(
        "Jane, Doe, F, 1990-05-20",
        "John, Doe, M, 1990-10-22"
    )
    fun testWithArgumentsAggregator(
        @AggregateWith(PersonAggregator::class) person: Person
    ) {
        // perform assertions against person
    }

    // end::ArgumentsAggregator_example[]
    // tag::ArgumentsAggregator_example_PersonAggregator[]
    class PersonAggregator : SimpleArgumentsAggregator() {
        override fun aggregateArguments(
            arguments: ArgumentsAccessor,
            targetType: Class<*>,
            context: AnnotatedElementContext,
            parameterIndex: Int
        ): Person =
            Person(
                arguments.getString(0),
                arguments.getString(1),
                arguments.get(2, Gender::class.java),
                arguments.get(3, LocalDate::class.java)
            )
    }
    // end::ArgumentsAggregator_example_PersonAggregator[]

    // tag::ArgumentsAggregator_with_custom_annotation_example[]
    @ParameterizedTest
    @CsvSource(
        "Jane, Doe, F, 1990-05-20",
        "John, Doe, M, 1990-10-22"
    )
    fun testWithCustomAggregatorAnnotation(
        @CsvToPerson person: Person
    ) {
        // perform assertions against person
    }
    // end::ArgumentsAggregator_with_custom_annotation_example[]

    // tag::ArgumentsAggregator_with_custom_annotation_example_CsvToPerson[]
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @AggregateWith(PersonAggregator::class)
    annotation class CsvToPerson
    // end::ArgumentsAggregator_with_custom_annotation_example_CsvToPerson[]

    // tag::custom_display_names[]
    @DisplayName("Display name of container")
    @ParameterizedTest(name = "{index} ==> the rank of {0} is {1}")
    @CsvSource("apple, 1", "banana, 2", "'lemon, lime', 3")
    fun testWithCustomDisplayNames(
        fruit: String,
        rank: Int
    ) {
    }
    // end::custom_display_names[]

    // tag::named_arguments[]
    @DisplayName("A parameterized test with named arguments")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("namedArguments")
    fun testWithNamedArguments(file: File) {
    }
    // end::named_arguments[]

    // tag::named_argument_set[]
    @DisplayName("A parameterized test with named argument sets")
    @ParameterizedTest
    @FieldSource("argumentSets")
    fun testWithArgumentSets(
        file1: File,
        file2: File
    ) {
    }
    // end::named_argument_set[]

    // tag::repeatable_annotations[]
    @DisplayName("A parameterized test that makes use of repeatable annotations")
    @ParameterizedTest
    @MethodSource("someProvider")
    @MethodSource("otherProvider")
    fun testWithRepeatedAnnotation(argument: String) {
        assertNotNull(argument)
    }
    // end::repeatable_annotations[]

    @Disabled("Fails prior to invoking the test method")
    // tag::argument_count_validation[]
    @ParameterizedTest(argumentCountValidation = ArgumentCountValidationMode.STRICT)
    @CsvSource("42, -666")
    fun testWithArgumentCountValidation(number: Int) {
        assertTrue(number > 0)
    }
    // end::argument_count_validation[]
}

// tag::stringProvider[]
fun stringProvider(): Stream<String> = Stream.of("apple", "banana")
// end::stringProvider[]

// tag::testWithDefaultLocalMethodSource_provider[]
fun testWithDefaultLocalMethodSource(): Stream<String> = Stream.of("apple", "banana")
// end::testWithDefaultLocalMethodSource_provider[]

// tag::range[]
fun range(): IntStream = IntStream.range(0, 20).skip(10)
// end::range[]

// tag::stringIntAndListProvider[]
fun stringIntAndListProvider(): Stream<Arguments> =
    Stream.of(
        arguments("apple", 1, listOf("a", "b")),
        arguments("lemon", 2, listOf("x", "y"))
    )
// end::stringIntAndListProvider[]

// tag::arrayOfFruits[]
val arrayOfFruits = arrayOf("apple", "banana")
// end::arrayOfFruits[]

// tag::listOfFruits[]
val listOfFruits = listOf("apple", "banana")
// end::listOfFruits[]

// tag::additionalFruits[]
val additionalFruits = listOf("cherry", "dewberry")
// end::additionalFruits[]

// tag::namedArgumentsSupplier[]
val namedArgumentsSupplier: Supplier<Stream<Arguments>> =
    Supplier {
        Stream.of(
            arguments(named("Apple", "apple")),
            arguments(named("Banana", "banana"))
        )
    }
// end::namedArgumentsSupplier[]

// tag::stringIntAndListArguments[]
var stringIntAndListArguments =
    listOf(
        arguments("apple", 1, listOf("a", "b")),
        arguments("lemon", 2, listOf("x", "y"))
    )
// end::stringIntAndListArguments[]

// tag::namedArguments[]
fun namedArguments(): Stream<Arguments> =
    Stream.of(
        arguments(named("An important file", File("path1"))),
        arguments(named("Another file", File("path2")))
    )
// end::namedArguments[]

// tag::argumentSets[]
val argumentSets =
    listOf(
        Arguments.argumentSet("Important files", File("path1"), File("path2")),
        Arguments.argumentSet("Other files", File("path3"), File("path4"))
    )
// end::argumentSets[]

// tag::someProvider[]
fun someProvider(): Stream<String> = Stream.of("foo")
// end::someProvider[]

// tag::otherProvider[]
fun otherProvider(): Stream<String> = Stream.of("bar")
// end::otherProvider[]
