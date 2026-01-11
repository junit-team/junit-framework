/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.io;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apiguardian.api.API;

/**
 * {@code Resource} represents a resource on the classpath.
 *
 * <p><strong>WARNING</strong>: a {@code Resource} must provide correct
 * {@link Object#equals(Object) equals} and {@link Object#hashCode() hashCode}
 * implementations since a {@code Resource} may potentially be stored in a
 * collection or map.
 *
 * @since 1.14
 * @see org.junit.platform.commons.support.ResourceSupport
 */
@API(status = MAINTAINED, since = "1.14")
public interface Resource {

	/**
	 * Create a new {@link Resource} with the given name and URI.
	 *
	 * @param name the name of the resource; never {@code null}
	 * @param uri the URI of the resource; never {@code null}
	 * @return a new {@code Resource}
	 * @since 1.14
	 */
	static Resource of(String name, URI uri) {
		return new DefaultResource(name, uri);
	}

	/**
	 * Get the name of this resource.
	 *
	 * <p>The resource name is a {@code /}-separated path. The path is relative
	 * to the classpath root in which the resource is located.
	 *
	 * @return the resource name; never {@code null}
	 */
	String getName();

	/**
	 * Get the URI of this resource.
	 *
	 * @return the URI of the resource; never {@code null}
	 */
	URI getUri();

	/**
	 * Get an {@link InputStream} for reading this resource.
	 *
	 * <p>The default implementation delegates to {@link java.net.URL#openStream()}
	 * for this resource's {@link #getUri() URI}.
	 *
	 * @return an input stream for this resource; never {@code null}
	 * @throws IOException if an I/O exception occurs
	 */
	default InputStream getInputStream() throws IOException {
		return getUri().toURL().openStream();
	}

}
