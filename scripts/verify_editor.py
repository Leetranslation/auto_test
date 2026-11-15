#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""用 Playwright 打开可视化编排器并截图，验证四栏布局渲染"""
from playwright.sync_api import sync_playwright

out = "E:/test_demo/autotest-platform/artifacts/_editor_check.png"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.goto("http://localhost:5174/", wait_until="networkidle")
    # 登录
    page.get_by_test_id("login-username").fill("admin")
    page.get_by_test_id("login-password").fill("admin123")
    page.get_by_test_id("login-submit").click()
    page.wait_for_url("**/dashboard", timeout=10000)
    # 进场景编排
    page.goto("http://localhost:5174/scene", wait_until="networkidle")
    page.wait_for_timeout(1500)
    # 点第一个「编排」按钮（精确匹配，避免命中侧边栏「场景编排」菜单）
    btns = page.get_by_role("button", name="编排", exact=True)
    if btns.count() == 0:
        print("未找到「编排」按钮，先建场景")
        page.get_by_role("button", name="新建场景", exact=True).click()
        page.wait_for_timeout(500)
        page.locator(".el-dialog input").first.fill("可视化编排验证场景")
        page.locator(".el-dialog").get_by_role("button", name="保存").click()
        page.wait_for_timeout(1000)
        btns = page.get_by_role("button", name="编排", exact=True)
    btns.first.click()
    page.wait_for_timeout(2500)
    page.screenshot(path=out, full_page=False)
    # 验证关键元素存在
    has_palette = "节点组件库" in page.content()
    has_canvas = page.locator(".vue-flow").count() > 0
    has_config = "节点配置" in page.content()
    has_bottom = "运行日志" in page.content() and "变量" in page.content()
    print(f"左侧节点组件库: {has_palette}")
    print(f"中间画布(vue-flow): {has_canvas}")
    print(f"右侧节点配置: {has_config}")
    print(f"底部运行日志+变量: {has_bottom}")
    browser.close()

print("截图已保存:", out)
