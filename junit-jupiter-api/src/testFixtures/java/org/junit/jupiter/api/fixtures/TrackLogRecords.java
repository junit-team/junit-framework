/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * {@code @TrackLogRecords} registers an extension that tracks log records
 * logged via JUnit's logging facade for JUL.
 *
 * <p>Log records are tracked on a per-method basis (e.g., for a single
 * test method).
 *
 * <p>Test methods can gain access to the {@link LogRecordListener} managed by
 * the extension by having an instance of {@code LogRecordListener} injected as
 * a method parameter.
 *
 * @since 5.1
 * @see LoggerFactory
 * @see LogRecordListener
 */
@Target({ ElementType.TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TrackLogRecords.Extension.class)
public @interface TrackLogRecords {

	class Extension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

		@Override
		public void beforeEach(ExtensionContext context) {
			LoggerFactory.addListener(getListener(context));
		}

		@Override
		public void afterEach(ExtensionContext context) {
			LoggerFactory.removeListener(getListener(context));
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			boolean isTestMethodLevel = extensionContext.getTestMethod().isPresent();
			boolean isListener = parameterContext.getParameter().getType() == LogRecordListener.class;
			return isTestMethodLevel && isListener;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return getListener(extensionContext);
		}

		private LogRecordListener getListener(ExtensionContext context) {
			return getStore(context).computeIfAbsent(LogRecordListener.class);
		}

		private Store getStore(ExtensionContext context) {
			return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
		}

	}

}
