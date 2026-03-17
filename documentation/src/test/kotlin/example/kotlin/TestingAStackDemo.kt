/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

// tag::user_guide[]
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.EmptyStackException
import java.util.Stack

@DisplayName("A stack")
class TestingAStackDemo {
    @Test
    @DisplayName("is instantiated with new Stack()")
    fun isInstantiatedWithNew() {
        Stack<Any>()
    }

    @Nested
    @DisplayName("when new")
    inner class WhenNew {
        lateinit var stack: Stack<Any>

        @BeforeEach
        fun createNewStack() {
            stack = Stack()
        }

        @Test
        @DisplayName("is empty")
        fun isEmpty() {
            assertTrue(stack.isEmpty())
        }

        @Test
        @DisplayName("throws EmptyStackException when popped")
        fun throwsExceptionWhenPopped() {
            assertThrows<EmptyStackException> { stack.pop() }
        }

        @Test
        @DisplayName("throws EmptyStackException when peeked")
        fun throwsExceptionWhenPeeked() {
            assertThrows<EmptyStackException> { stack.peek() }
        }

        @Nested
        @DisplayName("after pushing an element")
        inner class AfterPushing {
            val anElement = "an element"

            @BeforeEach
            fun pushAnElement() {
                stack.push(anElement)
            }

            @Test
            @DisplayName("it is no longer empty")
            fun isNotEmpty() {
                assertFalse(stack.isEmpty())
            }

            @Test
            @DisplayName("returns the element when popped and is empty")
            fun returnElementWhenPopped() {
                assertEquals(anElement, stack.pop())
                assertTrue(stack.isEmpty())
            }

            @Test
            @DisplayName("returns the element when peeked but remains not empty")
            fun returnElementWhenPeeked() {
                assertEquals(anElement, stack.peek())
                assertFalse(stack.isEmpty())
            }
        }
    }
}
// end::user_guide[]
