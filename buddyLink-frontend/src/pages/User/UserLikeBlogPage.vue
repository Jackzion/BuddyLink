<template>
  <basic-layout>
    <blog-card-list :blogList="blogList" @delete-blog="deleteBlog"/>
    <van-empty v-show="!blogList || blogList.length < 1" description="您还未点赞过任何一篇博客" />
  </basic-layout>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import BlogCardList from "../../components/BlogCardList.vue";
import {showToast} from "vant";
import {useRoute} from "vue-router";
import myAxios from "../../config/myAxios.ts";
import BasicLayout from "../../layouts/BasicLayout.vue";

const blogList = ref([]);
const route = useRoute();

const id = route.params.id;

const loadData = async () => {
  // 查询点赞过的博客
  const res: any = await myAxios.post('/blog/interaction/list', {
    pageNum: 1,
    pageSize: 20,
    type: 1,
  });
  if (res?.code === 0) {
    blogList.value = res.data;
  } else {
    showToast('查询失败');
  }
};

onMounted(async () => {
  loadData();
});

// 触发 emit 事件
const deleteBlog = async () => {
  loadData();
};

</script>

<style scoped>

</style>