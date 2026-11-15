<template>
  <div class="scene-node" :class="{ selected: selected }" :style="{ borderColor: color }">
    <Handle type="target" :position="Position.Left" class="handle" />
    <div class="node-head" :style="{ background: color }">{{ typeLabel }}</div>
    <div class="node-body">{{ data.label || typeLabel }}</div>
    <Handle type="source" :position="Position.Right" class="handle" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps<{ data: any; selected?: boolean }>()

const NODE_META: Record<string, { label: string; color: string }> = {
  START: { label: '开始', color: '#16a34a' },
  END: { label: '结束', color: '#ca8a04' },
  API: { label: 'API 请求', color: '#2563eb' },
  UI: { label: 'UI 操作', color: '#7c3aed' },
  ASSERT: { label: '断言', color: '#dc2626' },
  EXTRACT: { label: '变量提取', color: '#0891b2' },
  CONDITION: { label: '条件分支', color: '#ea580c' },
  LOOP: { label: '循环', color: '#db2777' },
  WAIT: { label: '等待', color: '#64748b' },
  SUBSCENE: { label: '子场景', color: '#0d9488' },
  DATASET: { label: '数据集迭代', color: '#4f46e5' },
  SCRIPT: { label: '脚本', color: '#475569' }
}

const color = computed(() => NODE_META[props.data?.nodeType]?.color || '#64748b')
const typeLabel = computed(() => NODE_META[props.data?.nodeType]?.label || props.data?.nodeType)
</script>

<style scoped>
.scene-node {
  width: 160px;
  border: 2px solid #64748b;
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.scene-node.selected {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.35);
}
.node-head {
  color: #fff;
  padding: 4px 8px;
  font-weight: 600;
  border-radius: 6px 6px 0 0;
}
.node-body {
  padding: 8px;
  color: #1f2937;
  word-break: break-all;
}
.handle {
  width: 10px;
  height: 10px;
  background: #94a3b8;
  border: 2px solid #fff;
}
</style>
