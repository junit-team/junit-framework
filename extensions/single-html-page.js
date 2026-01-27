'use strict'

const fsp = require('node:fs/promises')
const ospath = require('node:path')

module.exports.register = function ({ config }) {
  const converter = { convert, extname: '.html', mediaType: 'text/html' }
  this.require('@antora/assembler').configure(this, converter, config)
}

async function convert (doc, convertAttributes, buildConfig) {
  const Asciidoctor = this.require('@asciidoctor/core')()
  Asciidoctor.convert(doc.contents, {
    attributes: convertAttributes,
    safe: 'unsafe',
    to_file: convertAttributes.outfile,
    mkdirs: true,
  })
}
