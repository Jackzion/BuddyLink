<template>
  <basic-layout>
    <div id="teamPage">
      <van-search v-model="searchText" placeholder="搜索队伍" @search="onSearch"/>
      <van-tabs v-model:active="public" @change="onTabChange">
        <van-tab title="公开" name="public"/>
        <van-tab title="加密" name="secret"/>
      </van-tabs>
      <van-button class="addTeamButton" icon="plus" type="primary" @click="doAddTeam"/>
      <team-card-list @update-teams = "listTeam" :team-list="teamList"></team-card-list>
      <van-empty v-show="!teamList || teamList.length < 1" description="数据为空"/>
    </div>
  </basic-layout>
</template>

<script setup>
import {useRouter} from "vue-router";
import TeamCardList from "../../components/TeamCardList.vue";
import {onMounted, ref} from "vue";
import {showToast} from "vant";
import BasicLayout from "../../layouts/BasicLayout.vue";
import myAxios from "../../config/myAxios.ts";

const router = useRouter();

const searchText = ref('');

//跳转到加入队伍页面
const doAddTeam = () => {
  router.push({
    path: "/team/add"
  })
};

const teamList = ref([]);

// 根据搜索条件查询队伍
const listTeam = async (val = '', status = 0) => {
  const res = await myAxios.get('/team/list', {
    params: {
      searchText: val,
      pageNum: 1,
      status,
    }
  });
  if (res?.code === 0) {
    teamList.value = res.data;
  } else {
    showToast("加载队伍失败，请刷新重试！");
  }
}

// 页面加载时查询所有队伍
onMounted(async () => {
  listTeam();
})

// 点击tab栏触发的事件
const onTabChange = (name) => {
  if (name === 'public') {
    // 查公开
    listTeam(searchText.value, 0);
  } else {
    // 查加密
    listTeam(searchText.value, 2);
  }
}

// 根据输入的文本搜索队伍
const onSearch = (val) => {
  listTeam(val);
}

</script>

<style scoped>
.addTeamButton {
  position: fixed;
  bottom: 60px;
  height: 50px;
  left: 12px;
  width: 50px;
  border-radius: 50%;
  z-index: 9999;
}
</style>