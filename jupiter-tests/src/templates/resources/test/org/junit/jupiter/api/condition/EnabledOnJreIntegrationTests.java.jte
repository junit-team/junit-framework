@import java.util.List
@import junitbuild.generator.model.JRE

@param List<JRE> allJres
@param List<JRE> supportedJres
@param List<JRE> supportedJresSortedByStringValue
@param String licenseHeader
${licenseHeader}
package org.junit.jupiter.api.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
@for(var jre : supportedJresSortedByStringValue)<%--
--%>import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava${jre.getVersion()};
@endfor<%--
--%>import static org.junit.jupiter.api.condition.JavaVersionPredicates.onKnownVersion;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link EnabledOnJre @EnabledOnJre}, generated from
 * {@code EnabledOnJreIntegrationTests.java.jte}.
 *
 * @since 5.1
 */
class EnabledOnJreIntegrationTests {

	@Test
	@Disabled("Only used in a unit test via reflection")
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@EnabledOnJre
	void missingVersionDeclaration() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@EnabledOnJre(JRE.UNDEFINED)
	void jreUndefined() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@EnabledOnJre(value = JRE.JAVA_17, versions = { 21, 7 })
	void version7() {
	}

	@SuppressWarnings({ "removal", "deprecation" })
	@Test
	@EnabledOnJre({ //
@for(var jre : allJres)<%--
--%>			JRE.JAVA_${jre.getVersion()}, //
@endfor<%--
--%>			JRE.OTHER //
	})
	void enabledOnAllJavaVersions() {
	}
@for(var jre : supportedJres)
	@Test
	@EnabledOnJre(JRE.JAVA_${jre.getVersion()})
	void jre${jre.getVersion()}() {
		assertTrue(onJava${jre.getVersion()}());
	}
@endfor<%--
--%>@for(var jre : supportedJres)
	@Test
	@EnabledOnJre(versions = ${jre.getVersion()})
	void version${jre.getVersion()}() {
		assertTrue(onJava${jre.getVersion()}());
	}
@endfor
	@Test
	@SuppressWarnings("deprecation")
	@EnabledOnJre(value = JRE.OTHER, disabledReason = "Disabled on almost every JRE")
	void other() {
		assertFalse(onKnownVersion());
	}

}
