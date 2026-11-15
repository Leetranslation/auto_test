#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""验证第一层场景主页面布局（左 API 列表 / 中画布 / 右 4 标签 / 顶部操作栏）"""
from playwright.sync_api import sync_playwright

out = "E:/test_demo/autotest-platform/artifacts/_scene_main.png"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.goto("http://localhost:5174/", wait_until="networkidle")
    page.get_by_test_id("login-username").fill("admin")
    page.get_by_test_id("login-password").fill("admin123")
    page.get_by_test_id("login-submit").click()
    page.wait_for_url("**/dashboard", timeout=10000)
    page.goto("http://localhost:5174/scene", wait_until="networkidle")
    page.wait_for_timeout(1500)
    btn = page.get_by_role("button", name="编排", exact=True)
    btn.first.click()
    page.wait_for_timeout(2500)
    page.screenshot(path=out)

    c = page.content()
    checks = {
        "左侧API列表(标签)": "API 列表" in c,
        "添加API按钮": "添加 API" in c,
        "中间画布(vue-flow)": page.locator(".vue-flow").count() > 0,
        "右侧基础信息": "基础信息" in c,
        "右侧上下文管理": "上下文管理" in c,
        "右侧脚本管理": "脚本管理" in c,
        "右侧流程管理": "流程管理" in c,
        "顶部保存按钮": page.get_by_role("button", name="保存", exact=True).count() > 0,
        "顶部执行按钮": page.get_by_role("button", name="执行", exact=True).count() > 0,
        "顶部取消按钮": page.get_by_role("button", name="取消", exact=True).count() > 0,
    }
    for k, v in checks.items():
        print(f"{k}: {v}")
    browser.close()

print("截图:", out)
