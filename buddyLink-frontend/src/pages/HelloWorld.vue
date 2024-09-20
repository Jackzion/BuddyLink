<template>
  <div>
    <h1>SSE Example</h1>
    <div v-if="message">
      <p>New Message: {{ message }}</p>
    </div>
  </div>
</template>

<script>
import {showNotify} from "vant";

export default {
  data() {
    return {
      message: null
    };
  },
  mounted() {
    // 创建 SSE 连接
    const eventSource = new EventSource("http://localhost:8080/api/sss");

    // 监听服务器推送的消息
    eventSource.onmessage = (event) => {
      this.message = event.data;
      // 弹窗弹出消息
      showNotify({
        background: '#00ff98',
        duration: 1000,
        message:event.data
      });
    };

    // 监听错误
    eventSource.onerror = (error) => {
      console.error("SSE connection error:", error);
    };
  },
};
</script>

<style scoped>
h1 {
  color: #42b983;
}
</style>
