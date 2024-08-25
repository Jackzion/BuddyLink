<template>
  <Viewer :value="content" :plugins="plugins"/>
</template>

<script setup lang="ts">
import gfm from "@bytemd/plugin-gfm";
import highlight from "@bytemd/plugin-highlight";
import { Viewer } from "@bytemd/vue-next";

// 获取 props
interface Props {
  content: string;
}
const props = withDefaults(defineProps<Props>(), {
  content: () => "",
});

// 导入了两个插件，一个用于支持 GFM (GitHub Flavored Markdown)，另一个用于代码高亮显示。
const plugins = [gfm(), highlight()];

</script>

<style>
</style>