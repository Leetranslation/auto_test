#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UI 执行器（Playwright Runner）
- 输入：任务 JSON（stdin 或 --task-file 指向 json 文件）
- 输出：结构化结果 JSON（stdout），含每步状态、截图、Trace 路径
- 用途：阶段3/阶段5 UI 用例与混合场景的 UI 节点执行

任务 JSON 结构：
{
  "baseUrl": "http://127.0.0.1:8090",
  "storageState": {"cookies": [], "localStorage": {"token": "..."}},  // 可选：登录态注入
  "timeoutMs": 10000,
  "steps": [
    {"action":"open","url":"/login"},
    {"action":"input","locator":{"strategy":"DATA_TESTID","value":"login-username"},"text":"admin"},
    {"action":"input","locator":{"strategy":"DATA_TESTID","value":"login-password"},"text":"admin123"},
    {"action":"click","locator":{"strategy":"DATA_TESTID","value":"login-submit"}},
    {"action":"wait","locator":{"strategy":"DATA_TESTID","value":"some-element"},"timeout":5000},
    {"action":"assert","locator":{"strategy":"DATA_TESTID","value":"some-element"},"visible":true},
    {"action":"screenshot"}
  ]
}
"""
import argparse
import json
import sys
import traceback


def build_locator(page, loc):
    strategy = (loc or {}).get("strategy", "DATA_TESTID").upper()
    value = (loc or {}).get("value", "")
    if strategy == "DATA_TESTID":
        return page.get_by_test_id(value)
    if strategy == "ROLE":
        return page.get_by_role("button", name=value)
    if strategy == "LABEL":
        return page.get_by_label(value)
    if strategy == "TEXT":
        return page.get_by_text(value)
    if strategy == "CSS":
        return page.locator(value)
    if strategy == "XPATH":
        return page.locator(f"xpath={value}")
    if strategy == "NAME":
        return page.locator(f"[name='{value}']")
    if strategy == "ID":
        return page.locator(f"#{value}")
    return page.locator(value)


def run(task):
    from playwright.sync_api import sync_playwright

    results = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        # 登录态注入
        storage = task.get("storageState")
        if storage:
            if storage.get("cookies"):
                context.add_cookies(storage["cookies"])
            for k, v in (storage.get("localStorage") or {}).items():
                context.add_init_script(f"localStorage.setItem({json.dumps(k)}, {json.dumps(v)});")
        page = context.new_page()
        page.set_default_timeout(task.get("timeoutMs", 10000))

        base_url = task.get("baseUrl", "")
        for i, step in enumerate(task.get("steps", [])):
            action = step.get("action")
            step_result = {"index": i, "action": action, "status": "PASSED", "error": None}
            try:
                if action == "open":
                    page.goto(base_url + step.get("url", ""))
                elif action == "input":
                    build_locator(page, step.get("locator")).fill(step.get("text", ""))
                elif action == "click":
                    build_locator(page, step.get("locator")).click()
                elif action == "wait":
                    build_locator(page, step.get("locator")).wait_for(timeout=step.get("timeout", 5000))
                elif action == "assert":
                    el = build_locator(page, step.get("locator"))
                    if step.get("visible"):
                        el.wait_for(state="visible", timeout=step.get("timeout", 5000))
                    if step.get("text") is not None:
                        content = el.inner_text()
                        assert step["text"] in content, f"文本不匹配: {content}"
                elif action == "screenshot":
                    page.screenshot(path=f"step_{i}.png", full_page=False)
                    step_result["screenshot"] = f"step_{i}.png"
                else:
                    step_result["status"] = "FAILED"
                    step_result["error"] = f"未知动作: {action}"
            except Exception as e:
                step_result["status"] = "FAILED"
                step_result["error"] = str(e)
                try:
                    page.screenshot(path=f"step_{i}_fail.png")
                    step_result["screenshot"] = f"step_{i}_fail.png"
                except Exception:
                    pass
            results.append(step_result)

        browser.close()
    return results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task-file", help="任务 JSON 文件路径")
    args = parser.parse_args()

    if args.task_file:
        with open(args.task_file, "r", encoding="utf-8") as f:
            task = json.load(f)
    else:
        task = json.load(sys.stdin)

    try:
        results = run(task)
        print(json.dumps({"code": 0, "results": results}, ensure_ascii=False))
    except Exception:
        print(json.dumps({"code": 500, "message": traceback.format_exc()}, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    main()
