package com.autotest.platform.service;

import java.util.HashMap;
import java.util.Map;

/**
 * 接口名称解析器：把「方法 + 路径」映射为业务中文名（对齐 ERP 接口规范/原型命名）。
 * 优先精确匹配；未命中时按 模块/资源/动作 规则推导。
 */
public final class ApiNameResolver {

    private static final Map<String, String> EXACT = new HashMap<>();

    static {
        put("POST", "/api/auth/login", "登录");
        put("GET", "/api/sales/order/list", "销售订单列表");
        put("GET", "/api/sales/order/items", "销售订单明细");
        put("POST", "/api/sales/order/create", "创建销售订单");
        put("POST", "/api/sales/order/submit", "提交销售订单");
        put("POST", "/api/sales/order/approve", "审核销售订单");
        put("POST", "/api/sales/order/unapprove", "反审核销售订单");
        put("GET", "/api/sales/outbound/list", "销售出库列表");
        put("GET", "/api/sales/return/list", "销售退货列表");
        put("GET", "/api/sales/quotation/list", "销售报价单列表");
        put("GET", "/api/sales/contract/list", "销售合同列表");
        put("GET", "/api/sales/invoice/list", "销售发票列表");
        put("POST", "/api/sales/invoice/create", "创建销售发票");
        put("POST", "/api/sales/invoice/submit", "提交销售发票");
        put("POST", "/api/sales/invoice/approve", "审核销售发票");
        put("GET", "/api/sales/verification/list", "销售核销列表");
        put("POST", "/api/sales/verification/create", "创建销售核销");
        put("POST", "/api/sales/verification/submit", "提交销售核销");
        put("POST", "/api/sales/verification/approve", "审核销售核销");
        put("POST", "/api/sales/verification/unapprove", "反审核销售核销");
        put("GET", "/api/sales/stocking/list", "销售备货单列表");
        put("GET", "/api/sales/afterSale/list", "售后单列表");
        put("GET", "/api/sales/packing/list", "装箱单列表");
        put("GET", "/api/sales/designCenter/list", "设计中心列表");

        put("GET", "/api/purchase/order/list", "采购订单列表");
        put("GET", "/api/purchase/order/items", "采购订单明细");
        put("POST", "/api/purchase/order/create", "创建采购订单");
        put("POST", "/api/purchase/order/submit", "提交采购订单");
        put("POST", "/api/purchase/order/approve", "审核采购订单");
        put("GET", "/api/purchase/requisition/list", "采购申请单列表");
        put("GET", "/api/purchase/inquiry/list", "采购询价单列表");
        put("GET", "/api/purchase/invoice/list", "采购发票列表");
        put("GET", "/api/purchase/verification/list", "采购核销列表");
        put("GET", "/api/purchase/return/list", "采购退货列表");
        put("GET", "/api/purchase/inbound/list", "采购入库列表");

        put("GET", "/api/production/order/list", "生产工单列表");
        put("POST", "/api/production/order/start", "工单开始");
        put("POST", "/api/production/order/finish", "工单完工");
        put("POST", "/api/production/order/void", "工单作废");
        put("GET", "/api/production/reportWork/list", "报工单列表");
        put("POST", "/api/production/reportWork/approve", "审工通过");
        put("GET", "/api/production/schedule/list", "排产计划列表");
        put("GET", "/api/production/mps/list", "主生产计划列表");
        put("GET", "/api/production/issue/list", "领料单列表");
        put("GET", "/api/production/inbound/list", "入库单列表");
        put("GET", "/api/production/completeAnalysis/analysis", "齐套分析");

        put("GET", "/api/inventory/list", "库存列表");
        put("GET", "/api/inventory/warning", "库存预警");
        put("GET", "/api/inventory/batchTrack", "批号跟踪");
        put("GET", "/api/inventory/serialTrack", "序列号跟踪");
        put("GET", "/api/inventory/stocktake/list", "盘点单列表");
        put("GET", "/api/inventory/transfer/list", "调拨单列表");
        put("GET", "/api/inventory/assembly/list", "组装拆卸单列表");
        put("GET", "/api/inventory/otherStock/list", "其他出入库列表");
        put("GET", "/api/inventory/otherStock/items", "其他出入库明细");
        put("GET", "/api/inventory/beginBalance/list", "期初余额列表");

        put("GET", "/api/mrp/plans", "MRP计划");
        put("GET", "/api/mrp/list", "MRP计划列表");

        put("GET", "/api/base/customer/list", "客户列表");
        put("GET", "/api/base/supplier/list", "供应商列表");
        put("GET", "/api/base/material/list", "物料列表");
        put("GET", "/api/base/product/list", "产品列表");
        put("GET", "/api/base/warehouse/list", "仓库列表");
        put("GET", "/api/base/location/list", "库位列表");
        put("GET", "/api/base/area/list", "库区列表");
        put("GET", "/api/base/workshop/list", "车间列表");
        put("GET", "/api/base/process/list", "工序列表");
        put("GET", "/api/base/processRoute/list", "工艺路线列表");
        put("GET", "/api/base/processRoute/items", "工艺路线明细");
        put("GET", "/api/base/machine/list", "机台列表");
        put("GET", "/api/base/unit/list", "单位列表");
        put("GET", "/api/base/unitConversion/list", "单位换算列表");
        put("GET", "/api/base/serialRule/list", "序列号规则列表");
        put("GET", "/api/base/bom/list", "BOM列表");
        put("GET", "/api/base/dict/list", "字典列表");
        put("GET", "/api/base/defect/list", "不良品列表");
        put("GET", "/api/base/substitute/list", "替代料列表");
        put("GET", "/api/base/customerPrice/list", "客户价格列表");
        put("GET", "/api/base/workCalendar/list", "工作日历列表");

        put("GET", "/api/system/user/list", "用户列表");
        put("POST", "/api/system/user/create", "创建用户");
        put("POST", "/api/system/user/assign-roles", "用户分配角色");
        put("GET", "/api/system/role/list", "角色列表");
        put("POST", "/api/system/role/create", "创建角色");
        put("POST", "/api/system/role/assign", "角色授权");
        put("GET", "/api/system/role/permission-list", "权限列表");

        put("GET", "/api/quality/apply/list", "质检申请单列表");
        put("GET", "/api/quality/base/method/list", "检验方法列表");
        put("GET", "/api/quality/base/scheme/list", "检验方案列表");

        put("GET", "/api/finance/receivable/list", "应收台账");
        put("GET", "/api/finance/payable/list", "应付台账");
        put("GET", "/api/finance/receipt/list", "收款单列表");
        put("GET", "/api/finance/payment/list", "付款单列表");

        put("GET", "/api/outsource/order/list", "外协单列表");
        put("GET", "/api/equipment/check/list", "点检记录列表");
        put("GET", "/api/equipment/maintenance/list", "保养记录列表");
        put("GET", "/api/equipment/repair/list", "维修工单列表");

        put("GET", "/api/dashboard/stats", "首页统计");
        put("GET", "/api/dashboard/cockpit", "驾驶舱统计");
        put("GET", "/api/notification/list", "通知列表");
    }

    private static void put(String method, String path, String name) {
        EXACT.put(method.toUpperCase() + " " + path, name);
    }

    public static String resolve(String method, String path) {
        String p = normalize(path);
        String key = (method == null ? "GET" : method.toUpperCase()) + " " + p;
        String exact = EXACT.get(key);
        if (exact != null) return exact;
        return generic(method, p);
    }

    private static String normalize(String path) {
        if (path == null) return "";
        String p = path;
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);
        return p;
    }

    /** 规则推导兜底：模块 + 资源 + 动作 */
    private static String generic(String method, String path) {
        String[] seg = path.split("/"); // ["", "api", module, resource?, action?]
        if (seg.length < 3) return method + " " + path;
        String module = MODULE.getOrDefault(seg[2], seg[2]);
        String resource = seg.length > 3 ? seg[3] : "";
        String action = seg.length > 4 ? seg[4] : "";
        String resName = RESOURCE.getOrDefault(module + "/" + resource, module);
        if (action.isEmpty() || "list".equals(action)) {
            return resName + "列表";
        }
        String actName = ACTION.getOrDefault(action, action);
        // 动作在前/在后：创建/编辑/删除/提交/审核 等前置，其余后置
        if (PREFIX_ACTION.contains(action)) {
            return actName + resName;
        }
        return resName + actName;
    }

    private static final Map<String, String> MODULE = Map.ofEntries(
            Map.entry("sales", "销售"), Map.entry("purchase", "采购"),
            Map.entry("production", "生产"), Map.entry("inventory", "库存"),
            Map.entry("mrp", "MRP"), Map.entry("base", "基础资料"),
            Map.entry("system", "系统"), Map.entry("quality", "质量"),
            Map.entry("finance", "财务"), Map.entry("cost", "成本"),
            Map.entry("equipment", "设备"), Map.entry("outsource", "外协"),
            Map.entry("auth", "认证"), Map.entry("dashboard", "驾驶舱"),
            Map.entry("notification", "通知"));

    private static final Map<String, String> RESOURCE = Map.ofEntries(
            Map.entry("sales/order", "销售订单"), Map.entry("purchase/order", "采购订单"),
            Map.entry("production/order", "生产工单"), Map.entry("sales/quotation", "销售报价单"),
            Map.entry("sales/contract", "销售合同"), Map.entry("sales/invoice", "销售发票"),
            Map.entry("sales/verification", "销售核销"), Map.entry("sales/stocking", "销售备货"),
            Map.entry("sales/afterSale", "售后"), Map.entry("sales/packing", "装箱"),
            Map.entry("sales/return", "销售退货"), Map.entry("sales/outbound", "销售出库"),
            Map.entry("purchase/requisition", "采购申请"), Map.entry("purchase/inquiry", "采购询价"),
            Map.entry("purchase/invoice", "采购发票"), Map.entry("purchase/verification", "采购核销"),
            Map.entry("purchase/return", "采购退货"), Map.entry("purchase/inbound", "采购入库"),
            Map.entry("production/mps", "主生产计划"), Map.entry("production/schedule", "排产计划"),
            Map.entry("production/reportWork", "报工"), Map.entry("production/issue", "领料"),
            Map.entry("production/inbound", "入库"), Map.entry("inventory/inventory", "库存"),
            Map.entry("inventory/stocktake", "盘点"), Map.entry("inventory/transfer", "调拨"),
            Map.entry("inventory/assembly", "组装拆卸"), Map.entry("inventory/otherStock", "其他出入库"),
            Map.entry("inventory/beginBalance", "期初余额"), Map.entry("mrp/mrp", "MRP计划"),
            Map.entry("base/customer", "客户"), Map.entry("base/supplier", "供应商"),
            Map.entry("base/material", "物料"), Map.entry("base/product", "产品"),
            Map.entry("base/warehouse", "仓库"), Map.entry("base/location", "库位"),
            Map.entry("base/area", "库区"), Map.entry("base/workshop", "车间"),
            Map.entry("base/process", "工序"), Map.entry("base/processRoute", "工艺路线"),
            Map.entry("base/machine", "机台"), Map.entry("base/unit", "单位"),
            Map.entry("base/unitConversion", "单位换算"), Map.entry("base/serialRule", "序列号规则"),
            Map.entry("base/bom", "BOM"), Map.entry("base/dict", "字典"),
            Map.entry("base/defect", "不良品"), Map.entry("base/substitute", "替代料"),
            Map.entry("base/customerPrice", "客户价格"), Map.entry("base/workCalendar", "工作日历"),
            Map.entry("system/user", "用户"), Map.entry("system/role", "角色"),
            Map.entry("system/department", "部门"), Map.entry("quality/apply", "质检申请"),
            Map.entry("quality/base", "质检基础"), Map.entry("finance/receivable", "应收"),
            Map.entry("finance/payable", "应付"), Map.entry("finance/receipt", "收款"),
            Map.entry("finance/payment", "付款"), Map.entry("cost/cost", "成本"),
            Map.entry("equipment/check", "点检"), Map.entry("equipment/maintenance", "保养"),
            Map.entry("equipment/repair", "维修"), Map.entry("outsource/order", "外协单"),
            Map.entry("auth/auth", "认证"), Map.entry("dashboard/dashboard", "驾驶舱"),
            Map.entry("notification/notification", "通知"));

    private static final Map<String, String> ACTION = Map.ofEntries(
            Map.entry("list", "列表"), Map.entry("items", "明细"),
            Map.entry("create", "创建"), Map.entry("update", "编辑"),
            Map.entry("delete", "删除"), Map.entry("status", "启停"),
            Map.entry("submit", "提交"), Map.entry("approve", "审核"),
            Map.entry("unapprove", "反审核"), Map.entry("start", "开始"),
            Map.entry("finish", "完工"), Map.entry("void", "作废"),
            Map.entry("generate", "生成"), Map.entry("convert", "转换"),
            Map.entry("writeoff", "核销"), Map.entry("calculate", "核算"),
            Map.entry("save", "保存"), Map.entry("login", "登录"),
            Map.entry("assign", "授权"), Map.entry("assign-roles", "分配角色"));

    private static final java.util.Set<String> PREFIX_ACTION = java.util.Set.of(
            "create", "update", "delete", "submit", "approve", "unapprove",
            "generate", "convert", "writeoff", "calculate", "save");
}
