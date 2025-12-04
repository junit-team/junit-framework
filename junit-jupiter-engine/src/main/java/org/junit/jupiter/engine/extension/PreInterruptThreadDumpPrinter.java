/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.PreInterruptCallback;
import org.junit.jupiter.api.extension.PreInterruptContext;
import org.junit.jupiter.engine.Constants;

/**
 * Default implementation of {@link PreInterruptCallback}, which prints the stacks
 * of all {@link Thread}s to {@code System.out}.
 *
 * <p>Note: This is disabled by default and must be enabled via
 * {@link Constants#EXTENSIONS_TIMEOUT_THREAD_DUMP_ENABLED_PROPERTY_NAME}.
 *
 * @since 5.12
 */
final class PreInterruptThreadDumpPrinter implements PreInterruptCallback {

	private static final String NL = "\n";

	@Override
	public void beforeThreadInterrupt(PreInterruptContext preInterruptContext, ExtensionContext extensionContext) {
		Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();

		StringBuilder sb = new StringBuilder("Thread ");
		appendThreadName(sb, preInterruptContext.getThreadToInterrupt());
		sb.append(" will be interrupted.");
		sb.append(NL);

		for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
			Thread thread = entry.getKey();
			StackTraceElement[] stack = entry.getValue();
			if (stack.length > 0) {
				sb.append(NL);
				appendThreadName(sb, thread);
				for (StackTraceElement stackTraceElement : stack) {
					sb.append(NL);
					// Use the same prefix as java.lang.Throwable.printStackTrace(PrintStreamOrWriter)
					sb.append("\tat ");
					sb.append(stackTraceElement);
				}
				sb.append(NL);
			}
		}

		System.out.println(sb);
	}

	/**
	 * Append the {@link Thread} name and ID in a similar fashion as {@code jstack}.
	 * @param builder the builder to append to
	 * @param thread the thread whose information should be appended
	 */
	@SuppressWarnings("deprecation") // Thread.getId() is deprecated on JDK 19+
	private static void appendThreadName(StringBuilder builder, Thread thread) {
		// Use same format as java.lang.management.ThreadInfo.toString()
		builder.append("\"");
		builder.append(thread.getName());
		builder.append("\"");
		if (thread.isDaemon()) {
			builder.append(" daemon");
		}
		builder.append(" prio=");
		builder.append(thread.getPriority());
		builder.append(" Id=");
		builder.append(thread.getId());
		builder.append(" ");
		builder.append(thread.getState());
	}

}
