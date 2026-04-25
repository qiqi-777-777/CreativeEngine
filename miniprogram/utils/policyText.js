const KNOWN_SEARCH_TERMS = [
  '高校毕业生',
  '毕业生',
  '大学生',
  '创业'
]

function normalizeLine(line) {
  return String(line || '').replace(/\s+/g, '').trim()
}

function addSearchTerm(rawValue, terms) {
  const value = String(rawValue || '').trim()
  if (!value) return

  let matchedKnownTerm = false
  let remaining = value
  KNOWN_SEARCH_TERMS.forEach((term) => {
    if (remaining.includes(term)) {
      terms.push(term)
      remaining = remaining.replace(term, ' ')
      matchedKnownTerm = true
    }
  })

  if (!matchedKnownTerm) {
    terms.push(value)
    return
  }

  remaining
    .trim()
    .split(/\s+/)
    .filter((item) => item.trim().length > 1)
    .forEach((item) => terms.push(item.trim()))
}

function parseSearchTerms(keyword) {
  const terms = []
  String(keyword || '')
    .trim()
    .replace(/[,，、+\s;/；|]+/g, ' ')
    .split(' ')
    .forEach((item) => addSearchTerm(item, terms))

  return [...new Set(terms)]
}

function isChromeLine(line) {
  const value = line.trim()
  const compact = normalizeLine(value)

  if (!value || value === '|' || value === '>' || value === '<') return true
  if (/^https?:\/\/www\.gov\.cn\/?$/.test(value)) return true
  if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}/.test(value)) return true
  if (/^字号[:：]/.test(value)) return true
  if (/^责任编辑[:：]/.test(value)) return true
  if (/^首页\s*>/.test(value)) return true

  return [
    '首页',
    '简',
    '繁',
    'EN',
    '登录',
    '个人中心',
    '退出',
    '邮箱',
    '无障碍',
    '默认',
    '大',
    '超大',
    '打印',
    '【我要纠错】',
    '我要纠错',
    '相关稿件',
    '链接：',
    '全国人大',
    '全国政协',
    '国家监察委员会',
    '最高人民法院',
    '最高人民检察院',
    '国务院部门网站',
    '地方政府网站',
    '驻港澳机构网站',
    '驻外机构',
    '中国政府网',
    '网站纠错'
  ].includes(compact) ||
    compact.includes('网站无障碍开关') ||
    compact.startsWith('主办单位：') ||
    compact.startsWith('版权所有：') ||
    compact.startsWith('网站标识码') ||
    compact.startsWith('京ICP备') ||
    compact.startsWith('京公网安备')
}

function parseMeta(line, meta) {
  const sourceMatch = line.match(/^来源[:：]\s*(.+)$/)
  if (sourceMatch) {
    meta.sourceInfo = sourceMatch[1].trim()
    return true
  }

  const dateMatch = line.match(/^发布日期[:：]\s*(.+)$/)
  if (dateMatch) {
    meta.publishDate = dateMatch[1].trim()
    return true
  }

  const agencyMatch = line.match(/^发文机关[:：]\s*(.+)$/)
  if (agencyMatch) {
    meta.agency = agencyMatch[1].trim()
    return true
  }

  const docNoMatch = line.match(/^发文字号[:：]\s*(.+)$/)
  if (docNoMatch) {
    meta.docNo = docNoMatch[1].trim()
    return true
  }

  const urlMatch = line.match(/^原文链接[:：]\s*(.+)$/)
  if (urlMatch) {
    meta.originalUrl = urlMatch[1].trim()
    return true
  }

  return false
}

function isTitleDuplicate(line, title) {
  if (!title) return false

  const lineValue = normalizeLine(line.replace(/_.*?中国政府网.*/, ''))
  const titleValue = normalizeLine(title)
  return lineValue === titleValue || lineValue.includes(titleValue)
}

function parsePolicyText(content, title) {
  const meta = {}
  const paragraphs = []
  const lines = String(content || '')
    .replace(/\r/g, '\n')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)

  lines.forEach((line) => {
    if (parseMeta(line, meta)) return
    if (isChromeLine(line)) return
    if (isTitleDuplicate(line, title)) return

    paragraphs.push(line)
  })

  const uniqueParagraphs = paragraphs.filter((line, index) => {
    return paragraphs.findIndex((item) => normalizeLine(item) === normalizeLine(line)) === index
  })

  const bodyText = uniqueParagraphs.join('\n\n')
  const snippetSource = uniqueParagraphs.join('')
  const displaySnippet = snippetSource.length > 96
    ? `${snippetSource.slice(0, 96)}...`
    : snippetSource

  return {
    ...meta,
    contentParagraphs: uniqueParagraphs,
    displayContent: bodyText,
    displaySnippet
  }
}

function formatPolicy(policy) {
  const parsed = parsePolicyText(policy && policy.content, policy && policy.policyName)
  return {
    ...policy,
    ...parsed
  }
}

module.exports = {
  parsePolicyText,
  formatPolicy,
  parseSearchTerms
}
