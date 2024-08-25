<template>
  <van-search v-model="searchText" placeholder="搜索博客" @search="onSearch"/>
  <blog-card-list :blogList="blogList" @delete-blog="deleteBlog"/>
  <van-empty v-show="!blogList || blogList.length < 1" description="还没有博客捏"/>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import {showSuccessToast, showToast} from "vant";
import {useRoute} from "vue-router";
import myAxios from "../../config/myAxios.ts";
import BlogCardList from "../../components/BlogCardList.vue";

const blogList = ref([]);

const route = useRoute();

const id = route.params.id;

const searchText = ref('');

onMounted(async () => {
  loadData();
});

// 加载博客列表
const loadData = async () => {
  const res: any = await myAxios.post(`/blog/user/${id}`, {
    pageNum: 1,
    pageSize: 8,
  });
  if (res?.code === 0) {
    blogList.value = res.data;
    showToast('查询成功');
  } else {
    showToast('查询失败');
  }
};

// 搜索 blog title
const onSearch = async () => {
  const res: any = await myAxios.post(`/blog/user/${id}`, {
    title: searchText.value,
    pageNum: 1,
    pageSize: 8,
  });
  if (res?.code === 0) {
    blogList.value = res.data;
    showSuccessToast('查询成功');
  } else {
    showToast('查询失败');
  }
};

// 将 deleteBlog binding delete-blog 事件 ， 重新加载数据
const deleteBlog = async () => {
  loadData();
};

</script>

<style scoped>


</style>