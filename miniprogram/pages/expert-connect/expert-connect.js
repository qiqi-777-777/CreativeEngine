// pages/expert-connect/expert-connect.js
Page({
  data: {
    statusBarHeight: 20,
    activeCategory: 0,
    showDetail: false,
    selectedExpert: null,
    categories: ['全部', '法律顾问', '财务税务', '技术指导', '融资投资', '运营策略'],
    experts: [
      {
        id: 1, name: '张明远', title: '知识产权律师 · 10年执业经验',
        category: '法律顾问',
        tags: ['公司注册', '知识产权', '股权架构', '合同审查'],
        rating: 4.9, consultCount: 236,
        bio: '曾为超过200家初创企业提供法律顾问服务，擅长公司治理、股权激励、知识产权保护等领域。曾任某头部律所合伙人，现专注服务科技型创业公司。',
        price: '¥299/次', duration: '45分钟', mode: '视频/语音'
      },
      {
        id: 2, name: '李思涵', title: '注册会计师 · 创业财务专家',
        category: '财务税务',
        tags: ['财务规划', '税务筹划', '融资对接', '财务建模'],
        rating: 4.8, consultCount: 189,
        bio: '拥有注册会计师和税务师双重资质，曾任四大会计师事务所高级经理。专注为早期创业项目提供财务体系搭建、税务优化和融资财务规划服务。',
        price: '¥399/次', duration: '60分钟', mode: '视频/语音'
      },
      {
        id: 3, name: '王浩然', title: 'CTO · 前阿里巴巴高级技术专家',
        category: '技术指导',
        tags: ['系统架构', 'AI应用', '技术选型', '团队搭建'],
        rating: 4.9, consultCount: 157,
        bio: '15年技术研发与管理经验，曾主导多个千万级用户产品的架构设计。擅长帮助创业团队进行技术选型、MVP开发规划和技术团队组建。',
        price: '¥499/次', duration: '60分钟', mode: '视频'
      },
      {
        id: 4, name: '陈雅琪', title: '天使投资人 · 前红杉资本副总裁',
        category: '融资投资',
        tags: ['BP打磨', '融资策略', '估值谈判', '投资人对接'],
        rating: 5.0, consultCount: 312,
        bio: '累计投资超过50个早期项目，深耕消费科技和企业服务赛道。可为创业者提供BP打磨、融资节奏规划、估值谈判和投资人引荐等全方位融资辅导。',
        price: '¥599/次', duration: '45分钟', mode: '视频'
      },
      {
        id: 5, name: '刘子墨', title: '增长专家 · 前字节跳动运营总监',
        category: '运营策略',
        tags: ['增长黑客', '用户运营', '内容策略', '私域流量'],
        rating: 4.7, consultCount: 198,
        bio: '曾操盘多款DAU过千万产品的用户增长，擅长从0到1冷启动、社交裂变和私域运营。为创业者提供低成本获客和用户留存的实战方法论。',
        price: '¥349/次', duration: '45分钟', mode: '视频/语音'
      },
      {
        id: 6, name: '赵文博', title: '创业导师 · 连续创业者',
        category: '运营策略',
        tags: ['商业模式', '战略规划', '团队管理', '创业心态'],
        rating: 4.8, consultCount: 275,
        bio: '三次创业经历，其中两家公司成功被并购。现任多家孵化器创业导师，擅长帮助创业者梳理商业模式、制定阶段性战略和解决团队管理难题。',
        price: '¥399/次', duration: '60分钟', mode: '视频/语音'
      }
    ],
    filteredExperts: []
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync();
    this.setData({ statusBarHeight: sysInfo.statusBarHeight });
    this.filterExperts();
  },

  goBack() {
    wx.navigateBack();
  },

  onCategoryTap(e) {
    this.setData({ activeCategory: e.currentTarget.dataset.index });
    this.filterExperts();
  },

  filterExperts() {
    const category = this.data.categories[this.data.activeCategory];
    if (category === '全部') {
      this.setData({ filteredExperts: this.data.experts });
    } else {
      const filtered = this.data.experts.filter(e => e.category === category);
      this.setData({ filteredExperts: filtered });
    }
  },

  onExpertTap(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      selectedExpert: this.data.filteredExperts[index],
      showDetail: true
    });
  },

  closeDetail() {
    this.setData({ showDetail: false });
  },

  bookExpert() {
    wx.showToast({
      title: '预约功能开发中，敬请期待',
      icon: 'none',
      duration: 2000
    });
  }
})
