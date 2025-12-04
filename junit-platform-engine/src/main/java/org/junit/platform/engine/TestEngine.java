/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.module.ModuleDescriptor;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ModuleUtils;
import org.junit.platform.commons.util.PackageUtils;

/**
 * A {@code TestEngine} facilitates <em>discovery</em> and <em>execution</em> of
 * tests for a particular programming model.
 *
 * <p>For example, JUnit provides a {@code TestEngine} that discovers and
 * executes tests written using the JUnit Jupiter programming model.
 *
 * <p>Every {@code TestEngine} must {@linkplain #getId provide its own unique ID},
 * {@linkplain #discover discover tests} from
 * {@link EngineDiscoveryRequest EngineDiscoveryRequests},
 * and {@linkplain #execute execute those tests} according to
 * {@link ExecutionRequest ExecutionRequests}.
 *
 * <p>In order to facilitate test discovery within IDEs and tools prior
 * to launching the JUnit Platform, {@code TestEngine} implementations are
 * encouraged to make use of the
 * {@link org.junit.platform.commons.annotation.Testable @Testable} annotation.
 * For example, the {@code @Test} and {@code @TestFactory} annotations in JUnit
 * Jupiter are meta-annotated with {@code @Testable}. Consult the Javadoc for
 * {@code @Testable} for further details.
 *
 * @since 1.0
 * @see org.junit.platform.engine.EngineDiscoveryRequest
 * @see org.junit.platform.engine.ExecutionRequest
 * @see org.junit.platform.commons.annotation.Testable
 */
@API(status = STABLE, since = "1.0")
public interface TestEngine {

	/**
	 * Get the ID that uniquely identifies this test engine.
	 *
	 * <p>Each test engine must provide a unique ID. For example, JUnit Vintage
	 * and JUnit Jupiter use {@code "junit-vintage"} and {@code "junit-jupiter"},
	 * respectively. When in doubt, you may use the fully qualified name of your
	 * custom {@code TestEngine} implementation class.
	 *
	 * @return the ID of this test engine; never {@code null} or blank
	 */
	String getId();

	/**
	 * Discover tests according to the supplied {@link EngineDiscoveryRequest}.
	 *
	 * <p>The supplied {@link UniqueId} must be used as the unique ID of the
	 * returned root {@link TestDescriptor}. In addition, the {@code UniqueId}
	 * must be used to create unique IDs for children of the root's descriptor
	 * by calling {@link UniqueId#append}.
	 *
	 * <p>Furthermore, implementations must publish events about test discovery
	 * via the supplied {@link EngineDiscoveryRequest#getDiscoveryListener()
	 * EngineDiscoveryListener}.
	 *
	 * @param discoveryRequest the discovery request; never {@code null}
	 * @param uniqueId the unique ID to be used for this test engine's
	 * {@code TestDescriptor}; never {@code null}
	 * @return the root {@code TestDescriptor} of this engine, typically an
	 * instance of {@code EngineDescriptor}
	 * @see org.junit.platform.engine.support.descriptor.EngineDescriptor
	 */
	TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId);

	/**
	 * Execute tests according to the supplied {@link ExecutionRequest}.
	 *
	 * <p>The {@code request} passed to this method contains the root
	 * {@link TestDescriptor} that was previously returned by {@link #discover},
	 * the {@link EngineExecutionListener} to be notified of test execution
	 * events, and {@link ConfigurationParameters} that may influence test execution.
	 *
	 * @param request the request to execute tests for; never {@code null}
	 */
	void execute(ExecutionRequest request);

	/**
	 * Get the <em>Group ID</em> of the JAR in which this test engine is packaged.
	 *
	 * <p>This information is used solely for debugging and reporting purposes.
	 *
	 * <p>The default implementation returns an empty {@link Optional},
	 * signaling that the group ID is unknown.
	 *
	 * <p>Concrete test engine implementations may override this method in
	 * order to provide a known group ID.
	 *
	 * @return an {@code Optional} containing the group ID; never {@code null}
	 * but potentially empty if the group ID is unknown
	 * @see #getArtifactId()
	 * @see #getVersion()
	 */
	default Optional<String> getGroupId() {
		return Optional.empty();
	}

	/**
	 * Get the <em>Module Name</em> or <em>Artifact ID</em> of the JAR in which
	 * this test engine is packaged.
	 *
	 * <p>This information is used solely for debugging and reporting purposes.
	 *
	 * <p>The default implementation first attempts to retrieve the
	 * {@linkplain Module#getName() name} of the {@link Module} in which the
	 * engine resides. If the module name is not available, the default
	 * implementation then attempts to retrieve the artifact ID as explained
	 * below.
	 *
	 * <p>The default implementation assumes the implementation title is equivalent
	 * to the artifact ID and therefore attempts to query the
	 * {@linkplain Package#getImplementationTitle() implementation title}
	 * from the package attributes for the {@link Package} in which the engine
	 * resides. Note that a package only has attributes if the information is
	 * defined in the {@link java.util.jar.Manifest Manifest} of the JAR containing
	 * that package, and if the class loader created the {@link Package} instance
	 * with the attributes from the manifest.
	 *
	 * <p>If the implementation title cannot be queried from the package
	 * attributes, the default implementation returns an empty {@link Optional}.
	 *
	 * <p>Concrete test engine implementations may override this method in
	 * order to determine the artifact ID by some other means.
	 *
	 * @return an {@code Optional} containing the module name or artifact ID; never
	 * {@code null} but potentially empty if neither the module name nor the artifact
	 * ID can be determined
	 * @see Module#getName()
	 * @see Class#getPackage()
	 * @see Package#getImplementationTitle()
	 * @see #getGroupId()
	 * @see #getVersion()
	 */
	default Optional<String> getArtifactId() {
		Optional<String> moduleName = ModuleUtils.getModuleName(getClass());
		if (moduleName.isPresent()) {
			return moduleName;
		}
		return PackageUtils.getAttribute(getClass(), Package::getImplementationTitle);
	}

	/**
	 * Get the version of this test engine.
	 *
	 * <p>This information is used solely for debugging and reporting purposes.
	 *
	 * <p>The default implementation first attempts to retrieve the version
	 * from the JAR manifest attribute named {@code "Engine-Version-" + getId()}.
	 *
	 * <p>If the manifest attribute is not available, an attempt is made to retrieve
	 * the {@linkplain ModuleDescriptor#rawVersion() raw version} of the
	 * {@link Module} in which the engine resides.
	 *
	 * <p>If the module version is not available, an attempt is made to query the
	 * {@linkplain Package#getImplementationVersion() implementation version}
	 * from the package attributes for the {@link Package} in which the engine
	 * resides. Note that a package only has attributes if the information is
	 * defined in the {@link java.util.jar.Manifest Manifest} of the JAR
	 * containing that package, and if the class loader created the
	 * {@link Package} instance with the attributes from the manifest.
	 *
	 * <p>If the implementation version cannot be determined, the default
	 * implementation returns {@code "DEVELOPMENT"}.
	 *
	 * <p>Concrete test engine implementations may override this method to
	 * determine the version by some other means.
	 *
	 * @return an {@code Optional} containing the version; never {@code null}
	 * but potentially empty if the version is unknown
	 * @see Class#getPackage()
	 * @see Package#getImplementationVersion()
	 * @see #getGroupId()
	 * @see #getArtifactId()
	 */
	default Optional<String> getVersion() {
		Optional<String> standalone = PackageUtils.getAttribute(getClass(), "Engine-Version-" + getId());
		if (standalone.isPresent()) {
			return standalone;
		}
		return Optional.of(PackageUtils.getModuleOrImplementationVersion(getClass()).orElse("DEVELOPMENT"));
	}

}
