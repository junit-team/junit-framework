# Security Policy

[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/9607/badge)](https://www.bestpractices.dev/projects/9607) [![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/junit-team/junit-framework/badge)](https://scorecard.dev/viewer/?uri=github.com/junit-team/junit-framework)

## JAR Signing

JUnit JARs released on Maven Central are signed.
You'll find more information about the key here: [KEYS](./KEYS)

## Supported Versions

| Version | Supported          |
|---------| ------------------ |
| 6.0.x   | :white_check_mark: |
| 5.14.x  | :white_check_mark: |
| < 5.14  | :x:                |

## Reporting a Vulnerability

To report a security vulnerability, you have two options:

- [Privately report a vulnerability](https://github.com/junit-team/junit-framework/security/advisories/new) on GitHub (see [docs](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability) for details)
- Send an email to security@junit.org. You can use the [published OpenPGP key](https://keys.openpgp.org/search?q=security%40junit.org) with fingerprint `0152DA30EABC7ABADCB09D10D9A6B1329D191D25` to encrypt the message body.
