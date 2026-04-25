// pages/industry-atlas/industry-atlas.js
Page({
  data: {
    statusBarHeight: 20,
    activeIndustry: 0,
    industries: [
      {
        id: 1, name: '信息技术',
        overview: '新一代信息技术是数字经济的核心支撑，涵盖了从底层芯片、网络通信到上层软件和应用的完整产业链，正在加速向各行业渗透赋能。',
        marketSize: '12.3万亿', growth: '8.5%', heat: '极高',
        upstream: ['半导体材料', '电子元器件', '设备制造'],
        core: ['集成电路', '通信设备', '操作系统', '中间件'],
        downstream: ['消费电子', '政务数字', '企业信息化', '智能家居'],
        players: [
          { name: '华为', desc: '全球领先的ICT基础设施提供商' },
          { name: '腾讯', desc: '互联网科技领军企业' },
          { name: '中芯国际', desc: '中国大陆晶圆代工龙头' },
          { name: '紫光股份', desc: '云网协同综合提供商' }
        ]
      },
      {
        id: 2, name: '生物科技',
        overview: '生物科技通过生物工程技术解决人类健康与资源问题。中国生物科技产业正经历从仿制到创新的跨越式发展，AI制药等前沿领域成为投资热点。',
        marketSize: '3.2万亿', growth: '11.2%', heat: '高',
        upstream: ['科研试剂', '实验设备', '生物基础材料'],
        core: ['创新药研发', '基因编辑', '合成生物学', '干细胞技术'],
        downstream: ['医疗机构', '农业生物', '环保治理', '健康管理'],
        players: [
          { name: '百济神州', desc: '创新药出海先锋' },
          { name: '药明康德', desc: 'CXO一站式平台' },
          { name: '华大基因', desc: '基因测序行业龙头' },
          { name: '金斯瑞', desc: '合成生物学领军者' }
        ]
      },
      {
        id: 3, name: '新能源',
        overview: '在"双碳"目标驱动下，中国新能源产业迎来黄金发展期。光伏、储能、新能源汽车三大赛道持续高增长，中国企业在全球供应链中占据主导地位。',
        marketSize: '6.8万亿', growth: '18.5%', heat: '极高',
        upstream: ['硅料/锂矿', '电池材料', '光伏设备'],
        core: ['锂电池', '光伏组件', '风电整机', '储能系统'],
        downstream: ['新能源汽车', '充电桩', '分布式电站', '电力交易'],
        players: [
          { name: '宁德时代', desc: '全球动力电池龙头' },
          { name: '比亚迪', desc: '新能源汽车+电池双轮驱动' },
          { name: '隆基绿能', desc: '光伏硅片与组件龙头' },
          { name: '阳光电源', desc: '逆变器与储能领先者' }
        ]
      },
      {
        id: 4, name: '航空航天',
        overview: '商业航天迎来爆发期，从火箭发射到卫星互联网，民营力量正成为中国航天重要补充。深空探测与大飞机商业化运营也在同步加速。',
        marketSize: '1.5万亿', growth: '13.4%', heat: '高',
        upstream: ['特种材料', '电子元器件', '精密加工'],
        core: ['运载火箭', '卫星制造', '大飞机总装', '航空发动机'],
        downstream: ['卫星通信', '遥感数据', '航空运输', '太空旅游'],
        players: [
          { name: '中国商飞', desc: 'C919大飞机制造商' },
          { name: '航天科技', desc: '国家航天主力军' },
          { name: '星际荣耀', desc: '民营商业火箭领军者' },
          { name: '银河航天', desc: '卫星互联网探索者' }
        ]
      },
      {
        id: 5, name: '高端制造',
        overview: '高端制造是工业之母和智能制造的基石。数控机床、精密仪器等领域正加速国产替代，推动"中国制造"向高附加值的产业链上游攀升。',
        marketSize: '35.8万亿', growth: '6.5%', heat: '高',
        upstream: ['基础零部件', '传感器', '工业软件'],
        core: ['数控机床', '工业机器人', '3D打印设备', '精密测试仪器'],
        downstream: ['汽车制造', '3C电子', '半导体制造', '航空航天'],
        players: [
          { name: '汇川技术', desc: '工业自动化综合方案' },
          { name: '埃斯顿', desc: '国产工业机器人领先者' },
          { name: '海天精工', desc: '高端数控机床代表' },
          { name: '大族激光', desc: '激光加工设备龙头' }
        ]
      },
      {
        id: 6, name: '低空经济',
        overview: '低空经济作为新兴产业，以eVTOL(电动垂直起降飞行器)和无人机为核心，正以前所未有的速度改变城市交通、物流配送和应急巡查的格局。',
        marketSize: '5000亿', growth: '27.6%', heat: '极高',
        upstream: ['碳纤维材料', '航空电池', '飞行控制系统'],
        core: ['eVTOL制造', '工业无人机', '消费级无人机', '低空雷达'],
        downstream: ['城市空中交通', '快递物流', '农林植保', '应急救援'],
        players: [
          { name: '大疆创新', desc: '全球消费无人机霸主' },
          { name: '亿航智能', desc: 'eVTOL商业化先驱' },
          { name: '纵横股份', desc: '工业无人机领军企业' },
          { name: '小鹏汇天', desc: '飞行汽车探索者' }
        ]
      },
      {
        id: 7, name: '人工智能',
        overview: '人工智能正以前所未有的速度重塑全球产业格局。从大语言模型到具身智能，AI技术正加速从实验室走向产业化落地，赋能千行百业。',
        marketSize: '5780亿', growth: '24.8%', heat: '极高',
        upstream: ['芯片/GPU', '算力基础设施', '数据服务'],
        core: ['大模型', '计算机视觉', '自然语言处理', '机器学习框架'],
        downstream: ['智能驾驶', '智慧医疗', '智能制造', 'AIGC应用'],
        players: [
          { name: '百度', desc: '文心大模型，全栈AI布局' },
          { name: '阿里云', desc: '通义千问，云+AI战略' },
          { name: '商汤科技', desc: '计算机视觉龙头' },
          { name: '科大讯飞', desc: '语音AI领军者' }
        ]
      },
      {
        id: 8, name: '元宇宙',
        overview: '元宇宙是虚拟现实和互联网融合的新型数字生态。随着XR硬件设备的普及和内容生态的丰富，元宇宙正逐步从概念走向空间计算时代。',
        marketSize: '8500亿', growth: '32.5%', heat: '中高',
        upstream: ['光学器件', '显示屏幕', '空间计算芯片'],
        core: ['VR/AR硬件', '渲染引擎', '数字人技术', '区块链底层'],
        downstream: ['沉浸式游戏', '虚拟社交', '工业孪生', '文旅演艺'],
        players: [
          { name: 'PICO', desc: '国内VR一体机领头羊' },
          { name: '歌尔股份', desc: 'XR代工与光学元件巨头' },
          { name: '米哈游', desc: '内容驱动的虚拟世界构建者' },
          { name: '风语筑', desc: '数字展示与元宇宙应用' }
        ]
      },
      {
        id: 9, name: '机器人',
        overview: '机器人技术将自动化和智能化融合，尤其是具身智能概念的兴起，让人形机器人成为下一个产业风口，应用场景正从工厂向家庭服务延伸。',
        marketSize: '1900亿', growth: '15.7%', heat: '极高',
        upstream: ['伺服电机', '减速器', '力矩传感器'],
        core: ['人形机器人', '协作机器人', '移动机器人(AMR)', '机器人大模型'],
        downstream: ['仓储物流', '医疗手术', '家庭陪伴', '特种作业'],
        players: [
          { name: '优必选', desc: '人形机器人第一股' },
          { name: '傅利叶智能', desc: '通用人形机器人先行者' },
          { name: '极智嘉', desc: '全球AMR领军企业' },
          { name: '微创机器人', desc: '手术机器人突破者' }
        ]
      },
      {
        id: 10, name: '量子技术',
        overview: '量子技术利用量子力学原理，是下一代计算和通信范式。量子计算在解决复杂问题上的潜力巨大，量子通信则为信息安全提供了绝对保障。',
        marketSize: '650亿', growth: '29.8%', heat: '中高',
        upstream: ['稀释制冷机', '微波测控系统', '特殊线缆'],
        core: ['超导量子计算机', '光量子计算机', '量子密钥分发', '量子传感器'],
        downstream: ['新药研发加速', '密码学', '金融量化', '高精度导航'],
        players: [
          { name: '国盾量子', desc: '量子通信龙头' },
          { name: '本源量子', desc: '量子计算综合方案提供商' },
          { name: '量旋科技', desc: '桌面级量子计算机' },
          { name: '玻色量子', desc: '相干光量子计算机探索者' }
        ]
      },
      {
        id: 11, name: '6G',
        overview: '6G作为下一代移动通信技术，将实现空天地海一体化网络，提供比5G快百倍的速率和极致的低延迟，是实现万物智联的关键基础设施。',
        marketSize: '1200亿', growth: '45.0%', heat: '高',
        upstream: ['太赫兹芯片', '新型天线材料', '高频测试设备'],
        core: ['太赫兹通信', '星地融合组网', '通信感知一体化', '智能超表面'],
        downstream: ['全息通信', '自动驾驶集群', '远程医疗', '太空宽带'],
        players: [
          { name: '华为', desc: '6G标准研制核心主导者' },
          { name: '中兴通讯', desc: '通信设备与技术积累深厚' },
          { name: '中国移动', desc: '6G网络架构与试验网验证' },
          { name: '信科移动', desc: '无线通信核心技术攻关' }
        ]
      },
      {
        id: 12, name: '算力',
        overview: '算力是数字经济时代的重要生产要素。随着AI大模型的爆发，智能算力需求呈指数级增长，智算中心与液冷技术成为产业链核心焦点。',
        marketSize: '8200亿', growth: '25.3%', heat: '极高',
        upstream: ['AI芯片', '光模块', '服务器整机'],
        core: ['智算中心', '液冷系统', '算力调度网络', '边缘计算'],
        downstream: ['AI模型训练', '云游戏', '科学计算', '智慧城市大脑'],
        players: [
          { name: '中科曙光', desc: '高性能计算与液冷领军者' },
          { name: '浪潮信息', desc: 'AI服务器全球前列' },
          { name: '中际旭创', desc: '高速光模块龙头' },
          { name: '润泽科技', desc: '大规模算力中心运营商' }
        ]
      },
      {
        id: 13, name: '大数据',
        overview: '数据已被正式列为新型生产要素。数据要素市场化配置改革加速，数据确权、数据交易、隐私计算等领域正迎来前所未有的发展机遇。',
        marketSize: '1.5万亿', growth: '20.1%', heat: '高',
        upstream: ['数据采集工具', '存储设备', '数据库'],
        core: ['数据湖/仓库', '隐私计算', '数据治理服务', '数据资产评估'],
        downstream: ['金融风控', '精准营销', '政务服务', '工业质检'],
        players: [
          { name: '星环科技', desc: '国产大数据基础软件领军' },
          { name: '数据港', desc: '第三方数据中心服务商' },
          { name: '华宇软件', desc: '政法/信创大数据应用' },
          { name: '奇安信', desc: '数据安全防护体系提供商' }
        ]
      },
      {
        id: 14, name: '工业互联网',
        overview: '工业互联网是第四次工业革命的基石。通过连接工业生产各环节，实现数据驱动的智能制造，正在深度赋能千行百业的数字化转型。',
        marketSize: '1.3万亿', growth: '14.8%', heat: '高',
        upstream: ['工业传感器', '边缘网关', '5G工业模组'],
        core: ['工业PaaS平台', '工业大数据引擎', '数字孪生平台', '工业安全软件'],
        downstream: ['智慧工厂', '供应链协同', '设备预测性维护', '能源管理'],
        players: [
          { name: '卡奥斯', desc: '海尔旗下国家级双跨平台' },
          { name: '树根互联', desc: '工程机械领域工业互联网' },
          { name: '宝信软件', desc: '钢铁/重工业信息化龙头' },
          { name: '用友网络', desc: '精智工业互联网平台' }
        ]
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

  onIndustryTap(e) {
    this.setData({ activeIndustry: e.currentTarget.dataset.index });
  },

  goToImageInterpretation() {
    const activeIndex = this.data.activeIndustry;
    wx.navigateTo({ url: `/pages/image-interpretation/image-interpretation?index=${activeIndex}` });
  }
})
