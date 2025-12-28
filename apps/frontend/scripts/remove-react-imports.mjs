import { promises as fs } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, '..');
const srcRoot = path.join(projectRoot, 'src');

const reactDefaultOnlyRegex = /^import React from ['"]react['"];\n?/m;
const reactDefaultWithNamedRegex = /^import React,\s*\{([^}]+)\}\s*from ['"]react['"];\n?/m;

async function walk(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...await walk(fullPath));
    } else if (entry.isFile() && (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx'))) {
      files.push(fullPath);
    }
  }
  return files;
}

const files = await walk(srcRoot);
await Promise.all(files.map(async (filePath) => {
  const contents = await fs.readFile(filePath, 'utf8');
  let next = contents;
  if (reactDefaultWithNamedRegex.test(next)) {
    next = next.replace(reactDefaultWithNamedRegex, "import {$1} from 'react';\n");
  }
  if (reactDefaultOnlyRegex.test(next)) {
    next = next.replace(reactDefaultOnlyRegex, '');
  }
  if (next === contents) {
    return;
  }
  await fs.writeFile(filePath, next, 'utf8');
}));
