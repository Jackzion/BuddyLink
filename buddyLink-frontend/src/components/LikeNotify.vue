<template>
  <div>
    <van-notify v-model:show="showNotify" :style="customStyle"  >
      <div>
        <img class="avatar" src="../assets/like.png" alt="Avatar" />
        <span>用户名</span>
        <span>点赞了你的动态</span>
      </div>
      <van-icon name="cross" class="close-icon" @click="showNotify = false" />
    </van-notify>

    <van-button type="primary" @click="triggerNotify">显示点赞消息</van-button>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, toRefs} from 'vue'
const showNotify = ref(false);
const timer = ref();
const customStyle = {
    background: 'rgba(0, 0, 0, 0.6)',
    borderRadius: '10px',
    color: '#fff',
    padding: '5px',
};

const triggerNotify = () => {
  showNotify.value = true;
  timer.value = setInterval(closeNotify, 2000);
};
const closeNotify = () => {
  showNotify.value = false;
  clearInterval(timer.value);
}


</script>

<style scoped>
</style>