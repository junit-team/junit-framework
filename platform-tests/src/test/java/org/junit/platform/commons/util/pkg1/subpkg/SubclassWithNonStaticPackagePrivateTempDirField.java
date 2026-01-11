/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util.pkg1.subpkg;

import java.nio.file.Path;

import org.junit.platform.commons.util.pkg1.InstanceLevelDir;
import org.junit.platform.commons.util.pkg1.SuperclassWithStaticPackagePrivateTempDirField;

/**
 * @see https://github.com/junit-team/junit-framework/issues/3553
 */
public class SubclassWithNonStaticPackagePrivateTempDirField extends SuperclassWithStaticPackagePrivateTempDirField {

	@InstanceLevelDir
	Path tempDir;

}
