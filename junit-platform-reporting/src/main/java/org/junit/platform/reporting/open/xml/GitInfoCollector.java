/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.open.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.StringUtils;

/**
 * @since 1.13.2
 */
interface GitInfoCollector {

	static Optional<GitInfoCollector> get(Path workingDir) {
		ProcessExecutor executor = new ProcessExecutor(workingDir);
		boolean gitInstalled = executor.exec("git", "--version").isPresent();
		return gitInstalled ? Optional.of(new CliGitInfoCollector(executor)) : Optional.empty();
	}

	Optional<String> getOriginUrl();

	Optional<String> getBranch();

	Optional<String> getCommitHash();

	Optional<String> getStatus();

	class CliGitInfoCollector implements GitInfoCollector {

		private static final String ALLOWED_USERNAME = "git";
		private static final String USER_INFO_REPLACEMENT = "***";
		private static final String USER_INFO_SEPARATOR = "@";

		private final ProcessExecutor executor;

		CliGitInfoCollector(ProcessExecutor executor) {
			this.executor = executor;
		}

		@Override
		public Optional<String> getOriginUrl() {
			return executor.exec("git", "config", "--get", "remote.origin.url") //
					.filter(StringUtils::isNotBlank) //
					.flatMap(this::toGitUrl) //
					.flatMap(this::obfuscateUserInfo);
		}

		@Override
		public Optional<String> getBranch() {
			return executor.exec("git", "rev-parse", "--abbrev-ref", "HEAD") //
					.filter(StringUtils::isNotBlank);
		}

		@Override
		public Optional<String> getCommitHash() {
			return executor.exec("git", "rev-parse", "--verify", "HEAD") //
					.filter(StringUtils::isNotBlank);
		}

		@Override
		public Optional<String> getStatus() {
			return executor.exec("git", "status", "--porcelain");
		}

		private Optional<String> obfuscateUserInfo(GitUrl gitUrl) {
			try {
				if (gitUrl.uri.getUserInfo() != null) {
					URI newUri = new URI(gitUrl.uri.getScheme(), USER_INFO_REPLACEMENT, gitUrl.uri.getHost(),
						gitUrl.uri.getPort(), gitUrl.uri.getPath(), gitUrl.uri.getQuery(), gitUrl.uri.getFragment());
					return Optional.of(newUri.toString().substring(gitUrl.addedPrefix.length()));
				}
				if (gitUrl.uri.getAuthority() != null && gitUrl.uri.getAuthority().contains(USER_INFO_SEPARATOR)) {
					String oldAuthority = gitUrl.uri.getAuthority();
					String[] parts = oldAuthority.split(USER_INFO_SEPARATOR, 2);
					if (!ALLOWED_USERNAME.equals(parts[0])) {
						String newAuthority = USER_INFO_REPLACEMENT + USER_INFO_SEPARATOR + parts[1];
						URI newUri = new URI(gitUrl.uri.getScheme(), newAuthority, gitUrl.uri.getPath(),
							gitUrl.uri.getQuery(), gitUrl.uri.getFragment());
						return Optional.of(newUri.toString().substring(gitUrl.addedPrefix.length()));
					}
				}
				return Optional.of(gitUrl.rawValue);
			}
			catch (URISyntaxException e) {
				return Optional.empty();
			}
		}

		private Optional<GitUrl> toGitUrl(String remoteUrl) {
			try {
				return Optional.of(new GitUrl(remoteUrl, new URI(remoteUrl), ""));
			}
			catch (URISyntaxException ex) {
				try {
					return Optional.of(new GitUrl(remoteUrl, new URI("ssh://" + remoteUrl), "ssh://"));
				}
				catch (URISyntaxException ignore) {
					return Optional.empty();
				}
			}
		}
	}

	class ProcessExecutor {

		private final Path workingDir;

		ProcessExecutor(Path workingDir) {
			this.workingDir = workingDir;
		}

		Optional<String> exec(String... args) {

			Process process = startProcess(args);

			try (Reader out = newBufferedReader(process.getInputStream());
					Reader err = newBufferedReader(process.getErrorStream())) {

				StringBuilder output = new StringBuilder();
				readAllChars(out, (chars, numChars) -> output.append(chars, 0, numChars));

				readAllChars(err, (__, ___) -> {
					// ignore
				});

				boolean terminated = process.waitFor(10, TimeUnit.SECONDS);
				return terminated && process.exitValue() == 0 ? Optional.of(trimAtEnd(output)) : Optional.empty();
			}
			catch (InterruptedException e) {
				throw ExceptionUtils.throwAsUncheckedException(e);
			}
			catch (IOException ignore) {
				return Optional.empty();
			}
			finally {
				process.destroyForcibly();
			}
		}

		private static BufferedReader newBufferedReader(InputStream stream) {
			return new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()));
		}

		private Process startProcess(String[] command) {
			Process process;
			try {
				process = new ProcessBuilder().directory(workingDir.toFile()).command(command).start();
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to start process", e);
			}
			return process;
		}

		private static void readAllChars(Reader reader, BiConsumer<char[], Integer> consumer) throws IOException {
			char[] buffer = new char[1024];
			int numChars;
			while ((numChars = reader.read(buffer)) != -1) {
				consumer.accept(buffer, numChars);
			}
		}

		private static String trimAtEnd(StringBuilder value) {
			int endIndex = value.length();
			for (int i = value.length() - 1; i >= 0; i--) {
				if (Character.isWhitespace(value.charAt(i))) {
					endIndex--;
					break;
				}
			}
			return value.substring(0, endIndex);
		}
	}

	class GitUrl {

		private final String rawValue;
		private final URI uri;
		private final String addedPrefix;

		GitUrl(String rawValue, URI uri, String addedPrefix) {
			this.rawValue = rawValue;
			this.uri = uri;
			this.addedPrefix = addedPrefix;
		}
	}

}
