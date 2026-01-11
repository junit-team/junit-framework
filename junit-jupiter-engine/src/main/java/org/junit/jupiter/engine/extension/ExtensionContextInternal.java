/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code ExtensionContextInternal} extends the {@link ExtensionContext} with internal API.
 *
 * @since 5.12
 * @see ExtensionContext
 */
@API(status = INTERNAL, since = "5.12")
public interface ExtensionContextInternal extends ExtensionContext {

	/**
	 * Returns a list of registered extension at this context of the passed {@code extensionType}.
	 *
	 * @param <E> the extension type
	 * @param extensionType the extension type
	 * @return the list of extensions
	 */
	<E extends Extension> List<E> getExtensions(Class<E> extensionType);
}
