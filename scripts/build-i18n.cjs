/* Rebuild both runtime catalogues from the same reviewed, offline UI translations. */
const fs = require('fs');
const crypto = require('crypto');
const directory = 'src/main/resources/i18n';
const translations = {
    ...JSON.parse(fs.readFileSync(`${directory}/catalog.en.json`, 'utf8')),
    ...JSON.parse(fs.readFileSync(`${directory}/overrides.en.json`, 'utf8'))
};
const entries = Object.entries(translations).sort(([a],[b]) => a.localeCompare(b));
const key = source => 'ui_'+crypto.createHash('sha1').update(source.trim().replace(/\s+/g,' ')).digest('hex').slice(0,12);
// Keep supplementary Unicode characters explicit and portable in the XML catalogue.
const escape = value => value.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/[\u{10000}-\u{10ffff}]/gu, character => '&#x'+character.codePointAt(0).toString(16)+';');
fs.writeFileSync(`${directory}/messages_en.xml`, '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">\n<properties>\n'+entries.map(([source,value])=>'<entry key="'+key(source)+'">'+escape(value)+'</entry>').join('\n')+'\n</properties>\n');
fs.writeFileSync('src/main/webapp/js/i18n-catalog.js', '// Generated offline UI translations. No translation service is used by the application.\n'+'globalThis.HrmTranslations = '+JSON.stringify(Object.fromEntries(entries),null,2)+';\n');
console.log(`Generated ${entries.length} UI translations for JSP and JavaScript.`);
