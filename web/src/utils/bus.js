/**
 * 极简事件总线（项目仅 auth:expired 一处跨组件广播，不值得引入 mitt）
 */
const handlers = {}

export const bus = {
  on(event, fn) {
    ;(handlers[event] = handlers[event] || []).push(fn)
  },
  off(event, fn) {
    if (!handlers[event]) return
    handlers[event] = handlers[event].filter((f) => f !== fn)
  },
  emit(event, payload) {
    ;(handlers[event] || []).slice().forEach((fn) => {
      try {
        fn(payload)
      } catch (e) {
        console.error(`[bus] ${event} handler error`, e)
      }
    })
  },
}
