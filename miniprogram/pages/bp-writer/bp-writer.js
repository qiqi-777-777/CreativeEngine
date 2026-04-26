// pages/bp-writer/bp-writer.js
const app = getApp()
const userData = require('../../utils/userData')

function nowIso() {
  return new Date().toISOString()
}

function emptyForm() {
  return {
    projectName: '',
    oneLiner: '',
    painPoints: '',
    competitiveEdge: '',
    targetUsers: ''
  }
}

function cleanBPContent(text) {
  return String(text || '')
    .replace(/\r/g, '\n')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/^#{1,6}\s*/gm, '')
    .replace(/\*\*([^*]+?)\*\*/g, '$1')
    .replace(/\*([^*\n]+?)\*/g, '$1')
    .replace(/`([^`]+?)`/g, '$1')
    .replace(/^\s*[-*+]\s+/gm, '• ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function buildFallbackBP(formData) {
  const { projectName, oneLiner, painPoints, competitiveEdge, targetUsers } = formData
  return cleanBPContent(`${projectName} 项目计划书

一、执行摘要
${oneLiner}
本项目围绕目标用户的真实需求，提供清晰、可落地的产品服务方案，并通过持续运营和资源整合形成长期竞争力。

二、项目背景与市场机会
当前目标市场存在明确的效率、成本或体验痛点，用户对更便捷、更安全、更高效的解决方案有持续需求。项目可优先从校园或青年群体高频场景切入，逐步验证商业价值。

三、目标用户与需求分析
目标用户：${targetUsers || '高校学生、青年创业者或具有相关需求的垂直人群'}
核心需求：${painPoints || '降低使用门槛、提升服务效率、获得更可信的交易或服务体验'}

四、产品与服务方案
项目将围绕 ${oneLiner} 搭建核心产品能力，优先完成基础交易/服务闭环，再逐步扩展数据分析、智能推荐、信用评价、运营工具等增强功能。

五、商业模式
项目初期以用户增长和场景验证为主，中期可通过服务佣金、会员权益、增值工具、品牌合作或企业服务形成收入来源。

六、竞争格局与核心优势
核心优势：${competitiveEdge || '更贴近目标用户场景、产品体验轻量、运营响应速度快、具备持续迭代空间'}
竞争策略：避开大平台的泛化竞争，优先深耕细分场景，通过服务质量、用户信任和本地资源形成壁垒。

七、运营推广计划
第一阶段完成种子用户获取和产品验证；第二阶段通过社群、校园组织、内容传播和合作资源扩大覆盖；第三阶段建立标准化运营流程并复制到更多场景。

八、团队与执行计划
团队需要覆盖产品设计、技术开发、运营推广和商务合作四类能力。近期重点是完成 MVP、获取首批真实用户、沉淀反馈并快速迭代。

九、财务预测与资金用途
早期资金主要用于产品开发、基础运营、市场推广和必要的人力成本。建议按 6-12 个月周期规划预算，优先保障产品上线和核心用户增长。

十、风险与应对措施
主要风险包括用户增长不及预期、同类竞品进入、运营成本上升和合规要求变化。应通过小步试错、数据监测、差异化定位和合规审查降低风险。`)
}

Page({
  data: {
    statusBarHeight: 20,
    currentStep: 0,
    showResult: false,
    isGenerating: false,
    isExporting: false,
    bpContent: '',
    currentDraftId: '',
    lastSavedText: '',
    formData: emptyForm()
  },

  onLoad(options = {}) {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })

    if (options.draftId) {
      this.loadDraft(options.draftId)
      return
    }

    this.restoreCurrentDraft()
  },

  async loadDraft(draftId) {
    const drafts = await userData.getBpDrafts()
    const draft = drafts.find((item) => item.id === draftId)
    if (!draft) {
      wx.showToast({ title: '草稿不存在', icon: 'none' })
      return
    }

    userData.setCurrentBpDraftId(draft.id)
    this.setData({
      currentDraftId: draft.id,
      formData: draft.formData || emptyForm(),
      bpContent: draft.content || '',
      showResult: Boolean(draft.content),
      currentStep: draft.content ? 3 : 0,
      isGenerating: false,
      lastSavedText: draft.updatedAt ? '已恢复草稿' : ''
    })
  },

  async restoreCurrentDraft() {
    const draftId = userData.getCurrentBpDraftId()
    if (!draftId) return

    const drafts = await userData.getBpDrafts()
    const draft = drafts.find((item) => item.id === draftId)
    if (!draft) return

    this.setData({
      currentDraftId: draft.id,
      formData: draft.formData || emptyForm(),
      bpContent: draft.content || '',
      showResult: Boolean(draft.content),
      currentStep: draft.content ? 3 : 0,
      lastSavedText: '已恢复上次草稿'
    })
  },

  goBack() {
    this.saveBPDraft({ silent: true })
    wx.navigateBack({
      fail: () => {
        wx.switchTab({ url: '/pages/mine/mine' })
      }
    })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`formData.${field}`]: e.detail.value
    }, () => {
      this.saveBPDraft({ silent: true })
    })
  },

  nextStep() {
    this.saveBPDraft({ silent: true })
    if (this.data.currentStep < 3) {
      this.setData({ currentStep: this.data.currentStep + 1 })
    }
  },

  prevStep() {
    this.saveBPDraft({ silent: true })
    if (this.data.currentStep > 0) {
      this.setData({ currentStep: this.data.currentStep - 1 })
    }
  },

  buildPrompt() {
    const { projectName, oneLiner, painPoints, competitiveEdge, targetUsers } = this.data.formData
    return `你是一位创业项目计划书顾问。请根据以下信息，生成一份适合大学生创业项目、创新创业比赛或项目申报使用的《项目计划书》正文。

项目名称：${projectName}
一句话描述：${oneLiner}
解决的痛点：${painPoints || '未填写'}
核心竞争优势：${competitiveEdge || '未填写'}
目标用户群体：${targetUsers || '未填写'}

输出要求：
1. 使用中文，语气专业、务实、可落地。
2. 不要使用 Markdown 语法，不要输出 #、##、**、代码块。
3. 章节必须按以下结构输出：
一、执行摘要
二、项目背景与市场机会
三、目标用户与需求分析
四、产品与服务方案
五、商业模式
六、竞争格局与核心优势
七、运营推广计划
八、团队与执行计划
九、财务预测与资金用途
十、风险与应对措施
4. 每个章节写成完整段落，不要只列标题。
5. 内容要贴合项目本身，不要泛泛而谈。`
  },

  generateBP() {
    const { projectName, oneLiner } = this.data.formData

    if (!projectName.trim() || !oneLiner.trim()) {
      wx.showToast({ title: '请至少填写项目名称和一句话描述', icon: 'none' })
      return
    }

    this.saveBPDraft({ silent: true })
    this.setData({ showResult: true, isGenerating: true, bpContent: '' })

    wx.request({
      url: `${app.globalData.apiBase}/ai/session/create`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success: (sessionRes) => {
        const sessionId = sessionRes.statusCode === 200 && sessionRes.data.code === 200
          ? sessionRes.data.data.sessionId
          : ''

        wx.request({
          url: `${app.globalData.apiBase}/ai/chat`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
          },
          data: {
            sessionId,
            message: this.buildPrompt()
          },
          success: (res) => {
            if (res.statusCode === 200 && res.data.code === 200) {
              this.finishBP(res.data.data.response)
            } else {
              this.finishBP(buildFallbackBP(this.data.formData))
            }
          },
          fail: () => {
            this.finishBP(buildFallbackBP(this.data.formData))
          }
        })
      },
      fail: () => {
        this.finishBP(buildFallbackBP(this.data.formData))
      }
    })
  },

  finishBP(content) {
    this.setData({
      isGenerating: false,
      bpContent: cleanBPContent(content)
    }, () => {
      this.saveBPDraft({ silent: true })
    })
  },

  resetForm() {
    userData.removeCurrentBpDraftId()
    this.setData({
      currentStep: 0,
      showResult: false,
      isGenerating: false,
      isExporting: false,
      bpContent: '',
      currentDraftId: '',
      lastSavedText: '',
      formData: emptyForm()
    })
  },

  copyBP() {
    wx.setClipboardData({
      data: this.data.bpContent,
      success: () => {
        wx.showToast({ title: '已复制到剪贴板', icon: 'success' })
      }
    })
  },

  saveBPDraft(options = {}) {
    const { silent = false } = options
    const { currentDraftId, formData, bpContent } = this.data
    const hasAnyInput = Object.values(formData).some((value) => String(value || '').trim()) || Boolean(bpContent)

    if (!hasAnyInput) return ''

    const drafts = userData.getLocalList(userData.KEYS.bpDrafts)
    const now = nowIso()
    const existed = drafts.find((item) => item.id === currentDraftId)
    const draft = {
      id: currentDraftId || `bp-${Date.now()}`,
      projectName: formData.projectName.trim() || '未命名项目',
      oneLiner: formData.oneLiner || '',
      formData,
      content: bpContent,
      createdAt: existed ? existed.createdAt : now,
      updatedAt: now
    }

    const next = [
      draft,
      ...drafts.filter((item) => item.id !== draft.id)
    ].slice(0, 30)

    userData.setLocalList(userData.KEYS.bpDrafts, next)
    userData.setCurrentBpDraftId(draft.id)
    userData.saveBpDraft(draft)
    this.setData({
      currentDraftId: draft.id,
      lastSavedText: '已自动保存'
    })

    if (!silent) {
      wx.showToast({ title: '已保存草稿', icon: 'success' })
    }

    return draft.id
  },

  exportBP(e) {
    const format = e.currentTarget.dataset.format

    if (!this.data.bpContent) {
      wx.showToast({ title: '请先生成计划书', icon: 'none' })
      return
    }

    this.saveBPDraft({ silent: true })
    this.setData({ isExporting: true })

    wx.request({
      url: `${app.globalData.apiBase}/bp/export/${format}`,
      method: 'POST',
      responseType: 'arraybuffer',
      header: {
        'Content-Type': 'application/json'
      },
      data: {
        projectName: this.data.formData.projectName || '项目计划书',
        content: this.data.bpContent
      },
      success: (res) => {
        if (res.statusCode !== 200 || !res.data) {
          wx.showToast({ title: '导出失败', icon: 'none' })
          return
        }

        const ext = format === 'pdf' ? 'pdf' : 'doc'
        const fileName = `${this.data.formData.projectName || '项目计划书'}.${ext}`
        const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
        const fs = wx.getFileSystemManager()

        fs.writeFile({
          filePath,
          data: res.data,
          success: () => {
            wx.openDocument({
              filePath,
              fileType: ext,
              showMenu: true,
              fail: () => {
                wx.showToast({ title: '文件已生成，打开失败', icon: 'none' })
              }
            })
          },
          fail: () => {
            wx.showToast({ title: '写入文件失败', icon: 'none' })
          }
        })
      },
      fail: () => {
        wx.showToast({ title: '网络错误，导出失败', icon: 'none' })
      },
      complete: () => {
        this.setData({ isExporting: false })
      }
    })
  }
})
