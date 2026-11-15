#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UI 执行器（Playwright Runner）v2

按「Java-Python 执行器通信协议 v1.0」实现，补齐：
  1. Playwright Trace 录制与导出（context 级，导出 trace.zip）
  2. 浏览器上下文完整生命周期管理（launch -> context -> tracing -> page -> stop -> close）
  3. 截图 + Trace 统一归档（outputDir 下按 executionId 归档）
  4. 多浏览器类型支持（CHROMIUM / FIREFOX / WEBKIT）
  5. 步骤级失败重试 + 任务级超时控制

输入：任务 JSON（--task-file 或 stdin）
输出：结构化结果 JSON（stdout），含每步 attemptNo、截图/Trace 路径、汇总

任务 JSON：
{
  "protocolVersion": "1.0",
  "executionId": 123,
  "baseUrl": "http://127.0.0.1:8090",
  "browserType": "CHROMIUM",                // CHROMIUM|FIREFOX|WEBKIT
  "viewport": {"width": 1280, "height": 720},
  "timeoutMs": 10000,
  "taskTimeoutMs": 300000,
  "retry": {"maxAttempts": 2, "retryDelayMs": 300},
  "failFast": false,
  "outputDir": "E:/.../artifacts/123/ui",
  "storageState": {"cookies": [], "localStorage": {"token": "..."}},
  "steps": [ { "action":"open","url":"/login" }, ... ]
}
"""
import argparse
import json
import os
import sys
import time
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


def get_browser(p, browser_type):
    bt = (browser_type or "CHROMIUM").upper()
    if bt == "FIREFOX":
        return p.firefox
    if bt == "WEBKIT":
        return p.webkit
    return p.chromium


def run_action(page, step, base_url):
    action = step.get("action")
    if action == "open":
        page.goto(base_url + step.get("url", ""))
    elif action == "goto":
        page.goto(step.get("url", ""))
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
        pass  # 由外层统一截图
    else:
        raise ValueError(f"未知动作: {action}")


def execute_step(page, step, idx, base_url, out_dir, max_attempts, retry_delay):
    """单步执行，含失败重试；返回 step_result"""
    action = step.get("action")
    attempts = max_attempts if max_attempts and max_attempts >= 1 else 1
    last_error = None
    last_shot = None
    for attempt in range(attempts):
        try:
            run_action(page, step, base_url)
            shot = None
            if action == "screenshot" or step.get("screenshot"):
                shot = os.path.join(out_dir, f"step_{idx}_{attempt}.png")
                page.screenshot(path=shot)
            return {
                "index": idx, "action": action, "status": "PASSED",
                "attemptNo": attempt, "durationMs": 0, "error": None,
                "screenshot": shot,
            }
        except Exception as e:
            last_error = str(e)
            try:
                last_shot = os.path.join(out_dir, f"step_{idx}_{attempt}_fail.png")
                page.screenshot(path=last_shot)
            except Exception:
                last_shot = None
            if attempt < attempts - 1:
                time.sleep(retry_delay / 1000.0)
    return {
        "index": idx, "action": action, "status": "FAILED",
        "attemptNo": attempts - 1, "durationMs": 0, "error": last_error,
        "screenshot": last_shot,
    }


def run(task):
    from playwright.sync_api import sync_playwright

    out_dir = task.get("outputDir") or "artifacts"
    os.makedirs(out_dir, exist_ok=True)
    browser_type = (task.get("browserType") or "CHROMIUM").upper()
    viewport = task.get("viewport") or {"width": 1280, "height": 720}
    timeout_ms = task.get("timeoutMs", 10000)
    task_timeout_ms = task.get("taskTimeoutMs", 300000)
    retry = task.get("retry") or {}
    max_attempts = retry.get("maxAttempts", 1)
    retry_delay = retry.get("retryDelayMs", 300)
    fail_fast = bool(task.get("failFast", False))

    results = []
    screenshots = []
    trace_path = os.path.join(out_dir, "trace.zip")
    started_at = time.time()

    with sync_playwright() as p:
        try:
            browser = get_browser(p, browser_type).launch(headless=True)
        except Exception as e:
            # 浏览器未安装（如 firefox/webkit 未下载）时给出明确错误
            return {
                "code": 500,
                "message": f"启动浏览器失败({browser_type}): {e}",
                "results": [], "summary": {"total": 0, "passed": 0, "failed": 1},
            }

        # ---- 上下文生命周期管理 ----
        context = browser.new_context(viewport=viewport)
        storage = task.get("storageState")
        if storage:
            if storage.get("cookies"):
                context.add_cookies(storage["cookies"])
            for k, v in (storage.get("localStorage") or {}).items():
                context.add_init_script(f"localStorage.setItem({json.dumps(k)}, {json.dumps(v)});")

        # ---- Trace 录制（全任务一个 context 级 trace）----
        context.tracing.start(screenshots=True, snapshots=True, sources=True)
        page = context.new_page()
        page.set_default_timeout(timeout_ms)
        base_url = task.get("baseUrl", "")

        try:
            for i, step in enumerate(task.get("steps", [])):
                # 任务级超时控制
                if (time.time() - started_at) * 1000 >= task_timeout_ms:
                    results.append({
                        "index": i, "action": step.get("action"), "status": "TIMEOUT",
                        "attemptNo": 0, "durationMs": 0, "error": "任务超时", "screenshot": None,
                    })
                    continue
                r = execute_step(page, step, i, base_url, out_dir, max_attempts, retry_delay)
                results.append(r)
                if r.get("screenshot"):
                    screenshots.append(r["screenshot"])
                if r["status"] == "FAILED" and fail_fast:
                    break
        finally:
            # 收尾：最终截图 + 停止并导出 trace + 关闭上下文与浏览器
            try:
                final_shot = os.path.join(out_dir, "final.png")
                page.screenshot(path=final_shot)
                screenshots.append(final_shot)
            except Exception:
                pass
            try:
                context.tracing.stop(path=trace_path)
            except Exception:
                trace_path = None
            context.close()
            browser.close()

    total = len(results)
    passed = sum(1 for r in results if r["status"] == "PASSED")
    failed = sum(1 for r in results if r["status"] == "FAILED")
    return {
        "code": 0,
        "results": results,
        "artifacts": {"trace": trace_path, "screenshots": screenshots},
        "summary": {"total": total, "passed": passed, "failed": failed},
    }


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
        result = run(task)
        print(json.dumps(result, ensure_ascii=False))
        if result.get("code") != 0:
            sys.exit(1)
    except Exception:
        print(json.dumps({"code": 500, "message": traceback.format_exc()}, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    main()
