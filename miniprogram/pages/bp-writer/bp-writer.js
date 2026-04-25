// pages/bp-writer/bp-writer.js
const app = getApp()

Page({
  data: {
    statusBarHeight: 20,
    currentStep: 0,
    showResult: false,
    isGenerating: false,
    bpContent: '',
    formData: {
      projectName: '',
      oneLiner: '',
      painPoints: '',
      competitiveEdge: '',
      targetUsers: ''
    }
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync();
    this.setData({ statusBarHeight: sysInfo.statusBarHeight });
  },

  goBack() {
    if (this.data.showResult) {
      this.setData({ showResult: false, currentStep: 3 });
    } else if (this.data.currentStep > 0) {
      this.setData({ currentStep: this.data.currentStep - 1 });
    } else {
      wx.navigateBack();
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`formData.${field}`]: e.detail.value
    });
  },

  nextStep() {
    if (this.data.currentStep < 3) {
      this.setData({ currentStep: this.data.currentStep + 1 });
    }
  },

  prevStep() {
    if (this.data.currentStep > 0) {
      this.setData({ currentStep: this.data.currentStep - 1 });
    }
  },

  generateBP() {
    const { projectName, oneLiner, painPoints, competitiveEdge, targetUsers } = this.data.formData;

    if (!projectName.trim() || !oneLiner.trim()) {
      wx.showToast({ title: '请至少填写项目名称和一句话描述', icon: 'none' });
      return;
    }

    this.setData({ showResult: true, isGenerating: true, bpContent: '' });

    // 构建结构化 prompt
    const prompt = `你是一位资深的创业导师和商业计划书撰写专家。请根据以下信息，为我生成一份专业、完整的商业计划书大纲和核心内容。

项目名称：${projectName}
一句话描述：${oneLiner}
解决的痛点：${painPoints || '未填写'}
核心竞争优势：${competitiveEdge || '未填写'}
目标用户群体：${targetUsers || '未填写'}

请按以下结构输出（使用中文）：
1. 执行摘要
2. 市场分析（市场规模、增长趋势、目标市场）
3. 产品/服务描述
4. 商业模式（盈利方式）
5. 竞争分析
6. 营销策略
7. 团队规划
8. 财务预测（初步）
9. 融资需求与用途

请确保内容专业、数据合理、逻辑严密。`;

    const baseUrl = app.globalData.apiBase;

    // 先创建会话
    wx.request({
      url: baseUrl + '/ai/session/create',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success: (sessionRes) => {
        let sessionId = '';
        if (sessionRes.statusCode === 200 && sessionRes.data.code === 200) {
          sessionId = sessionRes.data.data.sessionId;
        }

        // 调用 AI chat
        wx.request({
          url: baseUrl + '/ai/chat',
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
          },
          data: {
            sessionId: sessionId,
            message: prompt
          },
          success: (res) => {
            if (res.statusCode === 200 && res.data.code === 200) {
              this.setData({
                isGenerating: false,
                bpContent: res.data.data.response
              });
            } else {
              this._useFallbackBP();
            }
          },
          fail: () => {
            this._useFallbackBP();
          }
        });
      },
      fail: () => {
        this._useFallbackBP();
      }
    });
  },

  // AI 接口不可用时的降级方案
  _useFallbackBP() {
    const { projectName, oneLiner, painPoints, competitiveEdge, targetUsers } = this.data.formData;
    const fallback = `# ${projectName} - 商业计划书

## 1. 执行摘要
${oneLiner}

本项目旨在通过创新的技术手段和商业模式，解决目标市场中的核心痛点，构建可持续的竞争壁垒。

## 2. 市场分析
目标市场正处于高速增长期，市场空间广阔。${painPoints ? '当前市场痛点包括：' + painPoints : ''}

## 3. 产品/服务描述
${oneLiner}。我们将通过技术创新与用户体验优化，提供差异化的解决方案。

## 4. 商业模式
项目将采用 SaaS 订阅制 + 增值服务的混合商业模式，确保稳定的现金流和可扩展的盈利能力。

## 5. 竞争分析
${competitiveEdge ? '我们的核心竞争优势：' + competitiveEdge : '通过技术壁垒和先发优势构建护城河。'}

## 6. 营销策略
采用"产品驱动增长 (PLG)"策略，结合社交媒体营销与线下推广，快速获取种子用户。

## 7. 团队规划
${targetUsers ? '目标用户群体：' + targetUsers : '核心团队将覆盖技术、产品、运营三大方向。'}

## 8. 财务预测
预计第一年投入 50-100 万元，第二年实现盈亏平衡，第三年实现规模化盈利。

## 9. 融资需求
首轮融资目标：100-300 万元（天使轮），主要用于产品研发、市场推广和团队搭建。

---
*本商业计划书由 AI 辅助生成，仅供参考，请根据实际情况调整完善。*`;

    this.setData({
      isGenerating: false,
      bpContent: fallback
    });
  },

  resetForm() {
    this.setData({
      currentStep: 0,
      showResult: false,
      isGenerating: false,
      bpContent: '',
      formData: {
        projectName: '',
        oneLiner: '',
        painPoints: '',
        competitiveEdge: '',
        targetUsers: ''
      }
    });
  },

  copyBP() {
    wx.setClipboardData({
      data: this.data.bpContent,
      success: () => {
        wx.showToast({ title: '已复制到剪贴板', icon: 'success' });
      }
    });
  }
})
