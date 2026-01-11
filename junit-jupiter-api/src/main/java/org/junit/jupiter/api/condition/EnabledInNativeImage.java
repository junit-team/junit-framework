/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @EnabledInNativeImage} is used to signal that the annotated test class
 * or test method is only <em>enabled</em> when executing within a GraalVM native
 * image.
 *
 * <p>When applied at the class level, all test methods within that class will
 * be enabled within a native image.
 *
 * <p>This annotation is not {@link java.lang.annotation.Inherited @Inherited}.
 * Consequently, if you wish to apply the same semantics to a subclass, this
 * annotation must be redeclared on the subclass.
 *
 * <p>If a test method is disabled via this annotation, that prevents execution
 * of the test method and method-level lifecycle callbacks such as
 * {@code @BeforeEach} methods, {@code @AfterEach} methods, and corresponding
 * extension APIs. However, that does not prevent the test class from being
 * instantiated, and it does not prevent the execution of class-level lifecycle
 * callbacks such as {@code @BeforeAll} methods, {@code @AfterAll} methods, and
 * corresponding extension APIs.
 *
 * <p>This annotation may be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of this
 * annotation.
 *
 * <h2>Technical Details</h2>
 *
 * <p>JUnit detects whether tests are executing within a GraalVM native image by
 * checking for the presence of the {@code org.graalvm.nativeimage.imagecode}
 * system property (see
 * <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">org.graalvm.nativeimage.ImageInfo</a>
 * for details). The GraalVM compiler sets the property to {@code buildtime} while
 * compiling a native image; the property is set to {@code runtime} while a native
 * image is executing; and the Gradle and Maven plug-ins in the GraalVM
 * <a href="https://graalvm.github.io/native-build-tools/latest/">Native Build Tools</a>
 * project set the property to {@code agent} while executing tests with the GraalVM
 * <a href="https://www.graalvm.org/reference-manual/native-image/metadata/AutomaticMetadataCollection/">tracing agent</a>.
 *
 * @since 5.9.1
 * @see org.junit.jupiter.api.condition.EnabledIf
 * @see org.junit.jupiter.api.condition.DisabledIf
 * @see org.junit.jupiter.api.condition.EnabledOnOs
 * @see org.junit.jupiter.api.condition.DisabledOnOs
 * @see org.junit.jupiter.api.condition.EnabledOnJre
 * @see org.junit.jupiter.api.condition.DisabledOnJre
 * @see org.junit.jupiter.api.condition.EnabledForJreRange
 * @see org.junit.jupiter.api.condition.DisabledForJreRange
 * @see org.junit.jupiter.api.condition.DisabledInNativeImage
 * @see org.junit.jupiter.api.condition.EnabledIfSystemProperty
 * @see org.junit.jupiter.api.condition.DisabledIfSystemProperty
 * @see org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
 * @see org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
 * @see org.junit.jupiter.api.Disabled
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnabledIfSystemProperty(named = "org.graalvm.nativeimage.imagecode", matches = ".+", //
		disabledReason = "Not currently executing within a GraalVM native image")
@API(status = STABLE, since = "5.9.1")
public @interface EnabledInNativeImage {
}
