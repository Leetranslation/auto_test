#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""阶段6：批量创建 ERP 业务场景并真实执行验证"""
import json
import urllib.request

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

def login(u, p):
    return call("POST", "/api/auth/login", body={"username": u, "password": p})["data"]["token"]

token = login("admin", "admin123")

def make_graph(login_user, login_pwd, qmethod, qurl, atype, aexpect):
    return json.dumps({"nodes": [
        {"nodeKey": "node_1", "nodeType": "START", "nodeName": "开始", "config": {}},
        {"nodeKey": "node_2", "nodeType": "API", "nodeName": "登录", "config": {
            "method": "POST", "url": "http://127.0.0.1:8081/api/auth/login",
            "body": json.dumps({"username": login_user, "password": login_pwd}),
            "assertions": [{"type": "bodyCode", "expect": 0}],
            "extracts": [{"varName": "token", "path": "data.token"}]}},
        {"nodeKey": "node_3", "nodeType": "API", "nodeName": "查询", "config": {
            "method": qmethod, "url": qurl, "assertions": [{"type": atype, "expect": aexpect}]}},
        {"nodeKey": "node_4", "nodeType": "END", "nodeName": "结束", "config": {}}
    ], "edges": []})

SCENES = [
    ("认证-登录成功", "admin", "admin123", "GET", "http://127.0.0.1:8081/api/inventory/list?page=1&pageSize=1", "bodyCode", 0),
    ("权限-越权拦截", "warehouse01", "Wh@123", "GET", "http://127.0.0.1:8081/api/sales/order/list?page=1&pageSize=1", "httpStatus", 403),
    ("销售-订单列表查询", "admin", "admin123", "GET", "http://127.0.0.1:8081/api/sales/order/list?page=1&pageSize=1", "bodyCode", 0),
    ("采购-订单列表查询", "admin", "admin123", "GET", "http://127.0.0.1:8081/api/purchase/order/list?page=1&pageSize=1", "bodyCode", 0),
    ("生产-工单列表查询", "admin", "admin123", "GET", "http://127.0.0.1:8081/api/production/order/list?page=1&pageSize=1", "bodyCode", 0),
    ("库存-库存查询", "admin", "admin123", "GET", "http://127.0.0.1:8081/api/inventory/list?page=1&pageSize=1", "bodyCode", 0),
    ("MRP-计划列表查询", "admin", "admin123", "GET", "http://127.0.0.1:8081/api/mrp/plans", "bodyCode", 0),
]

print("场景名 | 执行结果")
for name, u, p, m, url, at, ae in SCENES:
    scene = call("POST", "/api/scene/create", token, {"projectId": 1, "sceneName": name})["data"]
    call("POST", "/api/scene/version/save", token,
         {"sceneId": scene["id"], "graphJson": make_graph(u, p, m, url, at, ae), "publish": True})
    run = call("POST", "/api/exec/run", token,
               {"targetType": "SCENE", "targetId": scene["id"], "projectId": 1, "environmentId": 1})["data"]
    print(f"{name} | {run['status']} {run['summaryJson']}")
