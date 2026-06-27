#!/usr/bin/env node
const { Command } = require('commander');
const Table = require('cli-table3');
const chalk = require('chalk');
const fs = require('fs');
const path = require('path');
const { listProducts, fetchOrdersPage, listUsers, banUser, getToken } = require('./api');

const program = new Command();

program
  .name('campus-cli')
  .description('校园二手交易平台 CLI 管理工具')
  .version('1.0.0');

// 全局选项 — token
function withToken(cmd) {
  return cmd.option('-t, --token <token>', 'JWT token（也可用环境变量 CAMPUS_TOKEN）');
}

// ==================== list-products ====================
withToken(program.command('list-products')
  .description('搜索商品列表')
  .option('--keyword <kw>', '搜索关键词（标题）')
  .option('--category <cat>', '分类: BOOK/ELECTRONICS/LIFESTYLE/SPORTS/OTHER')
  .option('--page <n>', '页码', '1')
  .option('--size <n>', '每页条数', '20')
  .action(async (opts) => {
    try {
      const data = await listProducts(opts);
      if (!data || !data.records || data.records.length === 0) {
        console.log(chalk.yellow('暂无商品'));
        return;
      }
      printProductTable(data.records);
      console.log(chalk.gray(`第 ${opts.page || 1} 页 / 共 ${data.total} 条`));
    } catch (err) {
      console.error(chalk.red(err.message));
      process.exit(1);
    }
  }));

// ==================== export-orders ====================
withToken(program.command('export-orders')
  .description('导出订单为 CSV')
  .option('--status <s>', '订单状态过滤')
  .option('--from <date>', '开始日期 YYYY-MM-DD')
  .option('--to <date>', '结束日期 YYYY-MM-DD')
  .option('--output <path>', '输出文件路径')
  .action(async (opts) => {
    try {
      const token = getToken(opts);
      if (!token) {
        console.error(chalk.red('请提供管理员 token：--token 或设置环境变量 CAMPUS_TOKEN'));
        process.exit(1);
      }

      // 分页拉取买到的 + 卖出的订单
      const orders = [];
      let page = 1;
      let hasMore = true;

      while (hasMore) {
        const bought = await fetchOrdersPage(token, '/orders/bought', { page, size: 50, status: opts.status });
        const sold = await fetchOrdersPage(token, '/orders/sold', { page, size: 50, status: opts.status });

        const allRecords = [...(bought?.records || []), ...(sold?.records || [])];
        orders.push(...allRecords);
        page++;
        hasMore = (bought?.records?.length > 0 || sold?.records?.length > 0);
      }

      if (orders.length === 0) {
        console.log(chalk.yellow('没有符合条件的订单'));
        return;
      }

      const outputPath = opts.output || `orders_${today()}.csv`;
      writeCSV(outputPath, orders, ['id', 'buyerId', 'sellerId', 'productId', 'amount', 'status', 'paidAt', 'createdAt']);
      console.log(chalk.green(`已导出 ${orders.length} 条订单 → ${outputPath}`));
    } catch (err) {
      console.error(chalk.red(err.message));
      process.exit(1);
    }
  }));

// ==================== ban-user ====================
withToken(program.command('ban-user')
  .description('封禁用户')
  .requiredOption('--userId <id>', '用户 ID')
  .action(async (opts) => {
    try {
      const token = getToken(opts);
      if (!token) {
        console.error(chalk.red('请提供管理员 token：--token 或设置环境变量 CAMPUS_TOKEN'));
        process.exit(1);
      }
      const res = await banUser(token, opts.userId);
      if (res.code === 200) {
        console.log(chalk.green(`用户 ${opts.userId} 已封禁`));
      } else {
        console.error(chalk.red(res.message || '封禁失败'));
        process.exit(1);
      }
    } catch (err) {
      console.error(chalk.red(err.message));
      process.exit(1);
    }
  }));

program.parse();

// ==================== 工具函数 ====================

function printProductTable(products) {
  const table = new Table({
    head: ['ID', '标题', '价格', '分类', '卖家'],
    colWidths: [6, 30, 10, 15, 15],
    wordWrap: true,
  });
  products.forEach(p => {
    table.push([
      p.id || '',
      truncate(p.title, 30),
      `¥${p.price || 0}`,
      p.category || '',
      p.sellerName || `用户${p.userId || ''}`,
    ]);
  });
  console.log(table.toString());
}

function truncate(str, len) {
  if (!str) return '';
  return str.length > len ? str.slice(0, len - 3) + '...' : str;
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function writeCSV(filePath, rows, columns) {
  const header = columns.join(',');
  const body = rows.map(row =>
    columns.map(col => {
      const val = row[col];
      if (val === null || val === undefined) return '';
      const str = String(val).replace(/"/g, '""');
      return /[,"\n\r]/.test(str) ? `"${str}"` : str;
    }).join(',')
  ).join('\n');
  fs.writeFileSync(path.resolve(filePath), '﻿' + header + '\n' + body, 'utf-8');
}
