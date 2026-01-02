package junitbuild.javadoc

import org.gradle.api.provider.Provider
import org.gradle.external.javadoc.JavadocOptionFileOption
import org.gradle.external.javadoc.internal.JavadocOptionFileWriterContext

class JavadocValuesOption(private val option: String, private var values: Provider<List<String>>) :
    JavadocOptionFileOption<Provider<List<String>>> {

    override fun getOption() = option

    override fun getValue() = values

    override fun setValue(value: Provider<List<String>>) {
        this.values = value
    }

    override fun write(writerContext: JavadocOptionFileWriterContext) {
        writerContext.writeValuesOption(option, values.get(), ",")
    }
}
