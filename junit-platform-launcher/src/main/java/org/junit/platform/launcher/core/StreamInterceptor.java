/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * @since 1.3
 */
class StreamInterceptor extends PrintStream {

	private final Deque<RewindableByteArrayOutputStream> mostRecentOutputs = new ConcurrentLinkedDeque<>();

	private final PrintStream originalStream;
	private final Consumer<PrintStream> unregisterAction;
	private final int maxNumberOfBytesPerThread;

	private final ThreadLocal<RewindableByteArrayOutputStream> output = ThreadLocal.withInitial(
		RewindableByteArrayOutputStream::new);

	static Optional<StreamInterceptor> registerStdout(int maxNumberOfBytesPerThread) {
		return register(System.out, System::setOut, maxNumberOfBytesPerThread);
	}

	static Optional<StreamInterceptor> registerStderr(int maxNumberOfBytesPerThread) {
		return register(System.err, System::setErr, maxNumberOfBytesPerThread);
	}

	static Optional<StreamInterceptor> register(PrintStream originalStream, Consumer<PrintStream> streamSetter,
			int maxNumberOfBytesPerThread) {
		if (originalStream instanceof StreamInterceptor) {
			return Optional.empty();
		}
		StreamInterceptor interceptor = new StreamInterceptor(originalStream, streamSetter, maxNumberOfBytesPerThread);
		streamSetter.accept(interceptor);
		return Optional.of(interceptor);
	}

	private StreamInterceptor(PrintStream originalStream, Consumer<PrintStream> unregisterAction,
			int maxNumberOfBytesPerThread) {
		super(originalStream);
		this.originalStream = originalStream;
		this.unregisterAction = unregisterAction;
		this.maxNumberOfBytesPerThread = maxNumberOfBytesPerThread;
	}

	void capture() {
		RewindableByteArrayOutputStream out = output.get();
		out.mark();
		pushToTop(out);
	}

	String consume() {
		RewindableByteArrayOutputStream out = output.get();
		String result = out.rewind();
		if (!out.isMarked()) {
			mostRecentOutputs.remove(out);
		}
		return result;
	}

	void unregister() {
		unregisterAction.accept(originalStream);
	}

	@Override
	public void write(int b) {
		RewindableByteArrayOutputStream out = getOutput();
		if (out != null && out.size() < maxNumberOfBytesPerThread) {
			pushToTop(out);
			out.write(b);
		}
		super.write(b);
	}

	@Override
	public void write(byte[] b) {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		RewindableByteArrayOutputStream out = getOutput();
		if (out != null) {
			int actualLength = Math.max(0, Math.min(len, maxNumberOfBytesPerThread - out.size()));
			if (actualLength > 0) {
				pushToTop(out);
				out.write(buf, off, actualLength);
			}
		}
		super.write(buf, off, len);
	}

	private void pushToTop(RewindableByteArrayOutputStream out) {
		if (!out.equals(mostRecentOutputs.peek())) {
			mostRecentOutputs.remove(out);
			mostRecentOutputs.push(out);
		}
	}

	private @Nullable RewindableByteArrayOutputStream getOutput() {
		RewindableByteArrayOutputStream out = output.get();
		return out.isMarked() ? out : mostRecentOutputs.peek();
	}

	static class RewindableByteArrayOutputStream extends ByteArrayOutputStream {

		private final Deque<Integer> markedPositions = new ArrayDeque<>();

		boolean isMarked() {
			return !markedPositions.isEmpty();
		}

		void mark() {
			markedPositions.addFirst(count);
		}

		String rewind() {
			Integer position = markedPositions.pollFirst();
			if (position == null || position == count) {
				return "";
			}
			int length = count - position;
			count -= length;
			return new String(buf, position, length, Charset.defaultCharset());
		}
	}
}
