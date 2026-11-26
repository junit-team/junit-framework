# JUnit Documentation Site

This branch contains the Antora project for building JUnit's documentation site.

## Prerequisites

It requires Node.js (for Antora) and Ruby (for asciidoctor.pdf).

A `.tool-versions` file can be used with `asdf` or `mise`:

```
asdf install # or mise install
npm install
bundle install
```

## Building

```
npm run build
```

The site is written to `build/site`.

## Previewing

```
npm run serve
```

This serves the built site at http://localhost:8080.
The search only works when the site is served via HTTP, not when opening the files directly.

## Search

Search is provided by [Pagefind](https://pagefind.app) via the [antora-pagefind-extension](https://github.com/marcphilipp/antora-pagefind-extension).
The index is built as part of `npm run build`.
To exclude a page or a component version from the index, set the `noindex` attribute as described in the extension's README.
