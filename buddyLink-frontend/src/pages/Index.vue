<template>
  <BasicLayout>
    <van-search v-model="searchText" placeholder="搜索附近用户" @search="onSearch(searchText)"/>
    <!--  心动模式开关-->
    <van-cell center title="心动模式">
      <template #right-icon>
        <van-switch v-model="isMatchMode" size="24" @change="onMatchModeChange"/>
      </template>
    </van-cell>
    <!--  用户列表-->
    <user-card-list :user-list="userList" :loading="loading"/>
    <!--  回到顶部-->
    <van-back-top right="15vw" bottom="10vh" />
    <!--  滚动组件触发-->
    <div style="display: flex; justify-content: center;">
      <infinite-loading :identifier="infiniteId" @infinite="loadMore" v-if="!isMatchMode"/>
    </div>
    <!--  空状态骨架屏-->
    <van-empty v-if="!userList || userList.length < 1" description="数据为空"/>
  </BasicLayout>
</template>

<script setup lang="ts">
import {ref, watchEffect} from "vue";
import {showToast} from "vant";
import InfiniteLoading from "v3-infinite-loading";
import {UserType} from "../models/user";
import myAxios from "../config/myAxios.ts";
import BasicLayout from "../layouts/BasicLayout.vue";

const searchText = ref('');
const userList = ref<UserType[]>([]);
const isMatchMode = ref<boolean>(false); // ISMATCHMODE 随着组件自动变化
const loading = ref(true);
const infiniteId = 'infinite-loading-id';

let pageNum = 1;
let hasMoreData = ref(true);

/**
 * 加载数据
 */
const loadData = async () => {
  let userListData;
  loading.value = true;

  // 匹配模式
  if (isMatchMode.value) {
    const num = 10;
    // 请求
    userListData = await myAxios.get('/user/match', {
      params: { num },
    }).then(response => {
      console.log('/user/match succeed', response);
      return response?.data;
    }).catch(error => {
      console.log('/user/match error', error);
    });

    if (userListData) {
      userListData.forEach((user: UserType) => {
        // tags 处理 ， string to array
        if (user.tags) {
          user.tags = JSON.parse(user.tags);
        }
      });
    }
    // 复制
    userList.value = userListData || [];
    loading.value = false;
    hasMoreData.value = false;

  }
  // 普通模式
  else {
    // 请求
    userListData = await myAxios.get('/user/recommend', {
      params: { pageSize: 4, pageNum },
    }).then(response => {
      console.log('/user/recommend succeed', response);
      return response?.data;
    }).catch(error => {
      console.log('/user/recommend error', error);
    });
    // tags 处理 ， string to array
    if (userListData) {
      userListData.forEach((user: UserType) => {
        if (user.tags) {
          user.tags = JSON.parse(user.tags);
        }
      });
    // 首次加载 ， 直接赋值
      if (pageNum === 1) {
        userList.value = userListData;
      } else {
    // 二次加载 ， 追加数据
        userList.value = [...userList.value, ...userListData];
      }
    // 判断是否还有更多数据 ， 当获取 userListData 长度为 0 时，表示没有更多数据了
      hasMoreData.value = userListData.length > 0;
    }

    // 用来 异步获取数据状态 . 无数据可采取空状态
    loading.value = false;
  }
};

// 监听心动模式开关变化
const onMatchModeChange = () => {
  // 重置 pageNum ， hasMoreData ， userList , isMatchMode
  userList.value = [];
  pageNum = 1;
  hasMoreData.value = true;
};

// 滚动触发
const loadMore = () => {
  // 上次加载 hasMoreData 为 true ， 则继续加载 ， 为 false 则停止加载
  if (hasMoreData.value) {
    pageNum++;
    loadData();
  }
};

// 搜索附近用户
const onSearch = async (searchText: string) => {
  let userListData;
  loading.value = true;
  // 请求 ， 返回 userList
  const res : any = await myAxios.get('/user/searchNearby', {
    params: { radius: searchText }
  });

  if (res?.code === 0) {
    userListData = res?.data;

    if (userListData) {
      userListData.forEach((user: UserType) => {
        // users.tags 处理 ， string to array
        if (user.tags) {
          user.tags = JSON.parse(user.tags);
        }
      });

      userList.value = userListData;
    }

    loading.value = false;

  } else {
    showToast('搜索失败' + (res.description ? `，${res.description}` : ''));
  }

  loading.value = false;
};

watchEffect(() => {
  // 监听 loadData 里的响应数据 ，更新 userList , --MathMode..
  loadData();
});

</script>

<style scoped>
</style>
