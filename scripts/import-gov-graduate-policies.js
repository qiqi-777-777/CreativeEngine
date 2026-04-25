#!/usr/bin/env node

const fs = require('fs')
const path = require('path')

const DEFAULT_KEYWORD = '毕业生,创业,大学生'
const SEARCH_CODE = '17da70961a7'
const SEARCH_DATA_TYPE = '107'
const DEFAULT_OUTPUT = path.join('scripts', 'generated', 'graduate_policies.sql')

const fallbackUrls = [
  'https://www.gov.cn/zhengce/zhengceku/202505/content_7025914.htm',
  'https://www.gov.cn/zhengce/202505/content_7025870.htm',
  'https://www.gov.cn/zhengce/zhengceku/202406/content_6958297.htm',
  'https://www.gov.cn/zhengce/zhengceku/202504/content_7021064.htm',
  'https://www.gov.cn/zhengce/zhengceku/202507/content_7033089.htm',
  'https://www.gov.cn/zhengce/202404/content_6942996.htm',
  'https://www.gov.cn/zhengce/zhengceku/202503/content_7015975.htm',
  'https://www.gov.cn/zhengce/zhengceku/202312/content_6918529.htm',
  'https://www.gov.cn/zhengce/zhengceku/2021-11/04/content_5648774.htm',
  'https://www.gov.cn/zhengce/zhengceku/2022-02/11/content_5673073.htm',
  'https://www.gov.cn/zhengce/zhengceku/2021-10/12/content_5642037.htm',
  'https://www.gov.cn/zhengce/zhengceku/2020-07/03/content_5523777.htm',
  'https://www.gov.cn/zhengce/202406/content_6958294.htm',
  'https://www.gov.cn/zhengce/content/2009-01/23/content_5468.htm',
  'https://www.gov.cn/zhengce/zhengceku/2014-05/13/content_8802.htm',
  'https://www.gov.cn/zhengce/content/2011-06/01/content_6594.htm',
  'https://www.gov.cn/zhengce/zhengceku/2018-12/31/content_5441621.htm'
]

function getArg(name, fallback) {
  const prefix = `--${name}=`
  const found = process.argv.find((arg) => arg.startsWith(prefix))
  return found ? found.slice(prefix.length) : fallback
}

function parseKeywords(value) {
  return String(value || '')
    .split(/[,\s，、]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function includesAny(text, keywords) {
  return keywords.some((keyword) => text.includes(keyword))
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchText(url) {
  const res = await fetch(url, {
    headers: {
      'User-Agent': 'CreativeEnginePolicyImporter/1.0 (+local educational policy import)',
      'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
    }
  })

  if (!res.ok) {
    throw new Error(`HTTP ${res.status} ${res.statusText}`)
  }

  const buffer = Buffer.from(await res.arrayBuffer())
  return buffer.toString('utf8')
}

function decodeEntities(text) {
  return text
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
}

function normalizeText(text) {
  return decodeEntities(text)
    .replace(/\r/g, '\n')
    .replace(/\u3000/g, ' ')
    .replace(/[ \t]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function stripHtml(html) {
  return normalizeText(
    html
      .replace(/<script[\s\S]*?<\/script>/gi, '')
      .replace(/<style[\s\S]*?<\/style>/gi, '')
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/p>/gi, '\n')
      .replace(/<\/div>/gi, '\n')
      .replace(/<[^>]+>/g, '')
  )
}

function normalizeLine(line) {
  return String(line || '').replace(/\s+/g, '').trim()
}

function isArticleEndLine(line) {
  const compact = normalizeLine(line)
  return [
    '【我要纠错】',
    '我要纠错',
    '责任编辑：',
    '相关稿件',
    '链接：',
    '网站纠错',
    '主办单位：国务院办公厅运行维护单位：中国政府网运行中心'
  ].some((item) => compact.startsWith(normalizeLine(item))) ||
    compact.startsWith('版权所有：') ||
    compact.startsWith('网站标识码') ||
    compact.startsWith('京ICP备')
}

function isChromeLine(line) {
  const value = line.trim()
  const compact = normalizeLine(value)

  if (!value || value === '|' || /^https:\/\/www\.gov\.cn\/?$/.test(value)) return true
  if (/^字号[:：]/.test(value)) return true
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
    '全国人大',
    '全国政协',
    '国家监察委员会',
    '最高人民法院',
    '最高人民检察院',
    '国务院部门网站',
    '地方政府网站',
    '驻港澳机构网站',
    '驻外机构',
    '中国政府网'
  ].includes(compact) || compact.includes('网站无障碍开关')
}

function extractArticleFromText(text, title) {
  const lines = text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)

  let startIndex = lines.findIndex((line) => normalizeLine(line) === '打印')
  if (startIndex >= 0) {
    startIndex += 1
  } else if (title) {
    startIndex = lines.findIndex((line) => normalizeLine(line) === normalizeLine(title))
    if (startIndex >= 0) startIndex += 1
  }

  if (startIndex < 0) return ''

  const bodyLines = []
  for (let index = startIndex; index < lines.length; index += 1) {
    const line = lines[index]

    if (isArticleEndLine(line)) break
    if (isChromeLine(line)) continue
    if (title && normalizeLine(line.replace(/_.*?中国政府网.*/, '')) === normalizeLine(title)) continue
    if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}/.test(line)) continue
    if (/^来源[:：]?$/.test(line)) continue

    bodyLines.push(line)
  }

  return normalizeText(bodyLines.join('\n\n'))
}

function absolutizeUrl(href) {
  if (!href) return ''
  if (href.startsWith('//')) return `https:${href}`
  if (href.startsWith('/')) return `https://www.gov.cn${href}`
  return href
}

function extractSearchUrls(html) {
  const urls = new Set()
  const linkPattern = /href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi
  let match

  while ((match = linkPattern.exec(html))) {
    const href = absolutizeUrl(decodeEntities(match[1]))
    if (/^https:\/\/www\.gov\.cn\/zhengce\//.test(href) && /\.htm(l)?$/.test(href)) {
      urls.add(href.split('#')[0])
    }
  }

  return [...urls]
}

async function searchGov(keyword, pages) {
  const urls = new Set()

  for (let pageNo = 1; pageNo <= pages; pageNo += 1) {
    const url = new URL('https://sousuo.www.gov.cn/sousuo/search.shtml')
    url.searchParams.set('code', SEARCH_CODE)
    url.searchParams.set('dataTypeId', SEARCH_DATA_TYPE)
    url.searchParams.set('searchWord', keyword)
    url.searchParams.set('pageNo', String(pageNo))

    try {
      const html = await fetchText(url.toString())
      extractSearchUrls(html).forEach((item) => urls.add(item))
    } catch (err) {
      console.warn(`[warn] search page ${pageNo} failed: ${err.message}`)
    }

    await sleep(1200)
  }

  return [...urls]
}

function matchField(text, label) {
  const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp(`${escaped}\\s*[:：]?\\s*([^\\n]+)`)
  const match = text.match(pattern)
  return match ? normalizeText(match[1]) : ''
}

function extractTitle(html, text) {
  const h1 = html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i)
  if (h1) return stripHtml(h1[1])

  const title = html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)
  if (title) {
    return stripHtml(title[1])
      .replace(/_.*?中国政府网.*/, '')
      .replace(/-.*?中国政府网.*/, '')
      .trim()
  }

  return text.split('\n').find((line) => line.length > 6) || ''
}

function extractContent(html, title) {
  const candidates = []
  const patterns = [
    /<div[^>]+class=["'][^"']*(?:pages_content|article|TRS_Editor|content|pages-box)[^"']*["'][^>]*>([\s\S]*?)<\/div>/gi,
    /<td[^>]+class=["'][^"']*(?:b12c|content)[^"']*["'][^>]*>([\s\S]*?)<\/td>/gi,
    /<div[^>]+id=["'][^"']*(?:UCAP-CONTENT|content|zoom)[^"']*["'][^>]*>([\s\S]*?)<\/div>/gi
  ]

  for (const pattern of patterns) {
    let match
    while ((match = pattern.exec(html))) {
      const text = stripHtml(match[1])
      if (text.length > 120) {
        candidates.push(text)
      }
    }
  }

  const best = candidates.sort((a, b) => b.length - a.length)[0] || ''
  const fallback = stripHtml(html)
  const article = extractArticleFromText(fallback, title)

  if (best.length > 800) return best
  if (article.length > 120) return article
  return best.length > 120 ? best : fallback
}

function buildKeywords(policy, seedKeywords) {
  const candidates = [
    ...seedKeywords,
    '高校毕业生',
    '就业',
    '创业',
    '基层就业',
    '三支一扶',
    '自主创业',
    '灵活就业',
    '招聘',
    '补贴',
    '贷款',
    '培训',
    '见习',
    '青年就业'
  ]

  const source = `${policy.title}\n${policy.content}`
  return [...new Set(candidates.filter((item) => source.includes(item)))].join('、')
}

async function fetchPolicy(url, seedKeywords) {
  const html = await fetchText(url)
  const fullText = stripHtml(html)
  const title = extractTitle(html, fullText)
  const content = extractContent(html, title)
  const agency = matchField(fullText, '发文机关')
  const docNo = matchField(fullText, '发文字号')
  const publishDate = matchField(fullText, '发布日期')
  const sourceName = matchField(fullText, '来源')

  const meta = [
    sourceName ? `来源：${sourceName}` : '来源：中国政府网',
    publishDate ? `发布日期：${publishDate}` : '',
    agency ? `发文机关：${agency}` : '',
    docNo ? `发文字号：${docNo}` : '',
    `原文链接：${url}`
  ].filter(Boolean)

  const cleanContent = normalizeText(`${meta.join('\n')}\n\n${content}`)

  return {
    title,
    keywords: buildKeywords({ title, content }, seedKeywords),
    content: cleanContent,
    url,
    sourceName: sourceName || '中国政府网',
    publishDate,
    agency,
    docNo
  }
}

function sqlString(value) {
  return `'${String(value || '').replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function toSql(policies) {
  const rows = policies.map((policy) => {
    const title = sqlString(policy.title)
    const keywords = sqlString(policy.keywords)
    const content = sqlString(policy.content)

    return [
      'INSERT INTO policy_data (policy_name, keywords, content, create_time, update_time)',
      `SELECT ${title}, ${keywords}, ${content}, NOW(), NOW()`,
      `WHERE NOT EXISTS (SELECT 1 FROM policy_data WHERE policy_name = ${title});`,
      `UPDATE policy_data SET keywords = ${keywords}, content = ${content}, update_time = NOW() WHERE policy_name = ${title};`
    ].join('\n')
  })

  return [
    '-- Generated by scripts/import-gov-graduate-policies.js',
    '-- Source: China Government Network public policy pages',
    '-- Keywords: graduate, startup, and college student policy import',
    '',
    ...rows,
    ''
  ].join('\n\n')
}

async function main() {
  const keywordArg = getArg('keyword', DEFAULT_KEYWORD)
  const keywords = parseKeywords(keywordArg)
  const pages = Number(getArg('pages', '2'))
  const output = getArg('output', DEFAULT_OUTPUT)

  console.log(`[info] searching gov.cn policies with keywords: ${keywords.join('、')}`)
  const searchedUrls = []
  for (const keyword of keywords) {
    const keywordUrls = await searchGov(keyword, pages)
    searchedUrls.push(...keywordUrls)
  }
  const urls = [...new Set([...searchedUrls, ...fallbackUrls])]

  console.log(`[info] candidate urls: ${urls.length}`)

  const policies = []
  for (const url of urls) {
    try {
      console.log(`[info] fetching ${url}`)
      const policy = await fetchPolicy(url, keywords)
      if (policy.title && includesAny(`${policy.title}\n${policy.keywords}\n${policy.content}`, keywords)) {
        policies.push(policy)
      }
    } catch (err) {
      console.warn(`[warn] skip ${url}: ${err.message}`)
    }

    await sleep(1500)
  }

  if (policies.length === 0) {
    throw new Error('No policies were fetched. Check network access or gov.cn page structure.')
  }

  fs.mkdirSync(path.dirname(output), { recursive: true })
  fs.writeFileSync(output, toSql(policies), 'utf8')
  console.log(`[info] wrote ${policies.length} policies to ${output}`)
}

main().catch((err) => {
  console.error(`[error] ${err.message}`)
  process.exit(1)
})
