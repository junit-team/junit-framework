// Modified version of https://github.com/spring-io/antora-extensions/blob/fee110d465480db87f029426bfab7da8068e282e/lib/inject-collector-cache-config-extension.js
// SPDX-License-Identifier: Apache-2.0

'use strict'

const expandPath = require('@antora/expand-path-helper')
const ospath = require('path')
const resolvedCopyRecursiveJs = require.resolve('./cache-scandir')
const { createHash } = require('crypto')
// @actions/cache is ESM-only, so load it via dynamic import from this CommonJS module.
const cacheLib = import('@actions/cache')

module.exports.register = function ({ playbook, config = {} }) {
  const logger = this.getLogger('collector-cache')
  const excludes = config.excludes || []
  const getUserCacheDir = this.require('cache-directory')
  const fs = this.require('fs')

  const cacheInfo = []
  this.once('contentAggregated', async ({ playbook, contentAggregate }) => {
    const cache = await cacheLib
    const cacheAvailable = cache.isFeatureAvailable()
    if (!cacheAvailable) {
      logger.info('GitHub Actions cache is not available; skipping restore/save operations')
    }
    const baseCacheDir = getBaseCacheDir(getUserCacheDir, playbook)
    for (const { origins } of contentAggregate) {
      for (const origin of origins) {
        const { url, gitdir, refhash, worktree, descriptor, refname } = origin
        const repositoryCacheDirName = generateWorktreeFolderName({ url, gitdir, worktree })
        const baseCollectorCacheCacheDir = ospath.join(baseCacheDir, 'collector-cache', repositoryCacheDirName)
        const collectorConfig = descriptor?.ext?.collector
        if (collectorConfig !== undefined && !excludes.includes(refname)) {
          const shortref = refhash.slice(0, 7)
          const cacheDirName = `${shortref}-${refname}`
          const cacheDir = ospath.join(baseCollectorCacheCacheDir, cacheDirName)
          const cacheKey = `collector-cache-${repositoryCacheDirName}-${cacheDirName}`
          if (!fs.existsSync(baseCollectorCacheCacheDir)) {
            fs.mkdirSync(baseCollectorCacheCacheDir, { recursive: true })
          }
          if (cacheAvailable && !fs.existsSync(cacheDir)) {
            // try and restore from the GitHub Actions cache
            const restoredKey = await cache.restoreCache([cacheDir], cacheKey).catch((err) => {
              logger.warn(`Failed to restore cache for key ${cacheKey}: ${err.message}`)
              return undefined
            })
            if (restoredKey) {
              logger.info(`Successfully restored cache for key ${cacheKey}`)
            } else {
              logger.info(`Unable to restore cache for key ${cacheKey}`)
            }
          }
          if (fs.existsSync(cacheDir)) {
            // use the cache
            origin.descriptor.ext.collector = {
              scan: {
                dir: cacheDir,
              },
            }
            logger.info(`Using the cache found at ${cacheDir}`)
          } else {
            const cachedConfig = []
            const normalizedCollectorConfig = Array.isArray(collectorConfig) ? collectorConfig : [collectorConfig]
            logger.info(`Configuring collector for caching (url: ${url} | ref: ${refname})`)
            origin.descriptor.ext.collector = cachedConfig
            normalizedCollectorConfig.forEach((collector) => {
              cachedConfig.push(collector)
              const { scan: scanConfig = [] } = collector
              const normalizedScanConfigs = Array.isArray(scanConfig) ? scanConfig : [scanConfig]
              normalizedScanConfigs.forEach((normalizedScanConfig) => {
                // cache the output of the build
                const cachedCollectorConfig = createCachedCollectorConfig(normalizedScanConfig.dir, cacheDir, normalizedScanConfig.into)
                cachedConfig.push.apply(cachedConfig, cachedCollectorConfig)
              })
            })
            if (cacheAvailable) {
              cacheInfo.push({ cacheDir, cacheKey })
            }
          }
        }
      }
    }
  })
  this.once('beforePublish', async () => {
    if (cacheInfo) {
      const cache = await cacheLib
      for (const {cacheDir, cacheKey} of cacheInfo) {
        await cache.saveCache([cacheDir], cacheKey)
        logger.info(`Saved cache for key ${cacheKey}`)
      }
    }
  })
}

function getBaseCacheDir (getUserCacheDir, { dir: dot, runtime: { cacheDir: preferredDir } = {} }) {
  return preferredDir == null
    ? getUserCacheDir(`antora${process.env.NODE_ENV === 'test' ? '-test' : ''}`) || ospath.join(dot, '.cache/antora')
    : expandPath(preferredDir, { dot })
}

function generateWorktreeFolderName ({ url, gitdir, worktree }) {
  if (worktree === undefined) return ospath.basename(gitdir, '.git')
  return `${url.substr(url.lastIndexOf('/') + 1)}-${createHash('sha1').update(url).digest('hex')}`
}

function createCachedCollectorConfig (scanDir, cacheDir, subDir) {
  return [
    {
      run: {
        command: `\${{env.NODE}} '${resolvedCopyRecursiveJs}' '.' '${cacheDir}${subDir ? `/${subDir}` : ''}'`,
        dir: scanDir,
      },
    },
  ]
}
