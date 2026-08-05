/* eslint-disable @typescript-eslint/no-require-imports */
const fs = require('fs');
const postcss = require('postcss');
const css = fs.readFileSync(process.argv[2], 'utf8');
try {
  postcss.parse(css);
  console.log('PARSE OK, length=' + css.length);
} catch (e) {
  console.log('PARSE ERROR: ' + e.message);
  if (e.line && e.column) console.log('at line ' + e.line + ', column ' + e.column);
}
