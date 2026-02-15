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
import static org.junit.jupiter.api.Assertions.fail;
@for(var jre : supportedJresSortedByStringValue)<%--
--%>import static org.junit.jupiter.api.condition.JavaVersionPredicates.onJava${jre.getVersion()};
@endfor<%--
--%>import static org.junit.jupiter.api.condition.JavaVersionPredicates.onKnownVersion;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DisabledOnJre @DisabledOnJre}, generated from
 * {@code DisabledOnJreIntegrationTests.java.jte}.
 *
 * @since 5.1
 */
class DisabledOnJreIntegrationTests {

	@Test
	@Disabled("Only used in a unit test via reflection")
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledOnJre
	void missingVersionDeclaration() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledOnJre(JRE.UNDEFINED)
	void jreUndefined() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledOnJre(value = JRE.JAVA_17, versions = { 21, 7 })
	void version7() {
	}

	@SuppressWarnings({ "removal", "deprecation" })
	@Test
	@DisabledOnJre(disabledReason = "Disabled on every JRE", value = { //
@for(var jre : allJres)<%--
--%>			JRE.JAVA_${jre.getVersion()}, //
@endfor<%--
--%>			JRE.OTHER //
	})
	void disabledOnAllJavaVersions() {
		fail("should be disabled");
	}
@for(var jre : supportedJres)
	@Test
	@DisabledOnJre(JRE.JAVA_${jre.getVersion()})
	void jre${jre.getVersion()}() {
		assertFalse(onJava${jre.getVersion()}());
	}
@endfor<%--
--%>@for(var jre : supportedJres)
	@Test
	@DisabledOnJre(versions = ${jre.getVersion()})
	void version${jre.getVersion()}() {
		assertFalse(onJava${jre.getVersion()}());
	}
@endfor
	@Test
	@SuppressWarnings("deprecation")
	@DisabledOnJre(JRE.OTHER)
	void other() {
		assertTrue(onKnownVersion());
	}

}
