<template>
<!--  <div>-->
<!--    <h1>SSE Example</h1>-->
<!--    <div v-if="message">-->
<!--      <p>New Message: {{ message }}</p>-->
<!--    </div>-->
<!--  </div>-->
<!--  消息弹窗设计测试 -->
<!--  <like-notify ref = "likeNotify"></like-notify>-->
<!--  <star-notify ref = "starNotify"></star-notify>-->
<!--  <chat-notify ref = "chatNotify"></chat-notify>-->
<!--  <van-button type="primary" @click="test">显示点赞消息</van-button>-->

<!--  <div v-for = "(notify,index) in notifyBox" :key = "index">-->
<!--    <component-->
<!--        :is = "notify.type"-->
<!--        v-bind = "notify.props"-->
<!--        ref = "notifyRefs"-->
<!--    >-->
<!--    </component>-->
<!--  </div>-->
<!--  <h1>-->
<!--    {{notifyBox.length}}-->
<!--  </h1>-->
  <notify-box></notify-box>

</template>

<script setup lang = "ts">


import NotifyBox from "../components/notify/NotifyBox.vue";
</script>


<style scoped>

</style>
