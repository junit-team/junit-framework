/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example

class FibonacciCalculator {
    private val fibonacci =
        sequence {
            yield(0) // 0th Fibonacci number
            yield(1) // first Fibonacci number
            var cur = 1
            var next = 1
            while (true) {
                yield(next) // next Fibonacci number
                val tmp = cur + next
                cur = next
                next = tmp
            }
        }

    fun fib(fibonacciNumber: Int) = fibonacci.elementAt(fibonacciNumber)
}
