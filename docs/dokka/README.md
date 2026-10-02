# Dokka branding

The HTML documentation uses Dokka's standard layout with an additive Weblate
stylesheet. Configure the assets through `pluginsConfiguration.html` in the
Android library's Gradle build. Javadoc output uses its existing configuration.

The HTML index includes the root `README.md`. Its first heading must be
`# Module Kotlin SDK for Weblate`, matching the HTML publication's `moduleName`
in the Android library's Gradle build. Register the include through
`dokkaSourceSets.configureEach` so Dokka's source analysis picks up the module
documentation. Keep all README content below this heading so Dokka includes it
in the module documentation.

The SVGs are copied unchanged from the Weblate graphics repository:

- `Logo-Darktext.svg`: `logo-text/Logo-Darktext.svg`, used in light mode.
- `Logo-Whitetext.svg`: `logo-text/Logo-Whitetext.svg`, used in dark mode.
- `logo-icon.svg`: `logo/weblate.svg`, used as the favicon.

The logos are licensed under Apache-2.0, matching the package license. Asset
copyright and licensing are recorded in the root `REUSE.toml`.

The fonts match the official documentation and load from these stylesheets:

- <https://weblate.org/static/weblate_fonts/source-sans-3.css>
- <https://weblate.org/static/weblate_fonts/source-code-pro.css>

System fonts provide fallbacks when those resources are unavailable. The palette
uses Weblate green with neutral surfaces inspired by the official Furo theme;
dark mode uses a brighter green for readable links.

After updating Dokka, generate HTML with
`./gradlew :weblate-android:dokkaGeneratePublicationHtml` and serve
`weblate-android/build/dokka/html` over HTTP. Check the landing page, package list,
and a class/member page in both themes at desktop and mobile widths. Verify the
logos, nested asset paths, fonts and font fallbacks, keyboard focus, search,
source-set filtering, sidebar navigation and resizing, and saved theme preference.
The header's permanent `theme-dark` class and the UI kit's stylesheet ordering
require explicit overrides in `weblate.css`.
