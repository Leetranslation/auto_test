#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""打开有多节点的场景，验证直角折线连线样式"""
from playwright.sync_api import sync_playwright

out = "E:/test_demo/autotest-platform/artifacts/_canvas_orth.png"

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
    # 找「DAG分支验证」那一行的「编排」按钮
    rows = page.locator(".el-table__body tr")
    target = None
    for i in range(rows.count()):
        if "DAG分支验证" in rows.nth(i).inner_text():
            target = rows.nth(i)
            break
    if target is None:
        # 回退：第一个编排
        target = page.get_by_role("button", name="编排", exact=True).first
    else:
        target = target.get_by_role("button", name="编排", exact=True)
    target.click()
    page.wait_for_timeout(2500)
    page.screenshot(path=out)

    # 检查边是否为 step 类型
    edge_types = page.evaluate("() => { const els = document.querySelectorAll('.vue-flow__edge'); return Array.from(els).map(e => e.getAttribute('class')); }")
    print("边类型 class:", edge_types)
    browser.close()
print("截图:", out)
