#!/usr/bin/env node
// 统一的前端启动入口（dev / start 共用）。
//
// 解决的问题：vinext 的 `dev`/`start` 不认 vite.config.ts 的 strictPort，
// 端口被占用时会悄悄切到下一个可用端口。本脚本在启动 vinext 之前先做一次
// 端口占用预检——被占用直接报错退出，绝不会自动切换到其他端口。
//
// 端口来源（单一来源，优先级从高到低）：
//   1. 环境变量 PORT
//   2. 项目根目录 .env 里的 PORT=（本地覆盖，已被 gitignore）
//   3. 规范缺省值 8847（与 vite.config.ts 的 server.port 保持一致）
//
// 用法：node scripts/serve.mjs <dev|start>

import { spawn } from "node:child_process";
import net from "node:net";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

const DEFAULT_PORT = 8847;
const COMMAND = process.argv[2];

if (COMMAND !== "dev" && COMMAND !== "start") {
  console.error("Usage: node scripts/serve.mjs <dev|start>");
  process.exit(1);
}

function resolvePort() {
  if (process.env.PORT) return Number(process.env.PORT);
  const envPath = path.join(root, ".env");
  if (fs.existsSync(envPath)) {
    const raw = fs.readFileSync(envPath, "utf8");
    const matched = raw.match(/^\s*PORT\s*=\s*(\d+)\s*$/m);
    if (matched) return Number(matched[1]);
  }
  return DEFAULT_PORT;
}

const PORT = resolvePort();
if (!Number.isInteger(PORT) || PORT < 1 || PORT > 65535) {
  console.error(`[serve] 无效的端口号: ${PORT}`);
  process.exit(1);
}

// 预检：尝试绑定端口。已被占用（EADDRINUSE）则直接报错退出，
// 不让 vinext 有机会自动切换到其它端口。
function assertPortFree() {
  return new Promise((resolve, reject) => {
    const tester = net.createServer();
    tester.once("error", (err) => {
      if (err.code === "EADDRINUSE") {
        reject(
          new Error(
            `端口 ${PORT} 已被占用，前端无法在此端口启动（已锁定，不会自动切换端口）。` +
              `请先释放该端口，或停止已运行的 dev / start 进程后再试。`
          )
        );
      } else {
        reject(err);
      }
    });
    tester.once("listening", () => {
      tester.close(() => resolve());
    });
    tester.listen(PORT, "0.0.0.0");
  });
}

try {
  await assertPortFree();
} catch (err) {
  console.error(`\n  [serve] 启动失败：${err.message}\n`);
  process.exit(1);
}

console.log(`\n  vinext ${COMMAND}  (port ${PORT}, locked)\n`);

// 显式把端口传给 vinext，避免它回落到缺省 3000。
const child = spawn("vinext", [COMMAND, "--port", String(PORT)], {
  stdio: "inherit",
  shell: process.platform === "win32",
  env: process.env,
});

child.on("exit", (code) => process.exit(code ?? 0));
child.on("error", (err) => {
  console.error(`\n  [serve] 无法启动 vinext: ${err.message}\n`);
  process.exit(1);
});
