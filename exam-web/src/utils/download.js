/**
 * 将 blob 响应保存为文件。res 为 axios 完整响应（http.js 对 responseType:'blob' 直接放行）。
 * 文件名优先取 Content-Disposition 的 filename*=UTF-8''，其次 filename=，都没有则用 fallbackName。
 */
export function downloadBlob(res, fallbackName) {
  const name = parseFilename(res?.headers?.['content-disposition']) || fallbackName || 'download'
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

/** 过滤文件名中的 \ / : * ? " < > | 及控制字符为 _，并截断到 60 字；为空时返回 fallback。 */
export function safeFileName(name, fallback = 'download') {
  // eslint-disable-next-line no-control-regex
  const cleaned = String(name ?? '').trim().replace(/[\\/:*?"<>|\x00-\x1f\x7f]/g, '_').slice(0, 60)
  return cleaned || fallback
}

function parseFilename(disposition) {
  if (!disposition) return ''
  const star = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/.exec(disposition)
  if (star) {
    try { return decodeURIComponent(star[1].trim()) } catch { return star[1].trim() }
  }
  const plain = /filename\s*=\s*(?:"([^"]*)"|([^;]+))/.exec(disposition)
  if (plain) {
    const raw = (plain[1] ?? plain[2] ?? '').trim()
    try { return decodeURIComponent(raw) } catch { return raw }
  }
  return ''
}

/**
 * blob 请求遇到后端业务错误时，响应体是 JSON 而不是文件。
 * 返回错误 message（表示失败），返回空字符串表示是正常文件流。
 */
export async function blobErrorMessage(res) {
  const blob = res?.data
  if (!blob || blob.type !== 'application/json') return ''
  try {
    const body = JSON.parse(await blob.text())
    return body.message || '请求失败'
  } catch {
    return '请求失败'
  }
}
