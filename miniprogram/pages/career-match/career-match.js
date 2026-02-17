// pages/career-match/career-match.js
const app = getApp()

Page({
  data: {
    // 图标资源 (Base64 SVGs)
    icons: {
      major: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9IiNmZmYiIHN0cm9rZS13aWR0aD0iMiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIj48cGF0aCBkPSJNMjIgMTBsLTEwIDUtMTAtNSA4LTQgOCA0eiIvPjxwYXRoIGQ9Ik02IDEydjUuMzNhNiA2IDAgMCAwIDEyIDBWMTIiLz48L3N2Zz4=',
      skill: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9IiNmZmYiIHN0cm9rZS13aWR0aD0iMiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIj48cmVjdCB4PSIyIiB5PSIzIiB3aWR0aD0iMjAiIGhlaWdodD0iMTQiIHJ4PSIyIiByeT0iMiIvPjxwYXRoIGQ9Ik04IDIxaDgiLz48cGF0aCBkPSJNMTIgMTd2NCIvPjwvc3ZnPg==',
      personality: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="%23fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="10" x2="9.01" y2="10"/><line x1="15" y1="10" x2="15.01" y2="10"/></svg>'
    },

    selectedCategory: 'major', // Keep for compatibility if needed, though mostly unused now
    currentCategoryName: '专业',

    // 各分类的选项数据
    majorOptions: [],
    skillOptions: [],
    personalityOptions: [],

    // 技术分类 (Not used in new design but kept for data safety)
    skillCategories: [
      { id: 'programming', name: '编程语言', icon: '💻' },
      { id: 'database', name: '数据库', icon: '🗄️' },
      { id: 'framework', name: '框架工具', icon: '🛠️' },
      { id: 'other', name: '其他技能', icon: '📦' }
    ],
    selectedSkillCategory: 'programming',
    skillCategoryOptions: {
      programming: [],
      database: [],
      framework: [],
      other: []
    },

    // 当前显示的选项 (Unused)
    currentOptions: [],

    // 已选择的项
    selectedMajors: [],
    selectedSkills: [],
    selectedPersonalities: [],

    totalSelected: 0,

    // 性格详情弹窗
    showPersonalityModal: false,
    personalityDetails: [
      { type: 'AEC型', description: '艺术-企业-常规型', traits: '具有创造力、领导力和组织能力，适合需要创新管理的职业' },
      { type: 'AES型', description: '艺术-企业-社交型', traits: '富有创意、善于交际、具备领导才能，适合创意营销、公关等领域' },
      { type: 'AIC型', description: '艺术-研究-常规型', traits: '具有创造力和研究精神，注重细节，适合设计、研发等工作' },
      { type: 'CRI型', description: '常规-研究-研究型', traits: '逻辑严密、善于分析、注重细节，适合数据分析、科学研究' },
      { type: 'ERC型', description: '企业-研究-常规型', traits: '具有领导力和分析能力，适合项目管理、战略规划' },
      { type: 'ESC型', description: '企业-社交-常规型', traits: '外向、善于组织协调、责任心强，适合管理、行政工作' },
      { type: 'IAC型', description: '研究-艺术-常规型', traits: '理性与感性并重，适合需要创意和逻辑的工作' },
      { type: 'IAR型', description: '研究-艺术-现实型', traits: '实际且富有创意，适合工业设计、建筑设计' },
      { type: 'IAS型', description: '研究-艺术-社交型', traits: '内向但善于表达，适合教育、咨询等领域' },
      { type: 'ICR型', description: '研究-常规-现实型', traits: '逻辑严密、注重细节和实际操作，适合技术工程师' },
      { type: 'IEC型', description: '研究-企业-常规型', traits: '有研究精神和管理能力，适合技术管理、产品经理' },
      { type: 'IES型', description: '研究-企业-社交型', traits: '有分析能力和人际技巧，适合咨询、培训' },
      { type: 'IRC型', description: '研究-现实-常规型', traits: '实际且理性，适合工程、技术工作' },
      { type: 'IRE型', description: '研究-现实-企业型', traits: '理性且具备领导力，适合技术创业、项目管理' },
      { type: 'ISA型', description: '研究-社交-艺术型', traits: '分析能力强且富有同情心，适合心理咨询、教育' },
      { type: 'ISC型', description: '研究-社交-常规型', traits: '理性且关注他人，适合医疗、社会工作' },
      { type: 'ISR型', description: '研究-社交-现实型', traits: '实际且关心他人，适合护理、社区服务' },
      { type: 'RIS型', description: '现实-研究-社交型', traits: '实际且具有分析能力，适合技术支持、培训' },
      { type: 'SAE型', description: '社交-艺术-企业型', traits: '外向、富有创意和领导力，适合公关、品牌管理' },
      { type: 'SAI型', description: '社交-艺术-研究型', traits: '外向且具有创意和研究精神，适合用户研究、内容创作' },
      { type: 'SAR型', description: '社交-艺术-现实型', traits: '外向且实际，适合销售、客户服务' },
      { type: 'SCA型', description: '社交-常规-艺术型', traits: '外向、有组织力和创意，适合活动策划、品牌运营' },
      { type: 'SEC型', description: '社交-企业-常规型', traits: '外向、有领导力和组织能力，适合人力资源、运营管理' },
      { type: 'SRA型', description: '社交-现实-艺术型', traits: '外向、实际且富有创意，适合营销、市场推广' },
      { type: 'SRI型', description: '社交-现实-研究型', traits: '外向、实际且有分析能力，适合技术销售、项目经理' }
    ]
  },

  onLoad(options) {
    this.loadData()
  },

  // 加载数据
  loadData() {
    wx.showLoading({ title: '加载中...' })

    Promise.all([
      this.loadMajors(),
      this.loadSkills(),
      this.loadPersonalities()
    ]).then(() => {
      wx.hideLoading()
      this.updateCurrentOptions()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      console.error('加载数据失败:', err)
    })
  },

  // 加载专业列表
  loadMajors() {
    return new Promise((resolve, reject) => {
      wx.request({
        url: app.globalData.apiBase + '/occupation/majors',
        method: 'GET',
        success: (res) => {
          if (res.data.code === 0) {
            this.setData({
              majorOptions: res.data.data || []
            })
            resolve()
          } else {
            reject(new Error(res.data.message))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },

  // 加载技术列表
  loadSkills() {
    return new Promise((resolve, reject) => {
      wx.request({
        url: app.globalData.apiBase + '/occupation/skills',
        method: 'GET',
        success: (res) => {
          if (res.data.code === 0) {
            const allSkills = res.data.data || []
            this.setData({
              skillOptions: allSkills
            })
            // 将技能分类
            this.categorizeSkills(allSkills)
            resolve()
          } else {
            reject(new Error(res.data.message))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },

  // 技能分类
  categorizeSkills(skills) {
    const programming = []
    const database = []
    const framework = []
    const other = []

    const programmingKeywords = ['Java', 'Python', 'C++', 'Go', 'JavaScript', 'TypeScript', 'PHP', 'Ruby', 'Swift', 'Kotlin', 'Rust', 'C#', 'C', 'R', 'Scala', 'HTML', 'CSS']
    const databaseKeywords = ['MySQL', 'Oracle', 'MongoDB', 'Redis', 'PostgreSQL', 'SQL', 'SQLite', 'Elasticsearch', 'Neo4j', 'Cassandra']
    const frameworkKeywords = ['React', 'Vue', 'Angular', 'Spring', 'Django', 'Flask', 'Express', 'Node', 'Docker', 'Kubernetes', 'Git', 'Maven', 'Gradle', 'Webpack', 'Jenkins']

    skills.forEach(skill => {
      if (programmingKeywords.some(keyword => skill.includes(keyword))) {
        programming.push(skill)
      } else if (databaseKeywords.some(keyword => skill.includes(keyword))) {
        database.push(skill)
      } else if (frameworkKeywords.some(keyword => skill.includes(keyword))) {
        framework.push(skill)
      } else {
        other.push(skill)
      }
    })

    this.setData({
      'skillCategoryOptions.programming': programming,
      'skillCategoryOptions.database': database,
      'skillCategoryOptions.framework': framework,
      'skillCategoryOptions.other': other
    })
  },

  // 加载性格列表
  loadPersonalities() {
    return new Promise((resolve, reject) => {
      wx.request({
        url: app.globalData.apiBase + '/occupation/personalities',
        method: 'GET',
        success: (res) => {
          if (res.data.code === 0) {
            this.setData({
              personalityOptions: res.data.data || []
            })
            resolve()
          } else {
            reject(new Error(res.data.message))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  },

  // 切换分类
  onCategoryTap(e) {
    const categoryId = e.currentTarget.dataset.id
    const category = this.data.categories.find(c => c.id === categoryId)

    this.setData({
      selectedCategory: categoryId,
      currentCategoryName: category.name
    })

    this.updateCurrentOptions()
  },

  // 更新当前显示的选项
  updateCurrentOptions() {
    const { selectedCategory, majorOptions, skillCategoryOptions, selectedSkillCategory, personalityOptions } = this.data

    let currentOptions = []

    switch (selectedCategory) {
      case 'major':
        currentOptions = majorOptions
        break
      case 'skill':
        currentOptions = skillCategoryOptions[selectedSkillCategory] || []
        break
      case 'personality':
        currentOptions = personalityOptions
        break
    }

    this.setData({
      currentOptions
    })

    this.updateTotalSelected()
  },

  // 切换技术子分类
  onSkillCategoryTap(e) {
    const categoryId = e.currentTarget.dataset.id
    console.log('点击技术子分类:', categoryId)
    this.setData({
      selectedSkillCategory: categoryId
    }, () => {
      console.log('当前技术子分类:', this.data.selectedSkillCategory)
      this.updateCurrentOptions()
    })
  },

  // 选择或取消选择选项
  onOptionTap(e) {
    const item = e.currentTarget.dataset.item
    // 支持从 data-category 获取分类，兼容旧逻辑（虽然旧逻辑不再需要，但保持健壮性）
    const category = e.currentTarget.dataset.category || this.data.selectedCategory
    const normalizedItem = category === 'personality' && item && typeof item === 'object' && item.type ? item.type : item

    console.log('点击选项:', normalizedItem, '分类:', category)

    // 根据当前分类更新对应的选择列表
    switch (category) {
      case 'major': {
        let selectedMajors = [...this.data.selectedMajors]
        const index = selectedMajors.indexOf(normalizedItem)
        if (index > -1) {
          selectedMajors.splice(index, 1)
        } else {
          selectedMajors.push(normalizedItem)
        }
        this.setData({ selectedMajors })
        break
      }
      case 'skill': {
        let selectedSkills = [...this.data.selectedSkills]
        const index = selectedSkills.indexOf(normalizedItem)
        if (index > -1) {
          selectedSkills.splice(index, 1)
        } else {
          selectedSkills.push(normalizedItem)
        }
        this.setData({ selectedSkills })
        break
      }
      case 'personality': {
        let selectedPersonalities = [...this.data.selectedPersonalities]
        const index = selectedPersonalities.indexOf(normalizedItem)
        if (index > -1) {
          selectedPersonalities.splice(index, 1)
        } else {
          selectedPersonalities.push(normalizedItem)
        }
        this.setData({ selectedPersonalities })
        break
      }
    }

    this.updateTotalSelected()
  },

  // 更新总选择数
  updateTotalSelected() {
    const total = this.data.selectedMajors.length +
      this.data.selectedSkills.length +
      this.data.selectedPersonalities.length

    this.setData({ totalSelected: total })
  },

  // 重置选择
  onReset() {
    wx.showModal({
      title: '提示',
      content: '确定要重置所有选择吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            selectedMajors: [],
            selectedSkills: [],
            selectedPersonalities: [],
            totalSelected: 0
          })
          this.updateCurrentOptions()
        }
      }
    })
  },

  // 开始匹配
  onMatch() {
    const { selectedMajors, selectedSkills, selectedPersonalities, totalSelected } = this.data

    if (totalSelected === 0) {
      wx.showToast({
        title: '请至少选择一项',
        icon: 'none'
      })
      return
    }

    wx.showLoading({ title: '匹配中...' })

    wx.request({
      url: app.globalData.apiBase + '/occupation/match',
      method: 'POST',
      data: {
        majors: selectedMajors,
        skills: selectedSkills,
        personalities: selectedPersonalities
      },
      success: (res) => {
        wx.hideLoading()

        if (res.data.code === 0) {
          const matchedOccupations = res.data.data || []

          if (matchedOccupations.length === 0) {
            wx.showModal({
              title: '未找到匹配职业',
              content: '根据您的选择，暂未找到匹配的职业。请尝试调整选择条件。',
              showCancel: false
            })
          } else {
            // 跳转到结果页面
            wx.navigateTo({
              url: `/pages/career-match-result/career-match-result?data=${encodeURIComponent(JSON.stringify(matchedOccupations))}`
            })
          }
        } else {
          wx.showToast({
            title: res.data.message || '匹配失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        wx.hideLoading()
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        })
        console.error('匹配失败:', err)
      }
    })
  },

  // 显示性格详情
  showPersonalityDetail() {
    this.setData({
      showPersonalityModal: true
    })
  },

  // 隐藏性格详情
  hidePersonalityDetail() {
    this.setData({
      showPersonalityModal: false
    })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 阻止点击弹窗内容时关闭弹窗
  }
})
