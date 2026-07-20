# Alias Source Navigator

Alias Source Navigator is a JetBrains plugin that connects generated TypeScript auto-import aliases to their original source exports for both navigation and usage search.

[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32888-alias-source-navigator) · [Releases](https://github.com/HassanDomeDenea/alias-source-navigator/releases)

It is designed for Vue and Vite projects using tools such as [unplugin-auto-import](https://github.com/unplugin/unplugin-auto-import) and [unplugin-vue-components](https://github.com/unplugin/unplugin-vue-components). It also supports [Laravel Wayfinder](https://github.com/laravel/wayfinder) route methods in PhpStorm.

## Why

Auto-import tools generate declarations such as:

```ts
declare const useAuth: typeof import('../stores/auth').useAuth
```

JetBrains IDEs may navigate to the generated declaration instead of `stores/auth.ts`, while **Find Usages** on the source export may report no usages. Alias Source Navigator recognizes the declaration, continues navigation to the original named or default export, and contributes the alias references back to source usage searches.

This covers auto-imported references in TypeScript and Vue files, including stores and composables used from other TypeScript modules.

### Laravel Wayfinder

Wayfinder generates TypeScript wrappers for Laravel controller routes. Navigating a usage such as:

```ts
PatientController.autocomplete()
```

normally lands on the generated default-export object. With this plugin, **Goto Declaration** offers:

1. The PHP controller method (`PatientController::autocomplete`)
2. The Wayfinder named export (`export const autocomplete`)

Resolution uses the `@see` metadata Wayfinder already emits in the generated TypeScript file, then PhpStorm's PHP index. Non-Wayfinder symbols keep the existing navigation behavior.

## Supported declarations

```ts
const useAuth: typeof import('../stores/auth').useAuth
LoginForm: typeof import('../components/LoginForm.vue')['default']
```

The plugin handles relative module paths in `auto-imports.d.ts`, `imports.d.ts`, and `components.d.ts`.

## Installation

Install it from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32888-alias-source-navigator), or build the plugin and install the ZIP from **Settings → Plugins → Install Plugin from Disk**:

```powershell
.\gradlew.bat buildPlugin
```

The archive is written to `build/distributions`.

## Development

The default build downloads PhpStorm 2026.1.4. Local PhpStorm 2026.2+ requires Java 25 (the IDE JBR works):

```powershell
$env:JAVA_HOME = "E:\Program Files\Jetbrains\PhpStorm\jbr"
.\gradlew.bat test buildPlugin -PlocalIdePath="E:/Program Files/Jetbrains/PhpStorm"
```

## Inspiration

The behavior is inspired by Anthony Fu's [vscode-goto-alias](https://github.com/antfu/vscode-goto-alias). This project is an independent implementation for the IntelliJ Platform navigation API.

## License

[MIT](LICENSE)
