/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.options;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectDirectory;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectFile;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUri;

import java.net.URI;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.ResourceUtils;
import org.junit.platform.engine.DiscoverySelectorIdentifier;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.FilePosition;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;

import picocli.CommandLine.ITypeConverter;

class SelectorConverter {

	private static final Pattern URI_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

	static class Module implements ITypeConverter<ModuleSelector> {
		@Override
		public ModuleSelector convert(String value) {
			return selectModule(value);
		}
	}

	static class Uri implements ITypeConverter<UriSelector> {
		@Override
		public UriSelector convert(String value) {
			return selectUri(value);
		}
	}

	static class File implements ITypeConverter<FileSelector> {
		@Override
		public FileSelector convert(String value) {
			ParsedSelectorInput parsed = parsePathAndQuery(value);
			FilePosition filePosition = FilePosition.fromQuery(parsed.query).orElse(null);
			String path = parsed.path;
			return selectFile(path, filePosition);
		}
	}

	static class Directory implements ITypeConverter<DirectorySelector> {
		@Override
		public DirectorySelector convert(String value) {
			return selectDirectory(value);
		}
	}

	@SuppressWarnings("JavaLangClash")
	static class Package implements ITypeConverter<PackageSelector> {
		@Override
		public PackageSelector convert(String value) {
			return selectPackage(value);
		}
	}

	@SuppressWarnings("JavaLangClash")
	static class Class implements ITypeConverter<ClassSelector> {
		@Override
		public ClassSelector convert(String value) {
			return selectClass(value);
		}
	}

	static class Method implements ITypeConverter<MethodSelector> {
		@Override
		public MethodSelector convert(String value) {
			return selectMethod(value);
		}
	}

	static class ClasspathResource implements ITypeConverter<ClasspathResourceSelector> {
		@Override
		public ClasspathResourceSelector convert(String value) {
			ParsedSelectorInput parsed = parsePathAndQuery(value);
			FilePosition filePosition = FilePosition.fromQuery(parsed.query).orElse(null);
			String path = parsed.path;
			return selectClasspathResource(path, filePosition);
		}
	}

	static class Iteration implements ITypeConverter<IterationSelector> {
		@Override
		public IterationSelector convert(String value) {
			DiscoverySelectorIdentifier identifier = DiscoverySelectorIdentifier.create(
				IterationSelector.IdentifierParser.PREFIX, value);
			return (IterationSelector) DiscoverySelectors.parse(identifier) //
					.orElseThrow(() -> new PreconditionViolationException("Invalid format: Failed to parse selector"));
		}
	}

	static class UniqueId implements ITypeConverter<UniqueIdSelector> {
		@Override
		public UniqueIdSelector convert(String value) {
			return selectUniqueId(value);
		}
	}

	static class Identifier implements ITypeConverter<DiscoverySelectorIdentifier> {
		@Override
		public DiscoverySelectorIdentifier convert(String value) {
			return DiscoverySelectorIdentifier.parse(value);
		}
	}

	private static ParsedSelectorInput parsePathAndQuery(String value) {
		if (looksLikeUri(value) && !looksLikeWindowsDrivePath(value)) {
			@Nullable ParsedSelectorInput parsed = parseUri(value);
			if (parsed != null) {
				return parsed;
			}
			if (value.indexOf(' ') >= 0) {
				parsed = parseUri(value.replace(" ", "%20"));
				if (parsed != null) {
					return parsed;
				}
			}
			URI.create(value);
		}
		return parseRawPath(value);
	}

	private static @Nullable ParsedSelectorInput parseUri(String value) {
		try {
			URI uri = URI.create(value);
			String path = ResourceUtils.stripQueryComponent(uri).getPath();
			if (path == null) {
				return null;
			}
			return new ParsedSelectorInput(path, uri.getQuery());
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static ParsedSelectorInput parseRawPath(String value) {
		int queryIndex = value.indexOf('?');
		if (queryIndex < 0) {
			return new ParsedSelectorInput(value, null);
		}
		String path = value.substring(0, queryIndex);
		String query = value.substring(queryIndex + 1);
		return new ParsedSelectorInput(path, query);
	}

	private static boolean looksLikeUri(String value) {
		return URI_SCHEME.matcher(value).matches();
	}

	private static boolean looksLikeWindowsDrivePath(String value) {
		return value.length() >= 2 && Character.isLetter(value.charAt(0)) && value.charAt(1) == ':';
	}

	private static final class ParsedSelectorInput {
		private final String path;
		private final @Nullable String query;

		private ParsedSelectorInput(String path, @Nullable String query) {
			this.path = path;
			this.query = query;
		}
	}

}
