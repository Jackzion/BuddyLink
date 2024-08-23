

<script setup lang="ts">
  import {useRoute} from "vue-router";
  import {computed} from "vue";

  const route = useRoute(); // 获取当前路由
  const default_layout = "default";
  const isRouterAlive = true;
  console.log(route.meta.layout);
  // 值与值 binding , route.meta.layout and layout
  const layout = computed(() => route.meta.layout || default_layout) + "-layout";
</script>

<template>
  <div id="app">
<!--    属性 binding layout ， 根据 layout 选出 component-->
    <component :is = "layout">
      <router-view v-if="isRouterAlive && $route.meta.isShow" />
    </component>
  </div>
</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
