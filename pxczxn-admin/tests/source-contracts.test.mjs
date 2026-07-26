import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function source(path) {
  return readFile(new URL(`../${path}`, import.meta.url), 'utf8')
}

test('development ports target the accepted local topology', async () => {
  const viteConfig = await source('vite.config.ts')

  assert.match(viteConfig, /port:\s*8848/)
  assert.match(viteConfig, /http:\/\/localhost:8849/)
  assert.match(viteConfig, /['"]\/admin-api['"]/)
})

test('community governance routes and APIs remain registered', async () => {
  const [router, api] = await Promise.all([
    source('src/router/index.ts'),
    source('src/api/community.ts')
  ])

  for (const area of ['comments', 'moments', 'interactions', 'reviews']) {
    assert.match(router, new RegExp(`community/${area}`))
    assert.match(api, new RegExp(`/admin-api/community/${area}`))
  }
})

test('the three supported visual themes remain available', async () => {
  const themeStore = await source('src/stores/theme.ts')

  for (const theme of ['light-theme', 'dark-theme', 'starry-theme']) {
    assert.match(themeStore, new RegExp(theme))
  }
})
