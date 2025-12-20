'use strict'

module.exports.register = function ({ config }) {
  const { rootComponentName, fileName = rootComponentName } = config
  this.on('navigationBuilt', ({ contentCatalog }) => {
    const { versions } = contentCatalog.getComponent(rootComponentName)
    versions.forEach(version => {
      const file = contentCatalog.resolveResource(`${version.version}@${rootComponentName}:ROOT:index.pdf`, {}, 'export', ['export'])
      if (file) {
        if (version.prerelease === '-SNAPSHOT') {
          contentCatalog.removeFile(file)
        } else {
          removeRootComponentNameFromFile(file, `${fileName}-${version.version}`)
        }
      }
    })
  })
}

function removeRootComponentNameFromFile(file, fileName) {
  if (file.out) {
    file.out.dirname = removeFirstSegment(file.out.dirname)
    file.out.basename = file.out.basename.replace('index', fileName)
    file.out.path = removeFirstSegment(file.out.path).replace('index', fileName)
    file.out.moduleRootPath = '.'
    file.out.rootPath = '..'
  }
  if (file.pub) {
    if (file.pub.rootPath) {
      file.pub.rootPath = '..'
    }
    if (file.pub.moduleRootPath) {
      file.pub.moduleRootPath = '.'
    }
    file.pub.url = removeFirstSegment(file.pub.url).replace('index', fileName)
  }
}

function removeFirstSegment(path) {
  if (path) {
    if (path.startsWith('/')) {
      return ('/' + path.split('/').slice(2).join('/')) || '.'
    } else {
      return (path.split('/').slice(1).join('/')) || '.'
    }
  }
  return path
}
