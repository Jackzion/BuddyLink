<template>
<!--  <div>-->
<!--    <h1>SSE Example</h1>-->
<!--    <div v-if="message">-->
<!--      <p>New Message: {{ message }}</p>-->
<!--    </div>-->
<!--  </div>-->
<!--  消息弹窗设计测试 -->
  <like-notify></like-notify>
  <star-notify></star-notify>
  <chat-notify></chat-notify>
</template>

<script>
import {showNotify} from "vant";
import {ref} from "vue";
import LikeNotify from "../components/LikeNotify.vue";
import StarNotify from "../components/StarNotify.vue";
import ChatNotify from "../components/ChatNotify.vue";

export default {
  components: {ChatNotify, StarNotify, LikeNotify}
  // data() {
  //   return {
  //     message: null
  //   };
  // },
  // mounted() {
  //   // 创建 SSE 连接
  //   const eventSource = new EventSource("http://localhost:8080/api/sss");
  //
  //   // 监听服务器推送的消息
  //   eventSource.onmessage = (event) => {
  //     this.message = event.data;
  //     // 弹窗弹出消息
  //     showNotify({
  //       background: '#00ff98',
  //       duration: 1000,
  //       message:event.data
  //     });
  //   };
  //
  //   // 监听错误
  //   eventSource.onerror = (error) => {
  //     console.error("SSE connection error:", error);
  //   };
  // },
};
</script>


<style scoped>

</style>
