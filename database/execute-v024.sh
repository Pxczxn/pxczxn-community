#!/bin/bash

# V024 数据库迁移执行脚本
# 用法: ./execute-v024.sh [host] [port] [database] [username]

set -e

HOST=${1:-localhost}
PORT=${2:-3306}
DATABASE=${3:-pxczxn_community}
USERNAME=${4:-root}

echo "========================================="
echo "V024 数据库迁移执行"
echo "========================================="
echo "主机: $HOST:$PORT"
echo "数据库: $DATABASE"
echo "用户: $USERNAME"
echo "========================================="
echo ""

read -sp "请输入数据库密码: " PASSWORD
echo ""
echo ""

# 执行迁移
echo "[1/3] 执行 V024 迁移..."
mysql -h "$HOST" -P "$PORT" -u "$USERNAME" -p"$PASSWORD" "$DATABASE" < database/migrations/V024__m3_team_application_review.sql
if [ $? -eq 0 ]; then
    echo "✓ V024 迁移执行成功"
else
    echo "✗ V024 迁移执行失败"
    exit 1
fi
echo ""

# 执行验证
echo "[2/3] 执行 V024 验证..."
mysql -h "$HOST" -P "$PORT" -u "$USERNAME" -p"$PASSWORD" "$DATABASE" < database/verify/V024__verify_team_application_review.sql
if [ $? -eq 0 ]; then
    echo "✓ V024 验证通过"
else
    echo "✗ V024 验证失败"
    exit 1
fi
echo ""

# 检查唯一索引
echo "[3/3] 检查唯一索引..."
INDEX_COUNT=$(mysql -h "$HOST" -P "$PORT" -u "$USERNAME" -p"$PASSWORD" "$DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$DATABASE' AND TABLE_NAME='team_application' AND INDEX_NAME='uk_team_slug_active';")
if [ "$INDEX_COUNT" -ge 2 ]; then
    echo "✓ 唯一索引 uk_team_slug_active 已创建 ($INDEX_COUNT 列)"
else
    echo "✗ 唯一索引未找到或不完整"
    exit 1
fi
echo ""

echo "========================================="
echo "V024 迁移全部完成！"
echo "========================================="
echo ""
echo "执行时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
