<template>
  <basic-layout>
    <blog-card-list :blogList="blogList" @delete-blog="deleteBlog"/>
    <van-empty v-show="!blogList || blogList.length < 1" description="您还未浏览过任何一篇博客" />
  </basic-layout>
</template>

<script setup lang="ts">
import {onMounted, ref, watchEffect} from "vue";
import BlogCardList from "../../components/BlogCardList.vue";import {showToast} from "vant";
import {useRoute} from "vue-router";
import BasicLayout from "../../layouts/BasicLayout.vue";
import myAxios from "../../config/myAxios.ts";

const blogList = ref([]);

const route = useRoute();

const id = route.params.id;

const loadData = async () => {
  // 查找自己浏览过的 blogs
  const res: any = await myAxios.post('/blog/interaction/list', {
    pageNum: 1,
    pageSize: 20,
    type: 2,
  });
  if (res?.code === 0) {
    blogList.value = res.data;
  } else {
    showToast('查询失败');
  }
};

// todo : not need watch
watchEffect(async () => {
  loadData();
});

// emit delete to update blogs
const deleteBlog = async () => {
  loadData();
};
</script>

<style scoped>

</style>