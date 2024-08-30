<template>
  <basic-layout>
    <div id="teamPage">
      <!--    搜索框-->
      <van-search v-model="searchText" placeholder="搜索队伍" @search="onSearch"/>
      <!--    队伍列表-->
      <my-team-card-list :team-list="teamList"/>
      <!--    空状态骨架屏-->
      <van-empty v-show="!teamList || teamList.length < 1" description="您还未创建队伍" />
    </div>
  </basic-layout>
</template>

<script setup>
import {useRouter} from "vue-router";
import {onMounted, ref} from "vue";
import {showToast} from "vant";
import MyTeamCardList from "../../components/MyTeamCardList.vue";
import myAxios from "../../config/myAxios.ts";
import BasicLayout from "../../layouts/BasicLayout.vue";

const router = useRouter();

const searchText = ref('');

// 查找我创建队伍
const listTeam = async (val = '') => {
  const res = await myAxios.get('/team/list/my/create', {
    params: {
      searchText: val,
      pageNum: 1,
    }
  });
  if (res?.code === 0) {
    teamList.value = res.data;
  } else {
    showToast("加载队伍失败，请刷新重试！" + (`${res.description}` ? `，${res.description}` : ''));
  }

}

const teamList = ref([]);

onMounted( () => {
  listTeam();
})



// 搜索队伍
const onSearch = (val) => {
  listTeam(val);
}

</script>

<style scoped>

</style>