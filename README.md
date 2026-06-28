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
