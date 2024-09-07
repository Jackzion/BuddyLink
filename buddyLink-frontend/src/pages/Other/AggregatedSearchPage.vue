<template>
  <basic-layout>

    <van-sticky>
<!--      搜索框-->
      <van-search
          v-model="searchText"
          shape="round"
          placeholder="请输入搜索关键词"
      >
        <template #action>
          <div @click="doSearch">搜索</div>
        </template>
      </van-search>
<!--      导航栏-->
      <van-tabs v-model:active="active">
        <van-tab title="users" icon="home-o" >
          <div>
            <team-card-list v-if="active === 'users' && loading === false" :team-list="team-list"></team-card-list>
          </div>
        </van-tab>
        <van-tab title="teams" icon="search" >
          <user-card-list v-if="active === 'teams' && loading === false" :user-list="team-list"></user-card-list>
        </van-tab>
        <van-tab title="blogs" icon="friends-o">
          <blog-index-card-list v-if="active === 'blogs' && loading === false" :blog-list="team-list"></blog-index-card-list>
        </van-tab>
      </van-tabs>

    </van-sticky>

  </basic-layout>
</template>

<script setup lang="ts">
import BasicLayout from "../../layouts/BasicLayout.vue";
import {ref} from "vue";
import TeamCardList from "../../components/TeamCardList.vue";
import UserCardList from "../../components/UserCardList.vue";
import BlogIndexCardList from "../../components/BlogIndexCardList.vue";
import {showToast} from "vant";

const searchText = ref('');
const active = ref('users');
const loading = ref(false);
const list = ref([]);

const doSearch = () =>{
  loading.value = true;
  // todo:发请求，获取list
  console.log(searchText.value);
  showToast(active.value)
  loading.value = false;
}
</script>

<style scoped>

</style>