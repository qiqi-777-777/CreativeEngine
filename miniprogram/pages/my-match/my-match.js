// pages/my-match/my-match.js
const app = getApp()
const { formatPolicy } = require('../../utils/policyText')
const userData = require('../../utils/userData')

const DEFAULT_POLICY_TERMS = ['就业', '创业', '高校毕业生', '大学生', '补贴', '贷款', '培训', '招聘']
const FALLBACK_POLICIES = [
  {
    policyName: '高校毕业生创业担保贷款政策',
    keywords: '高校毕业生,创业,贷款,贴息,就业',
    content: '面向符合条件的高校毕业生自主创业项目，提供创业担保贷款和贴息支持，帮助早期项目降低启动资金压力。'
  },
  {
    policyName: '青年就业见习与技能培训支持',
    keywords: '青年就业,见习,培训,招聘,补贴',
    content: '支持青年参加就业见习、职业技能培训和岗位实践，提升求职能力与岗位适配度，符合条件的可享受补贴。'
  },
  {
    policyName: '大学生创新创业项目扶持',
    keywords: '大学生,创新创业,项目孵化,场地,补贴',
    content: '面向大学生创新创业项目，提供项目孵化、场地支持、创业培训和资源对接服务。'
  }
]

function formatDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function getCareerScore(item, index) {
  if (item.matchRate) return item.matchRate
  if (item.score) return Math.round(item.score)

  const heatScore = {
    A: 92,
    B: 84,
    C: 76,
    D: 68
  }
  return Math.max(60, (heatScore[item.industryHeat] || 88) - index * 5)
}

function normalizeCareer(item, index, recordDate) {
  const skills = Array.isArray(item.skills) ? item.skills : []
  const majors = Array.isArray(item.majors) ? item.majors : []
  const tags = [...skills, ...majors].filter(Boolean).slice(0, 4)

  return {
    id: item.id || `${item.name || item.title}-${index}`,
    title: item.name || item.title || '职业方向',
    category: item.category || item.industry || (majors.length ? majors.slice(0, 2).join(' / ') : `${item.personalityCode || '综合'}型`),
    matchRate: getCareerScore(item, index),
    desc: item.reason || item.description || '该方向与您的能力、专业背景和职业兴趣具有较高关联度。',
    tags: tags.length ? tags : [item.industryHeat ? `热度 ${item.industryHeat}` : '推荐方向'],
    date: formatDate(recordDate),
    raw: item
  }
}

function unique(values) {
  return [...new Set(values.map((item) => String(item || '').trim()).filter(Boolean))]
}

function buildPolicyTerms(careerResults) {
  const careerTerms = careerResults.flatMap((item) => [
    item.title,
    item.category,
    ...(item.tags || [])
  ])
  return unique([...careerTerms, ...DEFAULT_POLICY_TERMS]).slice(0, 16)
}

function summarizePolicy(policy) {
  const text = String(policy.displaySnippet || policy.displayContent || policy.content || '').replace(/\s+/g, ' ').trim()
  return text.length > 78 ? `${text.slice(0, 78)}...` : text || '暂无政策摘要'
}

function getPolicyTags(policy, terms) {
  const keywords = String(policy.keywords || '')
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter(Boolean)

  const text = `${policy.policyName || ''} ${policy.keywords || ''} ${policy.displayContent || ''}`
  const matched = terms.filter((term) => text.includes(term))
  return unique([...matched, ...keywords]).slice(0, 4)
}

function scorePolicy(policy, terms) {
  const title = policy.policyName || ''
  const text = `${policy.policyName || ''} ${policy.keywords || ''} ${policy.displayContent || ''} ${policy.content || ''}`
  let score = 62

  terms.forEach((term) => {
    if (title.includes(term)) score += 8
    else if (text.includes(term)) score += 4
  })

  if (text.includes('就业')) score += 8
  if (text.includes('创业')) score += 8
  if (text.includes('高校毕业生') || text.includes('大学生')) score += 6
  if (text.includes('补贴') || text.includes('贷款') || text.includes('培训')) score += 4

  return Math.min(98, score)
}

function makePolicyReason(policy, tags) {
  const title = policy.policyName || ''
  if (tags.includes('创业') || title.includes('创业')) return '与创业扶持、资金补贴或项目发展相关，适合继续查看申报条件。'
  if (tags.includes('就业') || title.includes('就业')) return '与就业服务、能力提升或岗位支持相关，可结合职业方向重点关注。'
  if (tags.includes('高校毕业生') || tags.includes('大学生')) return '面向高校毕业生或大学生群体，与你的学生/青年发展场景相关。'
  return '根据最近的职业测评关键词筛选，可作为后续政策匹配参考。'
}

function normalizePolicy(policy, terms) {
  const formatted = formatPolicy(policy)
  const tags = getPolicyTags(formatted, terms)
  return {
    id: formatted.id,
    title: formatted.policyName || '政策详情',
    matchRate: scorePolicy(formatted, terms),
    summary: summarizePolicy(formatted),
    source: formatted.sourceInfo || formatted.agency || '政策库',
    date: formatted.publishDate || '查看详情',
    tags,
    reason: makePolicyReason(formatted, tags)
  }
}

function getFallbackPolicies(terms) {
  return FALLBACK_POLICIES
    .map((policy, index) => normalizePolicy({ ...policy, id: '' }, terms))
    .map((policy, index) => ({
      ...policy,
      matchRate: Math.max(76, policy.matchRate - index * 4)
    }))
}

Page({
  data: {
    statusBarHeight: 20,
    activeTab: 0,
    careerResults: [],
    policyResults: [],
    latestRecord: null,
    latestMatchDate: '',
    policyLoading: false,
    policyKeywords: [],
    displayPolicyKeywords: []
  },

  onLoad(options = {}) {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({
      statusBarHeight: sysInfo.statusBarHeight || 20,
      activeTab: Number(options.tab || 0)
    })
  },

  onShow() {
    this.loadCareerResults()
  },

  goBack() {
    wx.navigateBack()
  },

  switchTab(e) {
    const activeTab = parseInt(e.currentTarget.dataset.tab, 10)
    this.setData({ activeTab })
    if (activeTab === 1 && this.data.policyResults.length === 0) {
      this.loadMatchedPolicies()
    }
  },

  async loadCareerResults() {
    const records = await userData.getCareerRecords()
    const latestRecord = records[0] || null
    const rawResults = latestRecord ? (latestRecord.results || []) : []
    const careerResults = rawResults.map((item, index) => normalizeCareer(item, index, latestRecord.createdAt))
    const policyKeywords = buildPolicyTerms(careerResults)

    this.setData({
      latestRecord,
      careerResults,
      latestMatchDate: latestRecord ? formatDate(latestRecord.createdAt) : '',
      policyKeywords,
      displayPolicyKeywords: policyKeywords.slice(0, 6),
      policyResults: []
    })

    this.loadMatchedPolicies()
  },

  loadMatchedPolicies() {
    const terms = this.data.policyKeywords.length ? this.data.policyKeywords : DEFAULT_POLICY_TERMS
    this.setData({ policyLoading: true })

    wx.request({
      url: `${app.globalData.apiBase}/policy-data/list`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const policies = (res.data.data || [])
            .map((policy) => normalizePolicy(policy, terms))
            .sort((a, b) => b.matchRate - a.matchRate)
            .slice(0, 8)

          this.setData({ policyResults: policies.length ? policies : getFallbackPolicies(terms) })
        } else {
          this.setData({ policyResults: getFallbackPolicies(terms) })
        }
      },
      fail: () => {
        this.setData({ policyResults: getFallbackPolicies(terms) })
      },
      complete: () => {
        this.setData({ policyLoading: false })
      }
    })
  },

  openCareerResult(e) {
    const index = Number(e.currentTarget.dataset.index || 0)
    const result = this.data.careerResults[index]
    const payload = this.data.latestRecord ? this.data.latestRecord.results : (result ? [result.raw] : [])

    if (!payload || payload.length === 0) return

    wx.navigateTo({
      url: `/pages/career-match-result/career-match-result?fromRecord=1&data=${encodeURIComponent(JSON.stringify(payload))}`
    })
  },

  goToCareerMatch() {
    wx.navigateTo({ url: '/pages/career-match/career-match' })
  },

  goToPolicyDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) {
      wx.showToast({ title: '暂缺政策详情', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/policy-detail/policy-detail?id=${id}` })
  },

  goToPolicyList() {
    wx.navigateTo({ url: '/pages/policy/policy' })
  }
})
