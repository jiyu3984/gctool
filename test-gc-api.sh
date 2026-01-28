#!/bin/bash

# 测试 Grasscutter API 的实际响应格式
SERVER_URL="http://127.0.0.1:1145"

echo "=== 测试 Grasscutter OpenCommand API ==="
echo ""

echo "1. 测试 ping 请求:"
echo "请求: {\"action\":\"ping\"}"
curl -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d '{"action":"ping"}' \
  -w "\nHTTP状态码: %{http_code}\n" \
  2>&1

echo ""
echo "---"
echo ""

echo "2. 测试 online 请求:"
echo "请求: {\"action\":\"online\"}"
curl -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d '{"action":"online"}' \
  -w "\nHTTP状态码: %{http_code}\n" \
  2>&1

echo ""
echo "=== 测试完成 ==="
