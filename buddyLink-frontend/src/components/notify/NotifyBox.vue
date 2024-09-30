<template>
  <div v-for = "(notify,index) in notifyBox" :key = "index">
    <component
        :is = "notify.type"
        v-bind = "notify.props"
        ref = "notifyRefs"
    >
    </component>
  </div>
</template>

<script setup lang = "ts">
import {nextTick, onMounted, ref} from "vue";
import LikeNotify from "../notify/LikeNotify.vue";
import StarNotify from "../notify/StarNotify.vue";
import ChatNotify from "../notify/ChatNotify.vue";
import {SseMessageType} from "../../enums/SseMessageType.ts";
import {getCurrentUser} from "../../services/user.ts";

const EXPIRATION_TIME = 3000;  // 组件存活的时间，3秒后销毁
const notifyRefs = ref([]);  // 子组件引用 , 每放入一个 component ， 就对应放入一个 ref
const notifyBox = ref([]); // box ， 用于动态创建不同格式的消息弹窗
const currentUser = ref();

onMounted(async () => {
  currentUser.value = await getCurrentUser();
  // 创建 SSE 连接 , todo : 换常量 url
  const eventSource = new EventSource("http://localhost:8080/api/sse?UserId=" + currentUser.value?.id);

  // 监听服务器推送的消息
  eventSource.onmessage = (event) => {
    const index = notifyBox.value.length;
    // json Str 转为 对象
    const data = JSON.parse(event.data);
    console.log(data.type)
    // 创建 notify ， 并 push in box
    notifyBox.value.push({
      type: data.type === SseMessageType.MESSAGE_LIKE ? LikeNotify : data.type === SseMessageType.MESSAGE_STAR ? StarNotify : ChatNotify, // 组件类型
      props: {message : data.message}, // 弹窗信息
    })
    // 确保弹窗已经渲染，保证 refs 有数据，选中 ref 进行触发
    nextTick(() => {
      if (notifyRefs.value[index]) {
        notifyRefs.value[index].triggerNotify();  // 触发子组件的方法
      }
    })
    // 设置定时器 ， 3 秒后销毁组件
    setTimeout(() => {
      notifyBox.value.splice(index, 1);
    },EXPIRATION_TIME)

    // // 弹窗弹出消息
    // showNotify({
    //   background: '#00ff98',
    //   duration: 1000,
    //   message:event.data
    // });
  };

  // 监听错误
  eventSource.onerror = (error) => {
    console.error("SSE connection error:", error);
  };
})

</script>


<style scoped>

</style>
