<template>
  <div>
    <van-notify v-model:show="showNotify" :style="customStyle"  >
      <div>
        <img class="avatar" src="../assets/star.png" alt="Avatar" />
        <span>用户名 ：</span>
        <span>聊天信息</span>
      </div>
      <van-icon name="cross" class="close-icon" @click="showNotify = false" />
    </van-notify>

    <van-button type="primary" @click="triggerNotify">显示聊天消息</van-button>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, toRefs} from 'vue'
const showNotify = ref(false);
const timer = ref();
const customStyle = {
    background: 'rgba(173, 216, 230, 0.8)', // 半透明浅蓝色背景
    borderRadius: '10px',
    color: '#000',
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