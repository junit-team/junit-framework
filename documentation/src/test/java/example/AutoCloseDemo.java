/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import example.registration.WebClient;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

// tag::user_guide_example[]
class AutoCloseDemo {

	@AutoClose // <1>
	WebClient webClient = new WebClient(); // <2>

	String serverUrl = // specify server URL ...
		// end::user_guide_example[]
		"https://localhost";
	// tag::user_guide_example[]

	@Test
	void getProductList() {
		// Use WebClient to connect to web server and verify response
		assertEquals(200, webClient.get(serverUrl + "/products").getResponseStatus());
	}

}
// end::user_guide_example[]
