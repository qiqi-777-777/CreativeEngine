// pages/resource/resource.js
Page({
  data: {
    currentView: 'list',
    activeTab: '全部',
    categories: ['全部', '基础便捷', '学术辅助', '技能变现', '名企实习'],
    resourceData: [
      {
        id: 'R01',
        category: '基础便捷',
        title: '食堂帮工',
        en: 'CANTEEN HELPER',
        salary: '800-1000',
        unit: '元/月',
        desc: '负责打饭、收银、清洁餐具等，提供免费工作餐，工作集中在饭点，与上课冲突小。',
        requirements: ['细心负责，手脚麻利', '持有有效健康证', '能适应饭点高强度工作节奏'],
        location: '第一食堂',
        time: '每日 11:00-13:00 / 17:00-19:00',
        status: 'URGENT'
      },
      {
        id: 'R02',
        category: '基础便捷',
        title: '校园便利店店员',
        en: 'CONVENIENCE STORE CLERK',
        salary: '15-20',
        unit: '元/时',
        desc: '商品补货、收银、整理货架，空闲时可复习，适配零散时间。',
        requirements: ['热情礼貌，具备基本沟通能力', '会使用基本的收银系统(可入职培训)', '每周至少能提供10小时空闲时间'],
        location: '生活区便利店',
        time: '排班制，可根据课表协调',
        status: 'ACTIVE'
      },
      {
        id: 'R03',
        category: '学术辅助',
        title: 'CV算法助理',
        en: 'CV ALGORITHM ASST.',
        salary: '200-300',
        unit: '元/天',
        desc: '协助教授进行计算机视觉方向的数据清洗、模型跑批和初步分析工作，适合保研党。',
        requirements: ['计算机/人工智能相关专业大三以上', '熟练使用 Python, PyTorch', '具备基础的英语论文阅读能力'],
        location: '科技楼 A座 402实验室',
        time: '每周至少到岗 3 天',
        status: 'HOT'
      },
      {
        id: 'R04',
        category: '技能变现',
        title: '独立插画师(商单)',
        en: 'FREELANCE ILLUSTRATOR',
        salary: '500-2000',
        unit: '元/单',
        desc: '为校外合作企业绘制公众号配图、IP形象设计及海报物料，按件计酬，多劳多得。',
        requirements: ['熟练使用 PS, Procreate 等绘画软件', '有独立完成商业插画的案例/作品集', '沟通能力强，能准确理解甲方需求并按时交稿'],
        location: '线上远程 / 不限',
        time: '项目制，弹性工作',
        status: 'ACTIVE'
      }
    ],
    filteredResources: [],
    selectedResource: null,
  },

  onLoad() {
    this.filterData();
  },

  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      activeTab: tab
    });
    this.filterData();
  },

  filterData() {
    const { activeTab, resourceData } = this.data;
    let filteredResources = resourceData;
    if (activeTab !== '全部') {
      filteredResources = resourceData.filter(item => item.category === activeTab);
    }
    this.setData({ filteredResources });
  },

  onSelectResource(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      selectedResource: item,
      currentView: 'detail'
    });
  },

  onBackToList() {
    this.setData({
      currentView: 'list'
    });
    // Delay clearing selected resource to avoid abrupt UI change during transition if any
    setTimeout(() => {
      if(this.data.currentView === 'list') {
         this.setData({ selectedResource: null });
      }
    }, 300);
  },

  onPullDownRefresh() {
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
  },

  onShareAppMessage() {
    return {
      title: '资源对接 - 创享引擎',
      path: '/pages/resource/resource'
    };
  }
});
