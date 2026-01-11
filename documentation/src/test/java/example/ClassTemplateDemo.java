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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

// tag::user_guide[]
@ClassTemplate
@ExtendWith(ClassTemplateDemo.MyClassTemplateInvocationContextProvider.class)
class ClassTemplateDemo {

	static final List<String> WELL_KNOWN_FRUITS
	// tag::custom_line_break[]
		= List.of("apple", "banana", "lemon");

	//end::user_guide[]
	@Nullable
	//tag::user_guide[]
	private String fruit;

	@Test
	void notNull() {
		assertNotNull(fruit);
	}

	@Test
	void wellKnown() {
		assertTrue(WELL_KNOWN_FRUITS.contains(fruit));
	}

	// end::user_guide[]
	static
	// tag::user_guide[]
	public class MyClassTemplateInvocationContextProvider
			// tag::custom_line_break[]
			implements ClassTemplateInvocationContextProvider {

		@Override
		public boolean supportsClassTemplate(ExtensionContext context) {
			return true;
		}

		@Override
		public Stream<ClassTemplateInvocationContext>
				// tag::custom_line_break[]
				provideClassTemplateInvocationContexts(ExtensionContext context) {

			return Stream.of(invocationContext("apple"), invocationContext("banana"));
		}

		private ClassTemplateInvocationContext invocationContext(String parameter) {
			return new ClassTemplateInvocationContext() {
				@Override
				public String getDisplayName(int invocationIndex) {
					return parameter;
				}

				// end::user_guide[]
				@SuppressWarnings("Convert2Lambda")
				// tag::user_guide[]
				@Override
				public List<Extension> getAdditionalExtensions() {
					return List.of(new TestInstancePostProcessor() {
						@Override
						public void postProcessTestInstance(
								// tag::custom_line_break[]
								Object testInstance, ExtensionContext context) {
							((ClassTemplateDemo) testInstance).fruit = parameter;
						}
					});
				}
			};
		}
	}
}
// end::user_guide[]
