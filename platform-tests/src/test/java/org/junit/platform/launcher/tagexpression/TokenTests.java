/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.tagexpression;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

class TokenTests {

	@Test
	void startIndexOfTokenString() {
		assertThat(new Token(0, "!").strippedTokenStartIndex()).isEqualTo(0);
		assertThat(new Token(0, "  !").strippedTokenStartIndex()).isEqualTo(2);
		assertThat(new Token(7, "!").strippedTokenStartIndex()).isEqualTo(7);
	}

	@Test
	void endIndexExclusive() {
		assertThat(new Token(0, "!").endIndexExclusive()).isEqualTo(1);
		assertThat(new Token(0, "  !").endIndexExclusive()).isEqualTo(3);
		assertThat(new Token(7, "!").endIndexExclusive()).isEqualTo(8);
	}

	@Test
	void lastCharacterIndex() {
		assertThat(new Token(0, "!").lastCharacterIndex()).isEqualTo(0);
		assertThat(new Token(0, "  !").lastCharacterIndex()).isEqualTo(2);
		assertThat(new Token(7, "!").lastCharacterIndex()).isEqualTo(7);
	}

	@Test
	void concatenateTwoTokens() {
		var tokens = new Tokenizer().tokenize(" ! foo");
		var one = tokens.get(0);
		var two = tokens.get(1);
		var joined = one.concatenate(two);
		assertThat(joined.rawString()).isEqualTo(" ! foo");
		assertThat(joined.startIndex()).isEqualTo(0);
	}

}
