<template>
  <div class="scene-node" :class="{ selected }">
    <Handle v-if="kind === 'normal'" type="target" :position="Position.Left" class="handle" />
    <div v-if="kind === 'start'" class="ellipse start">开始</div>
    <div v-else-if="kind === 'end'" class="ellipse end">结束</div>
    <div v-else class="rect">
      <div class="node-name">{{ data.label || typeTag }}</div>
      <div class="node-meta">{{ typeTag }}</div>
    </div>
    <Handle v-if="kind === 'normal'" type="source" :position="Position.Right" class="handle" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps<{ data: any; selected?: boolean }>()

const TYPE_TAGS: Record<string, string> = {
  START: '开始', END: '结束', API: 'API', UI: 'UI', ASSERT: '断言',
  EXTRACT: '变量提取', CONDITION: '条件分支', LOOP: '循环', WAIT: '等待',
  SUBSCENE: '子场景', DATASET: '数据集迭代', SCRIPT: '脚本'
}

const kind = computed(() => {
  const t = props.data?.nodeType
  return t === 'START' ? 'start' : t === 'END' ? 'end' : 'normal'
})
const typeTag = computed(() => TYPE_TAGS[props.data?.nodeType] || props.data?.nodeType || '')
</script>

<style scoped>
.scene-node {
  font-size: 13px;
  position: relative;
}
.scene-node.selected .rect {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.4);
}
.ellipse {
  min-width: 72px;
  padding: 10px 18px;
  border-radius: 999px;
  text-align: center;
  font-weight: 600;
  color: #fff;
}
.ellipse.start {
  background: #16a34a;
}
.ellipse.end {
  background: #ea580c;
}
.rect {
  min-width: 140px;
  max-width: 220px;
  background: #fff;
  border: 1px solid #93c5fd;
  border-radius: 4px;
  padding: 8px 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.node-name {
  color: #1f2937;
  word-break: break-all;
  line-height: 1.4;
}
.node-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #9ca3af;
}
.handle {
  width: 9px;
  height: 9px;
  background: #94a3b8;
  border: 2px solid #fff;
}
</style>
