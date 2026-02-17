// pages/image-interpretation/image-interpretation.js
const app = getApp()

// Simple SVG icons (Base64 encoded for compatibility)
const svgs = {
  it: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHJlY3QgeD0iMiIgeT0iMyIgd2lkdGg9IjIwIiBoZWlnaHQ9IjE0IiByeD0iMiIgcnk9IjIiLz48bGluZSB4MT0iOCIgeTE9IjIxIiB4Mj0iMTYiIHkyPSIyMSIvPjxsaW5lIHgxPSIxMiIgeTE9IjE3IiB4Mj0iMTIiIHkyPSIyMSIvPjwvc3ZnPg==', // Monitor
  bio: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTIuMDUgMTAuNWE5IDkgMCAwIDEgNC03LjQ4TC42NC41Ii8+PHBhdGggZD0iTTE2Ljk5IDUuOTEgMjAuOTkgMiIvPjxwYXRoIGQ9Ik0xMiAxMmMtMi41IDItMi41IDYgMCA4czIuNS02IDAtOCIvPjxwYXRoIGQ9Ik02LjU2IDYuNzlhOSA5IDAgMCAxIDkuNTMtLjQ1Ii8+PHBhdGggZD0iTTYuNTYgMTcuMjFhOSA5IDAgMCAwIDkuNTMuNDYiLz48cGF0aCBkPSJNMjEuOTUgMTMuNWExMCAxMCAwIDAgMS00IDcuNDhMMjMuMzYgMjMuNSIvPjwvc3ZnPg==', // DNA-ish
  energy: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTEzIDJMMyAxNGg5bC0xOGgxMGwtMTAtMTJ6Ii8+PC9zdmc==', // Zap
  space: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTQuNSAxMS41QTkgOSAwIDAgMSAxMS41IDQuNWgwdjBIMTVhOSA5IDAgMCAxIDkgOXYwYTkgOSAwIDAgMS05IDloLTMuNWE5IDkgMCAwIDEtOS05eiIvPjwvc3ZnPg==', // Rocket simplified (using circle/shapes for robustness) -> actually lets use a better rocket path
  rocket: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTYgOUgxMmwtNSA5SDVWMjBIMXYtOWw1LTl6Ii8+PHBhdGggZD0iTTE4IDlIMTJsNSA5aDJWMjBoNHYtOWwtNS05eiIvPjxwYXRoIGQ9Ik0xMiAydjIwIi8+PC9zdmc+', // Actually just a generic shape, lets trust the 'rocket' icon from library concepts
  factory: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTIgMjB2LThoMnY4Ii8+PHBhdGggZD0iTTYgMjB2LThoMnY4Ii8+PHBhdGggZD0iTTEwIDIwdjFoNHYtMWg0di0xbDR2LTFsLTQtMVY2bC00LTF2MTNsLTQtMXY0SDJ2MUgxMCIvPjxwYXRoIGQ9Ik0xOCA4aDJ2NCIvPjwvc3ZnPg==', // Industry
  drone: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTIgOGw0IDRMMiAyMCIvPjxwYXRoIGQ9Ik0yMiA4bC00IDRMMjIgMjAiLz48Y2lyY2xlIGN4PSIxMiIgY3k9IjEzIiByPSIzIi8+PHBhdGggZD0iTTEyIDN2NyIvPjwvc3ZnPg==', // Drone-ish
  ai: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHJlY3QgeD0iMyIgeT0iNCIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE2IiByeD0iMiIvPjxjaXJjbGUgY3g9IjkiIGN5PSIxMCIgcj0iMiIvPjxjaXJjbGUgY3g9IjE1IiBjeT0iMTAiIHI9IjIiLz48cGF0aCBkPSJNOSAxNmE2IDYgMCAwIDAgNiAwIi8+PC9zdmc+', // Bot face
  meta: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTMgMTJoLTIgOGgxMGwtMTAtOHoiLz48cGF0aCBkPSJNMjIgMTJsMiA4aDEwbC0yLThoLTEweiIvPjwvc3ZnPg==', // Glasses
  robot: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHJlY3QgeD0iMyIgeT0iMTEiIHdpZHRoPSIxOCIgaGVpZ2h0PSIxMCIgcng9IjIiLz48Y2lyY2xlIGN4PSIxMiIgY3k9IjUiIHI9IjIiLz48cGF0aCBkPSJNMTIgN3Y0Ii8+PGxpbmUgeTE9IjE2IiB4Mj0iOCIgeTE9IjE2IiB4MT0iOCIvPjwvc3ZnPg==', // Robot
  atom: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTEyIDEyYTMgMyAwIDEgMCAwLTYgMyAzIDAgMCAwIDAgNnoiLz48ZWxsaXBzZSBjeD0iMTIiIGN5PSIxMiIgcng9IjgiIHJ5PSI0Ii8+PHBhdGggZD0iTTcuMSA3LjFhMSAxIDAgMCAxIDAgMS40TDUgMTBIMXY0aDRsMi4xIDEuNWExIDEgMCAwIDEgMCAxLjRMNy4xIDE2LjkiLz48L3N2Zz4=', // Atom
  signal: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTUgMTIuNTUgYTEwIDEwIDAgMCAxIDE0Ljg5IDBMITIgMjEgNSAxMi41NSIvPjxwYXRoIGQ9Ik04LjUgMTAuNTUgYTUgNSAwIDAgMSA2Ljg5IDBMMTIgMTUuNSA4LjUgMTAuNTUiLz48L3N2Zz4=', // Wifi/Signal
  server: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHJlY3QgeD0iMiIgeT0iMiIgd2lkdGg9IjIwIiBoZWlnaHQ9IjgiIHJ4PSIyIi8+PHJlY3QgeD0iMiIgeT0iMTQiIHdpZHRoPSIyMCIgaGVpZ2h0PSI4IiByeD0iMiIvPjxsaW5lIHgxPSI2IiB5MT0iNiIgeDI9IjYuMDEiIHkyPSI2Ii8+PGxpbmUgeTE9IjE4IiB4Mj0iNi4wMSIgeTE9IjE4IiB4MT0iNiIvPjwvc3ZnPg==', // Server
  chart: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PGxpbmUgeTE9IjIwIiB4Mj0iMyIgeTE9IjIwIiB4MT0iMjEiLz48bGluZSB4MT0iNiIgeTE9IjIwIiB4Mj0iNiIgeTI9IjE2Ii8+PGxpbmUgeTE9IjIwIiB4Mj0iMTIiIHkxPSIyMCIgeDE9IjEyIiB5Mj0iMTAiLz48bGluZSB4MT0iMTgiIHkxPSIyMCIgeDI9IjE4IiB5Mj0iNCIvPjwvc3ZnPg==', // Chart
  network: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PGNpcmNsZSBjeD0iMTgiIGN5PSI1IiByPSIzIi8+PGNpcmNsZSBjeD0iNiIgY3k9IjEyIiByPSIzIi8+PGNpcmNsZSBjeD0iMTgiIGN5PSIxOSIgcj0iMyIvPjxsaW5lIHgxPSI4LjU5IiB5MT0iMTMuNTEiIHgyPSIxNS40MiIgeTI9IjE3LjQ5Ii8+PGxpbmUgeTE9IjYuNSIgeDI9IjE1LjQxIiB5MT0iNi41IiB4MT0iOC41OSIvPjwvc3ZnPg==', // Network
  flask: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTEwIDJ2Ny4zMWwtNiA5SDE5bC02LTlWMiIvPjxsaW5lIHgxPSI4IiB5MT0iMiIgeDI9IjE2IiB5Mj0iMiIvPjwvc3ZnPg==', // New Material
}

const INDUSTRIES = [
  {
    id: 1,
    name: '信息技术',
    icon: svgs.it,
    color: '#3B82F6',
    desc: '新一代信息技术是数字经济的核心支撑。',
    interpretation: '信息技术是指利用计算机、网络等技术进行信息处理、传输和应用的技术体系，是数字经济的基础支撑。'
  },
  {
    id: 2,
    name: '生物科技',
    icon: svgs.bio,
    color: '#EF4444',
    desc: '通过生物工程技术解决人类健康与资源问题。',
    interpretation: '生物科技通过生物学原理开发新产品和新工艺，在医疗、农业、环保等领域具有重要应用价值。'
  },
  {
    id: 3,
    name: '新能源',
    icon: svgs.energy,
    color: '#10B981',
    desc: '绿色能源革命，推动碳中和目标实现。',
    interpretation: '新能源包括太阳能、风能、水能等可再生能源，是实现能源转型和碳中和的重要途径。'
  },
  {
    id: 4,
    name: '航空航天',
    icon: svgs.space,
    color: '#8B5CF6',
    desc: '探索深空，突破人类空间的物理极限。',
    interpretation: '航空航天产业涉及飞行器设计、制造和运营，是高科技产业的重要代表。'
  },
  {
    id: 5,
    name: '高端制造',
    icon: svgs.factory,
    color: '#F59E0B',
    desc: '工业之母，智能制造的基石。',
    interpretation: '高端制造是指采用先进技术和工艺生产高附加值产品的产业，代表制造业升级方向。'
  },
  {
    id: 6,
    name: '低空经济',
    icon: svgs.drone,
    color: '#A29BFE',
    desc: '利用低空空域发展的新兴产业。',
    interpretation: '低空经济是指利用低空空域发展的新兴产业，包括无人机、飞行汽车等应用。'
  },
  {
    id: 7,
    name: '人工智能',
    icon: svgs.ai,
    color: '#FF7675',
    desc: '实现智能化决策和自动化的核心技术。',
    interpretation: '人工智能是计算机科学的重要分支，通过机器学习等技术实现智能化决策和自动化。'
  },
  {
    id: 8,
    name: '元宇宙',
    icon: svgs.meta,
    color: '#74B9FF',
    desc: '虚拟现实和互联网融合的新型数字生态。',
    interpretation: '元宇宙是虚拟现实和互联网融合的新型数字生态，提供沉浸式的交互体验。'
  },
  {
    id: 9,
    name: '机器人',
    icon: svgs.robot,
    color: '#EC4899',
    desc: '自动化和智能化融合，广泛应用于制造与服务。',
    interpretation: '机器人技术将自动化和智能化融合，在制造、服务等领域广泛应用。'
  },
  {
    id: 10,
    name: '量子技术',
    icon: svgs.atom,
    color: '#6366F1',
    desc: '下一代计算范式，超越经典物理极限。',
    interpretation: '量子技术利用量子力学原理，在计算、通信、测量等领域具有革命性潜力。'
  },
  {
    id: 11,
    name: '6G',
    icon: svgs.signal,
    color: '#6C5CE7',
    desc: '下一代移动通信技术，提供更高速率。',
    interpretation: '6G是下一代移动通信技术，将提供更高速率、更低延迟的无线通信服务。'
  },
  {
    id: 12,
    name: '算力',
    icon: svgs.server,
    color: '#00B894',
    desc: '数字经济时代的重要生产要素。',
    interpretation: '算力是指计算机处理数据的能力，是数字经济时代的重要生产要素。'
  },
  {
    id: 13,
    name: '大数据',
    icon: svgs.chart,
    color: '#FF6348',
    desc: '为决策提供数据支撑和洞察。',
    interpretation: '大数据通过收集、处理和分析海量数据，为决策提供数据支撑和洞察。'
  },
  {
    id: 14,
    name: '工业互联网',
    icon: svgs.network,
    color: '#06B6D4',
    desc: '连接工业生产各环节，实现智能制造。',
    interpretation: '工业互联网连接工业生产各环节，实现数据驱动的智能制造和优化。'
  },
  {
    id: 15,
    name: "新材料",
    color: "#06B6D4",
    icon: svgs.flask,
    desc: "性能突破，改变产品形态的基础。",
    interpretation: '新材料是指新近发展或正在发展的具有优异性能的结构材料和有特殊性质的功能材料。'
  }
];

// Use 14 items effectively
const DISPLAY_ITEMS = INDUSTRIES.slice(0, 14);

Page({
  data: {
    items: DISPLAY_ITEMS,
    rotation: 0,
    activeIdx: 0,
    radius: 1100, // Increased radius to prevent intersection
    angleStep: 360 / 14,
    tiltAngle: -12, // degrees
    transitionDuration: 1, // Start with transition enabled
  },

  onLoad() {
    this.touchStartX = 0;
    this.startRotation = 0;
    this.lastTouchX = 0;
    this.lastMoveTime = 0;
    this.velocity = 0;
  },

  // 返回首页
  goBack() {
    wx.navigateBack({
      fail: () => {
        wx.switchTab({
          url: '/pages/index/index'
        })
      }
    })
  },

  // 触摸开始
  handleTouchStart(e) {
    this.touchStartX = e.touches[0].clientX;
    this.lastTouchX = e.touches[0].clientX;
    this.startRotation = this.data.rotation;
    this.lastMoveTime = Date.now();
    this.velocity = 0;

    // Disable transition for immediate follow
    this.setData({ transitionDuration: 0 });
  },

  // 触摸移动
  handleTouchMove(e) {
    const currentX = e.touches[0].clientX;
    const deltaX = currentX - this.touchStartX;

    // Calculate velocity
    const now = Date.now();
    const dt = now - this.lastMoveTime;
    if (dt > 10) {
      const dx = currentX - this.lastTouchX;
      this.velocity = dx / dt; // pixels per ms
      this.lastTouchX = currentX;
      this.lastMoveTime = now;
    }

    // Sensitivity factor: how many degrees per pixel?
    // Reduced from 0.5 to 0.25 for slower, more controlled movement
    const sensitivity = 0.25;
    const newRotation = this.startRotation - (deltaX * sensitivity);

    this.setData({ rotation: newRotation });
  },

  // 触摸结束
  handleTouchEnd(e) {
    // Add momentum based on velocity
    // Simple inertia
    let finalRotation = this.data.rotation;

    // If meaningful velocity, flick
    // Reduced multiplier from 300 to 150 for less slide
    if (Math.abs(this.velocity) > 0.1) {
      finalRotation -= this.velocity * 150;
    }

    // Snap to nearest item
    const { angleStep, items } = this.data;
    const total = items.length;

    // Find nearest multiple of angleStep
    // Math.round(rotation / step) * step
    const snapIndex = Math.round(finalRotation / angleStep);
    const targetRotation = snapIndex * angleStep;

    // Calculate activeIdx
    // activeIdx corresponds to the item at the front (rotation 0 relative)
    // index 0 is at 0 degrees.
    // If rotation is positive (prev), index 0 moves right. index 14 comes to center.
    // Normalized index = (-snapIndex) % total
    let normalizedIdx = (-snapIndex) % total;
    if (normalizedIdx < 0) normalizedIdx += total;

    this.setData({
      transitionDuration: 0.8, // Smooth snap back
      rotation: targetRotation,
      activeIdx: normalizedIdx
    });
  },

  next() {
    const { rotation, activeIdx, items } = this.data;
    const total = items.length;

    this.setData({
      transitionDuration: 0.8,
      rotation: rotation - this.data.angleStep,
      activeIdx: (activeIdx + 1) % total
    });
  },

  prev() {
    const { rotation, activeIdx, items } = this.data;
    const total = items.length;

    let newIdx = (activeIdx - 1) % total;
    if (newIdx < 0) newIdx += total;

    this.setData({
      transitionDuration: 0.8,
      rotation: rotation + this.data.angleStep,
      activeIdx: newIdx
    });
  },

  // 点击某个卡片直接跳转到该卡片
  onCardTap(e) {
    const idx = e.currentTarget.dataset.index;
    const { activeIdx, items, angleStep, rotation } = this.data;

    if (idx === activeIdx) return;

    let diff = idx - activeIdx;
    const total = items.length;
    if (diff > total / 2) diff -= total;
    if (diff < -total / 2) diff += total;

    this.setData({
      transitionDuration: 0.8,
      rotation: rotation - (diff * angleStep),
      activeIdx: idx
    });
  },

  onShareAppMessage() {
    return {
      title: '行业图说解读',
      path: '/pages/image-interpretation/image-interpretation'
    }
  }
})
