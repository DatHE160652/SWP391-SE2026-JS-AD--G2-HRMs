# Language settings

Open the appearance panel and use **Ngôn ngữ / Language** to choose **Tiếng Việt** or **English**. Vietnamese is the default. Changing language reloads the current URL and retains its query parameters.

The preference is saved in the `hrms.language` cookie for one year. The cookie allows JSP and JavaScript to render the same language. `hrms.language.v1` in local storage synchronizes other tabs on the same origin. Appearance settings use their existing, separate preference. This is a browser preference; it does not change employee records or require a database migration.

Static page copy, dynamic interface templates, accessibility labels, placeholders, validation feedback, response messages, enum labels, and date/number formatting use the selected language. Employee names, department names, account identifiers, passwords, free-text reasons and other entered data are not translated. Application requests do not contact a translation service.

## Maintaining translations

- Edit `src/main/resources/i18n/catalog.en.json` for additional Vietnamese-to-English UI copy. Reviewed terminology belongs in `overrides.en.json`.
- Run `node scripts/build-i18n.cjs` to regenerate `messages_en.xml` and `src/main/webapp/js/i18n-catalog.js` from the same catalogue. The XML resource is committed; local `.properties` configuration files are excluded.
- JavaScript uses `HrmI18n.t()` for static text and `HrmI18n.fragment()` for static HTML fragments, before interpolating record data. Do not translate business-data values or strings used in comparisons.
- Use `HrmI18n.label()` for system status codes; keep the original codes in requests, comparisons and CSS classes. Feedback templates can use `{0}`, `{1}` placeholders in the catalogue; matching feedback preserves the interpolated values.
- JSP uses `Language.text(request, key, vietnamese)` for escaped static copy. Keys are `ui_` plus the first 12 hexadecimal characters of the normalized source text's SHA-1 hash; the build script generates those keys. `Language.message()` translates server-owned feedback, with `<c:out>` handling HTML escaping.
- Restart/redeploy the application after changing Java or resource bundles. Browser scripts include their modification time to invalidate old cached versions.

## Verification

Run `node --test tests/i18n.test.cjs tests/form-validation.test.cjs`. `tests/LanguagePreferenceTest.java` checks Java cookie selection, catalogue lookup, data preservation, and output escaping against the compiled application and servlet dependencies.

The implementation was additionally checked by compiling all JSP pages and exercising English/Vietnamese login, appearance-panel switching, preference persistence, validation, and dialogs in a headless browser.
