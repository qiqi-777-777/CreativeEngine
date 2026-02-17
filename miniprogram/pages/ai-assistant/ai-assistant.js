const app = getApp()

Page({
  data: {
    statusBarHeight: 20,
    inputValue: '',
    messages: [],       // { role: 'user' | 'assistant', content: string }
    isTyping: false,
    scrollToView: '',
    sessionId: '',      // DeepSeek 会话ID
    suggestions: [
      "如何申请大学生创业补贴政策？",
      "我想了解最新的科技创新扶持政策",
      "如何制定一份规范的商业计划书？",
      "初创企业注册流程和注意事项？"
    ]
  },

  onLoad: function () {
    const sysInfo = wx.getSystemInfoSync();
    this.setData({
      statusBarHeight: sysInfo.statusBarHeight
    });

    // 创建AI会话
    this.createSession();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' &&
      this.getTabBar()) {
      this.getTabBar().setData({
        selected: 1
      })
    }
  },

  /**
   * 创建 AI 对话会话
   */
  createSession() {
    const baseUrl = app.globalData.apiBase;
    wx.request({
      url: baseUrl + '/ai/session/create',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 200) {
          this.setData({
            sessionId: res.data.data.sessionId
          });
          console.log('AI会话创建成功:', res.data.data.sessionId);
        } else {
          console.error('创建会话失败:', res.data);
          // 降级：使用本地模拟
          this.setData({ sessionId: '' });
        }
      },
      fail: (err) => {
        console.error('创建会话网络错误:', err);
        this.setData({ sessionId: '' });
      }
    });
  },

  handleInput(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  /**
   * 发送消息
   */
  handleSend() {
    const content = this.data.inputValue;
    if (!content.trim() || this.data.isTyping) {
      return;
    }

    // 1. 添加用户消息到列表
    const userMsg = { role: 'user', content: content };
    const newMessages = [...this.data.messages, userMsg];

    this.setData({
      messages: newMessages,
      inputValue: '',
      isTyping: true,
      scrollToView: 'scroll-bottom'
    });

    // 2. 调用后端 AI 接口
    if (this.data.sessionId) {
      this._callAiApi(content, newMessages);
    } else {
      // 没有会话ID，先创建会话再发送
      this._createSessionAndSend(content, newMessages);
    }
  },

  /**
   * 调用后端 AI Chat 接口
   */
  _callAiApi(content, currentMessages) {
    const baseUrl = app.globalData.apiBase;

    wx.request({
      url: baseUrl + '/ai/chat',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      data: {
        sessionId: this.data.sessionId,
        message: content
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 200) {
          // 成功获取 AI 回复
          const aiContent = res.data.data.response;
          this.setData({
            isTyping: false,
            messages: [...currentMessages, { role: 'assistant', content: aiContent }],
            scrollToView: 'scroll-bottom'
          });
        } else if (res.data.code === 404) {
          // 会话过期，重新创建
          console.warn('会话已过期，重新创建...');
          this._createSessionAndSend(content, currentMessages);
        } else {
          // 其他业务错误
          this.setData({
            isTyping: false,
            messages: [...currentMessages, {
              role: 'assistant',
              content: '抱歉，AI 服务暂时出了问题，请稍后再试。\n\n错误信息：' + (res.data.message || '未知错误')
            }],
            scrollToView: 'scroll-bottom'
          });
        }
      },
      fail: (err) => {
        console.error('AI请求失败:', err);
        this.setData({
          isTyping: false,
          messages: [...currentMessages, {
            role: 'assistant',
            content: '网络连接失败，请检查网络后重试。'
          }],
          scrollToView: 'scroll-bottom'
        });
      }
    });
  },

  /**
   * 会话不存在时，先创建会话再发送消息
   */
  _createSessionAndSend(content, currentMessages) {
    const baseUrl = app.globalData.apiBase;

    wx.request({
      url: baseUrl + '/ai/session/create',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 200) {
          this.setData({ sessionId: res.data.data.sessionId });
          // 创建成功后重新发送
          this._callAiApi(content, currentMessages);
        } else {
          this.setData({
            isTyping: false,
            messages: [...currentMessages, {
              role: 'assistant',
              content: '创建会话失败，请稍后重试。'
            }],
            scrollToView: 'scroll-bottom'
          });
        }
      },
      fail: (err) => {
        console.error('创建会话网络错误:', err);
        this.setData({
          isTyping: false,
          messages: [...currentMessages, {
            role: 'assistant',
            content: '网络连接失败，请检查网络后重试。'
          }],
          scrollToView: 'scroll-bottom'
        });
      }
    });
  },

  handleSuggestionClick(e) {
    const text = e.currentTarget.dataset.text;
    // 直接发送建议的问题
    this.setData({ inputValue: text }, () => {
      this.handleSend();
    });
  },

  handleAttach() {
    wx.showActionSheet({
      itemList: ['拍摄照片', '从相册选择', '选择文件'],
      success(res) {
        console.log(res.tapIndex)
      },
      fail(res) {
        console.log(res.errMsg)
      }
    })
  }
})
