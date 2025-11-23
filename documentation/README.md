# JUnit 5 User Guide

This subproject contains the Antora/AsciiDoc sources for the JUnit 5 User Guide.

## Structure

- `modules/ROOT/pages`: AsciiDoc files
- `src/test/java`: Java test source code that can be included in the AsciiDoc files
- `src/test/kotlin`: Kotlin test source code that can be included in the AsciiDoc files
- `src/test/resources`: Classpath resources that can be included in the AsciiDoc files or
  used in tests

## Usage

### Generate Antora site

This following Gradle command generates the HTML version of the User Guide as
`build/antora/build/site`.

```
gradlew antora
```

On Linux operating systems, the `graphviz` package providing `/usr/bin/dot` must be
installed in order to generate the PlantUML images used in the User Guide.
