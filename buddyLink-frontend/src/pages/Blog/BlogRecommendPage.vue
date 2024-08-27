<template>
  <basic-layout>
    <van-search v-model="searchText" placeholder="搜索我的博客" @search="loadBlogList"/>
    <!--  下拉更新-->
    <van-pull-refresh v-model="refreshed" @refresh="onRefresh">
      <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
      >
        <blog-index-card-list :blog-list="blogList"/>
      </van-list>
    </van-pull-refresh>

    <van-floating-bubble
        axis="xy"
        icon="plus"
        magnetic="y"
        @click="onClick"/>
    <van-empty v-show="!blogList || blogList.length < 1" description="还没有博客捏"/>
  </basic-layout>
</template>

<script setup lang="ts">
import {onMounted, ref, watchEffect} from "vue";
import {useRouter} from "vue-router";
import {showToast} from "vant";
import {BlogType} from "../../models/blog";
import myAxios from "../../config/myAxios.ts";
import BlogIndexCardList from "../../components/BlogIndexCardList.vue";
import BasicLayout from "../../layouts/BasicLayout.vue";

const router = useRouter();

const blogList = ref<BlogType[]>([]);  // 初始化为空数组

const finished = ref(false); // 是否有二次加载的 flag
const loading = ref(false); // list 底部触发 -- true ，
const refreshed = ref(false); // 下拉更新标识符 ， 加载完置为 false
const searchText = ref('');

let pageNum = 1;
let pageSize = 4;

const loadBlogList = async () => {
  // 根据 title 搜索博客
  const res: any = await myAxios.post('/blog/recommend', {
    title: searchText.value,
    pageNum: pageNum,
    pageSize: pageSize,
  });
  // 加载数据
  if (res?.code === 0 && res.data) {
    const dataList = res?.data.map((blog: BlogType) => {
      // 处理 tags to list
      if (blog.tags) {
        blog.tags = JSON.parse(blog.tags);
      }
      return blog;
    });
    if (pageNum === 1) {
      // 第一次加载 ， 直接赋值
      blogList.value = dataList;
    } else {
      // 二次加载 ， append
      blogList.value = [...blogList.value, ...dataList];
    }
    // 判断此次加载的数据是否达到 pageSize ， 没有说明还有数据能加载
    finished.value = dataList.length < pageSize;
  } else {
    showToast('加载失败');
  }
};

onMounted(() => {
  loadBlogList();
});

// 下拉加载数据
const onLoad = async () => {
  // 如果 finish flag 为 false ，说明还有数据，触发加载
  if (!finished.value) {
    // loading flag 置为 true ， 加载图标
    loading.value = true;
    pageNum++;
    await loadBlogList();
    loading.value = false;
  }
};

// 更新列表
const onRefresh = async () => {
  // 重置 数据
  blogList.value = [];
  finished.value = false;
  pageNum = 1;
  // 加载数据
  await loadBlogList();
  // refresh finish flag to false
  refreshed.value = false;
};

// to create page
const onClick = () => {
  router.push({
    path: '/blog/create'
  });
};
</script>

<style scoped>
</style>
