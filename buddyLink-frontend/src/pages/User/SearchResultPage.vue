<template>
  <UserCardList :user-list="userList"/>
  <van-empty v-show="!userList || userList.length < 1" description="暂无符合要求的用户" />
</template>

<script setup>
import {useRoute} from "vue-router";
import {onMounted, ref} from "vue";
import qs from 'qs';
import UserCardList from "../../components/UserCardList.vue";
import myAxios from "../../config/myAxios.ts";
const route = useRoute();
const { tags } = route.query;

const userList = ref([]);

onMounted(async() => {
  // 根据标签查询用户
  const userListData = await myAxios.get('/user/search/tags', {
    params: {
      tagNameList: tags
    },
    paramsSerializer: params => {
      return qs.stringify(params, { indices: false })
    }
  }).then(function (response) {
    console.log('/user/search/tags succeed', response);
    return response?.data;
  }).catch(function(error) {
    console.log('/user/search/tags error', error)
  })
  if(userListData) {
    userListData.forEach(user => {
      if(user.tags){
        // handle tags string to array
        user.tags = JSON.parse(user.tags);
      }
    })
    // 如果请求成功，就把响应结果返回给userList
    userList.value = userListData;
  }
});

</script>

<style scoped>

</style>