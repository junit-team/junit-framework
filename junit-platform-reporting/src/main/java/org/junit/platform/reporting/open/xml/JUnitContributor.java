/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.open.xml;

import static java.util.Collections.emptyList;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apiguardian.api.API;
import org.opentest4j.reporting.schema.Namespace;
import org.opentest4j.reporting.tooling.spi.htmlreport.Contributor;
import org.opentest4j.reporting.tooling.spi.htmlreport.KeyValuePairs;
import org.opentest4j.reporting.tooling.spi.htmlreport.Section;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contributes a section containing JUnit-specific metadata for each test node
 * to the open-test-reporting HTML report.
 *
 * @since 1.12
 */
@SuppressWarnings("exports") // we don't want to export 'org.opentest4j.reporting.tooling.spi' transitively
@API(status = INTERNAL, since = "1.12")
public class JUnitContributor implements Contributor {

	public JUnitContributor() {
	}

	@Override
	public List<Section> contributeSectionsForTestNode(Context context) {
		return findChild(context.element(), Namespace.REPORTING_CORE, "metadata") //
				.map(metadata -> {
					Map<String, String> table = new LinkedHashMap<>();
					findChild(metadata, JUnitFactory.NAMESPACE, "type") //
							.map(Node::getTextContent) //
							.ifPresent(value -> table.put("Type", value));
					findChild(metadata, JUnitFactory.NAMESPACE, "uniqueId") //
							.map(Node::getTextContent) //
							.ifPresent(value -> table.put("Unique ID", value));
					findChild(metadata, JUnitFactory.NAMESPACE, "legacyReportingName") //
							.map(Node::getTextContent) //
							.ifPresent(value -> table.put("Legacy reporting name", value));
					return table;
				}) //
				.filter(table -> !table.isEmpty()) //
				.map(table -> List.of(Section.builder() //
						.title("JUnit metadata") //
						.order(15) //
						.addBlock(KeyValuePairs.builder().content(table).build()) //
						.build())) //
				.orElse(emptyList());
	}

	private static Optional<Node> findChild(Node parent, Namespace namespace, String localName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (localName.equals(child.getLocalName()) && namespace.getUri().equals(child.getNamespaceURI())) {
				return Optional.of(child);
			}
		}
		return Optional.empty();
	}
}
