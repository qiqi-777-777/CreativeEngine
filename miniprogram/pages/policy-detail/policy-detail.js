// pages/policy-detail/policy-detail.js
const app = getApp()

Page({
  data: {
    policyId: null,           // 政策ID
    policy: null,             // 政策详情
    keywordList: [],          // 关键词列表
    question: '',             // 用户输入的问题
    chatHistory: [],          // 对话历史
    asking: false,            // 是否正在问答
    scrollToView: '',         // 滚动到指定消息
    loadError: ''             // 加载错误信息
  },

  onLoad(options) {
    const { id } = options
    if (id) {
      this.setData({ policyId: id })
      this.loadPolicyDetail()
    } else {
      this.setData({ loadError: '政策ID缺失' })
    }
  },

  // 清理Markdown格式符号
  cleanMarkdown(text) {
    if (!text) return ''

    return text
      // 去掉标题符号 ###
      .replace(/^#{1,6}\s+/gm, '')
      // 去掉粗体符号 **text**（多次执行确保清理干净）
      .replace(/\*\*([^*]+?)\*\*/g, '$1')
      .replace(/\*\*([^*]+?)\*\*/g, '$1')
      // 去掉单个星号（斜体或列表）
      .replace(/\*([^*\n]+?)\*/g, '$1')
      // 去掉行首的星号列表符号
      .replace(/^[\*]\s+/gm, '• ')
      // 去掉所有剩余的双星号
      .replace(/\*\*/g, '')
      // 去掉所有剩余的单星号（在空格后的）
      .replace(/\s\*/g, ' ')
      .replace(/^\*/gm, '')
      // 去掉代码块符号 ```
      .replace(/```[\s\S]*?```/g, '')
      .replace(/`([^`]+?)`/g, '$1')
      // 去掉数字列表符号但保留数字
      .replace(/^(\d+)\.\s+/gm, '$1. ')
      // 去掉引用符号 >
      .replace(/^>\s+/gm, '')
      // 去掉连字符列表
      .replace(/^[\-\+]\s+/gm, '• ')
      // 去掉多余的空行
      .replace(/\n{3,}/g, '\n\n')
      // 去掉开头和结尾的---
      .replace(/^---+$/gm, '')
      .trim()
  },

  // 返回
  onBack() {
    wx.navigateBack()
  },

  // 加载政策详情
  loadPolicyDetail() {
    const { policyId } = this.data

    wx.showLoading({ title: '加载中...' })

    wx.request({
      url: `${app.globalData.apiBase}/policy-data/detail/${policyId}`,
      method: 'GET',
      success: (res) => {
        console.log('政策详情响应:', res.data)
        if (res.data.code === 200) {
          const policy = res.data.data
          // 解析关键词（按顿号或逗号分隔）
          const keywordList = policy.keywords ?
            policy.keywords.split(/[、,，]/).filter(k => k.trim()) : []

          this.setData({
            policy,
            keywordList,
            loadError: ''
          })
        } else {
          this.setData({
            loadError: res.data.message || '加载失败'
          })
        }
      },
      fail: (err) => {
        console.error('加载政策详情失败:', err)
        this.setData({
          loadError: '网络错误，请检查网络连接'
        })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  // 问题输入
  onQuestionInput(e) {
    this.setData({
      question: e.detail.value
    })
  },

  // 收起对话（清空历史，恢复为小卡片）
  onCollapseChat() {
    this.setData({
      chatHistory: [],
      asking: false
    })
  },

  // 提问
  onAskQuestion() {
    const { question, policyId, chatHistory } = this.data

    if (!question || !question.trim()) {
      return
    }

    // 保存原始问题
    const questionText = question.trim()

    // 添加用户消息
    const userMessage = { role: 'user', content: questionText }
    const newHistory = [...chatHistory, userMessage]

    this.setData({
      chatHistory: newHistory,
      question: '',
      asking: true
    })

    // 滚动到最新消息
    wx.nextTick(() => {
      this.setData({
        scrollToView: `msg-${newHistory.length - 1}`
      })
    })

    // 调用AI问答API
    wx.request({
      url: `${app.globalData.apiBase}/policy-data/ask`,
      method: 'POST',
      data: {
        policyId: policyId,
        question: questionText
      },
      success: (res) => {
        if (res.data.code === 200) {
          const answer = res.data.data
          // 清理Markdown格式符号
          const cleanAnswer = this.cleanMarkdown(answer)

          const aiMessage = { role: 'ai', content: cleanAnswer }
          const updatedHistory = [...this.data.chatHistory, aiMessage]

          this.setData({
            chatHistory: updatedHistory
          })

          // 滚动到最新消息
          wx.nextTick(() => {
            this.setData({
              scrollToView: `msg-${updatedHistory.length - 1}`
            })
          })
        } else {
          wx.showToast({
            title: res.data.message || '问答失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        console.error('AI问答失败:', err)
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        })
      },
      complete: () => {
        this.setData({ asking: false })
      }
    })
  }
})
