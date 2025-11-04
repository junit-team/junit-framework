# <img src="https://junit.org/assets/img/junit6-temporary-logo.png" align="right" width="100">JUnit

This repository is the home of JUnit Platform, Jupiter, and Vintage.

## Sponsors

[![Support JUnit](https://img.shields.io/badge/%F0%9F%92%9A-Support%20JUnit-brightgreen.svg)](https://junit.org/sponsoring)

* **Gold Sponsors:** [JetBrains](https://jb.gg/junit-logo), [Netflix](https://www.netflix.com/)
* **Silver Sponsors:** [Micromata](https://www.micromata.de), [Quo Card](https://quo-digital.jp)
* **Bronze Sponsors:** [Premium Minds](https://www.premium-minds.com), [codefortynine](https://codefortynine.com), [Info Support](https://www.infosupport.com), [Code Intelligence](https://www.code-intelligence.com), [Route4Me](https://route4me.com/), [Testiny](https://www.testiny.io/), [LambdaTest](https://www.lambdatest.com)

## Latest Releases

- General Availability (GA): [JUnit 6.0.1](https://github.com/junit-team/junit-framework/releases/tag/r6.0.1) (October 31, 2025)
- Preview (Milestone/Release Candidate): N/A

## Documentation

- [User Guide]
- [Javadoc]
- [Release Notes]
- [Examples]

## Contributing

Contributions to JUnit are both welcomed and appreciated. For specific guidelines
regarding contributions, please see [CONTRIBUTING.md] in the root directory of the
project. Those willing to use milestone or SNAPSHOT releases are encouraged
to file feature requests and bug reports using the project's
[issue tracker](https://github.com/junit-team/junit-framework/issues). Issues marked with an
<a href="https://github.com/junit-team/junit-framework/issues?q=is%3Aissue+is%3Aopen+label%3Aup-for-grabs">`up-for-grabs`</a>
label are specifically targeted for community contributions.

## Getting Help

Ask JUnit-related questions on [StackOverflow] or use the Q&A category on [GitHub Discussions].

## Continuous Integration Builds

[![CI](https://github.com/junit-team/junit-framework/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/junit-team/junit-framework/actions/workflows/main.yml) [![Cross-Version](https://github.com/junit-team/junit-framework/actions/workflows/cross-version.yml/badge.svg?branch=main)](https://github.com/junit-team/junit-framework/actions/workflows/cross-version.yml)

Official CI build server used to perform quick checks on submitted pull requests and for
build matrices including the latest released OpenJDK and early access builds of the next
OpenJDK.

## Code Coverage

Code coverage using [JaCoCo] for the latest build is available on [Codecov].

A code coverage report can also be generated locally via the [Gradle Wrapper] by
executing `./gradlew clean jacocoRootReport`. The results will be available
in `build/reports/jacoco/jacocoRootReport/html/index.html`.

## Develocity

[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.junit.org/scans)

JUnit utilizes [Develocity](https://gradle.com/) for [Build Scans](https://scans.gradle.com/),
[Build Cache](https://docs.gradle.org/current/userguide/build_cache.html), and
[Predictive Test Selection](https://docs.gradle.com/enterprise/predictive-test-selection/).

The latest Build Scans are available on [ge.junit.org](https://ge.junit.org/). Currently,
only core team members can publish Build Scans on that server.
You can, however, publish a Build Scan to [scans.gradle.com](https://scans.gradle.com/) by
using the `--scan` parameter explicitly.

The remote Build Cache is enabled by default for everyone so that local builds can reuse
task outputs from previous CI builds.

## Building from Source

You need [JDK 25] to build JUnit. [Gradle toolchains] are used to detect and
potentially download additional JDKs for compilation and test execution.

All modules can be _built_ and _tested_ with the [Gradle Wrapper] using the following command:

`./gradlew build`

All modules can be _installed_ in a local Maven repository for consumption in other local
projects via the following command:

`./gradlew publishToMavenLocal`

## Dependency Metadata

[![JUnit Jupiter version](https://img.shields.io/maven-central/v/org.junit.jupiter/junit-jupiter/6..svg?color=25a162&label=Jupiter)](https://central.sonatype.com/search?namespace=org.junit.jupiter)
[![JUnit Vintage version](https://img.shields.io/maven-central/v/org.junit.vintage/junit-vintage-engine/6..svg?color=25a162&label=Vintage)](https://central.sonatype.com/search?namespace=org.junit.vintage)
[![JUnit Platform version](https://img.shields.io/maven-central/v/org.junit.platform/junit-platform-commons/6..svg?color=25a162&label=Platform)](https://central.sonatype.com/search?namespace=org.junit.platform)

Consult the [Dependency Metadata] section of the [User Guide] for a list of all artifacts
of the JUnit Platform, JUnit Jupiter, and JUnit Vintage.


[Codecov]: https://codecov.io/gh/junit-team/junit-framework
[CONTRIBUTING.md]: https://github.com/junit-team/junit-framework/blob/HEAD/CONTRIBUTING.md
[Dependency Metadata]: https://docs.junit.org/current/user-guide/#dependency-metadata
[GitHub Discussions]: https://github.com/junit-team/junit-framework/discussions/categories/q-a
[Gradle toolchains]: https://docs.gradle.org/current/userguide/toolchains.html
[Gradle Wrapper]: https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:using_wrapper
[JaCoCo]: https://www.eclemma.org/jacoco/
[Javadoc]: https://docs.junit.org/current/api/
[JDK 25]: https://javaalmanac.io/jdk/25/
[Release Notes]: https://docs.junit.org/current/release-notes/
[Examples]: https://github.com/junit-team/junit-examples
[StackOverflow]: https://stackoverflow.com/questions/tagged/junit5
[User Guide]: https://docs.junit.org/current/user-guide/
