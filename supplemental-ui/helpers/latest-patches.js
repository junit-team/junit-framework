'use strict'

// Antora sorts versions newest first, so the first entry of each major.minor line is its latest.
// The snapshot is a line of its own, so that it does not hide the latest release of its line.
module.exports = (versions) => {
  const seen = new Set()
  return versions.filter(({ version, displayVersion }) => {
    const line = version.split('.', 2).join('.') + (displayVersion.endsWith('-SNAPSHOT') ? '-SNAPSHOT' : '')
    return !seen.has(line) && seen.add(line)
  })
}
