/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import java.util.Optional;

import org.junit.platform.commons.util.PackageUtils;

import picocli.CommandLine;

class ManifestVersionProvider implements CommandLine.IVersionProvider {

	public static Optional<String> getImplementationVersion() {
		return PackageUtils.getModuleOrImplementationVersion(ManifestVersionProvider.class);
	}

	@Override
	public String[] getVersion() {
		return new String[] { //
				"@|bold JUnit Platform Console Launcher %s|@".formatted(getImplementationVersion().orElse("<unknown>")), //
				"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})", //
				"OS: ${os.name} ${os.version} ${os.arch}" //
		};
	}

}
