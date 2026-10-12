/**
 * blob 下载工具（目前仅成绩 CSV 导出使用）
 *
 * 后端导出接口：成功返回 text/csv（带 BOM），业务失败返回 HTTP 200 + JSON，
 * 因此必须按 blob.type 分流，不能只看 HTTP 状态码。
 */
import { ElMessage } from 'element-plus'

/**
 * 处理 blob 响应：是错误 JSON 则提示，是文件则触发浏览器下载
 * @param {object} response axios 原始响应（responseType: 'blob'，含 headers）
 * @param {string} fallbackName 拿不到响应头文件名时的兜底文件名
 * @returns {Promise<boolean>} true=已下载 false=业务错误
 */
export async function downloadBlobResponse(response, fallbackName = '下载文件') {
  const blob = response.data
  const ctype = blob?.type || ''

  // 成功是 text/csv；application/json 或纯 text 视为业务错误
  if (ctype.includes('json') || (ctype.includes('text') && !ctype.includes('csv'))) {
    let msg = '导出失败'
    try {
      const body = JSON.parse(await blob.text())
      if (body && body.msg) msg = body.msg
    } catch {
      /* 保持兜底文案 */
    }
    ElMessage.error(msg)
    return false
  }

  // 解析文件名：优先 filename*=UTF-8''xxx（RFC 5987），其次普通 filename
  let filename = fallbackName
  const disposition = String(response.headers?.['content-disposition'] || '')
  const star = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (star) {
    filename = decodeURIComponent(star[1])
  } else {
    const plain = disposition.match(/filename="?([^";]+)"?/i)
    if (plain) {
      try {
        filename = decodeURIComponent(plain[1])
      } catch {
        filename = plain[1]
      }
    }
  }

  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename.trim()
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
  return true
}
