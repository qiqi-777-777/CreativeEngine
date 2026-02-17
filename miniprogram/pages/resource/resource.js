// pages/resource/resource.js
Page({
  data: {
    searchKeyword: '',
    currentTab: 0,
    tabs: [
      { id: 0, name: '全部', count: 0 },
      { id: 1, name: '基础便捷', count: 0 },
      { id: 2, name: '学术辅助', count: 0 },
      { id: 3, name: '技能变现', count: 0 },
      { id: 4, name: '灵活服务', count: 0 }
    ],

    // 工作机会列表
    jobList: [
      // 一、基础便捷类
      {
        id: 1,
        category: 1,
        categoryName: '基础便捷',
        title: '食堂帮工',
        description: '负责打饭、收银、清洁餐具等，提供免费工作餐，工作集中在饭点，与上课冲突小。',
        salary: '800-1000',
        unit: '元/月',
        location: '第一食堂',
        time: '周一至周五',
        tags: ['包工作餐', '时间灵活'],
        color: '#34D399' // emerald-400
      },
      {
        id: 2,
        category: 1,
        categoryName: '基础便捷',
        title: '校园便利店店员',
        description: '商品补货、收银、整理货架，空闲时可复习，适配零散时间。',
        salary: '15-20',
        unit: '元/时',
        location: '生活区便利店',
        time: '排班制',
        tags: ['可复习', '简单易上手'],
        color: '#60A5FA' // blue-400
      },
      {
        id: 3,
        category: 1,
        categoryName: '基础便捷',
        title: '校园咖啡厅店员',
        description: '点单收银、制作基础饮品，部分提供免费饮品/简餐。',
        salary: '18-25',
        unit: '元/时',
        location: '图书馆咖啡吧',
        time: '课余时间',
        tags: ['环境舒适', '饮品免费'],
        color: '#A78BFA' // purple-400
      },
      {
        id: 4,
        category: 1,
        categoryName: '基础便捷',
        title: '教材发放助理',
        description: '开学前后清点、分类、发放教材，登记未领信息。',
        salary: '200-400',
        unit: '元/次',
        location: '教材中心',
        time: '开学周',
        tags: ['短期工作', '细心'],
        color: '#F472B6' // pink-400
      },

      // 二、学术辅助与研究类
      {
        id: 5,
        category: 2,
        categoryName: '学术辅助',
        title: '图书馆/实验室助理',
        description: '图书馆整理图书、协助查资料；实验室整理器材、准备实验材料。',
        salary: '15-30',
        unit: '元/时',
        location: '图书馆/实验楼',
        time: '长期固定',
        tags: ['环境好', '专业相关'],
        color: '#FBBF24' // amber-400
      },

      // 三、技能变现与创意类
      {
        id: 6,
        category: 3,
        categoryName: '技能变现',
        title: '校园设计/翻译',
        description: '承接海报设计、简历美化；或承接外文资料翻译。',
        salary: '80-300',
        unit: '元/稿',
        location: '线上',
        time: '自由接单',
        tags: ['专业技能', '高回报'],
        color: '#EC4899' // pink-500
      },
      {
        id: 7,
        category: 3,
        categoryName: '技能变现',
        title: '校园自媒体运营',
        description: '运营校园账号，分享趣事经验，粉丝达标后承接广告。',
        salary: '300-2k',
        unit: '元/条',
        location: '线上',
        time: '不限',
        tags: ['创意工作', '自媒体'],
        color: '#8B5CF6' // violet-500
      },
      {
        id: 8,
        category: 3,
        categoryName: '技能变现',
        title: '摄影摄像助理',
        description: '记录校园活动影像、整理素材，需掌握基础摄影剪辑。',
        salary: '80-150',
        unit: '元/次',
        location: '校园活动现场',
        time: '按任务',
        tags: ['摄影技能', '任务制'],
        color: '#10B981' // emerald-500
      },
      {
        id: 9,
        category: 3,
        categoryName: '技能变现',
        title: 'IT服务台助理',
        description: '解决师生电脑、校园网、软件安装问题。',
        salary: '20-35',
        unit: '元/时',
        location: 'IT服务中心',
        time: '值班制',
        tags: ['技术岗', '薪资高'],
        color: '#3B82F6' // blue-500
      },
      {
        id: 10,
        category: 3,
        categoryName: '技能变现',
        title: '校刊编辑助理',
        description: '选题策划、稿件收集、校对排版，适合出版兴趣者。',
        salary: '1k-1.8k',
        unit: '元/月',
        location: '编辑部',
        time: '长期',
        tags: ['策划能力', '稳定收入'],
        color: '#6366F1' // indigo-500
      },

      // 四、灵活服务与生活配套类
      {
        id: 11,
        category: 4,
        categoryName: '灵活服务',
        title: '校园跑腿',
        description: '代取快递、代买餐饮，适合行动力强的学生。',
        salary: '3-5',
        unit: '元/单',
        location: '全校',
        time: '自由',
        tags: ['灵活自由', '快速赚钱'],
        color: '#F97316' // orange-500
      },
      {
        id: 12,
        category: 4,
        categoryName: '灵活服务',
        title: '校园代理',
        description: '代理驾校、考证课程、网红零食，成交提成。',
        salary: '50-300',
        unit: '元/单',
        location: '全校',
        time: '自由',
        tags: ['销售能力', '高提成'],
        color: '#EF4444' // red-500
      },
      {
        id: 13,
        category: 4,
        categoryName: '灵活服务',
        title: '学生大使/导游',
        description: '招生季讲解校园、接待访客，适合擅长表达的学生。',
        salary: '100-200',
        unit: '元/场',
        location: '访客中心',
        time: '季节性',
        tags: ['表达能力', '接待工作'],
        color: '#14B8A6' // teal-500
      },
      {
        id: 14,
        category: 4,
        categoryName: '灵活服务',
        title: '自习室管理员',
        description: '开关门、维护秩序、检查设施，环境安静。',
        salary: '15-20',
        unit: '元/时',
        location: '自习室',
        time: '早晚班',
        tags: ['轻松', '适合备考'],
        color: '#8B5CF6' // violet-500
      },
      {
        id: 15,
        category: 4,
        categoryName: '灵活服务',
        title: '打印店排版员',
        description: '论文格式排版、海报设计、文件装订。',
        salary: '20-30',
        unit: '元/时',
        location: '打印店',
        time: '排班',
        tags: ['排版技能', '可加价'],
        color: '#64748B' // slate-500
      }
    ],
    displayList: [],
    allJobList: [],

    // TabBar Data (From Homepage)
    tabBarList: [
      {
        pagePath: "/pages/index/index",
        iconPath: "/images/icon/home.svg",
        selectedIconPath: "/images/icon/home-active.svg",
        text: "首页"
      },
      {
        pagePath: "/pages/ai-assistant/ai-assistant",
        iconPath: "/images/icon/ai-plus.svg",
        selectedIconPath: "/images/icon/ai-plus.svg",
        text: "AI助手",
        isSpecial: true
      },
      {
        pagePath: "/pages/mine/mine",
        iconPath: "/images/icon/user.svg",
        selectedIconPath: "/images/icon/user-active.svg",
        text: "我的"
      }
    ]
  },

  // 底部导航跳转
  switchTab(e) {
    const data = e.currentTarget.dataset
    const url = data.path
    wx.switchTab({ url })
  },

  onLoad() {
    // 备份原始数据并初始化显示
    this.setData({
      allJobList: this.data.jobList,
      displayList: this.data.jobList
    })

    // 更新分类计数
    this.updateTabCount()
  },

  // 更新分类计数
  updateTabCount() {
    const tabs = this.data.tabs
    const jobList = this.data.jobList

    tabs[0].count = jobList.length
    tabs[1].count = jobList.filter(item => item.category === 1).length
    tabs[2].count = jobList.filter(item => item.category === 2).length
    tabs[3].count = jobList.filter(item => item.category === 3).length
    tabs[4].count = jobList.filter(item => item.category === 4).length

    this.setData({ tabs })
  },

  // 切换标签
  onTabChange(e) {
    const index = e.currentTarget.dataset.index
    this.setData({
      currentTab: index
    })

    // 根据标签过滤数据
    this.filterData()
  },

  // 搜索输入
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword
    })

    // 执行搜索
    this.filterData()
  },

  // 过滤数据
  filterData() {
    const { currentTab, searchKeyword, allJobList } = this.data

    let displayList = allJobList

    // 分类过滤
    if (currentTab > 0) {
      displayList = displayList.filter(item => item.category === currentTab)
    }

    // 搜索过滤
    if (searchKeyword) {
      displayList = displayList.filter(item =>
        item.title.includes(searchKeyword) ||
        item.description.includes(searchKeyword) ||
        item.categoryName.includes(searchKeyword)
      )
    }

    this.setData({ displayList })
  },

  // 下拉刷新
  onPullDownRefresh() {
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '创享引擎 - 校园工作资源对接',
      path: '/pages/resource/resource'
    }
  }
})

