const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8849';

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function json(path) {
  const response = await fetch(`${baseUrl}${path}`);
  assert(response.ok, `${path} returned HTTP ${response.status}`);
  return response.json();
}

const health = await json('/api/v1/health');
assert(health.data?.status === 'UP', 'community health must be UP');

const search = await json('/api/v1/public/search?keyword=health&type=ALL&pageNum=1&pageSize=3');
assert(search.code === 200, 'public search must succeed');

const rss = await fetch(`${baseUrl}/api/v1/public/rss/articles.xml`);
assert(rss.ok, `RSS returned HTTP ${rss.status}`);
assert(rss.headers.get('etag'), 'RSS must set an ETag');
assert((rss.headers.get('content-type') || '').includes('application/rss+xml'), 'RSS content type must be XML');

const cachedRss = await fetch(`${baseUrl}/api/v1/public/rss/articles.xml`, {
  headers: { 'If-None-Match': rss.headers.get('etag') },
});
assert(cachedRss.status === 304, `RSS conditional request returned ${cachedRss.status}`);

const analytics = await json('/api/v1/analytics/me');
assert(analytics.code === 401, 'anonymous creator analytics must be rejected');

console.log(`M6 public smoke E2E passed against ${baseUrl}`);
