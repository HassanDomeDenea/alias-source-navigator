# Changelog

## 0.2.1 - 2026-07-14

- Run generated declaration discovery and PSI resolution inside a cancellable read action.
- Avoid read-access exceptions when Find Usages runs on a pooled thread in PhpStorm 2026.1.

## 0.2.0 - 2026-07-14

- Include references to generated auto-import declarations when searching usages of the original source export.
- Support source usage discovery from both TypeScript and Vue consumers.
- Share declaration resolution between navigation and reference search.

## 0.1.0 - 2026-07-13

- Initial release.
- Redirect generated TypeScript auto-import aliases to named source exports.
- Redirect generated Vue component aliases to their source files.
