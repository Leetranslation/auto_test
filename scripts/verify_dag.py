#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""验证 DAG 编排引擎：条件分支是否按 edges 正确跳转"""
import json, time, urllib.request

BASE = "http://127.0.0.1:9090"

def call(method, path, token=None, body=None):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode("utf-8"))

token = call("POST", "/api/auth/login", body={"username": "admin", "password": "admin123"})["data"]["token"]

# 分支场景：health -> 条件(恒真) -> 真分支API / 假分支API
graph = {
    "nodes": [
        {"nodeKey": "node_1", "nodeType": "START", "nodeName": "开始", "config": {}},
        {"nodeKey": "node_2", "nodeType": "API", "nodeName": "健康检查", "config": {
            "method": "GET", "url": "http://127.0.0.1:9090/api/health",
            "assertions": [{"type": "bodyCode", "expect": 0}]}},
        {"nodeKey": "node_3", "nodeType": "CONDITION", "nodeName": "恒真条件", "config": {
            "left": "x", "op": "EQ", "right": "x"}},
        {"nodeKey": "node_4", "nodeType": "API", "nodeName": "真分支接口", "config": {
            "method": "GET", "url": "http://127.0.0.1:9090/api/health",
            "assertions": [{"type": "bodyCode", "expect": 0}]}},
        {"nodeKey": "node_5", "nodeType": "API", "nodeName": "假分支接口(应跳过)", "config": {
            "method": "GET", "url": "http://127.0.0.1:9090/api/health"}},
    ],
    "edges": [
        {"source": "node_1", "target": "node_2", "condition": ""},
        {"source": "node_2", "target": "node_3", "condition": ""},
        {"source": "node_3", "target": "node_4", "condition": "true"},
        {"source": "node_3", "target": "node_5", "condition": "false"},
    ]
}

scene = call("POST", "/api/scene/create", token, {"projectId": 1, "sceneName": "DAG分支验证"})["data"]
call("POST", "/api/scene/version/save", token,
     {"sceneId": scene["id"], "graphJson": json.dumps(graph, ensure_ascii=False), "publish": True})
run = call("POST", "/api/exec/run", token,
           {"targetType": "SCENE", "targetId": scene["id"], "projectId": 1, "environmentId": 1})["data"]

time.sleep(3)  # 等待异步执行完成
detail = call("GET", "/api/exec/detail?executionId=%d" % run["id"], token)["data"]
steps = detail["steps"]

print("执行状态:", run["status"])
print("实际执行节点顺序:")
for s in steps:
    print("  [%d] %s - %s => %s" % (s["sortNo"], s["stepType"], s["stepName"], s["status"]))

names = [s["stepName"] for s in steps]
assert "真分支接口" in names, "真分支未执行！"
assert "假分支接口(应跳过)" not in names, "假分支被错误执行！"
print("\n✅ 验证通过：条件分支正确（真分支执行、假分支跳过），DAG 编排生效")
