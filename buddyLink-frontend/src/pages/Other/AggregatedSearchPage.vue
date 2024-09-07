<template>
  <basic-layout>
    <van-sticky>
<!--      搜索框-->
      <van-search
          v-model="searchText"
          show-action
          shape="round"
          placeholder="请输入搜索关键词"
          @search="doSearch"
      >
        <template #action>
          <div @click="doSearch">搜索</div>
        </template>
      </van-search>
<!--      导航栏-->
      <van-tabs v-model:active="active" :before-change="beforeChange">
        <van-tab title="users"  name = "users" icon="home-o" >
            <user-card-list :loading = "loading" :user-list="list"></user-card-list>
        </van-tab>
        <van-tab title="teams" name = "teams" icon="search" >
          <team-card-list v-if="active === 'teams' && loading === false" :loading = "loading" :team-list="list"></team-card-list>
        </van-tab>
        <van-tab title="blogs" name = "blogs" icon="friends-o">
          <blog-index-card-list v-if="active === 'blogs' && loading === false" :loading = "loading" :blog-list="list"></blog-index-card-list>
        </van-tab>
      </van-tabs>

    </van-sticky>

  </basic-layout>
</template>

<script setup lang="ts">
import BasicLayout from "../../layouts/BasicLayout.vue";
import {ref,watch} from "vue";
import TeamCardList from "../../components/TeamCardList.vue";
import UserCardList from "../../components/UserCardList.vue";
import BlogIndexCardList from "../../components/BlogIndexCardList.vue";
import {showToast} from "vant";
import myAxios from "../../config/myAxios.ts";

const searchText = ref(''); // 默认不赋值 ， 便于 watch 初始化
const active = ref<string>('')
const loading = ref(false);
const list = ref([]);

// // 标签栏变化前的钩子函数 --- 加载 list
// const beforeChange = async () => {
//   await doSearch();
//   showToast("标签改变")
//   return true;
// };



// 监听 active 值的变化
watch(
    active,
    async (newValue, oldValue) => {
      if (newValue !== oldValue) {
        await doSearch(); // Call your async function here
        showToast("标签改变");
      }
    },
    { immediate: true } // Optional: Call immediately when the watcher is initialized
);

const doSearch = async () => {
  loading.value = true;
  const endpointMap = {
    teams: '/team/search/es',
    blogs: '/blog/search/es',
    users: '/user/search/es', // 默认请求 users
  };

  const endpoint = endpointMap[active.value] || endpointMap.users; // 根据 active.value 获取相应的 endpoint
  let res: any = {}; // 修改为 let

  try {
    res = await myAxios.post(endpoint, {
      searchText: searchText.value,
      pageNum: 1,
      pageSize: 10,
    });

    if (res?.code === 0) {
      // tags 处理
      const dataList = res.data.map(item=>{
        if(item.tags){
          item.tags=JSON.parse(item.tags);
        }
        return item;
      });
      list.value = dataList; // 确保 res.data 存在且为数组或对象
    } else {
      showToast(`加载${active.value === 'teams' ? '队伍' : active.value === 'blogs' ? '博客' : '用户'}失败，请刷新重试！`);
    }
  } catch (error) {
    console.error("请求失败：", error);
    showToast("网络请求失败，请稍后重试！");
  } finally {
    loading.value = false;
  }

  // 输出调试信息
  console.log("Search Text:", searchText.value);
  console.log("Result List:", list.value);
  showToast(`搜索类别: ${active.value}`);
};

</script>

<style scoped>

</style>