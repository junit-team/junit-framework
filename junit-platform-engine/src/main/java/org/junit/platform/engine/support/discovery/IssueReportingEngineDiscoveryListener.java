/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static org.junit.platform.commons.util.UnrecoverableExceptions.rethrowIfUnrecoverable;
import static org.junit.platform.engine.SelectorResolutionResult.Status.FAILED;
import static org.junit.platform.engine.SelectorResolutionResult.Status.UNRESOLVED;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryListener;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource;
import org.junit.platform.engine.support.descriptor.DirectorySource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import org.junit.platform.engine.support.descriptor.FileSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.descriptor.PackageSource;
import org.junit.platform.engine.support.descriptor.UriSource;

class IssueReportingEngineDiscoveryListener implements EngineDiscoveryListener {

	private static final Logger logger = LoggerFactory.getLogger(IssueReportingEngineDiscoveryListener.class);

	private final EngineDiscoveryListener delegate;

	IssueReportingEngineDiscoveryListener(EngineDiscoveryListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void selectorProcessed(UniqueId engineId, DiscoverySelector selector, SelectorResolutionResult result) {
		delegate.selectorProcessed(engineId, selector, result);
		if (result.getStatus() == FAILED) {
			DiscoveryIssue issue = DiscoveryIssue.builder(Severity.ERROR, selector + " resolution failed") //
					.cause(result.getThrowable()) //
					.source(toSource(selector)) //
					.build();
			delegate.issueEncountered(engineId, issue);
		}
		else if (result.getStatus() == UNRESOLVED && selector instanceof UniqueIdSelector uniqueIdSelector) {
			UniqueId uniqueId = uniqueIdSelector.getUniqueId();
			if (uniqueId.hasPrefix(engineId)) {
				DiscoveryIssue issue = DiscoveryIssue.create(Severity.ERROR, selector + " could not be resolved");
				delegate.issueEncountered(engineId, issue);
			}
		}
	}

	private static @Nullable TestSource toSource(DiscoverySelector selector) {
		if (selector instanceof ClassSelector classSelector) {
			return ClassSource.from(classSelector.getClassName());
		}
		if (selector instanceof MethodSelector methodSelector) {
			return MethodSource.from(methodSelector.getClassName(), methodSelector.getMethodName(),
				methodSelector.getParameterTypeNames());
		}
		if (selector instanceof ClasspathResourceSelector resourceSelector) {
			String resourceName = resourceSelector.getClasspathResourceName();
			return resourceSelector.getPosition() //
					.map(IssueReportingEngineDiscoveryListener::convert) //
					.map(position -> ClasspathResourceSource.from(resourceName, position)) //
					.orElseGet(() -> ClasspathResourceSource.from(resourceName));
		}
		if (selector instanceof PackageSelector packageSelector) {
			return PackageSource.from(packageSelector.getPackageName());
		}
		try {
			// Both FileSource and DirectorySource call File.getCanonicalFile() to normalize the reported file which
			// can throw an exception for certain file names on certain file systems. UriSource.from(...) is affected
			// as well because it may return a FileSource or DirectorySource
			if (selector instanceof FileSelector fileSelector) {
				return fileSelector.getPosition() //
						.map(IssueReportingEngineDiscoveryListener::convert) //
						.map(position -> FileSource.from(fileSelector.getFile(), position)) //
						.orElseGet(() -> FileSource.from(fileSelector.getFile()));
			}
			if (selector instanceof DirectorySelector directorySelector) {
				return DirectorySource.from(directorySelector.getDirectory());
			}
			if (selector instanceof UriSelector uriSelector) {
				return UriSource.from(uriSelector.getUri());
			}
		}
		catch (Exception ex) {
			rethrowIfUnrecoverable(ex);
			logger.warn(ex, () -> "Failed to convert DiscoverySelector [%s] into TestSource".formatted(selector));
		}
		return null;
	}

	private static FilePosition convert(org.junit.platform.engine.discovery.FilePosition position) {
		return position.getColumn() //
				.map(column -> FilePosition.from(position.getLine(), column)) //
				.orElseGet(() -> FilePosition.from(position.getLine()));
	}

}
