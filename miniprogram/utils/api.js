/**
 * API接口定义
 */
const request = require('./request')

// ==================== 首页相关 ====================
/**
 * 获取首页数据
 */
function getHomeData() {
  return request.get('/home/data')
}

// ==================== 政策相关 ====================
/**
 * 获取政策列表
 * @param {Object} params 查询参数
 */
function getPolicyList(params) {
  return request.get('/policy/list', params)
}

/**
 * 获取政策详情
 * @param {Number} id 政策ID
 */
function getPolicyDetail(id) {
  return request.get(`/policy/detail/${id}`)
}

/**
 * 申请政策
 * @param {Object} data 申请数据
 */
function applyPolicy(data) {
  return request.post('/policy/apply', data)
}

// ==================== 任务相关 ====================
/**
 * 获取任务列表
 * @param {Object} params 查询参数
 */
function getTaskList(params) {
  return request.get('/task/list', params)
}

/**
 * 获取任务详情
 * @param {Number} id 任务ID
 */
function getTaskDetail(id) {
  return request.get(`/task/detail/${id}`)
}

/**
 * 参与任务
 * @param {Number} taskId 任务ID
 */
function joinTask(taskId) {
  return request.post('/task/join', { taskId })
}

/**
 * 提交任务方案
 * @param {Object} data 方案数据
 */
function submitTask(data) {
  return request.post('/task/submit', data)
}

/**
 * 获取我的任务列表
 */
function getMyTasks() {
  return request.get('/task/my-tasks')
}

// ==================== 资源相关 ====================
/**
 * 获取专家列表
 * @param {Object} params 查询参数
 */
function getExpertList(params) {
  return request.get('/resource/expert/list', params)
}

/**
 * 获取专家详情
 * @param {Number} id 专家ID
 */
function getExpertDetail(id) {
  return request.get(`/resource/expert/detail/${id}`)
}

/**
 * 预约专家咨询
 * @param {Object} data 预约数据
 */
function bookExpert(data) {
  return request.post('/resource/expert/book', data)
}

/**
 * 获取实验室列表
 * @param {Object} params 查询参数
 */
function getLabList(params) {
  return request.get('/resource/lab/list', params)
}

/**
 * 获取实验室详情
 * @param {Number} id 实验室ID
 */
function getLabDetail(id) {
  return request.get(`/resource/lab/detail/${id}`)
}

/**
 * 预约实验室
 * @param {Object} data 预约数据
 */
function bookLab(data) {
  return request.post('/resource/lab/book', data)
}

// ==================== 用户相关 ====================
/**
 * 用户登录
 * @param {String} code 微信登录code
 */
function login(code) {
  return request.post('/user/login', { code })
}

/**
 * 获取用户信息
 */
function getUserInfo() {
  return request.get('/user/info')
}

/**
 * 更新用户信息
 * @param {Object} data 用户信息
 */
function updateUserInfo(data) {
  return request.post('/user/update', data)
}

/**
 * 获取用户统计数据
 */
function getUserStats() {
  return request.get('/user/stats')
}

// ==================== 上传相关 ====================
/**
 * 上传图片
 * @param {String} filePath 文件路径
 */
function uploadImage(filePath) {
  return request.uploadFile(filePath, '/upload/image')
}

/**
 * 上传文件
 * @param {String} filePath 文件路径
 */
function uploadFile(filePath) {
  return request.uploadFile(filePath, '/upload/file')
}

module.exports = {
  // 首页
  getHomeData,
  
  // 政策
  getPolicyList,
  getPolicyDetail,
  applyPolicy,
  
  // 任务
  getTaskList,
  getTaskDetail,
  joinTask,
  submitTask,
  getMyTasks,
  
  // 资源
  getExpertList,
  getExpertDetail,
  bookExpert,
  getLabList,
  getLabDetail,
  bookLab,
  
  // 用户
  login,
  getUserInfo,
  updateUserInfo,
  getUserStats,
  
  // 上传
  uploadImage,
  uploadFile
}

