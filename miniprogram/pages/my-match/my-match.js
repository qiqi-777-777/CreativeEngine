// pages/my-match/my-match.js
Page({
  data: {
    statusBarHeight: 20,
    activeTab: 0,
    careerResults: [
      {
        id: 1, title: '全栈开发工程师', category: '互联网/技术',
        matchRate: 92,
        desc: '基于您的计算机科学专业背景和全栈开发技能，该职业方向与您的能力高度匹配。建议关注 AI 应用开发和云原生领域。',
        tags: ['Python', 'React', 'AI应用', '高薪'],
        date: '2026-04-20'
      },
      {
        id: 2, title: '产品经理', category: '互联网/产品',
        matchRate: 85,
        desc: '您具备技术背景与用户思维的双重优势，适合在科技公司担任技术型产品经理，推动产品从0到1的落地。',
        tags: ['用户研究', '数据分析', '项目管理'],
        date: '2026-04-18'
      },
      {
        id: 3, title: '技术创业者', category: '创业/CEO',
        matchRate: 78,
        desc: '综合您的技术能力、创新意识和团队协作特质，您具备独立创业的潜力，建议从垂直赛道的 SaaS 工具切入。',
        tags: ['创业', 'SaaS', '融资', '商业模式'],
        date: '2026-04-15'
      }
    ],
    policyResults: [
      {
        id: 1, title: '大学生创业担保贷款贴息政策',
        amount: '最高30万',
        summary: '对符合条件的高校毕业生自主创业，可申请最高30万元的创业担保贷款，政府给予全额贴息。',
        source: '人力资源和社会保障部',
        deadline: '2026-12-31'
      },
      {
        id: 2, title: '科技型中小企业创新基金',
        amount: '最高50万',
        summary: '支持科技型中小企业开展技术创新活动，重点支持人工智能、新材料、生物医药等领域的早期项目。',
        source: '科技部',
        deadline: '2026-09-30'
      },
      {
        id: 3, title: '青年创业就业补贴',
        amount: '1万/年',
        summary: '对首次创办小微企业或个体工商户的高校毕业生，给予每年不低于1万元的创业补贴，补贴期限最长3年。',
        source: '市人社局',
        deadline: '2026-06-30'
      }
    ]
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync();
    this.setData({ statusBarHeight: sysInfo.statusBarHeight });
  },

  goBack() {
    wx.navigateBack();
  },

  switchTab(e) {
    this.setData({ activeTab: parseInt(e.currentTarget.dataset.tab) });
  },

  goToCareerMatch() {
    wx.navigateTo({ url: '/pages/career-match/career-match' });
  },

  goToPolicyDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/policy-detail/policy-detail?id=' + id });
  }
})
