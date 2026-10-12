/**
 * 倒计时 hook：基于服务器结束时间计算剩余
 * 时间串格式 "YYYY-MM-DD HH:mm:ss"（后端 @JsonFormat），用 dayjs 显式解析避免兼容问题
 *
 * @param {string|number|dayjs.Dayjs} endTime 结束时间
 * @param {object} opts
 * @param {Function} opts.onEnd 归零回调（只触发一次）
 */
import { ref, computed } from 'vue'
import dayjs from 'dayjs'

export function useCountdown(endTime, opts = {}) {
  const { onEnd } = opts
  const end = dayjs(endTime, 'YYYY-MM-DD HH:mm:ss')

  const remainMs = ref(end.valueOf() - Date.now())
  const text = ref('--:--:--')
  /** 剩余不足 5 分钟 */
  const urgent = computed(() => remainMs.value > 0 && remainMs.value <= 5 * 60 * 1000)
  const ended = ref(false)

  let timer = null

  function pad(n) {
    return String(n).padStart(2, '0')
  }

  function tick() {
    remainMs.value = end.valueOf() - Date.now()
    if (remainMs.value <= 0) {
      remainMs.value = 0
      text.value = '00:00:00'
      if (!ended.value) {
        ended.value = true
        stop()
        onEnd && onEnd()
      }
      return
    }
    const totalSeconds = Math.floor(remainMs.value / 1000)
    const h = Math.floor(totalSeconds / 3600)
    const m = Math.floor((totalSeconds % 3600) / 60)
    const s = totalSeconds % 60
    text.value = `${pad(h)}:${pad(m)}:${pad(s)}`
  }

  function start() {
    stop()
    tick()
    timer = setInterval(tick, 1000)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  return { text, urgent, ended, remainMs, start, stop }
}
