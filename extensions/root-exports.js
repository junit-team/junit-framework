'use strict'

module.exports.register = function ({ config }) {
  const { rootComponentName, fileName = rootComponentName } = config
  this.on('navigationBuilt', ({ contentCatalog }) => {
    contentCatalog.getFiles()
      .filter(file => file.src.family === 'export')
      .filter(file => file.src.component === rootComponentName)
      .forEach(file => removeRootComponentNameFromFile(file, `${fileName}-${file.src.version}`))
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
