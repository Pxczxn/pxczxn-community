import fs from 'node:fs';
import postcss from 'postcss';
import tailwind from '@tailwindcss/postcss';

const css = fs.readFileSync('app/globals.css', 'utf8');
postcss([tailwind])
  .process(css, { from: 'app/globals.css' })
  .then(result => {
    fs.writeFileSync('.tmp-tailwind-out.css', result.css);
    console.log('TAILWIND BUILD OK, 输出 ' + result.css.length + ' 字节');
  })
  .catch(e => {
    console.error('TAILWIND BUILD FAILED: ' + e.message);
    if (e.line) console.error('at line ' + e.line + ', column ' + e.column);
    process.exit(1);
  });
