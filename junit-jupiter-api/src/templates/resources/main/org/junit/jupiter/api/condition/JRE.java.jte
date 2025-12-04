@import java.util.List
@import gg.jte.support.ForSupport
@import junitbuild.generator.model.JRE

@param int minRuntimeVersion
@param List<JRE> allJres
@param String licenseHeader
${licenseHeader}
package org.junit.jupiter.api.condition;

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * Enumeration of Java Runtime Environment (JRE) versions.
 *
 * <p>If the current JRE version can be detected but is not one of the predefined
 * constants in this enum, {@link #OTHER} will be considered to be the
 * {@linkplain #isCurrentVersion current JRE version}.
 *
 * @since 5.1
@for(JRE jre : allJres)<%--
--%> * @see #JAVA_${jre.getVersion()}
@endfor<%--
--%> * @see #OTHER
 * @see EnabledOnJre
 * @see DisabledOnJre
 * @see EnabledForJreRange
 * @see DisabledForJreRange
 */
@API(status = STABLE, since = "5.1")
public enum JRE {

	/**
	 * An undefined JRE version.
	 *
	 * <p>This constant is used by JUnit as a default configuration value but is
	 * not intended to be used by users.
	 *
	 * <p>This constant returns {@code -1} for its {@linkplain #version() version}.
	 *
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	UNDEFINED(-1),
@for(var jre : allJres)
	/**
	 * Java ${jre.getVersion()}.
	@if(jre.getSince() != null || jre.getVersion() < minRuntimeVersion)<%--
--%> *
	@endif<%--
--%>@if(jre.getSince() != null)<%--
--%> * @since ${jre.getSince()}
	@endif<%--
--%>@if(jre.getVersion() < minRuntimeVersion)<%--
--%> * @deprecated No longer supported at runtime; please use {@link #JAVA_17} or later
	@endif<%--
--%> */
	@if(jre.getVersion() < minRuntimeVersion)<%--
--%>@API(status = DEPRECATED, since = "6.0") //
	@Deprecated(since = "6.0", forRemoval = true)
	@elseif(jre.getSince() != null)<%--
--%>@API(status = STABLE, since = "${jre.getSince()}")
	@endif<%--
--%>JAVA_${jre.getVersion()}(${jre.getVersion()}),
@endfor
	/**
	 * A JRE version other than <%--
--%>@for(var jre : ForSupport.of(allJres))<%--
	--%>@if(jre.isLast())or @endif<%--
	--%>{@link #JAVA_${jre.get().getVersion()}}<%--
	--%>@if(jre.isLast()).@else,@endif<%--
	--%>@if(jre.getIndex() % 3 == 1 && !jre.isLast())
	 * @elseif(!jre.isLast()) @endif<%--
--%>@endfor
	 *
	 * <p>This constant returns {@link Integer#MAX_VALUE} for its
	 * {@linkplain #version() version}. To retrieve the actual version number,
	 * use {@link #currentVersionNumber()}.
	 */
	OTHER(Integer.MAX_VALUE);

	static final int UNDEFINED_VERSION = -1;

	static final int MINIMUM_VERSION = ${allJres.getFirst().getVersion()};

	private static final int CURRENT_VERSION = Runtime.version().feature();

	private final int version;

	JRE(int version) {
		this.version = version;
	}

	/**
	 * Get the version of <em>this</em> {@code JRE}.
	 *
	 * <p>If this {@code JRE} is {@link #UNDEFINED}, this method returns
	 * {@code -1}. If this {@code JRE} is {@link #OTHER}, this method returns
	 * {@link Integer#MAX_VALUE}.
	 *
	 * @return the version of this {@code JRE}
	 * @since 5.12
	 * @see Runtime.Version#feature()
	 * @see #currentVersionNumber()
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public int version() {
		return this.version;
	}

	/**
	 * {@return {@code true} if <em>this</em> {@code JRE} is known to be the
	 * Java Runtime Environment version for the currently executing JVM or if
	 * the version is {@link #OTHER}}
	 *
	 * @see #currentJre()
	 * @see #currentVersionNumber()
	 */
	public boolean isCurrentVersion() {
		return this == currentJre();
	}

	/**
	 * {@return the {@link JRE} for the currently executing JVM, potentially
	 * {@link #OTHER}}
	 *
	 * @since 5.7
	 * @see #currentVersionNumber()
	 * @deprecated in favor of {@link #currentJre()}
	 */
	@API(status = DEPRECATED, since = "5.12")
	@Deprecated(since = "5.12")
	public static JRE currentVersion() {
		return currentJre();
	}

	/**
	 * {@return the {@link JRE} for the currently executing JVM, potentially
	 * {@link #OTHER}}
	 *
	 * @since 5.12
	 * @see #currentVersionNumber()
	 */
	@API(status = STABLE, since = "5.12")
	public static JRE currentJre() {
		return switch (CURRENT_VERSION) {<%--
				--%>@for(var jre : allJres)
			case ${jre.getVersion()} -> JAVA_${jre.getVersion()};<%--
				--%>@endfor
			default -> OTHER;
		};
	}

	/**
	 * {@return the version number for the currently executing JVM, or {@code -1}
	 * if the current JVM version could not be determined}
	 *
	 * @since 5.12
	 * @see Runtime.Version#feature()
	 * @see #currentJre()
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static int currentVersionNumber() {
		return CURRENT_VERSION;
	}

	/**
	 * {@return {@code true} if the supplied version number is known to be the
	 * Java Runtime Environment version for the currently executing JVM or if
	 * the supplied version number is {@code -1} and the current JVM version
	 * could not be determined}
	 *
	 * @since 5.12
	 * @see Runtime.Version#feature()
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static boolean isCurrentVersion(int version) {
		return version == CURRENT_VERSION;
	}

	static boolean isCurrentVersionWithinRange(int min, int max) {
		return CURRENT_VERSION >= min && CURRENT_VERSION <= max;
	}

}
