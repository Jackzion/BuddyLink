<template>
<!--  <van-image-->
<!--      round-->
<!--      width="10rem"-->
<!--      height="10rem"-->
<!--      :src="images[index]"-->
<!--  />-->
  <van-row :gutter="[10, 8]" style="padding: 0 16px; margin-top: 30px ">
    <van-col v-for="(image,idx) in images"
      :key = "idx" @click = "selectImage(idx)"
    >
      <van-image
          :class="{'selected-image': idx === selectedIndex}"
          round
          width="10rem"
          height="10rem"
          style=" margin-top: 30px"
          :src="image"
      />
    </van-col>
  </van-row>
  <div style="display: flex; align-items: center">
    <van-button block type="primary" style="margin: 12px" @click="doSearchResult" v-if="!isShow">注册</van-button>
    <van-button block type="primary" style="margin: 12px" @click="editUserImage" v-if="isShow">修改</van-button>
  </div>


</template>


<script setup lang="ts">
import {onMounted, ref} from "vue";
import image1 from '/src/assets/1.jpg';
import image2 from '/src/assets/2.jpg';
import image3 from '/src/assets/3.jpg';
import image4 from '/src/assets/4.jpg';
import {useRoute, useRouter} from "vue-router";
import {showSuccessToast, showToast} from "vant";
import myAxios from "../../config/myAxios.ts";
import {TagsType} from "../../enums/TagsType.ts";

const images = [
  image1,
  image2,
  image3,
  image4,
];

const selectedIndex = ref(-1);

const selectImage = (idx: number) => {
  selectedIndex.value = idx;
  console.log("Selected image index:", selectedIndex.value);
}

const route = useRoute()
const router = useRouter()
const { type } = route.query;
const { userId } = route.query;
// 获得表单
const { registerUser } = route.query;
const  isShow  = ref(false);

onMounted(() => {
  if(parseInt(type) === TagsType.EDIT){
    // 修改
    isShow.value = true;
  }else{
    // 注册
    isShow.value = false;
  }
})
// 注册
const doSearchResult = async () =>{
  if(selectedIndex.value > -1){
    const registerUserParam = JSON.parse(registerUser);
    // 发送注册请求
    const res = await myAxios.post('/user/register', {
          userAccount: registerUserParam.userAccount,
          userPassword: registerUserParam.userPassword,
          checkPassword: registerUserParam.checkPassword,
          username: registerUserParam.username,
          tagNameList: registerUserParam.tagNameList,
          longitude: registerUserParam.longitude,
          dimension: registerUserParam.dimension,
          avatarUrl: images[selectedIndex.value]
        }
    );
    if(res?.code===0){
      showSuccessToast("注册成功");
      router.push('/user/login'); // 跳转到登录页面
    }else{
      // todo : showToast 失效？
      showToast('注册失败' + (`${res.description}` ? `，${res.description}` : ''));
    }
  }else{
    showToast("请至少选择一个标签");
  }
}

// 修改用户头像
const editUserImage = async () =>{
  if(selectedIndex.value > -1){
    // 发送请求
    const res = await myAxios.post('/user/update', {
      avatarUrl: images[selectedIndex.value],
      id : parseInt(userId)
    });
    if(res?.code === 0){
      showSuccessToast("修改成功");
      showToast('修改成功');
      router.back(); // 返回上一页面
    }else{
      showToast('修改失败' + (`${res.description}` ? `，${res.description}` : ''));
    }
  }
  else{
    showToast("请至少选择一个标签");
  }
}

</script>

<style scoped>

.selected-image {
  border: 2px solid #409EFF; /* Blue border to indicate selection */
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3); /* Add shadow for more emphasis */
}

</style>