<template>
  <div v-if="user">
    <van-cell title="昵称" is-link to="/user/edit" :value="user?.username"
              @click="toEdit('username', '昵称', user.username)"/>
    <van-cell title="账号" is-link to="/user/edit" :value="user?.userAccount"
              @click="toEdit('userAccount', '账号', user.userAccount)"/>

    <van-cell title="头像" @click="toEditImage()">
      <template #right-icon>
        <van-image
            round
            width="3rem"
            height="3rem"
            :src="user.avatarUrl"
            style="margin-top: 20px; margin-bottom: 20px; box-shadow: 0 0 15px rgba(0, 0, 0, 0.4)"
        />
      </template>
    </van-cell>
    <van-cell title="性别" is-link to="/user/edit" :value="user.gender !== undefined && user.gender !== null ?
    (user.gender === 1 ? '男' : '女') : '未填写'" @click="toEdit('gender', '性别（0 女 1 男）', user.gender)"/>
    <van-cell title="电话" is-link to="/user/edit" :value="user.phone ? user.phone : '未填写'"
              @click="toEdit('phone', '电话', user.phone)"/>
    <van-cell title="邮箱" is-link to="/user/edit" :value="user?.email ? user.email : '未填写'"
              @click="toEdit('email', '邮箱', user.email)"/>
    <van-cell title="标签" is-link to="/user/edit" :value="user?.tags ? '' : '未填写'"
              @click="toEditTags()">
      <template #right-icon v-if="user.tags && user.tags.length > 0">
        <van-tag
            plain
            color="#ffe1e1"
            text-color="#ad0000"
            style="margin: 5px;"
            v-for="tag in JSON.parse(user.tags)">
          {{ tag }}
        </van-tag>
      </template>
    </van-cell>
    <van-cell title="个人介绍" is-link to="/user/edit" :value="user?.profile ? user.profile : '未填写'"
              @click="toEdit('profile', '个人介绍', user.profile)"/>
    <van-cell title="经度" is-link to="/user/edit" :value="user?.longitude ? user.longitude : '未填写'"
              @click="toEdit('longitude', '经度', user.longitude)"/>
    <van-cell title="维度" is-link to="/user/edit" :value="user?.dimension ? user.dimension : '未填写'"
              @click="toEdit('dimension', '维度', user.dimension)"/>
    <van-cell title="注册时间" :value="user.createTime"/>
  </div>


</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import {onMounted, ref} from "vue";
import {showToast, Tag} from "vant";
import myAxios from "../../config/myAxios.ts";
import {TagsType} from "../../enums/TagsType.ts";

const route = useRoute();

const user = ref();
const avatarUrl = ref('');
const avatarUrlList = ref([]);

onMounted(async () => {
  const res: any = await myAxios.get('/user/current');
  if (res?.code === 0) {
    user.value = res.data;
  } else {
    showToast('获取失败');
  }
  avatarUrlList.value.push({url: user.value.avatarUrl});
});

const router = useRouter();

const toEdit = (editKey: string, editName: string, currentValue: string) => {
  router.push({
    path: '/user/edit',
    query: {
      editKey,
      editName,
      currentValue
    }
  });
};

const toEditTags = () => {
  router.push({
    path: '/user/registerTags',
    query: {
      type: TagsType.EDIT,
      userId: user.value.id,
    }
  })
};

const toEditImage = () => {
  router.push({
    path: '/user/registerImage',
    query: {
      type: TagsType.EDIT, // 1 , 修改
      userId: user.value.id,
    }
  })
};

</script>
<style scoped>

</style>