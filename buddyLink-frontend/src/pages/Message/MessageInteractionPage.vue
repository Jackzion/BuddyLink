<template>
  <basic-layout>
    <message-card-list :messageVOList="messageVOList" @delete-message="deleteMessage"/>
    <van-empty v-if="!messageVOList || messageVOList.length < 1" description="暂无互动消息哦"/>
  </basic-layout>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import MessageCardList from "../../components/MessageCardList.vue";
import {showToast} from "vant";
import {useRoute} from "vue-router";
import myAxios from "../../config/myAxios.ts";
import BasicLayout from "../../layouts/BasicLayout.vue";

const messageVOList = ref([]);

const route = useRoute();

const {type} = route.query;

onMounted(async () => {
  loadData();
});

// 加载关注消息
const loadData = async () => {
  const res : any = await myAxios.post('/message/list', {
    type: type,
  });
  if (res?.code === 0) {
    messageVOList.value = res.data;
  } else {
    showToast('查询失败');
  }
};

const deleteMessage = async () => {
  loadData();
};

</script>

<style scoped>

</style>