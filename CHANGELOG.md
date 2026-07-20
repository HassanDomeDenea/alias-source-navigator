# Changelog

## 0.3.1 - 2026-07-20

- Fix Wayfinder PHP target resolution when navigation starts from the exported method name offset.
- Restore the dual-target chooser (PHP method + named TypeScript export) for Wayfinder route methods.

## 0.3.0 - 2026-07-20

- Add Laravel Wayfinder navigation for generated controller route methods.
- On Goto Declaration for a Wayfinder method usage, offer the PHP controller method first and the named TypeScript export second.
- Prefer the Wayfinder `export const <method>` declaration over the default-export object shorthand.

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
