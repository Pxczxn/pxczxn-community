import { onBeforeUnmount } from 'vue'

/**
 * Lets a view accept state changes from its newest in-flight request only.
 */
export function useLatestRequest() {
  let sequence = 0
  let unmounted = false

  onBeforeUnmount(() => {
    unmounted = true
    sequence += 1
  })

  function beginRequest() {
    const requestId = ++sequence
    return () => !unmounted && requestId === sequence
  }

  return { beginRequest }
}
