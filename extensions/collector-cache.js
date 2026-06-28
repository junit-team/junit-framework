// Modified version of https://github.com/spring-io/antora-extensions/blob/fee110d465480db87f029426bfab7da8068e282e/lib/inject-collector-cache-config-extension.js
// SPDX-License-Identifier: Apache-2.0

'use strict'

const expandPath = require('@antora/expand-path-helper')
const ospath = require('path')
const resolvedCopyRecursiveJs = require.resolve('./cache-scandir')
const { createHash } = require('crypto')

module.exports.register = function ({ playbook, config = {} }) {
  const logger = this.getLogger('collector-cache')
  const siteUrl = playbook.site?.url
  const excludes = config.excludes || []
  const configuredBaseCacheUrl = config.baseCacheUrl
  if (!siteUrl && !configuredBaseCacheUrl) {
    throw new Error(
      "One of playbook site.url or collector-cache plugin's base_cache_url property is required."
    )
  }
  const baseCacheUrl = configuredBaseCacheUrl || siteUrl + '/.cache'
  const getUserCacheDir = this.require('cache-directory')
  const fs = this.require('fs')
  const decompress = this.require('decompress')
  const { concat: get } = this.require('simple-get')

  const outputDir = ospath.join(playbook.dir, 'build/antora/collector-cache/.cache')
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true })
  }

  const zipInfo = []
  this.once('contentAggregated', async ({ playbook, contentAggregate }) => {
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
          const zipFileName = `${cacheDirName}.zip`
          const zipCacheFile = ospath.join(outputDir, zipFileName)
          if (!fs.existsSync(baseCollectorCacheCacheDir)) {
            fs.mkdirSync(baseCollectorCacheCacheDir, { recursive: true })
          }
          if (!fs.existsSync(cacheDir)) {
            // try and restore from URL by downloading zip
            const cacheUrl = `${baseCacheUrl}/${cacheDirName}.zip`
            const content = await download(get, cacheUrl).then((content) => content)
            if (content) {
              fs.writeFileSync(zipCacheFile, content)
              await decompress(zipCacheFile, ospath.join(baseCollectorCacheCacheDir, cacheDirName)).then((files) =>
                logger.debug(`Successfully unzipped ${zipCacheFile}.`)
              )
              logger.info(`Successfully restored cache from ${cacheUrl}`)
            } else {
              logger.info(`Unable to restore cache from ${cacheUrl}`)
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
            // add the zip of cache to be published
            zipInfo.push({ cacheDir, zipCacheFile })
          }
        }
      }
    }
  })
  this.once('beforePublish', async () => {
    for (const info of zipInfo) {
      await zip(fs, logger, info.cacheDir, info.zipCacheFile)
    }
  })
}

function download (get, url) {
  return new Promise((resolve, reject) =>
    get({ url }, (err, response, contents) => {
      // return resolve(undefined)
      if (response?.statusCode === 404) return resolve(undefined)
      if (err) return reject(err)
      if (response?.statusCode === 200) return resolve(contents)
      const message = `${url} returned response code ${response?.statusCode} (${response?.statusMessage})`
      reject(Object.assign(new Error(message), { name: 'HTTPError' }))
    })
  )
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

const zip = async function (fs, logger, src, destination) {
  const path = require('path')
  const { ZipArchive } = await import('archiver')
  const destParent = path.dirname(destination)
  if (!fs.existsSync(destParent)) {
    fs.mkdirs(destParent, { recursive: true })
  }
  const output = fs.createWriteStream(destination)
  const archive = new ZipArchive({
    zlib: { level: 9 }, // Sets the compression level.
  })

  // good practice to catch warnings (ie stat failures and other non-blocking errors)
  archive.on('warning', function (err) {
    if (err.code === 'ENOENT') {
      // log warning
    } else {
      // throw error
      throw err
    }
  })

  // good practice to catch this error explicitly
  archive.on('error', function (err) {
    throw err
  })

  // pipe archive data to the file
  archive.pipe(output)

  archive.directory(src, false)

  await archive.finalize()

  logger.info(`Saving ${src} into ${destination}`)
}
