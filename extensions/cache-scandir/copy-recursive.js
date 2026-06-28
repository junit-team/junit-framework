// Modified version of https://github.com/spring-io/antora-extensions/blob/fee110d465480db87f029426bfab7da8068e282e/lib/cache-scandir/copy-recursive.js
// SPDX-License-Identifier: Apache-2.0

'use strict'

const fs = require('fs')
const path = require('path')

const copyRecursiveSync = function (src, dest) {
  const exists = fs.existsSync(src)
  const stats = exists && fs.statSync(src)
  const isDirectory = exists && stats.isDirectory()
  if (isDirectory) {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest, { recursive: true })
    }
    fs.readdirSync(src).forEach(function (childItemName) {
      copyRecursiveSync(path.join(src, childItemName), path.join(dest, childItemName))
    })
  } else {
    fs.copyFileSync(src, dest)
  }
}

function copyRecursive (scanDir, cacheDir) {
  copyRecursiveSync(scanDir, cacheDir)
}

module.exports = copyRecursive
