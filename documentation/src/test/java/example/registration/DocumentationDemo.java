/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.registration;

import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

//tag::user_guide[]
class DocumentationDemo {

	//end::user_guide[]
	@Nullable
	//tag::user_guide[]
	static Path lookUpDocsDir() {
		// return path to docs dir
		// end::user_guide[]
		return null;
		// tag::user_guide[]
	}

	@RegisterExtension
	DocumentationExtension docs = DocumentationExtension.forPath(lookUpDocsDir());

	@Test
	void generateDocumentation() {
		// use this.docs ...
	}
}
//end::user_guide[]

@NullMarked
class DocumentationExtension implements AfterEachCallback {

	@SuppressWarnings("unused")
	private final @Nullable Path path;

	private DocumentationExtension(@Nullable Path path) {
		this.path = path;
	}

	static DocumentationExtension forPath(@Nullable Path path) {
		return new DocumentationExtension(path);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		/* no-op for demo */
	}

}
