const axios = require('axios');
const fs = require('fs');
const path = require('path');
const os = require('os');

const BASE_URL = process.env.CAMPUS_API || 'http://localhost:8081/api';

function getToken(opts) {
  if (opts && opts.token) return opts.token;
  if (process.env.CAMPUS_TOKEN) return process.env.CAMPUS_TOKEN;
  const cfgPath = path.join(os.homedir(), '.campus-cli', 'config.json');
  if (fs.existsSync(cfgPath)) {
    try {
      return JSON.parse(fs.readFileSync(cfgPath, 'utf-8')).token;
    } catch {}
  }
  return null;
}

function client(token) {
  return axios.create({
    baseURL: BASE_URL,
    timeout: 30000,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
}

function handleError(err) {
  if (err.response) {
    const { status, data } = err.response;
    throw new Error(`[${status}] ${data?.message || '请求失败'}`);
  }
  throw new Error(`网络错误: ${err.message}`);
}

// GET /api/products — 公开接口
async function listProducts({ keyword, category, page, size }) {
  try {
    const res = await client().get('/products', {
      params: { keyword, category, page: page || 1, size: size || 20 },
    });
    return res.data.data;
  } catch (err) {
    handleError(err);
  }
}

// 获取订单（分页，需要 token）
async function fetchOrdersPage(token, endpoint, params) {
  try {
    const res = await client(token).get(endpoint, { params });
    return res.data.data;
  } catch (err) {
    handleError(err);
  }
}

// GET /api/admin/users — 管理员接口
async function listUsers(token, { page, size, keyword, status }) {
  try {
    const res = await client(token).get('/admin/users', {
      params: { page: page || 1, size: size || 50, keyword, status },
    });
    return res.data.data;
  } catch (err) {
    handleError(err);
  }
}

// PUT /api/admin/users/{id}/ban — 管理员接口
async function banUser(token, userId) {
  try {
    const res = await client(token).put(`/admin/users/${userId}/ban`);
    return res.data;
  } catch (err) {
    handleError(err);
  }
}

module.exports = { listProducts, fetchOrdersPage, listUsers, banUser, getToken };
