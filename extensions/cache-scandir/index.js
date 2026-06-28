// Modified version of https://github.com/spring-io/antora-extensions/blob/fee110d465480db87f029426bfab7da8068e282e/lib/cache-scandir/index.js
// SPDX-License-Identifier: Apache-2.0

'use strict'

const copyRecursive = require('./copy-recursive')

const [, , ...args] = process.argv
const [scanDir, cacheDir] = args

copyRecursive(scanDir, cacheDir)
