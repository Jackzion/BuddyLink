<template>
  <div>
    <h1>SSE Example</h1>
    <div v-if="message">
      <p>New Message: {{ message }}</p>
    </div>
  </div>
<!--  消息弹窗设计测试 -->
  <like-notify ref = "likeNotify"></like-notify>
  <star-notify ref = "starNotify"></star-notify>
  <chat-notify ref = "chatNotify"></chat-notify>
  <van-button type="primary" @click="test">显示点赞消息</van-button>
</template>

<script setup lang = "ts">
import {onMounted, ref} from "vue";
import LikeNotify from "../components/LikeNotify.vue";
import StarNotify from "../components/StarNotify.vue";
import ChatNotify from "../components/ChatNotify.vue";
import {showNotify} from "vant";

const likeNotify = ref(null);
const starNotify = ref(null);
const chatNotify = ref(null);

const test = () =>{
  likeNotify.value.triggerNotify();
}

const message = ref();
onMounted(() => {
  // 创建 SSE 连接
  const eventSource = new EventSource("http://localhost:8080/api/sss");

  // 监听服务器推送的消息
  eventSource.onmessage = (event) => {
    message.value = event.data;
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
})

</script>


<style scoped>

</style>
