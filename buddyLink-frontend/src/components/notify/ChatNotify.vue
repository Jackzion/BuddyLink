<template>
  <div>
    <van-notify v-model:show="showNotify" :style="customStyle"  >
      <div>
        <img class="avatar" src="../../assets/star.png" alt="Avatar" />
        <span class="message">{{ message }}</span>
      </div>
      <van-icon name="cross" class="close-icon" @click="showNotify = false" />
    </van-notify>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
const showNotify = ref(false);
const timer = ref();
const customStyle = {
    background: 'rgba(173, 216, 230, 0.8)', // 半透明浅蓝色背景
    borderRadius: '10px',
    color: '#000',
    padding: '5px',
};
interface NotifyProps {
  message : String,
}
withDefaults(defineProps<NotifyProps>(), {})

const triggerNotify = () => {
  showNotify.value = true;
  timer.value = setInterval(closeNotify, 2000);
};
const closeNotify = () => {
  showNotify.value = false;
  clearInterval(timer.value);
}

// 通过 defineExpose 将 triggerNotify 暴露给父组件
defineExpose({
  triggerNotify,
})

</script>

<style scoped>
</style>