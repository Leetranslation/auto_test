#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""验证 UI 节点执行后 Trace/截图产物归档并可访问"""
import json, time, urllib.request

BASE = "http://127.0.0.1:9090"

def call(method, path, token=None, body=None):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read().decode("utf-8"))

token = call("POST", "/api/auth/login", body={"username": "admin", "password": "admin123"})["data"]["token"]

graph = {
    "nodes": [
        {"nodeKey": "node_1", "nodeType": "START", "nodeName": "开始", "config": {}},
        {"nodeKey": "node_2", "nodeType": "UI", "nodeName": "打开首页并截图", "config": {
            "baseUrl": "http://127.0.0.1:5174",
            "browserType": "CHROMIUM",
            "retry": {"maxAttempts": 2, "retryDelayMs": 200},
            "steps": [
                {"action": "open", "url": "/"},
                {"action": "screenshot"},
                {"action": "assert", "locator": {"strategy": "TEXT", "value": "不存在XYZ"}, "visible": True, "timeout": 1500}
            ]}},
    ],
    "edges": [{"source": "node_1", "target": "node_2", "condition": ""}]
}

scene = call("POST", "/api/scene/create", token, {"projectId": 1, "sceneName": "UI产物验证"})["data"]
call("POST", "/api/scene/version/save", token,
     {"sceneId": scene["id"], "graphJson": json.dumps(graph, ensure_ascii=False), "publish": True})
run = call("POST", "/api/exec/run", token,
           {"targetType": "SCENE", "targetId": scene["id"], "projectId": 1, "environmentId": 1})["data"]

time.sleep(6)
detail = call("GET", "/api/exec/detail?executionId=%d" % run["id"], token)["data"]
ui_step = [s for s in detail["steps"] if s["stepType"] == "UI"]
print("UI 步骤状态:", ui_step[0]["status"] if ui_step else "无")
if ui_step:
    snap = ui_step[0].get("outputSnapshot")
    print("outputSnapshot:", snap)
    try:
        obj = json.loads(snap) if isinstance(snap, str) else snap
        arts = obj.get("artifacts", {})
        print("trace 产物:", arts.get("trace"))
        print("截图产物:", arts.get("screenshots"))
        # 验证 trace URL 可访问
        trace_url = arts.get("trace")
        if trace_url:
            try:
                r = urllib.request.urlopen(BASE + trace_url, timeout=10)
                print("trace URL 访问: HTTP", r.status, "大小", len(r.read()))
            except Exception as e:
                print("trace URL 访问失败:", e)
    except Exception as e:
        print("解析 outputSnapshot 失败:", e)
